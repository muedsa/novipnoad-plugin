package com.muedsa.tvbox.demoplugin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BangumiTitle(
    @SerialName("language") val language: String,
    @SerialName("title") val title: String
)
