package com.muedsa.tvbox.demoplugin.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.service.IMediaSearchService

class MediaSearchService(
    private val danDanPlayApiService: DanDanPlayApiService
) : IMediaSearchService {
    override suspend fun searchMedias(query: String): MediaCardRow {
        val resp = danDanPlayApiService.searchAnime(keyword = query)
        if (resp.errorCode != 0) {
            throw RuntimeException(resp.errorMessage)
        }
        return MediaCardRow(
            title = "search list",
            cardWidth = 210 / 2,
            cardHeight = 302 / 2,
            list = resp.animes?.map {
                MediaCard(
                    id = it.animeId.toString(),
                    title = it.animeTitle,
                    detailUrl = it.animeId.toString(),
                    coverImageUrl = it.imageUrl,
                    subTitle = it.startOnlyDate
                )
            } ?: emptyList()
        )
    }
}