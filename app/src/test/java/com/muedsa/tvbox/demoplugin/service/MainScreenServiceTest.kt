package com.muedsa.tvbox.demoplugin.service

import com.muedsa.tvbox.demoplugin.TestPlugin
import com.muedsa.tvbox.demoplugin.checkMediaCardRows
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