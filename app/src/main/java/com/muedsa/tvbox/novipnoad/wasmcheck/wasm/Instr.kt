package com.muedsa.tvbox.novipnoad.wasmcheck.wasm

/**
 * 单条解码后的 Wasm 指令。
 *
 * @param op 操作码。
 * @param imm 立即数；无立即数的指令恒为 0，访存指令存 offset。
 * @param arity 仅 block / loop / if 使用：该块分支目标需要携带的结果个数。
 */
internal data class Instr(
    val op: Int,
    val imm: Int,
    val arity: Int = 0,
)

/**
 * 运行时控制帧。
 *
 * 三个索引均指向指令数组下标；[height] 是进入该块时的操作数栈高度，
 * 用于 `br` / `end` 时把栈恢复成「块入口高度 + 结果个数」。
 */
internal class Frame(
    val kind: Int,
    val startIdx: Int,
    val elseIdx: Int,
    val endIdx: Int,
    val arity: Int,
    val height: Int,
) {
    companion object {
        const val BLOCK = 0
        const val LOOP = 1
        const val IF = 2
        const val FUNCTION = 3
    }
}

/** 触发 Wasm 规范中的 trap：必须立即终止执行（除零、越界、unreachable 等）。 */
internal class WasmTrapException(message: String) : RuntimeException("wasm trap: $message")
