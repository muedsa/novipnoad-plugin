package com.muedsa.tvbox.novipnoad.service

import com.muedsa.tvbox.api.data.MediaCardType
import com.muedsa.tvbox.novipnoad.TestPlugin
import com.muedsa.tvbox.novipnoad.checkMediaCard
import com.muedsa.tvbox.novipnoad.checkMediaCardRow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaDetailServiceTest {

    private val service = TestPlugin.provideMediaDetailService()

    @Test
    fun getDetailData_test() = runTest {
        val detail = service.getDetailData("/tv/thailand/145005.html", "/tv/thailand/145005.html")
        check(detail.id.isNotEmpty())
        check(detail.title.isNotEmpty())
        check(detail.detailUrl.isNotEmpty())
        check(detail.backgroundImageUrl.isNotEmpty())
        detail.favoritedMediaCard?.let { favoritedMediaCard ->
            checkMediaCard(favoritedMediaCard, cardType = MediaCardType.STANDARD)
            check(favoritedMediaCard.cardWidth > 0)
            check(favoritedMediaCard.cardHeight > 0)
        }
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
        detail.rows.forEach {
            checkMediaCardRow(it)
        }
    }

    @Test
    fun getEpisodePlayInfo_test() = runTest{
        val detail = service.getDetailData("/movie/147601.html", "/movie/147601.html")
        check(detail.playSourceList.isNotEmpty())
        check(detail.playSourceList.flatMap { it.episodeList }.isNotEmpty())
        val mediaPlaySource = detail.playSourceList[0]
        val mediaEpisode = mediaPlaySource.episodeList[0]
        val playInfo = service.getEpisodePlayInfo(mediaPlaySource, mediaEpisode)
        check(playInfo.url.isNotEmpty())
    }

    @Test
    fun decode_test() {
        val h = "yUGnymOnyGGnsymnyGOnyUGnGymnyOUnsyGnyOUnyOUnymOnyGOnyGGnUmsnyOsnyGOnyGynssUnsyUnsyGnGymnyOUnsyGnyOsnOsUnyOsnsyGnyGmnGUUnGUOnyUmnymsnsyGnyUUnGUOnGsGnOssnUmsnUmmnOyynGymnyOUnyOsnyGynymOnyGGnsyUnymOnsyOnyUUnGUUnyUynssynymsnsyGnyUUnOGUnGUOnGyOnGyUnOmmnGyOnsymnOmGnGyOnGysnssUnOGOnOGGnsyGnOGOnOGOnGyynGyUnsyOnssynssUnOmmnssUnOGmnssynsymnGyynOmOnsyGnOGOnGysnsyOnsyOnsyGnGyOnGysnGysnGyUnOmGnGyynOGOnGyOnGUOnGsGnyGynsyGnsyOnOGUnGUOnGyGnyOsnyUmnGyGnymsnyGOnyGynsyGnssUnGyGnGyUnOmmnOGGnOmGnGyOnGysnGymnsysnyOsnyGmnymynGUOnGsGnymOnyGUnOGUnGUOnGyUnOGGnGyOnGymnGyUnOGmnGyynGymnGyUnOGmnOGmnGymnGyUnGyynGysnGUOnGsGnyOsnymOnyGmnsyGnOGUnGUOnGyUnOGmnGyynOGOnGyynOmGnGyUnGyynOGmnGyynGUOnyssnGUsnGUsnOGsn"
        val encode =MediaDetailService.decodeVKeyJS(h, "mGOUsynwS", 5, fromBase = 6, toBase = 13)
        println(encode)
    }
}