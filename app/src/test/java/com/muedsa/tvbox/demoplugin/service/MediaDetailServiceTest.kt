package com.muedsa.tvbox.demoplugin.service

import com.muedsa.tvbox.api.data.MediaCardType
import com.muedsa.tvbox.demoplugin.TestPlugin
import com.muedsa.tvbox.demoplugin.checkMediaCard
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MediaDetailServiceTest {

    private val service = TestPlugin.provideMediaDetailService()

    @Test
    fun getDetailData_test() = runTest{
        val detail = service.getDetailData("17998", "17998")
        check(detail.id.isNotEmpty())
        check(detail.title.isNotEmpty())
        check(detail.detailUrl.isNotEmpty())
        check(detail.backgroundImageUrl.isNotEmpty())
        checkMediaCard(detail.favoritedMediaCard, cardType = MediaCardType.STANDARD)
        check(detail.favoritedMediaCard.cardWidth > 0)
        check(detail.favoritedMediaCard.cardHeight > 0)
        check(detail.playSourceList.isNotEmpty())
        detail.playSourceList.forEach { mediaPlaySource ->
            check(mediaPlaySource.id.isNotEmpty())
            check(mediaPlaySource.name.isNotEmpty())
            check(mediaPlaySource.episodeList.isNotEmpty())
            mediaPlaySource.episodeList.forEach {
                check(it.id.isNotEmpty())
                check(it.name.isNotEmpty())
            }
        }
    }

    @Test
    fun getEpisodePlayInfo_test() = runTest{
        val detail = service.getDetailData("17998", "17998")
        check(detail.playSourceList.isNotEmpty())
        check(detail.playSourceList.flatMap { it.episodeList }.isNotEmpty())
        val mediaPlaySource = detail.playSourceList[0]
        val mediaEpisode = mediaPlaySource.episodeList[0]
        val playInfo = service.getEpisodePlayInfo(mediaPlaySource, mediaEpisode)
        check(playInfo.url.isNotEmpty())
    }

}