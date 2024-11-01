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

class NoVipNoadPlugin(tvBoxContext: TvBoxContext) : IPlugin(tvBoxContext = tvBoxContext) {

    override var options: PluginOptions = PluginOptions(enableDanDanPlaySearch = false)

    override suspend fun onInit() {}

    override suspend fun onLaunched() {}

    private val mainScreenService by lazy { MainScreenService() }
    private val mediaDetailService by lazy { MediaDetailService() }
    private val mediaSearchService by lazy { MediaSearchService() }

    override fun provideMainScreenService(): IMainScreenService = mainScreenService

    override fun provideMediaDetailService(): IMediaDetailService = mediaDetailService

    override fun provideMediaSearchService(): IMediaSearchService = mediaSearchService
}