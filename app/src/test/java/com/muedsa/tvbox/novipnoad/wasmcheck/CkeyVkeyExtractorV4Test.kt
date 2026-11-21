package com.muedsa.tvbox.novipnoad.wasmcheck

import com.muedsa.tvbox.novipnoad.wasmcheck.js.FingerprintParams
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 线上第 4 版完整性检查脚本（2026-11 抓取，存为测试资源）。
 *
 * 与旧版（内联在 [CkeyVkeyExtractorTest] 里的 #1/#3）的差异：
 *  - 指纹串末尾新增 location 检测段（命中时返回 71923），
 *    少这一段哈希就错，wasm 会产出随机字节而不是 ckey/vkey
 *  - wasm 改用 i32.rotl / i32.rotr / i32.rem_u / select，并多了一个本地函数调用
 */
class CkeyVkeyExtractorV4Test {

    @Test
    fun deriveKeys() {
        val js = requireNotNull(javaClass.classLoader?.getResourceAsStream("wasm/integrity-v4.js")) {
            "缺少测试资源 wasm/integrity-v4.js"
        }.use { it.readBytes().decodeToString() }

        val extractor = CkeyVkeyExtractor.fromJs(js)
        assertEquals(
            FingerprintParams(
                gridWidth = 161,
                canvasOk = true,
                domOk = true,
                cssWidth = 174,
                extraMagics = listOf(71923),
            ),
            extractor.fingerprint(),
        )

        val (ckey, vkey) = extractor.deriveKeys()
        assertEquals("743456046025a1d464deb9733dc63dc90f4e20af", ckey)
        assertEquals(
            decodeGolden("eyJyZWYiOiJodHRwczovL3d3dy5ub3ZpcG5vYWQudWsvbW92aWUvMTU1MDc2Lmh0bWwiLCJpcCI6IjE4MC4xNjUuMjguMTgzIiwidGltZSI6IjE3OTUyNzg1NzcifQ=="),
            vkey,
        )
    }
}
