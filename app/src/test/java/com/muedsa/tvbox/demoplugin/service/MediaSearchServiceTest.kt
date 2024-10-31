package com.muedsa.tvbox.demoplugin.service

import com.muedsa.tvbox.demoplugin.TestPlugin
import com.muedsa.tvbox.demoplugin.checkMediaCardRow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MediaSearchServiceTest {

    private val service = TestPlugin.provideMediaSearchService()

    @Test
    fun searchMedias_test() = runTest {
        val row = service.searchMedias("GIRLS BAND CRY")
        checkMediaCardRow(row = row)
    }
}