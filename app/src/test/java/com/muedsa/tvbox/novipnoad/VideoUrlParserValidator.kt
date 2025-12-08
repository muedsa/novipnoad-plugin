package com.muedsa.tvbox.novipnoad

import com.muedsa.tvbox.api.data.MediaCatalogConfig
import com.muedsa.tvbox.api.data.MediaCatalogOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.intArrayOf
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VideoUrlParserValidator {

    private val catalogService = TestPlugin.provideMediaCatalogService()
    private val detailService = TestPlugin.provideMediaDetailService()

    private var config: MediaCatalogConfig? = null
    private var configMutex = Mutex()

    private suspend fun getConfig(): MediaCatalogConfig = configMutex.withLock {
        if (config == null) {
            config = catalogService.getConfig()
        }
        return@withLock config!!
    }

    @Test
    fun videoUrlParser_valid() = runTest(timeout = 3.hours) {
        val config = getConfig()
        val options = MediaCatalogOption.getDefault(config.catalogOptions)
        for (loadKey in 1..10) {
            val catalogResult = catalogService.catalog(options = options, loadKey = loadKey.toString(), loadSize = 16)
            for (media in catalogResult.list) {
                val detailData = detailService.getDetailData(mediaId = media.id, detailUrl = media.detailUrl)
                val playSource = detailData.playSourceList[0]
                val episode = playSource.episodeList[0]
                delay(2.seconds)
                try {
                    val episodePlayInfo = detailService.getEpisodePlayInfo(playSource = playSource, episode = episode)
                    println("${media.title} - ${episodePlayInfo.url}")
                } catch (throwable: Throwable) {
                    println("error: ${media.title} - ${media.detailUrl}")
                    throw throwable
                }
            }
            delay(10.seconds)
        }
    }
}