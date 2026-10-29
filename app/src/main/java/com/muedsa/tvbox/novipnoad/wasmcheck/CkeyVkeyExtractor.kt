package com.muedsa.tvbox.novipnoad.wasmcheck

import com.muedsa.tvbox.novipnoad.wasmcheck.hash.Djb2
import com.muedsa.tvbox.novipnoad.wasmcheck.js.FingerprintParams
import com.muedsa.tvbox.novipnoad.wasmcheck.js.JsFingerprintExtractor
import com.muedsa.tvbox.novipnoad.wasmcheck.js.WasmBlobExtractor
import com.muedsa.tvbox.novipnoad.wasmcheck.wasm.MiniInterpreter
import com.muedsa.tvbox.novipnoad.wasmcheck.wasm.WasmModule

/**
 * 门面：从完整性检查 JS 或 Wasm 字节中派生 `ckey / vkey`。
 *
 * 三种用法：
 * ```kotlin
 * val ex = CkeyVkeyExtractor.fromJs(js)      // 自动提取指纹
 *
 * // 1) 直接出结果（使用自动提取的指纹）
 * val (ckey, vkey) = ex.deriveKeys()
 *
 * // 2) 手动指定指纹（真实浏览器实测值）
 * val (ckey, vkey) = ex.deriveKeys(FingerprintParams(gridWidth=132, true, true, 179))
 *
 * // 3) 给定 hash
 * val (ckey, vkey) = ex.deriveKeys(hash = 1234567890)
 * ```
 */
class CkeyVkeyExtractor(
    private val wasmBytes: ByteArray,
    private val defaultFingerprint: FingerprintParams? = null,
) {

    /** 使用 32 位 hash 派生。 */
    fun deriveKeys(hash: Int): Pair<String, String> {
        val module = WasmModule(wasmBytes)

        var ckey = ""
        var vkey = ""

        val interpreter = MiniInterpreter(module) { cp, cl, vp, vl ->
            ckey = String(module.memory, cp, cl, Charsets.ISO_8859_1)
            vkey = String(module.memory, vp, vl, Charsets.ISO_8859_1)
        }
        val runIdx = if (module.runFuncIdx >= 0) module.runFuncIdx else module.numImportedFuncs
        interpreter.run(runIdx, hash)
        return ckey to vkey
    }

    /** 使用显式指纹派生。 */
    fun deriveKeys(fp: FingerprintParams): Pair<String, String> =
        deriveKeys(Djb2.envHash(fp.gridWidth, fp.canvasOk, fp.domOk, fp.cssWidth))

    /** 使用 fromJs 自动提取的指纹派生。 */
    fun deriveKeys(): Pair<String, String> {
        val fp = defaultFingerprint
            ?: throw IllegalStateException(
                "该实例未携带指纹，请使用 deriveKeys(fp) / deriveKeys(hash)，" +
                        "或改用 CkeyVkeyExtractor.fromJs(js) 创建",
            )
        return deriveKeys(fp)
    }

    /** 查看自动提取到的指纹（从 fromWasm 创建时为 null）。 */
    fun fingerprint(): FingerprintParams? = defaultFingerprint

    companion object {

        /**
         * 从完整 JS 源码创建，同时自动提取指纹。
         * 如果指纹提取失败（例如代码结构变化），仍可创建实例，
         * 只是后续调用无参 deriveKeys() 时会抛异常。
         */
        fun fromJs(js: String): CkeyVkeyExtractor {
            val wasm = WasmBlobExtractor.extractWasmBytes(js)
            val fp = runCatching { JsFingerprintExtractor.extract(js) }.getOrNull()
            return CkeyVkeyExtractor(wasm, fp)
        }

        /** 只有 Wasm 字节时使用，需要手动传指纹或 hash。 */
        fun fromWasm(wasmBytes: ByteArray): CkeyVkeyExtractor =
            CkeyVkeyExtractor(wasmBytes)
    }
}