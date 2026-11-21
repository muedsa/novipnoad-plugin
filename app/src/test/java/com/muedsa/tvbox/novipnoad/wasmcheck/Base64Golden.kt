package com.muedsa.tvbox.novipnoad.wasmcheck

import kotlin.io.encoding.Base64

/**
 * 测试期望值统一以 base64 存放，避免在源码里留下明文 URL / IP / 时间戳。
 *
 * 生成方式：`printf '%s' '<明文>' | base64`
 */
internal fun decodeGolden(encoded: String): String = Base64.decode(encoded).decodeToString()
