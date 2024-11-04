package com.muedsa.tvbox.novipnoad.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.service.IMainScreenService
import com.muedsa.tvbox.novipnoad.NoVipNoadConst
import com.muedsa.tvbox.tool.feignChrome
import org.jsoup.Jsoup
import java.net.CookieStore

class MainScreenService(
    private val cookieStore: CookieStore
) : IMainScreenService {

    override suspend fun getRowsData(): List<MediaCardRow> {
        val body = Jsoup.connect("${NoVipNoadConst.URL}/")
            .feignChrome(cookieStore = cookieStore)
            .get()
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
                        coverImageUrl = imgEl.attr("data-original")
                    )
                }
            )
        }
    }
}