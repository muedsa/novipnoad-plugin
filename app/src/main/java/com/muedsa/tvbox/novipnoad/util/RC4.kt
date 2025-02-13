package com.muedsa.tvbox.novipnoad.util

class RC4(key: ByteArray) {
    private var s = IntArray(256)
    private var i = 0
    private var j = 0

    init {
        var j = 0
        for (i in s.indices) {
            s[i] = i
        }
        for (i in s.indices) {
            j = (j + s[i] + key[i % key.size]) and 0xFF
            s[i] = s[j].also { s[j] = s[i] }
        }
    }

    fun decrypt(data: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        for (k in data.indices) {
            i = (i + 1) and 0xFF
            j = (j + s[i]) and 0xFF
            s[i] = s[j].also { s[j] = s[i] }
            result[k] = (data[k].toInt() xor s[(s[i] + s[j]) and 0xFF]).toByte()
        }
        return result
    }
}
