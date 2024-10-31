package com.muedsa.tvbox.demoplugin.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BangumiShin(
    @SerialName("animeId") val animeId: Int,
    @SerialName("animeTitle") val animeTitle: String,
    @SerialName("imageUrl") val imageUrl: String,
    @SerialName("searchKeyword") val searchKeyword: String,
    @SerialName("isOnAir") val isOnAir: Boolean,
    @SerialName("airDay") val airDay: Int,
    @SerialName("isFavorited") val isFavorited: Boolean = false,
    @SerialName("isRestricted") val isRestricted: Boolean = false,
    @SerialName("rating") val rating: Float,
)
