package com.muedsa.tvbox.demoplugin

import kotlinx.coroutines.test.runTest
import org.junit.Test

class PluginTest {

    @Test
    fun create_test() {
        TestPlugin
    }

    @Test
    fun onInit_test() = runTest {
        TestPlugin.onInit()
    }

    @Test
    fun onLaunched_test() = runTest {
        TestPlugin.onLaunched()
    }

    @Test
    fun provideMainScreenService_test() {
        TestPlugin.provideMainScreenService()
    }

    @Test
    fun provideMediaDetailService_test() {
        TestPlugin.provideMediaDetailService()
    }

    @Test
    fun provideMediaSearchService_test() {
        TestPlugin.provideMediaSearchService()
    }
}