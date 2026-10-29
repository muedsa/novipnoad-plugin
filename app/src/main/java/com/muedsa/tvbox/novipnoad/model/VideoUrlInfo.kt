package com.muedsa.tvbox.novipnoad.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoUrlInfo(
    val code: Int,
    val quality: List<VideoUrlQuality> = emptyList(),
    val defaultQuality: Int = 0,
    val pic: String = ""
)