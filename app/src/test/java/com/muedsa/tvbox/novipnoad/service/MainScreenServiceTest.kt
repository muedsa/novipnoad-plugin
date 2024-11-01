package com.muedsa.tvbox.novipnoad.service

import com.muedsa.tvbox.novipnoad.TestPlugin
import com.muedsa.tvbox.novipnoad.checkMediaCardRows
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenServiceTest {

    private val service = TestPlugin.provideMainScreenService()

    @Test
    fun getRowsDataTest() = runTest{
        val rows = service.getRowsData()
        checkMediaCardRows(rows = rows)
    }

}