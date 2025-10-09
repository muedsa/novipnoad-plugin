package com.muedsa.tvbox.novipnoad.service

import com.muedsa.tvbox.api.data.MediaCatalogOption
import com.muedsa.tvbox.novipnoad.TestPlugin
import com.muedsa.tvbox.novipnoad.checkMediaCard
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaCatalogServiceTest {

    private val service = TestPlugin.provideMediaCatalogService()

    @Test
    fun getConfig_test() = runTest{
        val config = service.getConfig()
        check(config.initKey == "1")
        check(config.pageSize > 0)
        check(config.cardWidth > 0)
        check(config.cardHeight > 0)
        check(config.catalogOptions.isNotEmpty())
    }

    @Test
    fun catalog_test() = runTest {
        val config = service.getConfig()
        val options = MediaCatalogOption.getDefault(config.catalogOptions)
        val result = service.catalog(options = options, loadKey = "1", loadSize = 16)
        check(result.prevKey == null)
        check(result.nextKey == "2")
        check(result.list.isNotEmpty())
        result.list.forEach { card -> checkMediaCard(card = card, cardType = config.cardType) }
    }
}