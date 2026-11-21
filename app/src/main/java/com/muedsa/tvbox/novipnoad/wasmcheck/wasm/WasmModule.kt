package com.muedsa.tvbox.novipnoad.wasmcheck.wasm

/** 函数签名。当前引擎只走通 i32 子集，因此两个数组里只应出现 [WasmModule.I32]。 */
internal class FuncType(
    val params: IntArray,
    val results: IntArray,
) {
    val paramCount: Int get() = params.size
    val resultCount: Int get() = results.size
}

/**
 * 最小可用的 WebAssembly 模块解析器。
 *
 * 解析的段：
 *  - Type     (1)  函数签名，`call` 的实参个数与 `call_indirect` 的类型比较都依赖它
 *  - Import   (2)
 *  - Function (3)  每个定义函数的签名索引
 *  - Export   (7)
 *  - Code     (10)
 *  - Data     (11)
 *
 * 其他段按长度跳过。
 */
internal class WasmModule(input: ByteArray) {

    val memory = ByteArray(65536)                 // 1 页
    val bodies = mutableListOf<ByteArray>()       // 每个定义函数的 body（不含 local 声明）
    val extraLocalsCount = mutableListOf<Int>()   // 每个函数额外声明的 local 数量
    val types = mutableListOf<FuncType>()         // Type 段
    val exports = mutableMapOf<String, Int>()     // 导出名 -> 函数索引

    /** 函数索引（导入函数在前）-> Type 段下标。 */
    private val funcTypeIndices = mutableListOf<Int>()

    var numImportedFuncs = 0
        private set
    var runFuncIdx = -1
        private set

    init {
        val r = Reader(input)
        require(r.u8() == 0x00 && r.u8() == 0x61 && r.u8() == 0x73 && r.u8() == 0x6D) {
            "不是合法的 Wasm 魔数"
        }
        require(r.u8() == 0x01 && r.u8() == 0x00 && r.u8() == 0x00 && r.u8() == 0x00) {
            "不支持的 Wasm 版本"
        }

        while (!r.eof()) {
            val id = r.u8()
            val size = r.u32()
            val start = r.pos
            when (id) {
                1 -> parseTypes(r)
                2 -> parseImport(r)
                3 -> parseFunctions(r)
                7 -> parseExport(r)
                10 -> parseCode(r)
                11 -> parseData(r)
            }
            r.pos = start + size
        }
    }

    fun codeIndexOf(funcIdx: Int): Int = funcIdx - numImportedFuncs

    fun funcType(funcIdx: Int): FuncType? =
        types.getOrNull(funcTypeIndices.getOrNull(funcIdx) ?: -1)

    /** 导出函数索引，找不到返回 null。 */
    fun exportFunc(name: String): Int? = exports[name]

    /**
     * 把 blocktype 解析成「分支目标需要携带的结果个数」。
     *
     * 0x40 表示无结果；valtype 表示 1 个结果；非负值是类型索引（多值提案）。
     * 当前仅支持 i32 结果，且拒绝带参数的块。
     */
    fun blockArity(blockType: Int): Int {
        if (blockType == EMPTY_BLOCK) return 0
        if (blockType < 0) {
            // s33 解出来的负值就是 valtype 字节的补码（0x7f -> -1），还原后再比较
            requireI32(blockType and 0x7F, "块的返回类型")
            return 1
        }
        val t = types.getOrNull(blockType) ?: error("非法 blocktype 类型索引 #$blockType")
        require(t.paramCount == 0) { "暂不支持带参数的块（type #$blockType，${t.paramCount} 个参数）" }
        require(t.resultCount <= 1) {
            "暂不支持多返回值块（type #$blockType，${t.resultCount} 个结果）"
        }
        if (t.resultCount == 1) requireI32(t.results[0], "块的返回类型")
        return t.resultCount
    }

    fun requireI32(valType: Int, what: String) {
        require(valType == I32) { "$what 为 ${valTypeName(valType)}，当前引擎仅支持 i32" }
    }

    private fun parseTypes(r: Reader) {
        repeat(r.u32()) {
            val form = r.u8()
            require(form == FUNC_FORM) { "不支持的类型定义 0x${form.toString(16)}" }
            val params = IntArray(r.u32()) { r.u8() }
            val results = IntArray(r.u32()) { r.u8() }
            types.add(FuncType(params, results))
        }
    }

    private fun parseImport(r: Reader) {
        repeat(r.u32()) {
            val ml = r.u32(); r.bytes(ml)              // module
            val nl = r.u32(); r.bytes(nl)              // name
            when (r.u8()) {
                0 -> {                                 // func
                    funcTypeIndices.add(r.u32())
                    numImportedFuncs++
                }
                1 -> { r.u8(); skipLimits(r) }         // table
                2 -> skipLimits(r)                     // memory
                3 -> { r.u8(); r.u8() }                // global
                else -> error("不支持的导入种类")
            }
        }
    }

    private fun parseFunctions(r: Reader) {
        repeat(r.u32()) { funcTypeIndices.add(r.u32()) }
    }

    private fun parseExport(r: Reader) {
        repeat(r.u32()) {
            val nl = r.u32()
            val name = String(r.bytes(nl))
            val kind = r.u8()
            val idx = r.u32()
            if (kind == 0) {
                exports[name] = idx
                if (name == RUN_EXPORT) runFuncIdx = idx
            }
        }
    }

    private fun parseCode(r: Reader) {
        repeat(r.u32()) {
            val bodySize = r.u32()
            val bodyStart = r.pos

            val localDecl = r.u32()
            var extra = 0
            repeat(localDecl) {
                val n = r.u32()
                r.u8()                                 // valtype
                extra += n
            }

            val codeLen = bodyStart + bodySize - r.pos
            bodies.add(r.bytes(codeLen))
            extraLocalsCount.add(extra)
        }
    }

    private fun parseData(r: Reader) {
        repeat(r.u32()) {
            when (r.u32()) {
                0 -> {
                    r.u8()                             // 0x41
                    val off = r.s32()
                    r.u8()                             // 0x0B
                    val len = r.u32()
                    val data = r.bytes(len)
                    System.arraycopy(data, 0, memory, off, len)
                }
                1 -> {
                    val len = r.u32()
                    r.bytes(len)                       // passive，本任务用不到
                }
                2 -> {
                    r.u32(); r.u8()
                    val off = r.s32(); r.u8()
                    val len = r.u32()
                    val data = r.bytes(len)
                    System.arraycopy(data, 0, memory, off, len)
                }
            }
        }
    }

    /** limits 编码：flag 的最低位表示是否带 max。 */
    private fun skipLimits(r: Reader) {
        val flags = r.u8()
        r.u32()                                        // min
        if (flags and 1 != 0) r.u32()                  // max
    }

    companion object {
        const val I32 = 0x7F
        const val I64 = 0x7E
        const val F32 = 0x7D
        const val F64 = 0x7C

        private const val EMPTY_BLOCK = -64            // 0x40 的 s33 编码
        private const val FUNC_FORM = 0x60
        private const val RUN_EXPORT = "run"

        fun valTypeName(valType: Int): String = when (valType) {
            I32 -> "i32"
            I64 -> "i64"
            F32 -> "f32"
            F64 -> "f64"
            else -> "0x${valType.toString(16)}"
        }
    }
}
