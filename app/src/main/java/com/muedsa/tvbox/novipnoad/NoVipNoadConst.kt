package com.muedsa.tvbox.novipnoad

import com.muedsa.tvbox.tool.ChromeUserAgent
import com.muedsa.tvbox.tool.decodeBase64ToStr

object NoVipNoadConst {
    val URL = "aHR0cHM6Ly93d3cubm92aXBub2FkLnVr".decodeBase64ToStr()
    val PLAY_HOST = "cGxheWVyLm5vdmlwbm9hZC51aw==".decodeBase64ToStr()
    val PLAY_URL = "http://$PLAY_HOST"

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