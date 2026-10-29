package com.muedsa.tvbox.novipnoad.wasmcheck.hash

/**
 * JS 端 `__()` 使用的 DJB2 变体哈希。
 *
 * JS 代码：
 * ```
 * h = 5381
 * h = ((h << 5) + h) + charCode
 * h = h & 0xFFFFFFFF
 * ```
 */
object Djb2 {

    fun hash(s: String): Int {
        var h = 5381L
        for (c in s) {
            h = ((h shl 5) + h + c.code) and 0xFFFFFFFFL
        }
        return h.toInt()
    }

    /**
     * 把浏览器 4 项完整性检测结果拼成字符串再哈希。
     *
     * 顺序与 JS 端完全一致：
     * `gridWidth|canvasMagic|domMagic|cssWidth`
     */
    fun envHash(
        gridWidth: Int,
        canvasOk: Boolean,
        domOk: Boolean,
        cssWidth: Int,
    ): Int = hash(
        "$gridWidth|${if (canvasOk) 83927 else 0}|${if (domOk) 61934 else 0}|$cssWidth",
    )
}