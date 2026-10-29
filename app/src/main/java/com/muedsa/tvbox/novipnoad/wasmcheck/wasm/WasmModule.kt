package com.muedsa.tvbox.novipnoad.wasmcheck.wasm

/**
 * 最小可用的 WebAssembly 模块解析器。
 *
 * 仅解析本任务所需的段：
 *  - Import (2)
 *  - Export (7)
 *  - Code   (10)
 *  - Data   (11)
 *
 * 其他段按长度跳过。
 */
internal class WasmModule(input: ByteArray) {

    val memory = ByteArray(65536)                 // 1 页
    val bodies = mutableListOf<ByteArray>()       // 每个定义函数的 body（不含 local 声明）
    val extraLocalsCount = mutableListOf<Int>()   // 每个函数额外声明的 local 数量

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
                2 -> parseImport(r)
                7 -> parseExport(r)
                10 -> parseCode(r)
                11 -> parseData(r)
            }
            r.pos = start + size
        }
    }

    fun codeIndexOf(funcIdx: Int): Int = funcIdx - numImportedFuncs

    private fun parseImport(r: Reader) {
        repeat(r.u32()) {
            val ml = r.u32(); r.bytes(ml)              // module
            val nl = r.u32(); r.bytes(nl)              // name
            when (r.u8()) {
                0 -> { r.u32(); numImportedFuncs++ }
                1 -> { r.u8(); r.u8() }
                2 -> { r.u8(); r.u32() }
                3 -> { r.u8(); r.u8() }
            }
        }
    }

    private fun parseExport(r: Reader) {
        repeat(r.u32()) {
            val nl = r.u32()
            val name = String(r.bytes(nl))
            val kind = r.u8()
            val idx = r.u32()
            if (kind == 0 && name == "run") runFuncIdx = idx
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
}