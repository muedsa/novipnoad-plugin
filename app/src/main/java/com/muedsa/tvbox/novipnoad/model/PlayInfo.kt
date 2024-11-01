package com.muedsa.tvbox.novipnoad.model

import kotlinx.serialization.Serializable

@Serializable
data class PlayInfo(
    val vid: String = "",
    val pkey: String = "",
)