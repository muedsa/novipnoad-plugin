package com.muedsa.tvbox.novipnoad.wasmcheck.js


/**
 * 从 JS 代码中推导出的浏览器指纹期望值。
 *
 * - [gridWidth] 和 [cssWidth] 从测试代码直接计算；
 * - [canvasOk] / [domOk] 表示相应检测项是否命中"正常魔数"，
 *   正常浏览器恒为 `true`；只有模拟异常环境时才需要改。
 * - [extraMagics] 是 cssWidth 之后**追加**的检测段魔数（按出现顺序）。
 *   站点会不定期往指纹串里加检测项（例如 location 检测返回 71923），
 *   这些段参与 DJB2 哈希，少一段就会让 wasm 生成无效的 ckey/vkey。
 */
data class FingerprintParams(
    val gridWidth: Int,
    val canvasOk: Boolean,
    val domOk: Boolean,
    val cssWidth: Int,
    val extraMagics: List<Int> = emptyList(),
)
