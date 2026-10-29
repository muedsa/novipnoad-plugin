package com.muedsa.tvbox.novipnoad.wasmcheck.js

/**
 * 从完整性检查 JS 中自动推导正常浏览器的指纹期望值。
 *
 * 规则：
 *  - `gridWidth  = width - leftCol - rightCol`
 *    （取自含 grid-template-columns 的 style.cssText）
 *  - `cssWidth   = 高优先级 (#id .class) 规则的 width`
 *    （取自含 !important 的 textContent）
 *  - `canvasOk   = true`（正常浏览器返回 83927）
 *  - `domOk      = true`（正常浏览器返回 61934）
 *
 * 三个已知版本的提取结果：
 *  - #1：gridWidth=145, cssWidth=154
 *  - #3：gridWidth=132, cssWidth=179
 */
object JsFingerprintExtractor {

    private val GRID_CSS_TEXT = Regex(
        """\.style\.cssText\s*=\s*'([^']*grid-template-columns[^']*)'""",
    )
    private val CSS_TEXT_TEXT = Regex(
        """\.textContent\s*=\s*'([^']*!important[^']*)'""",
    )
    private val COLUMNS = Regex("""grid-template-columns:\s*(\d+)px\s+1fr\s+(\d+)px""")
    private val GRID_WIDTH = Regex("""(?<!min-)width:\s*(\d+)px""")
    private val HIGH_PRIORITY_RULE = Regex(
        """#[\w-]+\s+\.[\w-]+\s*\{\s*width\s*:\s*(\d+)px\s*!important""",
    )
    private val FIRST_WIDTH = Regex("""width\s*:\s*(\d+)px""")

    fun extract(js: String): FingerprintParams = FingerprintParams(
        gridWidth = extractGridWidth(js),
        canvasOk = true,
        domOk = true,
        cssWidth = extractCssWidth(js),
    )

    private fun extractGridWidth(js: String): Int {
        val cssText = GRID_CSS_TEXT.find(js)?.groupValues?.get(1)
            ?: error("未找到含 grid-template-columns 的 style.cssText")

        val cols = COLUMNS.find(cssText)
            ?: error("未解析到 grid-template-columns: Npx 1fr Mpx")
        val left = cols.groupValues[1].toInt()
        val right = cols.groupValues[2].toInt()

        val widthMatch = GRID_WIDTH.find(cssText)
            ?: error("未解析到 grid 测试的 width:Npx")
        val total = widthMatch.groupValues[1].toInt()

        return total - left - right
    }

    private fun extractCssWidth(js: String): Int {
        val cssText = CSS_TEXT_TEXT.find(js)?.groupValues?.get(1)
            ?: error("未找到含 !important 的 textContent")

        HIGH_PRIORITY_RULE.find(cssText)?.let { return it.groupValues[1].toInt() }
        FIRST_WIDTH.find(cssText)?.let { return it.groupValues[1].toInt() }
        error("未能从 CSS 规则中提取宽度")
    }
}