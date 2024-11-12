package com.muedsa.tvbox.novipnoad

import com.muedsa.tvbox.api.plugin.IPlugin
import com.muedsa.tvbox.api.plugin.PluginOptions
import com.muedsa.tvbox.api.plugin.TvBoxContext
import com.muedsa.tvbox.api.service.IMainScreenService
import com.muedsa.tvbox.api.service.IMediaDetailService
import com.muedsa.tvbox.api.service.IMediaSearchService
import com.muedsa.tvbox.novipnoad.service.MainScreenService
import com.muedsa.tvbox.novipnoad.service.MediaDetailService
import com.muedsa.tvbox.novipnoad.service.MediaSearchService
import com.muedsa.tvbox.tool.PluginCookieJar
import com.muedsa.tvbox.tool.SharedCookieSaver
import com.muedsa.tvbox.tool.createOkHttpClient

class NoVipNoadPlugin(tvBoxContext: TvBoxContext) : IPlugin(tvBoxContext = tvBoxContext) {

    override var options: PluginOptions = PluginOptions(enableDanDanPlaySearch = false)

    override suspend fun onInit() {}

    override suspend fun onLaunched() {}

    private val okHttpClient by lazy {
        createOkHttpClient(
            debug = tvBoxContext.debug,
            cookieJar = PluginCookieJar(saver = SharedCookieSaver(store = tvBoxContext.store))
        )
    }
    private val mainScreenService by lazy { MainScreenService(okHttpClient = okHttpClient) }
    private val mediaDetailService by lazy { MediaDetailService(okHttpClient = okHttpClient) }
    private val mediaSearchService by lazy { MediaSearchService(okHttpClient = okHttpClient) }

    override fun provideMainScreenService(): IMainScreenService = mainScreenService

    override fun provideMediaDetailService(): IMediaDetailService = mediaDetailService

    override fun provideMediaSearchService(): IMediaSearchService = mediaSearchService
}