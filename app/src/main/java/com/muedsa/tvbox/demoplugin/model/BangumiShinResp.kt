package com.muedsa.tvbox.demoplugin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BangumiShinResp(
    @SerialName("bangumiList") val bangumiList: List<BangumiShin> = emptyList(),
    @SerialName("success") val success: Boolean = false,
    @SerialName("errorCode") val errorCode: Int = -1,
    @SerialName("errorMessage") val errorMessage: String = "",
)