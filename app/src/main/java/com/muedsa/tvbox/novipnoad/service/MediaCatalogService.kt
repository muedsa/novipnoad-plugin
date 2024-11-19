package com.muedsa.tvbox.novipnoad.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCatalogConfig
import com.muedsa.tvbox.api.data.MediaCatalogOption
import com.muedsa.tvbox.api.data.MediaCatalogOptionItem
import com.muedsa.tvbox.api.data.PagingResult
import com.muedsa.tvbox.api.service.IMediaCatalogService
import com.muedsa.tvbox.novipnoad.NoVipNoadConst
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import okhttp3.OkHttpClient

class MediaCatalogService(
    private val okHttpClient: OkHttpClient
) : IMediaCatalogService {

    override suspend fun getConfig(): MediaCatalogConfig = CONFIG

    override suspend fun catalog(
        options: List<MediaCatalogOption>,
        loadKey: String,
        loadSize: Int
    ): PagingResult<MediaCard> {
        val page = loadKey.toInt()
        val category = options.find { option -> option.value == "category" }?.items[0]?.value
            ?: throw RuntimeException("必须选择一个类型")
        val orderBy = options.find { option -> option.value == "orderby" }?.items[0]?.value
            ?: throw RuntimeException("必须选择一个排序方式")
        val pageUrl = "${NoVipNoadConst.URL}/$category/page/$page/?orderby=$orderBy"
        val doc = pageUrl.toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
        val body = doc.body()
        val cards = body.select("#body #content .video-listing-content .video-item").map {
            val aEl = it.selectFirst(".item-thumbnail a")!!
            val imgEl = it.selectFirst(".item-thumbnail img")!!
            val detailUrl = aEl.attr("href").removePrefix(NoVipNoadConst.URL)
            MediaCard(
                id = detailUrl,
                title = it.selectFirst(".item-head h3")!!.text().trim(),
                detailUrl = detailUrl,
                coverImageUrl = imgEl.attr("data-original")
            )
        }
        val prevUrl =
            body.selectFirst("#body #content .wp-pagenavi .previouspostslink[href]")?.attr("href")
        val nextUrl =
            body.selectFirst("#body #content .wp-pagenavi .nextpostslink[href]")?.attr("href")
        return PagingResult(
            list = cards,
            prevKey = prevUrl?.let { urlToPage(it) },
            nextKey = nextUrl?.let { urlToPage(it) },
        )
    }

    companion object {
        val CONFIG = MediaCatalogConfig(
            initKey = "1",
            pageSize = 16,
            cardWidth = NoVipNoadConst.CARD_WIDTH,
            cardHeight = NoVipNoadConst.CARD_HEIGHT,
            catalogOptions = listOf(
                MediaCatalogOption(
                    name = "类型",
                    value = "category",
                    items = listOf(
                        MediaCatalogOptionItem(
                            name = "电影",
                            value = "movie",
                            defaultChecked = true
                        ),
                        MediaCatalogOptionItem(
                            name = "剧集",
                            value = "tv",
                        ),
                        MediaCatalogOptionItem(
                            name = "动画",
                            value = "anime",
                        ),
                        MediaCatalogOptionItem(
                            name = "综艺",
                            value = "shows",
                        ),
                        MediaCatalogOptionItem(
                            name = "音乐",
                            value = "music",
                        ),
                        MediaCatalogOptionItem(
                            name = "短片",
                            value = "short",
                        ),
                        MediaCatalogOptionItem(
                            name = "其他",
                            value = "other",
                        ),
                    ),
                    required = true
                ),
                MediaCatalogOption(
                    name = "排序",
                    value = "orderby",
                    items = listOf(
                        MediaCatalogOptionItem(
                            name = "时间",
                            value = "date",
                            defaultChecked = true
                        ),
                        MediaCatalogOptionItem(
                            name = "标题",
                            value = "title",
                        ),
                        MediaCatalogOptionItem(
                            name = "浏览量",
                            value = "view",
                        ),
                        MediaCatalogOptionItem(
                            name = "点赞",
                            value = "like",
                        ),
                        MediaCatalogOptionItem(
                            name = "评论",
                            value = "comment",
                        )
                    ),
                    required = true
                ),
            )
        )

        val URL_PAGE_REGEX = "/page/(\\d+)".toRegex()

        fun urlToPage(url: String): String {
            return URL_PAGE_REGEX.find(url)?.groups[1]?.value ?: "1"
        }
    }
}