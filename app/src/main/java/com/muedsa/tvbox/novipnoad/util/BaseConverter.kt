package com.muedsa.tvbox.novipnoad.util

object BaseConverter {

    private const val DIGITS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ+/"

    fun convert(input: String, fromBase: Int, toBase: Int): String {
        require(fromBase in 2..64) { "Invalid fromBase: $fromBase" }
        require(toBase in 2..64) { "Invalid toBase: $toBase" }

        val decimal = toDecimal(input, fromBase)
        return fromDecimal(decimal, toBase)
    }

    private fun toDecimal(input: String, base: Int): Long {
        var result = 0L
        input.forEach { char ->
            val digit = DIGITS.indexOf(char)
            require(digit in 0 until base) { "Invalid character for base $base: $char" }
            result = result * base + digit
        }
        return result
    }

    private fun fromDecimal(decimal: Long, base: Int): String {
        var num = decimal
        val result = StringBuilder()
        do {
            val digit = (num % base).toInt()
            result.append(DIGITS[digit])
            num /= base
        } while (num > 0)
        return result.reverse().toString()
    }
}