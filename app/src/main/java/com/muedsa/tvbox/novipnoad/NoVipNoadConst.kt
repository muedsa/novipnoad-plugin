package com.muedsa.tvbox.novipnoad

import com.muedsa.tvbox.tool.ChromeUserAgent
import org.jsoup.Connection

object NoVipNoadConst {
    const val URL = "https://www.novipnoad.net"

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

fun Connection.feignChrome(referrer: String? = null): Connection {
    return userAgent(ChromeUserAgent)
        .also {
            if (!referrer.isNullOrEmpty()) {
                it.referrer(referrer)
            }
        }
        .header("Cache-Control", "no-cache")
        .header("Pragma", "no-cache")
        .header("Priority", "u=0, i")
        .header("Sec-Ch-Ua", "\"Chromium\";v=\"130\", \"Google Chrome\";v=\"130\", \"Not?A_Brand\";v=\"99\"")
        .header("Sec-Ch-Ua-Platform", "\"Windows\"")
        .header("Sec-Fetch-Dest", "document")
        .header("Sec-Fetch-Mode", "navigate")
        .header("Sec-Fetch-Site", "none")
        .header("Upgrade-Insecure-Requests", "1")
}