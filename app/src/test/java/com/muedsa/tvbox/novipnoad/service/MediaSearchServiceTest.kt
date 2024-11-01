package com.muedsa.tvbox.novipnoad.service

import com.muedsa.tvbox.novipnoad.TestPlugin
import com.muedsa.tvbox.novipnoad.checkMediaCardRow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MediaSearchServiceTest {

    private val service = TestPlugin.provideMediaSearchService()

    @Test
    fun searchMedias_test() = runTest {
        val row = service.searchMedias("死侍")
        checkMediaCardRow(row = row)
    }
}