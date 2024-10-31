package com.muedsa.tvbox.demoplugin

import com.muedsa.tvbox.api.plugin.IPlugin
import com.muedsa.tvbox.api.plugin.PluginOptions
import com.muedsa.tvbox.api.plugin.TvBoxContext
import com.muedsa.tvbox.api.service.IMainScreenService
import com.muedsa.tvbox.api.service.IMediaDetailService
import com.muedsa.tvbox.api.service.IMediaSearchService
import com.muedsa.tvbox.api.store.IPluginPerfStore
import com.muedsa.tvbox.demoplugin.service.DanDanPlayApiService
import com.muedsa.tvbox.demoplugin.service.MainScreenService
import com.muedsa.tvbox.demoplugin.service.MediaDetailService
import com.muedsa.tvbox.demoplugin.service.MediaSearchService
import com.muedsa.tvbox.tool.PluginStoreCookieJar
import com.muedsa.tvbox.tool.createJsonRetrofit
import timber.log.Timber

class DemoPlugin(tvBoxContext: TvBoxContext) : IPlugin(tvBoxContext = tvBoxContext) {

    private val store: IPluginPerfStore = tvBoxContext.store

    override var options: PluginOptions = PluginOptions(enableDanDanPlaySearch = true)

    override suspend fun onInit() {}

    override suspend fun onLaunched() {
        val count = store.getOrDefault(key = LAUNCH_COUNT_PREF_KEY, default = 0) + 1
        Timber.i("DemoPlugin launched, count:$count")
        store.update(key = LAUNCH_COUNT_PREF_KEY, value = count)
    }

    private val danDanPlayApiService by lazy {
        createJsonRetrofit(
            baseUrl = "https://api.dandanplay.net/api/",
            service = DanDanPlayApiService::class.java,
            debug = tvBoxContext.debug,
            cookieJar = PluginStoreCookieJar(tvBoxContext.store)
        )
    }
    private val mainScreenService by lazy { MainScreenService(danDanPlayApiService) }
    private val mediaDetailService by lazy { MediaDetailService(danDanPlayApiService) }
    private val mediaSearchService by lazy { MediaSearchService(danDanPlayApiService) }

    override fun provideMainScreenService(): IMainScreenService = mainScreenService

    override fun provideMediaDetailService(): IMediaDetailService = mediaDetailService

    override fun provideMediaSearchService(): IMediaSearchService = mediaSearchService
}