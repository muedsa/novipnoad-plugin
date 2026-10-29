package com.muedsa.tvbox.novipnoad.util

import com.muedsa.tvbox.tool.decodeBase64ToStr
import kotlin.io.encoding.Base64

object JsonDecrypt {
    private val PREFIX = "cGxheWVyLm5vdmlwbm9hZC4=".decodeBase64ToStr()

    private val K9 = intArrayOf(
        0xfaf3, 0xc56d, 0x16a2, 0x9dd5, 0xbbd9, 0x2e02, 0xbefb, 0x6f03, 0x7ee1,
    )
    private val K16 = intArrayOf(
        0x9e37, 0x79b9, 0x1234, 0x5678, 0x9abc, 0xdef0, 0x2468, 0x1357,
        0xa5a5, 0x5a5a, 0xf00f, 0x0ff0, 0xface, 0xcafe, 0xbeef, 0xdead,
    )
    private val PERM = intArrayOf(
        7, 2, 15, 4, 11, 0, 13, 6, 9, 14, 1, 8, 3, 10, 5, 12,
        23, 18, 31, 20, 27, 16, 29, 22, 25, 30, 17, 24, 19, 26, 21, 28,
    )
    private val SEED = intArrayOf(
        0x11, 0x6c, 0xf5, 0x47, 0xc4, 0x96, 0x15, 0x5b, 0x70, 0x61, 0xdc, 0x7e, 0xeb, 0x69, 0x0b, 0x3b,
        0x9b, 0x3d, 0xc7, 0x68, 0x63, 0x70, 0xec, 0x97, 0xc0, 0xb8, 0x40, 0x55, 0x58, 0xba, 0xd9, 0x9e,
        0xc0, 0x9d, 0x77, 0x96, 0x76, 0x1b, 0x89, 0x7a, 0xd8, 0xb4, 0x8b, 0xa5, 0x72, 0x1a, 0x6e, 0x11,
        0x72, 0x65, 0xa5, 0x48, 0xae, 0x86, 0x3d, 0x0a, 0x2f, 0xec, 0x5b, 0x7c, 0xce, 0x09, 0xba, 0x83,
    )

    /** host 传页面的 location.host，返回 JSON 文本。 */
    fun decrypt(ciphertext: String, host: String): String {
        require(ciphertext.isNotEmpty()) { "Invalid ciphertext parameter" }
        val data = Base64.decode(ciphertext)
        val rc4Key = deriveRc4Key(keyHash(latin1(host)))
        try {
            rc4(rc4Key, data)
            return String(data, Charsets.UTF_8)
        } finally {
            rc4Key.fill(0)
        }
    }

    // 等价 JS 的 charCodeAt(i) & 0xff
    private fun latin1(s: String) = ByteArray(s.length) { (s[it].code and 0xff).toByte() }

    private fun keyHash(key: ByteArray): Int {
        if (key.size < 17) return 999
        var sum = 0
        for (i in 0 until 17) {
            val d = (key[i].toInt() and 0xff) - PREFIX[i].code
            sum += if (d < 0) -d else d
        }
        var h = -2128831035        // 0x811C9DC5
        for (i in 17 until key.size) {
            h = (h xor (key[i].toInt() and 0xff)) * 16777619   // Int 溢出回绕，与 wasm i32.mul 一致
        }
        val hu = (h.toLong() and 0xffffffffL) % 65521L
        var p = 1L
        for (j in 0 until 9) {
            val t = (hu - K9[j] + 65521L) % 65521L
            p = p * t % 65521L      // 必须用 Long：p*t 会超过 Int.MAX_VALUE
        }
        return ((sum + p) and 0xffffL).toInt()
    }

    private fun rotl16(x: Int, n: Int) = ((x shl n) or (x ushr (16 - n))) and 0xffff

    private fun deriveRc4Key(hash: Int): ByteArray {
        val s = IntArray(32) { (SEED[2 * it] and 0xff) or ((SEED[2 * it + 1] and 0xff) shl 8) }
        val t = IntArray(32)
        for (r in 0 until 16) {
            val k = K16[r]
            for (j in 0 until 16) {
                val a = s[j]
                val b = s[j + 16]
                val y = (a + rotl16((b xor k) and 0xffff, 7)) and 0xffff
                val z = ((b xor rotl16(y, 11)) xor hash) and 0xffff
                t[j] = y
                t[j + 16] = z
            }
            for (j in 0 until 32) s[PERM[j]] = t[j]
        }
        return ByteArray(8) { i ->
            val m = (s[(i + 7) % 32] ushr 8) and 0xff
            ((s[i] xor m) and 0xff).toByte()
        }
    }

    private fun rc4(key: ByteArray, data: ByteArray) {
        val s = IntArray(256) { it }
        var j = 0
        for (i in 0 until 256) {
            j = (j + s[i] + (key[i % key.size].toInt() and 0xff)) and 0xff
            val t = s[i]; s[i] = s[j]; s[j] = t
        }
        var i = 0; j = 0
        for (k in data.indices) {
            i = (i + 1) and 0xff
            val si = s[i]
            j = (j + si) and 0xff
            val sj = s[j]
            s[i] = sj; s[j] = si
            data[k] = (data[k].toInt() xor s[(si + sj) and 0xff]).toByte()
        }
    }
}