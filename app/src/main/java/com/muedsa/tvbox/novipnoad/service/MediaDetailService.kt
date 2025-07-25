package com.muedsa.tvbox.novipnoad.service

import com.muedsa.tvbox.api.data.DanmakuData
import com.muedsa.tvbox.api.data.DanmakuDataFlow
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
import com.muedsa.tvbox.novipnoad.model.PlayInfo
import com.muedsa.tvbox.novipnoad.model.VKey
import com.muedsa.tvbox.novipnoad.model.VideoUrlInfo
import com.muedsa.tvbox.novipnoad.util.BaseConverter
import com.muedsa.tvbox.novipnoad.util.RC4
import com.muedsa.tvbox.tool.ChromeUserAgent
import com.muedsa.tvbox.tool.LenientJson
import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.decodeBase64
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.parseHtml
import com.muedsa.tvbox.tool.stringBody
import com.muedsa.tvbox.tool.toRequestBuild
import kotlinx.coroutines.delay
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.Locale

class MediaDetailService(
    private val okHttpClient: OkHttpClient
) : IMediaDetailService {

    private val key = getKeyFormGithubKeyFile()

    override suspend fun getDetailData(mediaId: String, detailUrl: String): MediaDetail {
        val pageUrl = "${NoVipNoadConst.URL}$detailUrl"
        val doc = pageUrl.toRequestBuild()
            .feignChrome()
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
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
            backgroundImageHttpHeaders = NoVipNoadConst.IMAGE_HTTP_HEADERS,
            playSourceList = getMediaPlaySource(body = body, title = title, pageUrl = pageUrl),
            favoritedMediaCard = SavedMediaCard(
                id = mediaId,
                title = title,
                detailUrl = detailUrl,
                coverImageUrl = imgUrl,
                coverImageHttpHeaders = NoVipNoadConst.IMAGE_HTTP_HEADERS,
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
                        coverImageUrl = imgEl.attr("data-original"),
                        coverImageHttpHeaders = NoVipNoadConst.IMAGE_HTTP_HEADERS,
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
                "Origin" to "https://player.novipnoad.net",
                "User-Agent" to ChromeUserAgent
            )
        )
    }

    private suspend fun parseVideoUrlInfo(
        vid: String,
        pKey: String,
        videoPageUrl: String
    ): VideoUrlInfo {
        val pageUrl = "https://player.novipnoad.net/v1/?url=${vid}&pkey=${pKey}&ref=$videoPageUrl"
        val bodyHtml = pageUrl.toRequestBuild()
            .feignChrome(referer = "${NoVipNoadConst.URL}/")
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .parseHtml()
            .body()
            .html()
        val device = DEVICE_REGEX.find(bodyHtml)?.groups?.get(1)?.value
            ?: throw RuntimeException("解析播放信息失败 device")
        var matchGroups = ARG_NAMES_FOR_DECODE_V_KEY_FUNCTION_REGEX.find(bodyHtml)?.groups
            ?: throw RuntimeException("解析播放信息失败 vkey fun arg name")
        var argReplaceStr = ""
        val argNames = matchGroups.map {
            it?.value ?: throw RuntimeException("解析播放信息失败 vkey fun arg name")
        }.toMutableList().also {
            it.removeAt(0)
            argReplaceStr = it.last()
            it.removeAt(it.size - 1)
        }
        val argReplaceMap = mutableMapOf<String, String>()
        ARG_REPLACE_REGEX.findAll(argReplaceStr).forEach {
            argReplaceMap.put(it.groups[1]!!.value, it.groups[2]!!.value)
        }
        matchGroups = ARG_VALUES_FOR_DECODE_V_KEY_FUNCTION_REGEX.find(bodyHtml)?.groups
            ?: throw RuntimeException("解析播放信息失败 vkey fun arg value")
        val argValues = matchGroups.map {
            it?.value ?: throw RuntimeException("解析播放信息失败 vkey fun arg value")
        }.toMutableList().also {
            it.removeAt(0)
        }
        matchGroups = DATA_REPLACE_MAP_FOR_DECODE_V_KEY_REGEX.find(bodyHtml)?.groups
            ?: throw RuntimeException("解析播放信息失败 vkey data_replaceMap arg name")
        val dataArgName = matchGroups[1]?.value
            ?: throw RuntimeException("解析播放信息失败 vkey data arg name")
        val replaceMapArgName = matchGroups[3]?.value
            ?: throw RuntimeException("解析播放信息失败 vkey replaceMap arg name")
        matchGroups = TO_BASE_FOR_DECODE_V_KEY_REGEX.find(bodyHtml)?.groups
            ?: throw RuntimeException("解析播放信息失败 vkey toBase")
        val fromBaseArgName = matchGroups[1]?.value
            ?: throw RuntimeException("解析播放信息失败 vkey fromBaseArgName")
        val toBase = matchGroups[2]?.value?.toInt()
            ?: throw RuntimeException("解析播放信息失败 vkey toBase")
        val charCodeSaltArgName = matchGroups[3]?.value
            ?: throw RuntimeException("解析播放信息失败 vkey charCodeSaltArgName")
        val vKeyJs = decodeVKeyJS(
            data = decodeFunArgStringValue(dataArgName, argNames, argValues, argReplaceMap),
            replaceMap = decodeFunArgStringValue(
                replaceMapArgName,
                argNames,
                argValues,
                argReplaceMap
            ),
            charCodeSalt = decodeFunArgIntValue(
                charCodeSaltArgName,
                argNames,
                argValues,
                argReplaceMap
            ),
            fromBase = decodeFunArgIntValue(fromBaseArgName, argNames, argValues, argReplaceMap),
            toBase = toBase
        )
        return step2(vid = vid, device = device, vKey = parseVKey(vKeyJs), referrer = pageUrl)
    }

//    private suspend fun step2(
//        vid: String,
//        device: String,
//        vKey: VKey,
//        referrer: String
//    ): VideoUrlInfo {
//        delay(200)
//        val body =
//            "https://player.novipnoad.net/v1/player.php?id=${vid}&device=$device".toRequestBuild()
//                .feignChrome(referer = referrer)
//                .get(okHttpClient = okHttpClient)
//                .checkSuccess()
//                .parseHtml()
//                .body()
//        val jsApi = JSAPI_REGEX.find(body.html())?.groups?.get(1)?.value
//            ?: throw RuntimeException("解析播放信息失败 jsapi")
//        val jqText = "https://player.novipnoad.net/js/jquery.min.js"
//            .toRequestBuild()
//            .feignChrome(referer = referrer)
//            .get(okHttpClient = okHttpClient)
//            .checkSuccess()
//            .stringBody()
//        // location[_0x3d3628(0xc9,'R)wj')][_0x3d3628(0xb9,'%(hT')](/player.novipnoad.(com|net|cc|uk|us|org)/)!=null?_0x3d3628(0xb3,'smXA'):_0x3d3628(0xd1,'ku@n')
//        // _0x3d3628(0xb3, 'smXA')
//        val matchGroups = JQ_KEY_REGEX.find(jqText)?.groups
//            ?: throw RuntimeException("解析播放信息失败 JQ fun")
//        val dataKey = matchGroups[3]?.value
//            ?: throw RuntimeException("解析播放信息失败 JQ fun dataKey")
//        val dataFun = JQ_DATA_FUN_REGEX.find(jqText)?.groups?.get(3)?.value
//            ?: throw RuntimeException("解析播放信息失败 JQ dataFun")
//        val dataArr = JQ_DATA_ARR_REGEX.findAll(dataFun).mapNotNull {
//            it.groups[1]?.value
//        }.toList()
//        if (dataArr.isEmpty())  throw RuntimeException("解析播放信息失败 JQ dataArr")
//        return step3(
//            jsApi = jsApi,
//            vKey = vKey,
//            referrer = "https://player.novipnoad.net/",
//            dataArr = dataArr,
//            dataKey = dataKey,
//        )
//    }
//
//    private suspend fun step3(
//        jsApi: String,
//        vKey: VKey,
//        referrer: String,
//        dataArr: List<String>,
//        dataKey: String,
//    ): VideoUrlInfo {
//        delay(200)
//        val jsUrl = jsApi.toHttpUrl().newBuilder()
//            .addQueryParameter("ckey", vKey.ckey.uppercase(Locale.getDefault()))
//            .addQueryParameter("ref", vKey.ref)
//            .addQueryParameter("ip", vKey.ip)
//            .addQueryParameter("time", vKey.time)
//            .build()
//            .toString()
//        val jsText = jsUrl.toRequestBuild()
//            .feignChrome(referer = referrer)
//            .get(okHttpClient = okHttpClient)
//            .checkSuccess()
//            .stringBody()
//        if (!jsText.startsWith("var videoUrl=JSON.decrypt(\"")
//            || !jsText.endsWith("\");")
//        ) {
//            Timber.e("解析播放信息失败 videoUrl: $jsText")
//            throw RuntimeException("解析播放信息失败 videoUrl")
//        }
//        val videoUrlData = jsText.removePrefix("var videoUrl=JSON.decrypt(\"")
//            .removeSuffix("\");")
//        var videoUrlJson = ""
//        for (dataStr in dataArr) {
//            val decryptKey = RC4(dataKey.toByteArray(Charsets.ISO_8859_1))
//                .decrypt(dataStr.decodeFromJsjiami()) // e11ed29b
//            videoUrlJson = RC4(decryptKey)
//                .decrypt(videoUrlData.decodeBase64())
//                .toString(Charsets.ISO_8859_1)
//            if (videoUrlJson.startsWith("{") && videoUrlJson.endsWith("}")) {
//                break
//            }
//        }
//        if (!videoUrlJson.startsWith("{") || !videoUrlJson.endsWith("}")) {
//            throw RuntimeException("解析播放信息失败 decryptKey")
//        }
//        return LenientJson.decodeFromString<VideoUrlInfo>(videoUrlJson)
//    }

    private suspend fun step2(
        vid: String,
        device: String,
        vKey: VKey,
        referrer: String
    ): VideoUrlInfo {
        delay(200)
        val body =
            "https://player.novipnoad.net/v1/player.php?id=${vid}&device=$device".toRequestBuild()
                .feignChrome(referer = referrer)
                .get(okHttpClient = okHttpClient)
                .checkSuccess()
                .parseHtml()
                .body()
        val jsApi = JSAPI_REGEX.find(body.html())?.groups?.get(1)?.value
            ?: throw RuntimeException("解析播放信息失败 jsapi")
        return step3(
            jsApi = jsApi,
            vKey = vKey,
            referrer = "https://player.novipnoad.net/",
        )
    }

    private suspend fun step3(
        jsApi: String,
        vKey: VKey,
        referrer: String
    ): VideoUrlInfo {
        delay(200)
        val jsUrl = jsApi.toHttpUrl().newBuilder()
            .addQueryParameter("ckey", vKey.ckey.uppercase(Locale.getDefault()))
            .addQueryParameter("ref", vKey.ref)
            .addQueryParameter("ip", vKey.ip)
            .addQueryParameter("time", vKey.time)
            .build()
            .toString()
        val jsText = jsUrl.toRequestBuild()
            .feignChrome(referer = referrer)
            .get(okHttpClient = okHttpClient)
            .checkSuccess()
            .stringBody()
        if (!jsText.startsWith("var videoUrl=JSON.decrypt(\"")
            || !jsText.endsWith("\");")
        ) {
            Timber.e("解析播放信息失败 videoUrl: $jsText")
            throw RuntimeException("解析播放信息失败 videoUrl")
        }
        val videoUrlData = jsText.removePrefix("var videoUrl=JSON.decrypt(\"")
            .removeSuffix("\");")
        val videoUrlJson = RC4(key.toByteArray(Charsets.ISO_8859_1))
            .decrypt(videoUrlData.decodeBase64())
            .toString(Charsets.ISO_8859_1)
        if (!videoUrlJson.startsWith("{") || !videoUrlJson.endsWith("}")) {
            throw RuntimeException("解析播放信息失败 decryptKey")
        }
        return LenientJson.decodeFromString<VideoUrlInfo>(videoUrlJson)
    }

    private fun getKeyFormGithubKeyFile(): String {
        var key = requestKeyFromGithubUrl(KEY_GITHUB_URL_1)
        if (key.isBlank()) {
            key = requestKeyFromGithubUrl(KEY_GITHUB_URL_2)
        }
        if (key.isBlank()) {
            key = requestKeyFromGithubUrl(KEY_GITHUB_URL_3)
        }
        if (key.isBlank()) {
            key = "ce974576"
        }
        return key
    }

    private fun requestKeyFromGithubUrl(url: String): String {
        return try {
            url.toRequestBuild()
                .feignChrome()
                .get(okHttpClient = okHttpClient)
                .checkSuccess()
                .stringBody()
        } catch (_: Throwable) { "" }
    }

    companion object {
        val PLAY_INFO_REGEX = "<script>window.playInfo=(\\{.*?\\});</script>".toRegex()
        val DEVICE_REGEX = "params\\['device'] = '(\\w+)'".toRegex()

        // while(n[i]!==h[a]){{s+=n[i];i++}}
        val DATA_REPLACE_MAP_FOR_DECODE_V_KEY_REGEX =
            "while\\(([a-z])\\[([a-z])\\]!==([a-z])\\[[a-z]\\]\\)\\{+[a-z]\\+=([a-z])\\[\\2\\];\\2\\+\\+\\}+".toRegex()
        val TO_BASE_FOR_DECODE_V_KEY_REGEX =
            "\\+=String.fromCharCode\\(\\w+\\(\\w+,(\\w+),(\\d+)\\)-(\\w+)\\)".toRegex()
        val ARG_NAMES_FOR_DECODE_V_KEY_FUNCTION_REGEX =
            "eval\\(function\\(([a-z]),([a-z]),([a-z]),([a-z]),([a-z]),([a-z])\\)(.*?)for\\(".toRegex()
        val ARG_REPLACE_REGEX = "var ([a-z])=([a-z]);".toRegex()
        val ARG_VALUES_FOR_DECODE_V_KEY_FUNCTION_REGEX =
            "return decodeURIComponent\\(escape\\([a-z]\\)\\)\\}\\((\"\\w+\"|\\d+),(\"\\w+\"|\\d+),(\"\\w+\"|\\d+),(\"\\w+\"|\\d+),(\"\\w+\"|\\d+),(\"\\w+\"|\\d+)\\)\\)".toRegex()
        val JSAPI_REGEX = "const jsapi = '(.*?)';".toRegex()
        val V_KEY_JS_REGEX = "\\{ckey:'(\\w+)',ref:'(.*?)',ip:'(.*?)',time:'(\\d+)'\\}".toRegex()
        val JQ_KEY_REGEX =
            "location\\[(\\w+)\\(0x\\w+,'[^']+'\\)\\]\\[\\1\\(0x\\w+,'[^']+'\\)\\]\\(/player\\.novipnoad\\.\\([a-z|]+\\)/\\)!=null\\?\\1\\(0x(\\w+),'([^']+)'\\)".toRegex()
        val JQ_DATA_FUN_REGEX = "function (_0x\\w+)\\(\\)\\{var (_0x\\w+)=\\(function\\(\\)\\{return(.*?)\\}\\(\\)\\);\\1=function\\(\\)\\{return \\2;\\};return \\1\\(\\);\\}".toRegex()
        val JQ_DATA_ARR_REGEX = "'([^']+)'|(_0x\\w+),?".toRegex()

        const val KEY_GITHUB_URL_1 = "https://ghfast.top/https://raw.githubusercontent.com/muedsa/novipnoad-plugin/refs/heads/main/key"
        const val KEY_GITHUB_URL_2 = "https://gh-proxy.com/raw.githubusercontent.com/muedsa/novipnoad-plugin/refs/heads/main/key"
        const val KEY_GITHUB_URL_3 = "https://raw.githubusercontent.com/muedsa/novipnoad-plugin/refs/heads/main/key"

        fun decodeFunArgIntValue(
            argName: String,
            argNames: List<String>,
            argValues: List<String>,
            argReplaceMap: Map<String, String>,
        ): Int {
            val realArgName = argReplaceMap.getOrDefault(argName, argName)
            val index = argNames.indexOf(realArgName)
            if (index < 0) {
                throw RuntimeException("not fun arg [${argName}] value from: $argNames, $argReplaceMap")
            }
            return argValues[index].toInt()
        }

        fun decodeFunArgStringValue(
            argName: String,
            argNames: List<String>,
            argValues: List<String>,
            argReplaceMap: Map<String, String>,
        ): String {
            val realArgName = argReplaceMap.getOrDefault(argName, argName)
            val index = argNames.indexOf(realArgName)
            if (index < 0) {
                throw RuntimeException("not fun arg [${argName}] value from: $argNames, $argReplaceMap")
            }
            return argValues[index].removePrefix("\"").removeSuffix("\"")
        }

        fun decodeVKeyJS(
            data: String,
            replaceMap: String,
            charCodeSalt: Int,
            fromBase: Int,
            toBase: Int
        ): String {
            var result = ""
            var i = 0
            while (i < data.length) {
                var s = ""
                while (i < data.length && data[i] != replaceMap[fromBase]) {
                    s += data[i]
                    i++
                }
                for (j in replaceMap.indices) {
                    s = s.replace(replaceMap[j].toString(), j.toString())
                }
                val charCode = BaseConverter.convert(s, fromBase, toBase).toInt() - charCodeSalt
                result += Character.toChars(charCode).concatToString()
                i++
            }
            return result
        }

        private fun parseVKey(vKeyJs: String): VKey {
            if (vKeyJs.startsWith("window.sessionStorage.setItem('vkey','{")
                && vKeyJs.endsWith("}');")
            ) {
                return LenientJson.decodeFromString<VKey>(
                    vKeyJs
                        .removePrefix("window.sessionStorage.setItem('vkey','")
                        .removeSuffix("');")
                )
            }
            if (vKeyJs.startsWith("window.sessionStorage.setItem('vkey',JSON.stringify({") && vKeyJs.endsWith(
                    "}));"
                )
            ) {
                val vkMatchGroups = V_KEY_JS_REGEX.find(
                    vKeyJs.removePrefix("window.sessionStorage.setItem('vkey',JSON.stringify(")
                        .removeSuffix("));")
                )?.groups ?: throw RuntimeException("解析播放信息失败 vkey js")
                return VKey(
                    ckey = vkMatchGroups[1]?.value
                        ?: throw RuntimeException("解析播放信息失败 vkey ckey"),
                    ref = vkMatchGroups[2]?.value
                        ?: throw RuntimeException("解析播放信息失败 vkey ref"),
                    ip = vkMatchGroups[3]?.value
                        ?: throw RuntimeException("解析播放信息失败 vkey ip"),
                    time = vkMatchGroups[4]?.value
                        ?: throw RuntimeException("解析播放信息失败 vkey time")
                )
            }
            throw RuntimeException("解析播放信息失败 vkey")
        }

        @OptIn(ExperimentalStdlibApi::class)
        fun String.decodeFromJsjiami(): ByteArray = this.decodeBase64Like()
            .toString(Charsets.UTF_8)
            .toByteArray(Charsets.ISO_8859_1)

        fun String.decodeBase64Like(): ByteArray {
            val base64Chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+/="
            var temp = 0
            var bits = 0
            return ByteArrayOutputStream().use {
                for (char in this) {
                    val index = base64Chars.indexOf(char)
                    if (index == -1) continue
                    temp = (temp shl 6) or index
                    bits += 6
                    if (bits >= 8) {
                        val byteValue = (temp shr (bits - 8)) and 0xff
                        it.write(byteValue)
                        bits -= 8
                    }
                }
                it.toByteArray()
            }
        }
    }

    override suspend fun getEpisodeDanmakuDataList(episode: MediaEpisode): List<DanmakuData> =
        emptyList()

    override suspend fun getEpisodeDanmakuDataFlow(episode: MediaEpisode): DanmakuDataFlow? = null
}