package com.muedsa.tvbox.novipnoad.wasmcheck.wasm

/** 单条解码后的 Wasm 指令。`imm` 对无立即数指令恒为 0。 */
internal data class Instr(val op: Int, val imm: Int)

/** block/loop/if 的控制帧。三个索引均指向指令数组下标。 */
internal data class Frame(
    val kind: Int,
    val startIdx: Int,
    val elseIdx: Int,
    val endIdx: Int,
) {
    companion object {
        const val BLOCK = 0
        const val LOOP = 1
        const val IF = 2
    }
}