package com.muedsa.tvbox.novipnoad.util

import java.util.Stack

class ExpressionCalculator {
    // 运算符优先级映射
    private val opPriority = mapOf(
        "(" to 0,
        "+" to 1,  // 加法/单目正号
        "-" to 1,  // 减法/单目负号
        "*" to 2,
        "/" to 2,
        "%" to 2,
        "<<" to 3, // 左移
        ">>" to 3, // 右移
        "&" to 4,  // 按位与
        "|" to 5,  // 按位或
        "^" to 6,  // 按位异或
        "!" to 7,  // 逻辑非（纯单目）
        "~" to 7,  // 按位取反（纯单目）
        "**" to 8, // 幂运算
        "++" to 9, // 自增（前缀）
        "--" to 9  // 自减（前缀）
    )

    /**
     * 核心计算方法
     */
    fun calculate(expression: String): Int {
        val cleanExpr = expression.replace("\\s+".toRegex(), "")
        validateExpression(cleanExpr)

        val numStack = Stack<Int>()   // 数字栈
        val opStack = Stack<String>() // 运算符栈（支持多字符）

        var i = 0
        val len = cleanExpr.length

        while (i < len) {
            val c = cleanExpr[i]
            when {
                // 处理数字（多位数）
                c.isDigit() -> {
                    var num = 0
                    while (i < len && cleanExpr[i].isDigit()) {
                        num = num * 10 + (cleanExpr[i] - '0')
                        i++
                    }
                    numStack.push(num)
                }

                // 处理左括号
                c == '(' -> {
                    opStack.push("(")
                    i++
                }

                // 处理右括号：计算到左括号为止
                c == ')' -> {
                    while (opStack.isNotEmpty() && opStack.peek() != "(") {
                        val result = calculateSingleOp(numStack, opStack.pop())
                        numStack.push(result)
                    }
                    if (opStack.isEmpty()) throw IllegalArgumentException("括号不匹配：$cleanExpr")
                    opStack.pop() // 弹出左括号
                    i++
                }

                // 处理运算符（含多字符、单目运算符）
                isOperatorStart(c) -> {
                    // 识别多字符运算符（<<、>>、**、++、--）
                    val op = if (i + 1 < len) {
                        val twoChar = cleanExpr.substring(i, i + 2)
                        if (twoChar in opPriority.keys) twoChar else c.toString()
                    } else {
                        c.toString()
                    }

                    // 纯单目运算符：!、~（直接计算后续数字）
                    if (op == "!" || op == "~") {
                        i++ // 跳过!/-
                        // 处理后续的数字/括号表达式（核心修复：改用手动遍历找括号）
                        val (calcNum, newIndex) = calculateUnaryTarget(cleanExpr, i)
                        i = newIndex
                        // 直接计算并推入结果
                        val result = if (op == "!") {
                            if (calcNum == 0) 1 else 0 // 逻辑非
                        } else {
                            calcNum.inv() // 按位取反
                        }
                        numStack.push(result)
                    }
                    // 前缀自增/自减
                    else if (op == "++" || op == "--") {
                        i += 2 // 跳过++/--
                        val (calcNum, newIndex) = calculateUnaryTarget(cleanExpr, i)
                        i = newIndex
                        val result = if (op == "++") calcNum + 1 else calcNum - 1
                        numStack.push(result)
                    }
                    // 单目+/-（转换为0+num/0-num）
                    else if ((op == "+" || op == "-") && isUnaryOperator(cleanExpr, i)) {
                        numStack.push(0)
                        opStack.push(op)
                        i++
                    }
                    // 普通双目运算符
                    else {
                        while (opStack.isNotEmpty() && opPriority[opStack.peek()]!! >= opPriority[op]!!) {
                            val result = calculateSingleOp(numStack, opStack.pop())
                            numStack.push(result)
                        }
                        opStack.push(op)
                        i += op.length
                    }
                }

                // 非法字符
                else -> throw IllegalArgumentException("非法字符：$c")
            }
        }

        // 处理剩余运算符
        while (opStack.isNotEmpty()) {
            val result = calculateSingleOp(numStack, opStack.pop())
            numStack.push(result)
        }

        if (numStack.size != 1) throw IllegalArgumentException("表达式格式错误：$expression")
        return numStack.pop()
    }

    /**
     * 计算单目运算符的目标值（支持数字/括号表达式）
     * @return Pair(计算结果, 新的索引位置)
     */
    private fun calculateUnaryTarget(expr: String, startIndex: Int): Pair<Int, Int> {
        var i = startIndex
        // 处理括号包裹的表达式
        if (i < expr.length && expr[i] == '(') {
            var bracketCount = 1
            i++
            // 手动遍历：从当前i开始找匹配的右括号
            while (i < expr.length) {
                when (expr[i]) {
                    '(' -> bracketCount++
                    ')' -> {
                        bracketCount--
                        if (bracketCount == 0) {
                            // 找到匹配的右括号，计算括号内表达式
                            val subExpr = expr.substring(startIndex + 1, i)
                            val subResult = calculate(subExpr)
                            return Pair(subResult, i + 1) // 索引移到右括号后
                        }
                    }
                }
                i++
            }
            // 遍历完未找到匹配括号，抛出异常
            throw IllegalArgumentException("括号未闭合：$expr")
        }
        // 处理普通数字
        else if (i < expr.length && expr[i].isDigit()) {
            var num = 0
            while (i < expr.length && expr[i].isDigit()) {
                num = num * 10 + (expr[i] - '0')
                i++
            }
            return Pair(num, i)
        } else if (i >= expr.length) {
            throw IllegalArgumentException("单目运算符后无操作数：$expr")
        } else {
            throw IllegalArgumentException("单目运算符后非法字符：${expr[i]}")
        }
    }

    /**
     * 校验表达式语法
     */
    private fun validateExpression(expr: String) {
        if (expr.isEmpty()) throw IllegalArgumentException("表达式不能为空")

        // 1. 括号匹配校验
        val bracketStack = Stack<Char>()
        for (c in expr) {
            when (c) {
                '(' -> bracketStack.push(c)
                ')' -> {
                    if (bracketStack.isEmpty()) throw IllegalArgumentException("右括号多余：$expr")
                    bracketStack.pop()
                }
            }
        }
        if (bracketStack.isNotEmpty()) throw IllegalArgumentException("左括号未闭合：$expr")

        // 2. 首尾字符合法性校验
        val first = expr.first()
        val validFirstChars =
            setOf('(', '!', '~', '+', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '*')
        if (first !in validFirstChars) throw IllegalArgumentException("表达式不能以 $first 开头")

        val last = expr.last()
        val invalidLastChars =
            setOf('+', '-', '*', '/', '%', '<', '>', '&', '|', '^', '!', '~', '(', '*')
        if (last in invalidLastChars) throw IllegalArgumentException("表达式不能以 $last 结尾")

        // 3. 多字符运算符连续校验
        val multiOpRegex = Regex("(<{3,}|>{3,}|\\*{3,}\\+{3,}|\\-{3,})")
        if (multiOpRegex.containsMatchIn(expr)) {
            throw IllegalArgumentException("非法连续多字符运算符：${multiOpRegex.find(expr)?.value}")
        }
    }

    /**
     * 判断是否为运算符的起始字符
     */
    private fun isOperatorStart(c: Char): Boolean =
        c in setOf('+', '-', '*', '/', '%', '<', '>', '&', '|', '^', '!', '~')

    /**
     * 判断+/-是否为单目运算符
     */
    private fun isUnaryOperator(expr: String, index: Int): Boolean {
        return index == 0 || expr[index - 1] == '(' || isOperatorStart(expr[index - 1])
    }

    /**
     * 执行单个运算符计算
     */
    private fun calculateSingleOp(numStack: Stack<Int>, op: String): Int {
        return when (op) {
            // 原有运算符
            "+" -> {
                val (num1, num2) = getTwoNums(numStack, op)
                num1 + num2
            }

            "-" -> {
                val (num1, num2) = getTwoNums(numStack, op)
                num1 - num2
            }

            "*" -> {
                val (num1, num2) = getTwoNums(numStack, op)
                num1 * num2
            }

            "/" -> {
                val (num1, num2) = getTwoNums(numStack, op)
                if (num2 == 0) throw ArithmeticException("除零错误")
                num1 / num2
            }

            "%" -> {
                val (num1, num2) = getTwoNums(numStack, op)
                if (num2 == 0) throw ArithmeticException("取模除零错误")
                num1 % num2
            }

            "^" -> {
                val (num1, num2) = getTwoNums(numStack, op)
                num1 xor num2
            }

            // 新增位运算
            "<<" -> {
                val (num1, num2) = getTwoNums(numStack, op)
                num1 shl num2
            }

            ">>" -> {
                val (num1, num2) = getTwoNums(numStack, op)
                num1 shr num2
            }

            "&" -> {
                val (num1, num2) = getTwoNums(numStack, op)
                num1 and num2
            }

            "|" -> {
                val (num1, num2) = getTwoNums(numStack, op)
                num1 or num2
            }

            // 幂运算
            "**" -> {
                val (num1, num2) = getTwoNums(numStack, op)
                if (num2 < 0) throw ArithmeticException("幂运算不支持负指数")
                var result = 1
                repeat(num2) { result *= num1 }
                result
            }

            // 单目!/-已在识别阶段计算，此处无需处理
            else -> throw IllegalArgumentException("不支持的运算符：$op")
        }
    }

    /**
     * 安全获取两个操作数
     */
    private fun getTwoNums(numStack: Stack<Int>, op: String): Pair<Int, Int> {
        if (numStack.size < 2) throw IllegalArgumentException("运算符 $op 缺少操作数")
        val num2 = numStack.pop()
        val num1 = numStack.pop()
        return Pair(num1, num2)
    }
}