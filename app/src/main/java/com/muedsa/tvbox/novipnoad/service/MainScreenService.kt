package com.muedsa.tvbox.novipnoad.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.service.IMainScreenService
import com.muedsa.tvbox.novipnoad.NoVipNoadConst
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.toRequestBuild
import okhttp3.OkHttpClient

class MainScreenService(
    private val okHttpClient: OkHttpClient
) : IMainScreenService {

    override suspend fun getRowsData(): List<MediaCardRow> {
        val body = "${NoVipNoadConst.URL}/".toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
        return body.select("#body #content .smart-box[id]").map { boxEl ->
            MediaCardRow(
                title = boxEl.selectFirst(".smart-box-head h2")!!.text().trim(),
                cardWidth = NoVipNoadConst.CARD_WIDTH,
                cardHeight =  NoVipNoadConst.CARD_HEIGHT,
                list = boxEl.select(".smart-box-content .video-item").map { itemEl ->
                    val aEl = itemEl.selectFirst(".item-thumbnail a")!!
                    val imgEl = itemEl.selectFirst(".item-thumbnail img")!!
                    val detailUrl = aEl.attr("href").removePrefix(NoVipNoadConst.URL)
                    MediaCard(
                        id = detailUrl,
                        title = itemEl.selectFirst(".item-head h3")!!.text().trim(),
                        detailUrl = detailUrl,
                        coverImageUrl = imgEl.attr("data-original"),
                        coverImageHttpHeaders = NoVipNoadConst.IMAGE_HTTP_HEADERS,
                    )
                }
            )
        }
    }
}