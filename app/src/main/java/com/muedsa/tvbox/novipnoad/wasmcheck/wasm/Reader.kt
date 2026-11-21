package com.muedsa.tvbox.novipnoad.wasmcheck.wasm

/** LEB128 读取器，覆盖 Wasm 二进制所需的 u8 / u32 / s32 / s33 / 原始字节。 */
internal class Reader(
    private val bytes: ByteArray,
    var pos: Int = 0,
) {
    fun u8(): Int = bytes[pos++].toInt() and 0xFF

    fun u32(): Int {
        var result = 0
        var shift = 0
        while (true) {
            val b = u8()
            result = result or ((b and 0x7F) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        return result
    }

    fun s32(): Int {
        var result = 0
        var shift = 0
        var b: Int
        do {
            b = u8()
            result = result or ((b and 0x7F) shl shift)
            shift += 7
        } while (b and 0x80 != 0)
        if (shift < 32 && (b and 0x40) != 0) {
            result = result or (-1 shl shift)
        }
        return result
    }

    /**
     * 带符号 LEB128，最多 33 位（blocktype 的编码）。
     *
     * 实际取值只有两类：单字节的负值（0x40 / 0x7f…）和小整数类型索引，
     * 都不会超过 4 字节，因此 [shift] 不会溢出。
     */
    fun s33(): Int {
        var result = 0
        var shift = 0
        var b: Int
        do {
            b = u8()
            result = result or ((b and 0x7F) shl shift)
            shift += 7
        } while (b and 0x80 != 0)
        if (shift < 32 && (b and 0x40) != 0) {
            result = result or (-1 shl shift)
        }
        return result
    }

    fun bytes(n: Int): ByteArray {
        val out = bytes.copyOfRange(pos, pos + n)
        pos += n
        return out
    }

    fun eof(): Boolean = pos >= bytes.size
}