package com.muedsa.tvbox.novipnoad.model

import kotlinx.serialization.Serializable

@Serializable
data class VKey(
    val ckey: String = "",
    val ref: String = "",
    val ip: String = "",
    val time: String = ""
)
