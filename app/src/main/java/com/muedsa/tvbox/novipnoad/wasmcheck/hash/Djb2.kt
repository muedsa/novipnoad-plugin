package com.muedsa.tvbox.novipnoad.wasmcheck.hash

import com.muedsa.tvbox.novipnoad.wasmcheck.js.FingerprintParams

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

    /** 正常浏览器 canvas 检测命中的魔数。 */
    const val CANVAS_MAGIC = 83927

    /** 正常浏览器 DOM 检测命中的魔数。 */
    const val DOM_MAGIC = 61934

    fun hash(s: String): Int {
        var h = 5381L
        for (c in s) {
            h = ((h shl 5) + h + c.code) and 0xFFFFFFFFL
        }
        return h.toInt()
    }

    fun envHash(fp: FingerprintParams): Int = envHash(
        gridWidth = fp.gridWidth,
        canvasOk = fp.canvasOk,
        domOk = fp.domOk,
        cssWidth = fp.cssWidth,
        extraMagics = fp.extraMagics,
    )

    /**
     * 把浏览器完整性检测结果拼成字符串再哈希。
     *
     * 顺序与 JS 端完全一致：`gridWidth|canvasMagic|domMagic|cssWidth|[extra...]`。
     * 末尾的 [extraMagics] 对应站点后续追加的检测段（如 location 检测的 71923）。
     */
    fun envHash(
        gridWidth: Int,
        canvasOk: Boolean,
        domOk: Boolean,
        cssWidth: Int,
        extraMagics: List<Int> = emptyList(),
    ): Int {
        val parts = buildList {
            add(gridWidth.toString())
            add((if (canvasOk) CANVAS_MAGIC else 0).toString())
            add((if (domOk) DOM_MAGIC else 0).toString())
            add(cssWidth.toString())
            extraMagics.forEach { add(it.toString()) }
        }
        return hash(parts.joinToString("|"))
    }
}
