package com.muedsa.tvbox.novipnoad.wasmcheck.wasm

/**
 * 覆盖本任务指令集的迷你解释器。
 *
 * 支持的指令：
 *   i32.const / local.get / local.set
 *   i32.add / sub / mul / xor / and / ge_u
 *   i32.load8_u / i32.store8
 *   block / loop / if / else / end / br / br_if
 *   call
 *
 * 遇到新指令会抛异常，便于后续扩展。
 */
internal class MiniInterpreter(
    private val module: WasmModule,
    private val host: (Int, Int, Int, Int) -> Unit,
) {

    fun run(funcIdx: Int, arg0: Int) {
        val bodyIdx = module.codeIndexOf(funcIdx)
        val body = module.bodies[bodyIdx]

        val locals = IntArray(1 + module.extraLocalsCount[bodyIdx])
        locals[0] = arg0

        val instrs = decodeAll(body)
        val matchEnd = IntArray(instrs.size) { -1 }
        val matchElse = IntArray(instrs.size) { -1 }

        // 第一遍：建立 block/loop/if <-> end / else 的配对
        val ctrl = ArrayList<Int>()
        for ((i, ins) in instrs.withIndex()) {
            when (ins.op) {
                0x02, 0x03, 0x04 -> ctrl.add(i)
                0x05 -> if (ctrl.isNotEmpty()) matchElse[ctrl.last()] = i
                0x0B -> if (ctrl.isNotEmpty()) matchEnd[ctrl.removeAt(ctrl.size - 1)] = i
            }
        }

        // 第二遍：解释执行
        val stack = ArrayList<Int>()
        val frames = ArrayList<Frame>()

        fun push(v: Int) = stack.add(v)
        fun pop(): Int = stack.removeAt(stack.size - 1)

        var pc = 0
        while (pc < instrs.size) {
            val ins = instrs[pc]
            when (ins.op) {
                0x41 -> push(ins.imm)                                 // i32.const
                0x20 -> push(locals[ins.imm])                         // local.get
                0x21 -> locals[ins.imm] = pop()                       // local.set

                0x6A -> {
                    val b = pop()
                    val a = pop(); push(a + b)
                } // i32.add
                0x6B -> {
                    val b = pop()
                    val a = pop(); push(a - b)
                } // i32.sub
                0x6C -> {
                    val b = pop()
                    val a = pop(); push(a * b)
                } // i32.mul
                0x73 -> {
                    val b = pop()
                    val a = pop(); push(a xor b)
                } // i32.xor
                0x71 -> {
                    val b = pop()
                    val a = pop(); push(a and b)
                } // i32.and
                0x4F -> {                                             // i32.ge_u
                    val b = pop()
                    val a = pop()
                    push(if (a.toUInt() >= b.toUInt()) 1 else 0)
                }

                0x2D -> {                                             // i32.load8_u
                    val addr = pop() + ins.imm
                    push(module.memory[addr].toInt() and 0xFF)
                }

                0x3A -> {                                             // i32.store8
                    val v = pop()
                    val addr = pop() + ins.imm
                    module.memory[addr] = v.toByte()
                }

                0x02 -> frames.add(Frame(Frame.BLOCK, pc, -1, matchEnd[pc]))
                0x03 -> frames.add(Frame(Frame.LOOP, pc, -1, matchEnd[pc]))
                0x04 -> {                                             // if
                    val cond = pop()
                    if (cond != 0) {
                        frames.add(Frame(Frame.IF, pc, matchElse[pc], matchEnd[pc]))
                    } else if (matchElse[pc] >= 0) {
                        frames.add(Frame(Frame.IF, pc, matchElse[pc], matchEnd[pc]))
                        pc = matchElse[pc]
                    } else {
                        pc = matchEnd[pc]
                    }
                }

                0x05 -> {                                             // else（if 真分支执行完）
                    val f = frames.removeAt(frames.size - 1)
                    pc = f.endIdx
                }

                0x0B -> if (frames.isNotEmpty()) frames.removeAt(frames.size - 1) // end
                0x0C -> {
                    pc = doBranch(frames, ins.imm); continue
                }  // br
                0x0D -> if (pop() != 0) {                             // br_if
                    pc = doBranch(frames, ins.imm); continue
                }

                0x10 -> {                                             // call
                    val vl = pop()
                    val vp = pop()
                    val cl = pop()
                    val cp = pop()
                    host(cp, cl, vp, vl)
                }

                else -> error("不支持的 Wasm 指令 0x${ins.op.toString(16)} (pc=$pc)")
            }
            pc++
        }
    }

    /** 执行 br/br_if：弹出控制帧并返回下一跳 pc。 */
    private fun doBranch(frames: MutableList<Frame>, depth: Int): Int {
        val targetFrameIdx = frames.size - 1 - depth
        val f = frames[targetFrameIdx]
        return if (f.kind == Frame.LOOP) {
            // 跳回 loop 头部，保留 loop 帧
            while (frames.size > targetFrameIdx + 1) frames.removeAt(frames.size - 1)
            f.startIdx + 1
        } else {
            // 跳出 block/if，丢弃该帧
            while (frames.size > targetFrameIdx) frames.removeAt(frames.size - 1)
            f.endIdx + 1
        }
    }

    private fun decodeAll(body: ByteArray): List<Instr> {
        val r = Reader(body)
        val list = ArrayList<Instr>()
        while (!r.eof()) {
            val op = r.u8()
            val imm = when (op) {
                0x41 -> r.s32()                                       // i32.const
                0x20, 0x21, 0x10, 0x0C, 0x0D -> r.u32()               // get/set/call/br/br_if
                0x02, 0x03, 0x04 -> r.u8()                            // blocktype
                0x2D, 0x3A -> {
                    r.u32(); r.u32()
                }                    // align + offset
                0x05, 0x0B,
                0x6A, 0x6B, 0x6C, 0x73, 0x71, 0x4F -> 0

                else -> error("不支持的 Wasm 指令 0x${op.toString(16)}")
            }
            list.add(Instr(op, imm))
        }
        return list
    }
}