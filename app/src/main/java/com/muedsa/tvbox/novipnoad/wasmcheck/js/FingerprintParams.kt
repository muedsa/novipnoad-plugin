package com.muedsa.tvbox.novipnoad.wasmcheck.js


/**
 * 从 JS 代码中推导出的浏览器指纹期望值。
 *
 * - [gridWidth] 和 [cssWidth] 从测试代码直接计算；
 * - [canvasOk] / [domOk] 表示相应检测项是否命中"正常魔数"，
 *   正常浏览器恒为 `true`；只有模拟异常环境时才需要改。
 */
data class FingerprintParams(
    val gridWidth: Int,
    val canvasOk: Boolean,
    val domOk: Boolean,
    val cssWidth: Int,
)