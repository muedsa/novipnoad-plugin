package com.muedsa.tvbox.novipnoad

import com.muedsa.tvbox.api.plugin.TvBoxContext

val TestPlugin by lazy {
    NoVipNoadPlugin(
        tvBoxContext = TvBoxContext(
            screenWidth = 1920,
            screenHeight = 1080,
            debug = true,
            store = FakePluginPrefStore()
        )
    )
}