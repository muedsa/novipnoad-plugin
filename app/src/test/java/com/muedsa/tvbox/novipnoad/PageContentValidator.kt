package com.muedsa.tvbox.novipnoad

import com.muedsa.tvbox.tool.checkSuccess
import com.muedsa.tvbox.tool.createOkHttpClient
import com.muedsa.tvbox.tool.feignChrome
import com.muedsa.tvbox.tool.get
import com.muedsa.tvbox.tool.md5
import com.muedsa.tvbox.tool.stringBody
import com.muedsa.tvbox.tool.toRequestBuild
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class)
class PageContentValidator {

    @Test
    fun jq_valid() {
        val hash = "https://player.novipnoad.net/js/jquery.min.js".toRequestBuild()
            .feignChrome()
            .get(okHttpClient = createOkHttpClient(debug = true))
            .checkSuccess()
            .stringBody()
            .md5()
            .toHexString()
        println(hash)
        check("f975d8344e3ad3825c275369fa3370d9".contentEquals(hash))
    }

}