package com.muedsa.tvbox.novipnoad

import com.muedsa.tvbox.tool.ChromeUserAgent

object NoVipNoadConst {
    const val URL = "https://www.novipnoad.net"

    val IMAGE_HTTP_HEADERS = mapOf(
        "Referer" to listOf("$URL/"),
        "User-Agent" to listOf(ChromeUserAgent),
    )

    const val CARD_WIDTH = 273
    const val CARD_HEIGHT = 150

    val CARD_COLORS = listOf(
        0XFF_15_5A_32,
        0XFF_09_53_45,
        0XFF_15_43_61,
        0XFF_42_49_49,
        0XFF_78_42_13
    )
}