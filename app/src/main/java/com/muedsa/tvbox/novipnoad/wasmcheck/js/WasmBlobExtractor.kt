package com.muedsa.tvbox.novipnoad.wasmcheck.js

import kotlin.io.encoding.Base64

/**
 * 从浏览器完整性检查 JS 源码中提取内嵌的 WebAssembly 二进制。
 *
 * 目标形态：
 * ```js
 * var raw = w.atob("AGFzbQE...");
 * ```
 */
object WasmBlobExtractor {

    private val AT0B_REGEX =
        Regex("""atob\s*\(\s*['"]([^'"]+)['"]""", RegexOption.DOT_MATCHES_ALL)

    /** 提取 `atob('...')` 里的 base64 字符串。 */
    fun extractBase64(js: String): String =
        AT0B_REGEX.find(js)?.groupValues?.get(1)
            ?: throw IllegalArgumentException("未找到 atob('...') 调用")

    /** 提取并解码为 Wasm 字节。 */
    fun extractWasmBytes(js: String): ByteArray =
        Base64.decode(extractBase64(js))
}