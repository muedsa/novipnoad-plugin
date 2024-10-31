package com.muedsa.tvbox.demoplugin.service

import com.muedsa.tvbox.api.data.MediaDetail
import com.muedsa.tvbox.api.data.MediaEpisode
import com.muedsa.tvbox.api.data.MediaHttpSource
import com.muedsa.tvbox.api.data.MediaPlaySource
import com.muedsa.tvbox.api.data.SavedMediaCard
import com.muedsa.tvbox.api.service.IMediaDetailService

class MediaDetailService(
    private val danDanPlayApiService: DanDanPlayApiService
) : IMediaDetailService {

    override suspend fun getDetailData(mediaId: String, detailUrl: String): MediaDetail {
        val resp = danDanPlayApiService.getAnime(mediaId.toInt())
        if (resp.errorCode != 0) {
            throw RuntimeException(resp.errorMessage)
        }
        val bangumi = resp.bangumi ?: throw RuntimeException("bangumi not found")
        return MediaDetail(
            id = bangumi.animeId.toString(),
            title = bangumi.animeTitle,
            subTitle = bangumi.typeDescription,
            description = bangumi.summary,
            detailUrl = bangumi.animeId.toString(),
            backgroundImageUrl = bangumi.imageUrl,
            playSourceList = listOf(
                MediaPlaySource(
                    id = "bangumi",
                    name = "bangumi",
                    episodeList = bangumi.episodes.map {
                        MediaEpisode(
                            id = it.episodeId.toString(),
                            name = it.episodeTitle
                        )
                    }
                )
            ),
            favoritedMediaCard = SavedMediaCard(
                id = bangumi.animeId.toString(),
                title = bangumi.animeTitle,
                detailUrl = bangumi.animeId.toString(),
                coverImageUrl = bangumi.imageUrl,
                cardWidth = 210 / 2,
                cardHeight = 302 / 2,
            )
        )
    }

    override suspend fun getEpisodePlayInfo(
        playSource: MediaPlaySource,
        episode: MediaEpisode
    ): MediaHttpSource = MediaHttpSource(url = "https://media.w3.org/2010/05/sintel/trailer.mp4")
}