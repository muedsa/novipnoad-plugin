package com.muedsa.tvbox.novipnoad.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.service.IMediaSearchService
import com.muedsa.tvbox.novipnoad.NoVipNoadConst
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import okhttp3.OkHttpClient

class MediaSearchService(
    private val okHttpClient: OkHttpClient
) : IMediaSearchService {

    override suspend fun searchMedias(query: String): MediaCardRow {
        val body = "${NoVipNoadConst.URL}/?s=$query".toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .parseHtml()
            .body()
        return MediaCardRow(
            title = "search list",
            cardWidth = NoVipNoadConst.CARD_WIDTH,
            cardHeight = NoVipNoadConst.CARD_HEIGHT,
            list = body.select("#body #content .search-listing-content .video-item").map {
                val aEl = it.selectFirst(".item-thumbnail a")!!
                val imgEl = it.selectFirst(".item-thumbnail img")!!
                val detailUrl = aEl.attr("href").removePrefix(NoVipNoadConst.URL)
                MediaCard(
                    id = detailUrl,
                    title = it.selectFirst(".item-head h3")!!.text().trim(),
                    detailUrl = detailUrl,
                    coverImageUrl = imgEl.attr("data-original"),
                    subTitle = it.selectFirst(".blog-excerpt")?.text()?.trim()
                )
            }
        )
    }
}