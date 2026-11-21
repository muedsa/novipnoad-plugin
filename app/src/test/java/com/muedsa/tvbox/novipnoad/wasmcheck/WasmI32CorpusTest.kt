package com.muedsa.tvbox.novipnoad.wasmcheck

import com.muedsa.tvbox.novipnoad.wasmcheck.wasm.MiniInterpreter
import com.muedsa.tvbox.novipnoad.wasmcheck.wasm.WasmModule
import com.muedsa.tvbox.novipnoad.wasmcheck.wasm.WasmTrapException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 与 V8 的差分测试。
 *
 * 语料由 `tools/wasm-corpus/generate.mjs` 生成：
 *  - `i32-corpus.wasm`：每个测试点一个导出函数
 *  - `i32-corpus.tsv`：期望值，全部来自 V8 实际执行结果（含 trap 用例）
 *
 * 覆盖 i32 数值指令、有符号/无符号比较、移位与循环移位、
 * local.tee / drop / select、block 带结果的 br / br_if、if/else、loop、
 * 本地函数 call、8 位访存与越界 trap、unreachable trap。
 */
class WasmI32CorpusTest {

    @Test
    fun matchesV8() {
        val module = WasmModule(resource("wasm/i32-corpus.wasm"))
        val interpreter = MiniInterpreter(module) { cp, cl, vp, vl ->
            error("语料不应调用导入函数: $cp/$cl/$vp/$vl")
        }

        var checked = 0
        var traps = 0
        resource("wasm/i32-corpus.tsv").decodeToString()
            .lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .forEach { line ->
                val parts = line.split('\t')
                val name = parts[0]
                val rawArgs = parts[1]
                val expected = parts[2]
                val args = if (rawArgs.isEmpty()) {
                    IntArray(0)
                } else {
                    rawArgs.split(',').map(String::toInt).toIntArray()
                }
                val funcIdx = requireNotNull(module.exportFunc(name)) { "语料缺少导出 $name" }
                val actual = try {
                    interpreter.invoke(funcIdx, args).firstOrNull()?.toString() ?: "void"
                } catch (e: WasmTrapException) {
                    traps++
                    "trap"
                }
                assertEquals("$name(${args.joinToString()})", expected, actual)
                checked++
            }

        assertTrue("用例数异常: $checked", checked > 900)
        assertTrue("trap 用例数异常: $traps", traps > 0)
    }

    private fun resource(path: String): ByteArray =
        requireNotNull(javaClass.classLoader?.getResourceAsStream(path)) { "缺少测试资源 $path" }
            .use { it.readBytes() }
}
