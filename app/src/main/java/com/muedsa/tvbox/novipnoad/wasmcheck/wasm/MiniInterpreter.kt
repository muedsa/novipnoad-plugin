package com.muedsa.tvbox.novipnoad.wasmcheck.wasm

/**
 * 覆盖 i32 子集 + 结构化控制流的迷你解释器。
 *
 * 已支持的指令：
 *  - 常量 / 局部变量：`i32.const`、`local.get`、`local.set`、`local.tee`
 *  - 栈操作：`drop`、`select`
 *  - 控制流：`block` / `loop` / `if` / `else` / `end` / `br` / `br_if` / `return` / `call`、
 *    `nop`、`unreachable`；块可带 0 或 1 个 i32 结果，`br` 会按目标块的元数搬运栈值
 *  - i32 全部数值指令：clz / ctz / popcnt / add / sub / mul / div_s / div_u / rem_s / rem_u /
 *    and / or / xor / shl / shr_s / shr_u / rotl / rotr、eqz 与全部有符号/无符号比较
 *  - i32.extend8_s、i32.extend16_s
 *  - i32.load8_u、i32.store8
 *
 * 尚未支持：i64 / f32 / f64、32 位以上访存、`br_table`、`call_indirect`、
 * `memory.size` / `memory.grow`、引用类型指令。遇到时会抛出带指令名的异常。
 *
 * 语义严格按规范实现，包括除零 / 溢出 / 越界 / unreachable 的 trap。
 */
internal class MiniInterpreter(
    private val module: WasmModule,
    private val host: (Int, Int, Int, Int) -> Unit,
) {

    /** 入口：调用导出函数 `run(i32)`，结果由 [host] 回调读取内存得到。 */
    fun run(funcIdx: Int, arg0: Int) {
        invoke(funcIdx, intArrayOf(arg0))
    }

    /** 按函数索引调用（供测试与调试），返回 0 或 1 个 i32 结果。 */
    internal fun invoke(funcIdx: Int, args: IntArray): IntArray {
        val bodyIdx = module.codeIndexOf(funcIdx)
        require(bodyIdx in module.bodies.indices) { "函数索引越界: #$funcIdx" }
        val type = module.funcType(funcIdx)
        if (type != null) {
            require(type.paramCount == args.size) {
                "函数 #$funcIdx 需要 ${type.paramCount} 个参数，实际传入 ${args.size} 个"
            }
            type.params.forEach { module.requireI32(it, "函数 #$funcIdx 的参数类型") }
            require(type.resultCount <= 1) {
                "函数 #$funcIdx 有 ${type.resultCount} 个返回值，当前仅支持 0 或 1 个"
            }
            type.results.forEach { module.requireI32(it, "函数 #$funcIdx 的返回类型") }
        }
        val locals = IntArray(args.size + module.extraLocalsCount[bodyIdx])
        args.copyInto(locals)
        return invokeBody(funcIdx, bodyIdx, locals, 0)
    }

    private fun invokeBody(
        funcIdx: Int,
        bodyIdx: Int,
        locals: IntArray,
        callDepth: Int,
    ): IntArray {
        if (callDepth > MAX_CALL_DEPTH) throw WasmTrapException("call stack exhausted")

        val instrs = decodeAll(module.bodies[bodyIdx])
        val matchEnd = IntArray(instrs.size) { -1 }
        val matchElse = IntArray(instrs.size) { -1 }

        // 第一遍：建立 block/loop/if <-> end / else 的配对
        val open = ArrayList<Int>()
        for ((i, ins) in instrs.withIndex()) {
            when (ins.op) {
                0x02, 0x03, 0x04 -> open.add(i)
                0x05 -> if (open.isNotEmpty()) matchElse[open.last()] = i
                0x0B -> if (open.isNotEmpty()) matchEnd[open.removeAt(open.size - 1)] = i
            }
        }

        val resultArity = module.funcType(funcIdx)?.resultCount ?: 0
        val stack = ArrayList<Int>()
        val frames = ArrayList<Frame>()
        // 函数体自身是一个隐式的块，`br` 到最外层即 return
        frames.add(Frame(Frame.FUNCTION, -1, -1, instrs.size, resultArity, 0))

        fun push(value: Int) = stack.add(value)

        fun pop(): Int = stack.removeAt(stack.size - 1)

        fun truncate(height: Int) {
            while (stack.size > height) stack.removeAt(stack.size - 1)
        }

        fun takeTop(count: Int): IntArray {
            val values = IntArray(count)
            for (i in count - 1 downTo 0) values[i] = pop()
            return values
        }

        /** 把栈规整成「块入口高度 + 结果个数」，只保留顶部的结果值。 */
        fun normalize(height: Int, arity: Int) {
            if (stack.size == height + arity) return
            val values = takeTop(arity)
            truncate(height)
            values.forEach { push(it) }
        }

        fun binary(op: (Int, Int) -> Int) {
            val y = pop()
            val x = pop()
            push(op(x, y))
        }

        fun compare(predicate: (Int, Int) -> Boolean) {
            val y = pop()
            val x = pop()
            push(if (predicate(x, y)) 1 else 0)
        }

        /** 执行 br / br_if：搬运分支值、恢复栈高并返回下一跳 pc。 */
        fun doBranch(depth: Int): Int {
            val targetIdx = frames.size - 1 - depth
            if (targetIdx < 0) throw WasmTrapException("非法的分支深度 $depth")
            val target = frames[targetIdx]
            val values = takeTop(target.arity)
            truncate(target.height)
            values.forEach { push(it) }
            return when (target.kind) {
                // 跳到函数体标签等价于 return：保留函数帧，直接结束
                Frame.FUNCTION -> instrs.size
                // 跳回 loop 头部，保留 loop 帧
                Frame.LOOP -> {
                    while (frames.size > targetIdx + 1) frames.removeAt(frames.size - 1)
                    target.startIdx + 1
                }
                // 跳出 block / if，丢弃目标帧
                else -> {
                    while (frames.size > targetIdx) frames.removeAt(frames.size - 1)
                    target.endIdx + 1
                }
            }
        }

        var pc = 0
        while (pc < instrs.size) {
            val ins = instrs[pc]
            when (ins.op) {
                // ---------- 控制流 ----------
                0x00 -> throw WasmTrapException("unreachable")
                0x01 -> Unit                                          // nop

                0x02 -> frames.add(Frame(Frame.BLOCK, pc, -1, matchEnd[pc], ins.arity, stack.size))
                // loop 的分支目标是它自己的入口，按规范只携带参数（MVP 下为 0 个）
                0x03 -> frames.add(Frame(Frame.LOOP, pc, -1, matchEnd[pc], 0, stack.size))
                0x04 -> {                                             // if
                    val cond = pop()
                    val height = stack.size
                    if (cond != 0) {
                        frames.add(Frame(Frame.IF, pc, matchElse[pc], matchEnd[pc], ins.arity, height))
                    } else if (matchElse[pc] >= 0) {
                        frames.add(Frame(Frame.IF, pc, matchElse[pc], matchEnd[pc], ins.arity, height))
                        pc = matchElse[pc]
                    } else {
                        pc = matchEnd[pc]
                    }
                }
                0x05 -> {                                             // else：then 分支执行完
                    val frame = frames.removeAt(frames.size - 1)
                    normalize(frame.height, frame.arity)
                    pc = frame.endIdx                                // pc++ 时跳过 end
                }
                0x0B -> {                                             // end
                    if (frames.size == 1) break                       // 函数体结束
                    val frame = frames.removeAt(frames.size - 1)
                    normalize(frame.height, frame.arity)
                }
                0x0C -> {
                    pc = doBranch(ins.imm); continue
                }                                                     // br
                0x0D -> if (pop() != 0) {
                    pc = doBranch(ins.imm); continue
                }                                                     // br_if
                0x0F -> {
                    pc = instrs.size; continue
                }                                                     // return

                0x10 -> {                                             // call
                    val callee = ins.imm
                    if (callee < module.numImportedFuncs) {
                        val calleeType = module.funcType(callee)
                            ?: error("缺少导入函数 #$callee 的签名")
                        require(
                            calleeType.paramCount == HOST_PARAM_COUNT && calleeType.resultCount == 0,
                        ) { "不支持的导入函数 #$callee：仅支持 env.auth(i32,i32,i32,i32)" }
                        val vl = pop()
                        val vp = pop()
                        val cl = pop()
                        val cp = pop()
                        host(cp, cl, vp, vl)
                    } else {
                        val calleeBodyIdx = module.codeIndexOf(callee)
                        require(calleeBodyIdx in module.bodies.indices) { "函数索引越界: #$callee" }
                        val calleeType = module.funcType(callee)
                            ?: error("缺少函数 #$callee 的签名")
                        calleeType.params.forEach {
                            module.requireI32(it, "被调用函数 #$callee 的参数类型")
                        }
                        require(calleeType.resultCount <= 1) {
                            "被调用函数 #$callee 有 ${calleeType.resultCount} 个返回值"
                        }
                        calleeType.results.forEach {
                            module.requireI32(it, "被调用函数 #$callee 的返回类型")
                        }
                        val argv = IntArray(calleeType.paramCount)
                        for (i in argv.indices.reversed()) argv[i] = pop()
                        val calleeLocals =
                            IntArray(argv.size + module.extraLocalsCount[calleeBodyIdx])
                        argv.copyInto(calleeLocals)
                        invokeBody(callee, calleeBodyIdx, calleeLocals, callDepth + 1)
                            .forEach { push(it) }
                    }
                }

                // ---------- 栈操作 / 变量 ----------
                0x1A -> pop()                                         // drop
                0x1B -> {                                             // select
                    val cond = pop()
                    val second = pop()
                    val first = pop()
                    push(if (cond != 0) first else second)
                }

                0x20 -> push(locals[ins.imm])                         // local.get
                0x21 -> locals[ins.imm] = pop()                       // local.set
                0x22 -> {                                             // local.tee
                    val value = pop()
                    locals[ins.imm] = value
                    push(value)
                }

                // ---------- 内存（8 位）----------
                0x2D -> push(loadUnsigned8(effectiveAddress(pop(), ins.imm)))
                0x3A -> {                                             // i32.store8
                    val value = pop()
                    store8(effectiveAddress(pop(), ins.imm), value)
                }

                0x41 -> push(ins.imm)                                 // i32.const

                // ---------- i32 比较 ----------
                0x45 -> push(if (pop() == 0) 1 else 0)                // i32.eqz
                0x46 -> compare { x, y -> x == y }                    // i32.eq
                0x47 -> compare { x, y -> x != y }                    // i32.ne
                0x48 -> compare { x, y -> x < y }                     // i32.lt_s
                0x49 -> compare { x, y -> x.toUInt() < y.toUInt() }   // i32.lt_u
                0x4A -> compare { x, y -> x > y }                     // i32.gt_s
                0x4B -> compare { x, y -> x.toUInt() > y.toUInt() }   // i32.gt_u
                0x4C -> compare { x, y -> x <= y }                    // i32.le_s
                0x4D -> compare { x, y -> x.toUInt() <= y.toUInt() }  // i32.le_u
                0x4E -> compare { x, y -> x >= y }                    // i32.ge_s
                0x4F -> compare { x, y -> x.toUInt() >= y.toUInt() }  // i32.ge_u

                // ---------- i32 位运算 ----------
                0x67 -> push(pop().countLeadingZeroBits())            // i32.clz
                0x68 -> push(pop().countTrailingZeroBits())           // i32.ctz
                0x69 -> push(pop().countOneBits())                    // i32.popcnt
                0x71 -> binary { x, y -> x and y }                    // i32.and
                0x72 -> binary { x, y -> x or y }                     // i32.or
                0x73 -> binary { x, y -> x xor y }                    // i32.xor
                0x74 -> binary { x, y -> x shl (y and 31) }           // i32.shl
                0x75 -> binary { x, y -> x shr (y and 31) }           // i32.shr_s
                0x76 -> binary { x, y -> x ushr (y and 31) }          // i32.shr_u
                0x77 -> binary { x, y -> x.rotateLeft(y) }            // i32.rotl
                0x78 -> binary { x, y -> x.rotateRight(y) }           // i32.rotr

                // ---------- i32 算术 ----------
                0x6A -> binary { x, y -> x + y }                      // i32.add
                0x6B -> binary { x, y -> x - y }                      // i32.sub
                0x6C -> binary { x, y -> x * y }                      // i32.mul
                0x6D -> binary { x, y -> divideSigned(x, y) }         // i32.div_s
                0x6E -> binary { x, y -> divideUnsigned(x, y) }       // i32.div_u
                0x6F -> binary { x, y -> remainderSigned(x, y) }      // i32.rem_s
                0x70 -> binary { x, y -> remainderUnsigned(x, y) }    // i32.rem_u

                // ---------- 符号扩展 ----------
                0xC0 -> push(pop().toByte().toInt())                  // i32.extend8_s
                0xC1 -> push(pop().toShort().toInt())                 // i32.extend16_s

                else -> error("不支持的 Wasm 指令 0x${hex(ins.op)}")
            }
            pc++
        }

        normalize(frames[0].height, frames[0].arity)
        return IntArray(resultArity) { stack[it] }
    }

    /**
     * 逐条解码函数体。
     *
     * 所有立即数的长度都必须精确，否则后续字节会被错位当成操作码——
     * 这也是「不支持的指令」有时是假信号的原因，因此未知指令一律立即报错。
     */
    private fun decodeAll(body: ByteArray): List<Instr> {
        val r = Reader(body)
        val instrs = ArrayList<Instr>()
        while (!r.eof()) {
            val opPos = r.pos
            val op = r.u8()
            var arity = 0
            val imm: Int = when (op) {
                0x41 -> r.s32()                                        // i32.const
                0x20, 0x21, 0x22, 0x10, 0x0C, 0x0D -> r.u32()          // get/set/tee/call/br/br_if
                0x02, 0x03, 0x04 -> r.s33().also { arity = module.blockArity(it) }
                0x2D, 0x3A -> {
                    r.u32(); r.u32()                                   // align + offset
                }
                0x00, 0x01, 0x05, 0x0B, 0x0F, 0x1A, 0x1B,
                in 0x45..0x4F, in 0x67..0x78, 0xC0, 0xC1 -> 0
                else -> throw unsupported(op, opPos)
            }
            instrs.add(Instr(op, imm, arity))
        }
        return instrs
    }

    private fun unsupported(op: Int, offset: Int): IllegalStateException {
        val name = describe(op)
        val label = if (name.isEmpty()) "" else " ($name)"
        return IllegalStateException(
            "不支持的 Wasm 指令 0x${hex(op)}$label，函数体偏移 $offset。" +
                    "当前引擎已支持 i32 全部数值指令与控制流；" +
                    "i64 / f32 / f64 / 32 位以上访存 / br_table / call_indirect / " +
                    "memory.size / memory.grow 尚未实现。",
        )
    }

    private fun describe(op: Int): String = when (op) {
        0x0E -> "br_table"
        0x11 -> "call_indirect"
        0x28 -> "i32.load"
        0x2C -> "i32.load8_s"
        0x2E -> "i32.load16_s"
        0x2F -> "i32.load16_u"
        0x36 -> "i32.store"
        0x3B -> "i32.store16"
        0x3C, 0x3D, 0x3E -> "i64.store8/16/32"
        0x3F -> "memory.size"
        0x40 -> "memory.grow"
        0x42 -> "i64.const"
        0x43 -> "f32.const"
        0x44 -> "f64.const"
        in 0x29..0x35 -> "i64 / f32 / f64 访存"
        in 0x37..0x39 -> "i64 / f32 / f64 访存"
        in 0x50..0x66 -> "i64 / f32 / f64 比较"
        in 0x79..0x8A -> "i64 运算"
        in 0x8B..0xA6 -> "f32 / f64 运算"
        in 0xA7..0xBF -> "数值类型转换"
        in 0xC2..0xC4 -> "i64 符号扩展"
        0xD0, 0xD1, 0xD2 -> "引用类型指令"
        0xFC -> "0xFC 前缀扩展指令（饱和转换 / bulk memory）"
        else -> ""
    }

    private fun effectiveAddress(base: Int, offset: Int): Long =
        (base.toLong() and 0xFFFFFFFFL) + (offset.toLong() and 0xFFFFFFFFL)

    private fun loadUnsigned8(address: Long): Int {
        if (address < 0 || address >= module.memory.size) {
            throw WasmTrapException("out of bounds memory access")
        }
        return module.memory[address.toInt()].toInt() and 0xFF
    }

    private fun store8(address: Long, value: Int) {
        if (address < 0 || address >= module.memory.size) {
            throw WasmTrapException("out of bounds memory access")
        }
        module.memory[address.toInt()] = value.toByte()
    }

    private fun divideSigned(x: Int, y: Int): Int {
        if (y == 0) throw WasmTrapException("integer divide by zero")
        if (x == Int.MIN_VALUE && y == -1) throw WasmTrapException("integer overflow")
        return x / y
    }

    private fun divideUnsigned(x: Int, y: Int): Int {
        if (y == 0) throw WasmTrapException("integer divide by zero")
        return (x.toUInt() / y.toUInt()).toInt()
    }

    private fun remainderSigned(x: Int, y: Int): Int {
        if (y == 0) throw WasmTrapException("integer divide by zero")
        // Int.MIN_VALUE % -1 == 0，Java/Kotlin 不会抛异常，与规范一致
        return x % y
    }

    private fun remainderUnsigned(x: Int, y: Int): Int {
        if (y == 0) throw WasmTrapException("integer divide by zero")
        return (x.toUInt() % y.toUInt()).toInt()
    }

    private fun hex(value: Int): String = value.toString(16).padStart(2, '0')

    private companion object {
        const val MAX_CALL_DEPTH = 512
        const val HOST_PARAM_COUNT = 4
    }
}
