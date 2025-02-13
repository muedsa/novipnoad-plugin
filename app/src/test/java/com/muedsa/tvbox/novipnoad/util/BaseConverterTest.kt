package com.muedsa.tvbox.novipnoad.util

import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BaseConverterTest {
    @Test
    fun testBinaryToDecimal() {
        val result = BaseConverter.convert("1010", 2, 10)
        assertEquals("10", result)
    }

    @Test
    fun testDecimalToBinary() {
        val result = BaseConverter.convert("10", 10, 2)
        assertEquals("1010", result)
    }

    @Test
    fun testHexToDecimal() {
        val result = BaseConverter.convert("1a", 16, 10)
        assertEquals("26", result)
    }

    @Test
    fun testDecimalToHex() {
        val result = BaseConverter.convert("26", 10, 16)
        assertEquals("1a", result)
    }

    @Test
    fun testOctalToDecimal() {
        val result = BaseConverter.convert("12", 8, 10)
        assertEquals("10", result)
    }

    @Test
    fun testDecimalToOctal() {
        val result = BaseConverter.convert("10", 10, 8)
        assertEquals("12", result)
    }

    @Test
    fun testBase32ToDecimal() {
        val result = BaseConverter.convert("u", 32, 10)
        assertEquals("30", result)
    }

    @Test
    fun testDecimalToBase32() {
        val result = BaseConverter.convert("30", 10, 32)
        assertEquals("u", result)
    }

    @Test
    fun testBase64ToDecimal() {
        val result = BaseConverter.convert("10", 64, 10)
        assertEquals("64", result)
    }

    @Test
    fun testDecimalToBase64() {
        val result = BaseConverter.convert("64", 10, 64)
        assertEquals("10", result)
    }

    @Test
    fun testHexToBinary() {
        val result = BaseConverter.convert("1a", 16, 2)
        assertEquals("11010", result)
    }

    @Test
    fun testBinaryToHex() {
        val result = BaseConverter.convert("11010", 2, 16)
        assertEquals("1a", result)
    }

    @Test
    fun testOctalToBinary() {
        val result = BaseConverter.convert("12", 8, 2)
        assertEquals("1010", result)
    }

    @Test
    fun testBinaryToOctal() {
        val result = BaseConverter.convert("1010", 2, 8)
        assertEquals("12", result)
    }

    @Test
    fun testBase32ToBinary() {
        val result = BaseConverter.convert("u", 32, 2)
        assertEquals("11110", result)
    }

    @Test
    fun testBinaryToBase32() {
        val result = BaseConverter.convert("11110", 2, 32)
        assertEquals("u", result)
    }

    @Test
    fun testBase64ToBinary() {
        val result = BaseConverter.convert("10", 64, 2)
        assertEquals("1000000", result)
    }

    @Test
    fun testBinaryToBase64() {
        val result = BaseConverter.convert("1000000", 2, 64)
        assertEquals("10", result)
    }

    @Test
    fun testInvalidCharacter() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            BaseConverter.convert("1g", 16, 10)
        }
        assertEquals("Invalid character for base 16: g", exception.message)
    }

    @Test
    fun testInvalidFromBase() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            BaseConverter.convert("10", 1, 10)
        }
        assertEquals("Invalid fromBase: 1", exception.message)
    }

    @Test
    fun testInvalidToBase() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            BaseConverter.convert("10", 10, 65)
        }
        assertEquals("Invalid toBase: 65", exception.message)
    }

    @Test
    fun test() {
        val result = BaseConverter.convert("531", 6, 13)
        assertEquals("124", result)
    }
}