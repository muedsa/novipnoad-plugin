package com.muedsa.tvbox.novipnoad.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ExpressionCalculatorTest(
    private val testName: String,
    private val expression: String,
    private val expectedResult: Int
) {
    private val calculator = ExpressionCalculator()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}: {1} = {2}")
        fun testData(): Collection<Array<Any>> {
            return listOf(
                // ======================== 基础运算（补充边界） ========================
                arrayOf("基础四则运算", "1+2*3", 7),
                arrayOf("括号优先级", "(1+2)*3", 9),
                arrayOf("混合四则运算", "10/2+3*4", 17),
                arrayOf("括号+减法", "10-(2+3)*2", 0),
                arrayOf("取模运算(正)", "7%3", 1),
                arrayOf("取模运算(负)", "7%-3", 1), // Kotlin 整数取模规则：结果符号与被除数一致
                arrayOf("按位异或", "5^3", 6), // 5(101)^3(011)=6(110)
                arrayOf("单目负号+乘法", "-(3+4)*2", -14),
                arrayOf("嵌套单目负号", "-(-10)+5*2", 20),
                arrayOf("纯数字", "123", 123),
                arrayOf("纯负数", "-456", -456),
                arrayOf("带括号纯数字", "(789)", 789),
                arrayOf("多层括号纯数字", "((100))", 100),
                arrayOf("零值运算", "0+0*10-0", 0),
                arrayOf("整数除法(向下取整)", "7/2", 3), // Kotlin 整数除法默认向下取整
                arrayOf("负数除法", "-7/2", -3),
                arrayOf("括号嵌套四则运算", "((10-5)*(2+3))/5", 5),
                arrayOf("单目正号", "+100", 100),
                arrayOf("单目正号+括号", "+(10+20)", 30),

                // ======================== 位运算（Kotlin原生运算符） ========================
                arrayOf("按位与", "5&3", 1), // 5(101)&3(011)=1(001)
                arrayOf("按位或", "5|3", 7), // 5(101)|3(011)=7(111)
                arrayOf("按位异或(嵌套)", "(5^3)&2", 2), // 6(110)&2(010)=2
                arrayOf("左移位运算", "2<<1", 4), // 2(10)<<1=4(100)
                arrayOf("右移位运算", "4>>1", 2), // 4(100)>>1=2(10)
                arrayOf("负数左移", "-2<<1", -4), // -2的补码左移1位=-4
                arrayOf("位运算极值", "1<<30", 1073741824), // 2^30（Kotlin Int 最大值2^31-1）
                arrayOf("按位取反", "~1", -2), // ~1 = -2（Kotlin 补码规则）
                arrayOf("括号内按位取反", "~(2+3)", -6), // ~5 = -6

                // ======================== 自定义幂运算（代码中循环实现） ========================
                arrayOf("幂运算(2的3次方)", "2**3", 8), // 自定义**为幂运算，非Kotlin原生
                arrayOf("幂运算(3的2次方)", "3**2", 9),
                arrayOf("幂运算(1的任意次方)", "1**10", 1),
                arrayOf("幂运算(0的正次方)", "0**5", 0),

                // ======================== 逻辑非/自增自减（自定义单目运算符） ========================
                arrayOf("逻辑非(0)", "!0", 1), // 自定义!：0→1，非0→0
                arrayOf("逻辑非(非0)", "!5", 0),
                arrayOf("括号内逻辑非", "!(10-10)", 1), // !(0)=1
                arrayOf("前缀自增", "++5", 6), // 自定义++前缀自增
                arrayOf("前缀自减", "--5", 4), // 自定义--前缀自减
                arrayOf("括号内自增", "++(3+2)", 6), // ++5=6
                arrayOf("括号内自减", "--(10-3)", 6), // --7=6

                // ======================== 混合运算（边界场景） ========================
                arrayOf("最大单数字", "9", 9),
                arrayOf("最小负数", "-999999", -999999),
                arrayOf("多层括号嵌套", "((((1+1))))", 2),
                arrayOf("运算符连续合法场景", "-(-(10))", 10),
                arrayOf("混合所有基础运算符", "10+2*3-8/2%3", 15), // 10+6-4%3=10+6-1=15
                arrayOf("扩展运算符混合", "~(5&3)+!(7-7)**2", -1), // ~1 + 1**2 = -2+1=-1
                arrayOf("自增+取模", "++7%3", 2), // 8%3=2
                arrayOf("多层扩展运算", "((~2)&3)|(4**1)", 5) // (~2=-3)&3=1 |4=5
            )
        }
    }

    @Test
    fun testCalculate() {
        val actualResult = calculator.calculate(expression)
        assertEquals("测试[$testName]失败：表达式=$expression", expectedResult, actualResult)
    }

    // 错误场景测试
    @Test(expected = IllegalArgumentException::class)
    fun testUnclosedBracket() {
        calculator.calculate("(1+2")
    }
}