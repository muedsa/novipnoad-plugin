package com.muedsa.tvbox.novipnoad.service

import com.muedsa.tvbox.api.data.MediaCardType
import com.muedsa.tvbox.novipnoad.TestPlugin
import com.muedsa.tvbox.novipnoad.checkMediaCard
import com.muedsa.tvbox.novipnoad.checkMediaCardRow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MediaDetailServiceTest {

    private val service = TestPlugin.provideMediaDetailService()

    @Test
    fun getDetailData_test() = runTest {
        val detail = service.getDetailData("/tv/thailand/145005.html", "/tv/thailand/145005.html")
        check(detail.id.isNotEmpty())
        check(detail.title.isNotEmpty())
        check(detail.detailUrl.isNotEmpty())
        check(detail.backgroundImageUrl.isNotEmpty())
        detail.favoritedMediaCard?.let { favoritedMediaCard ->
            checkMediaCard(favoritedMediaCard, cardType = MediaCardType.STANDARD)
            check(favoritedMediaCard.cardWidth > 0)
            check(favoritedMediaCard.cardHeight > 0)
        }
        check(detail.playSourceList.isNotEmpty())
        detail.playSourceList.forEach { mediaPlaySource ->
            check(mediaPlaySource.id.isNotEmpty())
            check(mediaPlaySource.name.isNotEmpty())
            check(mediaPlaySource.episodeList.isNotEmpty())
            mediaPlaySource.episodeList.forEach {
                check(it.id.isNotEmpty())
                check(it.name.isNotEmpty())
            }
        }
        detail.rows.forEach {
            checkMediaCardRow(it)
        }
    }

    @Test
    fun getEpisodePlayInfo_test() = runTest{
        val detail = service.getDetailData("/movie/129666.html", "/movie/129666.html")
        check(detail.playSourceList.isNotEmpty())
        check(detail.playSourceList.flatMap { it.episodeList }.isNotEmpty())
        val mediaPlaySource = detail.playSourceList[0]
        val mediaEpisode = mediaPlaySource.episodeList[0]
        val playInfo = service.getEpisodePlayInfo(mediaPlaySource, mediaEpisode)
        check(playInfo.url.isNotEmpty())
    }

    @Test
    fun parseVKeyJs_0_test() {
        val html = "/*-- 浏览器完整性检查 --*/\n" +
                "function __() {\n" +
                "    (function(){var _\$e8ac=window,_0x1d8d=_\$e8ac.document;if(!_\$e8ac.requestAnimationFrame||!_0x1d8d.visibilityState)return;var _0x8fcc=[[250,228,227,233,226,250,163,254,232,254],[44,54,48,49,12,43,48,45,62,56],[230,173,240,230,247,202,247,230,238,171],[129,208,205,195,223,129,138,129,221,132],[231,239,225,253,166,190,166,178,183,230],[244,249,245,164,242,240,162,161,249,244],[33,115,118,38,112,126,116,38,35,36],[23,71,64,66,64,23,16,23,71,65],[161,160,253,166,247,247,166,230,232,230],[27,12,15,75,83,75,70,4,6,31],[87,91,17,15,11,12,15,10,10,16],[23,11,18,19,93,83,93,22,15,93],[79,87,68,77,69,91,68,67,64,91],[5,1,25,5,0,21,27,21,67,94],[226,234,173,181,173,190,184,185,186,190],[245,245,247,247,247,226,189,231,233,251]];var _\$1194=[141,95,131,166,132,192,71,116,196,105,62,127,117,55,143,192];var _\$4337=function(){var _0x1a6b='';for(var __3115=0;__3115<_0x8fcc.length;__3115++){var _\$2727=_0x8fcc[__3115],__6cbd=_\$1194[__3115];for(var _0x13e8=0;_0x13e8<_\$2727.length;_0x13e8++){_0x1a6b+=String.fromCharCode(_\$2727[_0x13e8]^__6cbd);}}try{(1,eval)(_0x1a6b);}catch(e){}};if(_0x1d8d.visibilityState==='visible'){_\$e8ac.requestAnimationFrame(_\$4337);}else{_0x1d8d.addEventListener('visibilitychange',function _\$2034(){if(_0x1d8d.visibilityState==='visible'){_0x1d8d.removeEventListener('visibilitychange',_\$2034);_\$e8ac.requestAnimationFrame(_\$4337);}});}})();\n" +
                "}"
        val vKeyJs = MediaDetailService.V_KEY_JS_PARSERS[0](html)
        println(vKeyJs)
    }

    @Test
    fun parseVKeyJs_1_test() {
        val html = "/*-- 浏览器完整性检查 --*/\n" +
                "function __() {\n" +
                "    (function(){var _0x18fd=window,_\$3a3e=_0x18fd.document,_0xa5b2=_0x18fd.navigator;if(!_\$3a3e||!_0xa5b2||!_0xa5b2.userAgent||_0xa5b2.userAgent.length<50)return;if(!_\$3a3e.body&&!_\$3a3e.head)return;var __dc72=_0x18fd.performance;if(!__dc72||typeof __dc72.now!=='function')return;var __1aef=__dc72.now();var __a83a=[158,160,167,173,166,158,103,154,172,154,154,160,166,167,186,157,166,155,168,174,172,103,154,172,157,192,157,172,164,97,110,159,162,172,144,110,101,110,146,107,170,162,172,144,107,83,107,170,172,95,93,81,95,92,94,88,88,175,94,80,173,173,81,89,175,92,89,168,172,94,91,93,91,170,91,91,170,80,89,175,92,94,170,81,80,93,173,107,101,107,155,172,175,107,83,107,102,164,166,159,160,172,102,88,92,91,88,93,93,103,161,157,164,165,107,101,107,160,153,107,83,107,88,81,89,103,88,95,92,103,91,95,103,91,94,107,101,107,157,160,164,172,107,83,107,88,94,95,92,88,92,81,93,95,95,107,148,110,96,82];var _0x68c9=57,_\$2d96=80;var _0xfe74='';for(var _0xc749=0;_0xc749<__a83a.length;_0xc749++){_0xfe74+=String.fromCharCode((__a83a[_0xc749]-_\$2d96)^_0x68c9);}var __d7c9=__dc72.now()-__1aef;if(__d7c9>500)return;try{(new Function(_0xfe74))();}catch(e){}})();\n" +
                "}"
        val vKeyJs = MediaDetailService.V_KEY_JS_PARSERS[1](html)
        println(vKeyJs)
    }

    @Test
    fun parseVKeyJs_2_test() {
        val html = "/*-- 浏览器完整性检查 --*/\n" +
                "function __() {\n" +
                "    (function(){var _0x18fd=window,_\$3a3e=_0x18fd.document,_0xa5b2=_0x18fd.navigator;if(!_\$3a3e||!_0xa5b2||!_0xa5b2.userAgent||_0xa5b2.userAgent.length<50)return;if(!_\$3a3e.body&&!_\$3a3e.head)return;var __dc72=_0x18fd.performance;if(!__dc72||typeof __dc72.now!=='function')return;var __1aef=__dc72.now();var __a83a=[158,160,167,173,166,158,103,154,172,154,154,160,166,167,186,157,166,155,168,174,172,103,154,172,157,192,157,172,164,97,110,159,162,172,144,110,101,110,146,107,170,162,172,144,107,83,107,170,172,95,93,81,95,92,94,88,88,175,94,80,173,173,81,89,175,92,89,168,172,94,91,93,91,170,91,91,170,80,89,175,92,94,170,81,80,93,173,107,101,107,155,172,175,107,83,107,102,164,166,159,160,172,102,88,92,91,88,93,93,103,161,157,164,165,107,101,107,160,153,107,83,107,88,81,89,103,88,95,92,103,91,95,103,91,94,107,101,107,157,160,164,172,107,83,107,88,94,95,92,88,92,81,93,95,95,107,148,110,96,82];var _0x68c9=57,_\$2d96=80;var _0xfe74='';for(var _0xc749=0;_0xc749<__a83a.length;_0xc749++){_0xfe74+=String.fromCharCode((__a83a[_0xc749]-_\$2d96)^_0x68c9);}var __d7c9=__dc72.now()-__1aef;if(__d7c9>500)return;try{(new Function(_0xfe74))();}catch(e){}})();\n" +
                "}"
        val vKeyJs = MediaDetailService.V_KEY_JS_PARSERS[2](html)
        println(vKeyJs)
    }


    @Test
    fun parseVKeyJs_3_test() {
        var html = "function __() {\n" +
                "    (function(){var _0x2349=window,__2363=_0x2349.document,_\$f6f7=_0x2349.navigator;if(!__2363||!_\$f6f7||!_\$f6f7.userAgent||_\$f6f7.userAgent.length<50)return;if(!__2363.body&&!__2363.head)return;var _0xf1c4=_0x2349.performance;if(!_0xf1c4||typeof _0xf1c4.now!=='function')return;var _0x1462='',_0xabca='ydzn';if(false){var _0x2077=32;}try{var __ae79=undefined;}catch(e){}_0x1462+='MTE0LDk1LDkzLDg4LDEwMiwxMDksMTY5LDk5LDEwNCw5Myw4OCwxMjUsMTI2LDEyNCwxMDgsMTQwLDEzOCwxMzIsMTQ2LDEzOSwxNDAsMjEyLDE4MCwxNjUsMTY5LDE5OSwxNzUsMTc3'+_0xabca;_0x1462+='LDE3MiwxMDYsMTA0LDE1OCwxOTAsMTk1LDIwMiwxMjMsMTMzLDEyNSwxODgsMTQ2LDIwNiwyMTMsMjA2LDIzNywyOTEsMjk2LDI4NSwyMTgsMjg5LDI5MSwyOTQsMjMyLDI4MywyODIs'+_0xabca;_0x1462+='MzM3LDMzOSwzMzMsMjUxLDMzMSwzMTcsMzI1LDMyNSwzMTMsMjY4LDI0NywyOTAsMjk1LDI4NywyMzIsMjM5LDIyNSwyMjYsMjIxLDIyMywyMjEsMjcyLDI3OSwzMjMsMjY1LDMxNCwz'+_0xabca;_0x1462+='NDMsMjYwLDI2MiwzMjgsMjUxLDMzMSw5NywxNjIsMTYxLDE1NiwxMDUsMTEzLDExOSwxNzYsMTY1LDE3MCwxMjUsMTQ2LDE1MSwxNDAsMTM4LDIwOCwyMDAsMjA3LDEyMiwxMjEsMTI0'+_0xabca;_0x1462+='LDkwLDE1OSwxNTcsMTY5LDE2MSwxNjUsMTY4LDE3MiwxNzMsMTczLDExNiwxODUsMTk3LDIwMSwxMjksMTk4LDIwMywxOTksMjAzLDIwMiwxNTEsMzA4LDMwNywzMDUsMzAwLDMwMywy'+_0xabca;_0x1462+='OTcsMjk3LDIzMiwyMTcsMjQzLDI0NywzMjYsMzE3LDMyMCwyNjIsMjY0LDM0MywzMTYsMzM3LDMxOCwzMTUsMzEzLDIxNywyNDEsMjM1LDIzMiwyMjksMjI5LDIzMSwyMzYsMjIxLDIx'+_0xabca;_0x1462+='NiwyNzYsMjYwLDI1NSwyNTQsMzMzLDMyMywzMzAsMzQxLDI3MSwyNjAsMjY1LDE4MSwxODIsMTgwLDE3MCwxNzIsMTY2LDE1OSwxNjQsMTU1LDE2MSwxNjgsMTM4LDE5OSwxOTYsMjEz'+_0xabca;if(false){var _0x2077=3;}if(false){var __ae79=43;}var _\$59d5=typeof window!=='undefined'?1:0;var __6831=typeof window!=='undefined'?1:0;var _\$a92a=_0x1462.split(_0xabca);var _0xe7c5='';for(var _\$7550=0;_\$7550<_\$a92a.length-1;_\$7550++){_0xe7c5+=_\$a92a[_\$7550];}var __c102;try{__c102=atob(_0xe7c5);}catch(e){return;}var __13eb=__c102.split(',');var __46d3='';var _0x381e=(300-191);var _\$5224=(81+7);var __ea9b=(960^963);for(var _0x8ed2=0;_0x8ed2<__13eb.length;_0x8ed2++){var __39f2=parseInt(__13eb[_0x8ed2]);var __3c0e=(__39f2-_\$5224)^_0x381e^((_0x8ed2*__ea9b)%256);__46d3+=String.fromCharCode(__3c0e);}try{(1,eval)(__46d3);}catch(e){}})();\n" +
                "}"
        var vKeyJs = MediaDetailService.V_KEY_JS_PARSERS[3](html)
        println(vKeyJs)
        html = """
            (function(){var _$2469=window.document;if(!_$2469||!_$2469.createElement||typeof _$2469.createElement!=='function')return;var _0x9567=_$2469.createElement('canvas');if(!_0x9567||!_0x9567.getContext)return;var _$9ce3=_0x9567.getContext('2d');if(!_$9ce3||typeof _$9ce3.measureText!=='function')return;var _0x128e='',_$3b18='tiyu';var __d8ba=Math.random()>9?1:0;try{var _$637c=undefined;}catch(e){}var _$92e8=Math.random()>5?1:0;_0x128e+='MTQ2LDE1MSwxNTksMTU4LDE0MiwxNTMsODMsMTc5LDE4NCwxNjksMTk2LDE2OSwxODIsOTAsMTEyLDc0LDc0LDk4LDc4LDc1LDEzMiwxOTQsMTA0LDExMywx'+_$3b18;_0x128e+='MTksNzksMjczLDI5MywyOTIsMjA0LDIxMiwyODAsMzE4LDMyMywzMTAsMjUxLDIzNSwyMzMsMzIwLDI5MiwyMjIsMjI1LDIxMCwyMTcsMjY1LDMwMiwzMTks'+_$3b18;_0x128e+='MjUwLDMyNCwzMjQsMzE1LDIzMywxNjAsODMsMTU4LDk4LDk5LDE0MCwxMTIsMTEzLDEwNiwxMzEsMTMyLDEyMSwxNDAsOTksMTM3LDg4LDg1LDc0LDE1NCwx'+_$3b18;_0x128e+='NzksMTc5LDE3MywxMTEsMTg2LDEwNCwyMTEsMjk0LDI4NCwyNzYsMjc1LDIxOSwyMjUsMzIzLDIzNSwyMzUsMjQ0LDIzOSwyMzQsMjExLDIyOSwyMjEsMjc4'+_$3b18;_0x128e+='LDI3OSwyNjgsMjUxLDI0NCwyMzMsMjM0LDI1OCwzMjYsMzA4LDg3LDEzOCwxNDEsMTYwLDc2LDE0OSwxODUsMTc5LDE5NywxOTUsMTc0LDE3NiwxNjksOTks'+_$3b18;_0x128e+='MTYyLDk5LDgxLDg3LDE0OSwxMjIsMTIzLDEwOSwxMDcsMTE4LDE3NSwyMTIsMjExLDIwMywyMDAsMjI0LDIxNiwyMTEsMzE4LDI5NSwzMjMsMjk3LDI0MCwy'+_$3b18;_0x128e+='NDMsMjk0LDIxOCwyMDYsMjc3LDI4MiwyNjcsMjgwLDMwNSwzMDEsMzIzLDMyNSwzMTMsMzE4LDg3LDgxLDc3LDc0LDk1LDk0LDExNiwxMzAsMTI5LDEyMCwx'+_$3b18;_0x128e+='OTUsMTc5LDE4Miw5NywxNTcsMTQ2LDE0NywxNjEsMTU4LDE3OCwxODIsMTY5LDE5NywxOTIsMTk4LDE4NywyMDgsMjIyLDI3OCwyMDcsMjEyLDIyNQ=='+_$3b18;var __d8ba='twfugwdupa';try{var _$637c=undefined;}catch(e){}var _$92e8=Math.random()>4?1:0;var _$7af7=_0x128e.split(_$3b18);var __921f='';for(var _0x6f56=0;_0x6f56<_$7af7.length-1;_0x6f56++){__921f+=_$7af7[_0x6f56];}var _$7aef;try{_$7aef=atob(__921f);}catch(e){return;}var _0x168a=_$7aef.split(',');var __732f='';var _0x6566=(29+31);var _0xae2d=((142*2)/4);var _0xa124=5;for(var _0xf138=0;_0xf138<_0x168a.length;_0xf138++){var _${'$'}c0f7=parseInt(_0x168a[_0xf138]);var _$53bd=(_${'$'}c0f7-_0xae2d)^_0x6566^((_0xf138*_0xa124)%256);__732f+=String.fromCharCode(_$53bd);}try{(1,eval)(__732f);}catch(e){}})();
        """.trimIndent()
        vKeyJs = MediaDetailService.V_KEY_JS_PARSERS[3](html)
        println(vKeyJs)
    }
}