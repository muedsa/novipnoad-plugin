package com.muedsa.tvbox.demoplugin

import com.muedsa.tvbox.api.plugin.TvBoxContext

val TestPlugin by lazy {
    DemoPlugin(
        tvBoxContext = TvBoxContext(
            screenWidth = 1920,
            screenHeight = 1080,
            debug = true,
            store = FakePluginPrefStore()
        )
    )
}