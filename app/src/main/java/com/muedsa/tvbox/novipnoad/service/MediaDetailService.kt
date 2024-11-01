package com.muedsa.tvbox.novipnoad.service

import com.muedsa.tvbox.api.data.MediaCard
import com.muedsa.tvbox.api.data.MediaCardRow
import com.muedsa.tvbox.api.data.MediaCardType
import com.muedsa.tvbox.api.data.MediaDetail
import com.muedsa.tvbox.api.data.MediaEpisode
import com.muedsa.tvbox.api.data.MediaHttpSource
import com.muedsa.tvbox.api.data.MediaPlaySource
import com.muedsa.tvbox.api.data.SavedMediaCard
import com.muedsa.tvbox.api.service.IMediaDetailService
import com.muedsa.tvbox.novipnoad.NoVipNoadConst
import com.muedsa.tvbox.novipnoad.feignChrome
import com.muedsa.tvbox.novipnoad.model.PlayInfo
import com.muedsa.tvbox.novipnoad.model.VKey
import com.muedsa.tvbox.novipnoad.model.VideoUrlInfo
import com.muedsa.tvbox.tool.ChromeUserAgent
import com.muedsa.tvbox.tool.LenientJson
import com.muedsa.tvbox.tool.decodeBase64
import kotlinx.coroutines.delay
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import timber.log.Timber
import java.util.Locale
import kotlin.math.pow

class MediaDetailService : IMediaDetailService {

    override suspend fun getDetailData(mediaId: String, detailUrl: String): MediaDetail {
        val pageUrl = "${NoVipNoadConst.URL}$detailUrl"
        val doc = Jsoup.connect(pageUrl)
            .feignChrome()
            .get()
        val body = doc.body()
        val videoItemEl = body.selectFirst("#content .video-item")!!
        val title = videoItemEl.selectFirst("h1")!!.text().trim()
        val imgUrl = doc.head().selectFirst("meta[property=\"og:image\"]")
            ?.attr("content")
            ?: ""
        var description = videoItemEl.selectFirst(".item-content p")!!.text().trim()
        videoItemEl.selectFirst(".item-content .item-tax-list")!!.children().forEach {
            val text = it.text().trim()
            if (text.isNotBlank()) {
                description += "\n$text"
            }
        }
        return MediaDetail(
            id = mediaId,
            title = title,
            subTitle = null,
            description = description,
            detailUrl = detailUrl,
            backgroundImageUrl = imgUrl,
            playSourceList = getMediaPlaySource(body = body, title = title, pageUrl = pageUrl),
            favoritedMediaCard = SavedMediaCard(
                id = mediaId,
                title = title,
                detailUrl = detailUrl,
                coverImageUrl = imgUrl,
                cardWidth = NoVipNoadConst.CARD_WIDTH,
                cardHeight = NoVipNoadConst.CARD_HEIGHT,
            ),
            rows = getDetailRows(body = body)
        )
    }

    private fun getMediaPlaySource(
        body: Element,
        title: String,
        pageUrl: String
    ): List<MediaPlaySource> {
        val playInfoJson = PLAY_INFO_REGEX.find(body.html())?.groups?.get(1)?.value
        if (playInfoJson.isNullOrEmpty()) {
            return emptyList()
        }
        val playInfo = LenientJson.decodeFromString<PlayInfo>(playInfoJson)
        val episodeList = if (playInfo.vid.isNotBlank()) {
            listOf(
                MediaEpisode(
                    id = playInfo.vid,
                    name = title,
                    flag5 = playInfo.vid,
                    flag6 = playInfo.pkey,
                    flag7 = pageUrl
                )
            )
        } else {
            body.select("#content .tm-multilink .multilink-btn[data-vid]").map { btnEl ->
                val vid = btnEl.attr("data-vid")
                val eTitle = btnEl.text().trim()
                MediaEpisode(
                    id = vid,
                    name = eTitle,
                    flag5 = vid,
                    flag6 = playInfo.pkey,
                    flag7 = pageUrl
                )
            }
        }
        return listOf(
            MediaPlaySource(
                id = "novipnoad",
                name = "novipnoad",
                episodeList = episodeList
            )
        )
    }

    private fun getDetailRows(body: Element): List<MediaCardRow> {
        return buildList {
            val navItemCards =
                body.select("#content .simple-navigation .row .simple-navigation-item")
                    .mapIndexedNotNull { index, it ->
                        if (it.children().isNotEmpty()) {
                            val aEl = it.selectFirst("a")!!
                            val detailUrl = aEl.attr("href").removePrefix(NoVipNoadConst.URL)
                            MediaCard(
                                id = detailUrl,
                                title = it.selectFirst(".simple-navigation-item-content h4")!!
                                    .text()
                                    .trim(),
                                detailUrl = detailUrl,
                                backgroundColor = NoVipNoadConst.CARD_COLORS[index % NoVipNoadConst.CARD_COLORS.size]
                            )
                        } else null
                    }
            if (navItemCards.isEmpty()) {
                add(
                    MediaCardRow(
                        title = "Next&Previous",
                        list = navItemCards,
                        cardWidth = NoVipNoadConst.CARD_WIDTH,
                        cardHeight = NoVipNoadConst.CARD_HEIGHT,
                        cardType = MediaCardType.NOT_IMAGE
                    )
                )
            }
            val relatedItemCards =
                body.select("#content .related-single .smart-box-content .video-item").map {
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
            if (navItemCards.isEmpty()) {
                add(
                    MediaCardRow(
                        title = "关联视频",
                        list = relatedItemCards,
                        cardWidth = NoVipNoadConst.CARD_WIDTH,
                        cardHeight = NoVipNoadConst.CARD_HEIGHT,
                        cardType = MediaCardType.STANDARD
                    )
                )
            }
        }
    }

    override suspend fun getEpisodePlayInfo(
        playSource: MediaPlaySource,
        episode: MediaEpisode
    ): MediaHttpSource {
        if (episode.flag5.isNullOrEmpty()
            || episode.flag6.isNullOrEmpty()
            || episode.flag7.isNullOrEmpty()
        ) {
            throw RuntimeException("解析视频地址失败 vid&pkey&ref")
        }
        val videoUrlInfo = parseVideoUrlInfo(
            vid = episode.flag5!!,
            pKey = episode.flag6!!,
            videoPageUrl = episode.flag7!!
        )
        if (videoUrlInfo.quality.isEmpty()) {
            throw RuntimeException("解析视频地址失败 videoUrlInfo.quality")
        }
        val i =
            if (videoUrlInfo.defaultQuality < videoUrlInfo.quality.size) videoUrlInfo.defaultQuality else 0
        return MediaHttpSource(
            url = videoUrlInfo.quality[i].url,
            httpHeaders = mapOf(
                "Referer" to "${NoVipNoadConst.URL}${episode.flag7!!}",
                "User-Agent" to ChromeUserAgent
            )
        )
    }

    private suspend fun parseVideoUrlInfo(
        vid: String,
        pKey: String,
        videoPageUrl: String
    ): VideoUrlInfo {
        val referrer = "${NoVipNoadConst.URL}$videoPageUrl"
        val body =
            Jsoup.connect("https://player.novipnoad.net/v1/?url=${vid}&pkey=${pKey}&ref=$videoPageUrl")
                .feignChrome(referrer)
                .get()
                .body()
        val device = DEVICE_REGEX.find(body.html())?.groups?.get(1)?.value
            ?: throw RuntimeException("解析播放信息失败 device")
        val matchGroups = DECODED_V_KEY_PARAMS_REGEX.find(body.html())?.groups
            ?: throw RuntimeException("解析播放信息失败 vkey params")
        val h = matchGroups[1]?.value ?: throw RuntimeException("解析播放信息失败 vkey params.h")
        val n = matchGroups[2]?.value ?: throw RuntimeException("解析播放信息失败 vkey params.n")
        val t = matchGroups[3]?.value?.toInt()
            ?: throw RuntimeException("解析播放信息失败 vkey params.t")
        val e = matchGroups[4]?.value?.toInt()
            ?: throw RuntimeException("解析播放信息失败 vkey params.e")
        val vKeyJs = decodeVKey(h, n, t, e)
        val vkMatchGroups = V_KEY_JS_REGEX.find(vKeyJs)?.groups
            ?: throw RuntimeException("解析播放信息失败 vkey js")
        val vKey = VKey(
            ckey = vkMatchGroups[1]?.value ?: throw RuntimeException("解析播放信息失败 vkey ckey"),
            ref = vkMatchGroups[2]?.value ?: throw RuntimeException("解析播放信息失败 vkey ref"),
            ip = vkMatchGroups[3]?.value ?: throw RuntimeException("解析播放信息失败 vkey ip"),
            time = vkMatchGroups[4]?.value ?: throw RuntimeException("解析播放信息失败 vkey time")
        )
        return step2(vid, device, vKey, referrer)
    }

    private suspend fun step2(
        vid: String,
        device: String,
        vKey: VKey,
        referrer: String
    ): VideoUrlInfo {
        delay(200)
        val body =
            Jsoup.connect("https://player.novipnoad.net/v1/player.php?id=${vid}&device=$device")
                .feignChrome(referrer)
                .get()
                .body()
        val jsApi = JSAPI_REGEX.find(body.html())?.groups?.get(1)?.value
            ?: throw RuntimeException("解析播放信息失败 jsapi")
        return step3(jsApi, vKey, referrer)
    }

    private suspend fun step3(jsApi: String, vKey: VKey, referrer: String): VideoUrlInfo {
        delay(200)
        val jsUrl = jsApi.toHttpUrl().newBuilder()
            .addQueryParameter("ckey", vKey.ckey.uppercase(Locale.getDefault()))
            .addQueryParameter("ref", vKey.ref)
            .addQueryParameter("ip", vKey.ip)
            .addQueryParameter("time", vKey.time)
            .build()
            .toString()
        val jsText = Jsoup.connect(jsUrl)
            .feignChrome(referrer)
            .get()
            .text()
        if (!jsText.startsWith("var videoUrl=JSON.decrypt(\"")
            || !jsText.endsWith("\");")
        ) {
            Timber.e("解析播放信息失败 videoUrl: $jsText")
            throw RuntimeException("解析播放信息失败 videoUrl")
        }
        val videoUrlData = jsText.removePrefix("var videoUrl=JSON.decrypt(\"")
            .removeSuffix("\");")
        val videoUrlJson = (decodeVideoUrlJson(videoUrlData, "5f3651b7"))
        return LenientJson.decodeFromString<VideoUrlInfo>(videoUrlJson)
    }

    companion object {
        val PLAY_INFO_REGEX = "<script>window.playInfo=(\\{.*?\\});</script>".toRegex()
        val DEVICE_REGEX = "params\\['device'] = '(\\w+)'".toRegex()
        val DECODED_V_KEY_PARAMS_REGEX =
            "return decodeURIComponent\\(escape\\(r\\)\\)\\}\\(\"(\\w+)\",\\d+,\"(\\w+)\",(\\d+),(\\d+),\\d+\\)\\)".toRegex()
        val JSAPI_REGEX = "const jsapi = '(.*?)';".toRegex()
        val V_KEY_JS_REGEX = "\\{ckey:'(\\w+)',ref:'(.*?)',ip:'(.*?)',time:'(\\d+)'\\}".toRegex()

        private fun decodeVKey(dataStr: String, nStr: String, t: Int, e: Int): String {
            val resultBuilder = StringBuilder()
            val charMap =
                nStr.toCharArray().zip(nStr.indices.map { it.toString().toCharArray()[0] }).toMap()
            var remainingData = dataStr
            while (remainingData.isNotEmpty()) {
                val splitIndex = remainingData.indexOf(nStr[e])
                if (splitIndex == -1) {
                    break
                }
                val s = remainingData.substring(0, splitIndex).toCharArray().toList()
                remainingData = remainingData.substring(splitIndex + 1)
                val updatedS = s.mapNotNull { charMap[it] }
                val h = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ+/"
                    .toCharArray()
                    .take(e)
                val j = updatedS.toCharArray().reversed().foldIndexed(0) { index, acc, cur ->
                    val p = e.toFloat().pow(index.toFloat()).toInt()
                    acc + h.indexOf(cur) * p
                }
                resultBuilder.append((j - t).toChar())
            }
            val result = resultBuilder.toString()
            if (!result.startsWith("window.sessionStorage.setItem('vkey',JSON.stringify({")) {
                throw RuntimeException("解析播放信息失败 vkey")
            }
            if (!result.endsWith("}));")) {
                throw RuntimeException("解析播放信息失败 vkey")
            }
            return result.removePrefix("window.sessionStorage.setItem('vkey',JSON.stringify(")
                .removeSuffix("));")
        }

        private fun decodeVideoUrlJson(data: String, key: String): String {
            val dataCharArray = data.decodeBase64().toString(Charsets.ISO_8859_1).toCharArray()
            val keyCharArray = key.toCharArray()
            val arr = IntArray(256)
            arr.forEachIndexed { i, _ -> arr[i] = i }
            var n = 0
            var temp: Int
            arr.forEachIndexed { i, _ ->
                n = (n + arr[i] + keyCharArray[i % keyCharArray.size].code) % arr.size
                temp = arr[i]
                arr[i] = arr[n]
                arr[n] = temp
            }
            var i = 0
            n = 0
            val resultBuilder = StringBuilder()
            dataCharArray.forEachIndexed { b, _ ->
                i = (i + 1) % arr.size
                n = (n + arr[i]) % arr.size
                temp = arr[i]
                arr[i] = arr[n]
                arr[n] = temp
                resultBuilder.append((dataCharArray[b].code xor arr[(arr[i] + arr[n]) % 256]).toChar())
            }
            return resultBuilder.toString()
        }
    }
}