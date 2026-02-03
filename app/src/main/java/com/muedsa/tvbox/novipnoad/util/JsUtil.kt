package com.muedsa.tvbox.novipnoad.util

import com.muedsa.js.lexer.Lexer
import com.muedsa.js.parser.Parser
import com.muedsa.js.runtime.Interpreter
import com.muedsa.js.runtime.VariableKind
import com.muedsa.js.runtime.initBrowserEnv
import com.muedsa.js.runtime.value.JSBoolean
import com.muedsa.js.runtime.value.JSNativeFunction
import com.muedsa.js.runtime.value.JSNumber
import com.muedsa.js.runtime.value.JSObject
import com.muedsa.js.runtime.value.JSString
import com.muedsa.js.runtime.value.JSUndefined
import com.muedsa.js.runtime.value.JSValue

object JsUtil {

    val nothingFun = JSNativeFunction(name = "") { _, _, _ ->
        JSUndefined
    }
    val emptyObject = JSObject()

    fun createRuntime(
        evalHandel: (String) -> Unit,
    ): Interpreter {
        return Interpreter()
            .initBrowserEnv()
            .also {
                val winObj = it.getGlobalValue("window") as JSObject
                val eval = JSNativeFunction(name = "eval") { interpreter, _, args ->
                    val code = interpreter.getPrimitiveString(args[0])
                    evalHandel(code)
                    JSUndefined
                }
                val functionConstructor =
                    JSNativeFunction(name = "Function") { interpreter, _, args ->
                        val code = interpreter.getPrimitiveString(args[0])
                        JSNativeFunction(name = "eval") { _, _, _ ->
                            evalHandel(code)
                            JSUndefined
                        }
                    }
                val canvas2dContext = JSObject(
                    properties = mutableMapOf(
                        "direction" to JSString("ltr"),
                        "fillStyle" to JSString.EmptyString,
                        "filter" to JSString.EmptyString,
                        "font" to JSString.EmptyString,
                        "fontKerning" to JSString.EmptyString,
                        "fontStretch" to JSString.EmptyString,
                        "fontVariantCaps" to JSString("normal"),
                        "globalAlpha" to JSNumber(1.0),
                        "globalCompositeOperation" to JSString.EmptyString,
                        "imageSmoothingEnabled" to JSBoolean.True,
                        "imageSmoothingQuality" to JSString("low"),
                        "letterSpacing" to JSString("0px"),
                        "lineCap" to JSString("butt"),
                        "lineDashOffset" to JSNumber(0.0),
                        "lineJoin" to JSString("miter"),
                        "lineWidth" to JSNumber(1.0),
                        "miterLimit" to JSNumber(10.0),
                        "shadowBlur" to JSNumber(0.0),
                        "shadowColor" to JSString("rgba(0, 0, 0, 0)"),
                        "shadowOffsetX" to JSNumber(0.0),
                        "shadowOffsetY" to JSNumber(0.0),
                        "strokeStyle" to JSString("#000000"),
                        "textAlign" to JSString("start"),
                        "textBaseline" to JSString("alphabetic"),
                        "textRendering" to JSString("auto"),
                        "wordSpacing" to JSString("0px"),

                        "arc" to nothingFun,
                        "arcTo" to nothingFun,
                        "beginPath" to nothingFun,
                        "bezierCurveTo" to nothingFun,
                        "clearRect=" to nothingFun,
                        "clip" to nothingFun,
                        "closePath" to nothingFun,
                        "createConicGradient" to nothingFun,
                        "createImageData" to nothingFun,
                        "createLinearGradient" to nothingFun,
                        "createPattern" to nothingFun,
                        "createRadialGradient" to nothingFun,
                        "drawFocusIfNeeded" to nothingFun,
                        "drawImage" to nothingFun,
                        "ellipse" to nothingFun,
                        "fill" to nothingFun,
                        "fillRect" to nothingFun,
                        "fillText" to nothingFun,
                        "getContextAttributes" to nothingFun,
                        "getImageData" to nothingFun,
                        "getLineDash" to nothingFun,
                        "getTransform" to nothingFun,
                        "isContextLost" to nothingFun,
                        "isPointInPath" to nothingFun,
                        "isPointInStroke" to nothingFun,
                        "lineTo" to nothingFun,
                        "measureText" to nothingFun,
                        "moveTo" to nothingFun,
                        "putImageData" to nothingFun,
                        "quadraticCurveTo" to nothingFun,
                        "rect" to nothingFun,
                        "reset" to nothingFun,
                        "resetTransform" to nothingFun,
                        "restore" to nothingFun,
                        "rotate" to nothingFun,
                        "roundRect" to nothingFun,
                        "save" to nothingFun,
                        "scale" to nothingFun,
                        "setLineDash" to nothingFun,
                        "setTransform" to nothingFun,
                        "stroke" to nothingFun,
                        "strokeRect" to nothingFun,
                        "strokeText" to nothingFun,
                        "transform" to nothingFun,
                        "translate" to nothingFun,
                    )
                )
                val canvasEl = JSObject(
                    properties = mutableMapOf(
                        "height" to JSNumber(1.0),
                        "width" to JSNumber(1.0),
                        "captureStream" to JSNativeFunction("captureStream") { _, _, _ ->
                            emptyObject
                        },
                        "getContext" to JSNativeFunction("getContext") { _, _, _ ->
                            canvas2dContext
                        },
                        "toBlob" to JSNativeFunction("toBlob") { _, _, _ ->
                            emptyObject
                        },
                        "toDataURL" to JSNativeFunction("toDataURL") { _, _, _ ->
                            emptyObject
                        },
                        "transferControlToOffscreen" to JSNativeFunction("transferControlToOffscreen") { _, _, _ ->
                            emptyObject
                        },
                    )
                )
                val document = JSObject(
                    properties = mutableMapOf(
                        "head" to JSObject(),
                        "body" to JSObject(),
                        "visibilityState" to JSString("visible"),
                        "createElement" to JSNativeFunction("createElement") { interpreter, _, args ->
                            val type =
                                interpreter.getPrimitiveString(args.getOrElse(0) { JSUndefined })
                            if (type == "canvas") canvasEl else emptyObject
                        }
                    )
                )
                val navigator = JSObject(
                    properties = mutableMapOf(
                        "appCodeName" to JSString("Mozilla"),
                        "appName" to JSString("Netscape"),
                        "appVersion" to JSString("5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"),
                        "userAgent" to JSString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36"),
                        "platform" to JSString("Win32"),
                        "product" to JSString("Gecko"),
                        "productSub" to JSString("20030107"),
                        "vendor" to JSString("Google Inc."),
                        "vendorSub" to JSString(""),
                    )
                )
                val performance = JSObject(
                    properties = mutableMapOf(
                        "timeOrigin" to JSNumber(System.currentTimeMillis().toDouble()),
                        "clearMarks" to nothingFun,
                        "clearMeasures" to nothingFun,
                        "mark" to nothingFun,
                        "measure" to nothingFun,
                        "now" to JSNativeFunction(name = "now") { interpreter, thisValue, _ ->
                            val thisObject = thisValue as JSObject
                            val diff = System.currentTimeMillis()
                                .toDouble() - interpreter.getPrimitiveNumber(
                                thisObject.getProperty(
                                    "timeOrigin"
                                )
                            )
                            JSNumber(diff)
                        },
                    )
                )
                winObj.setProperty("eval", eval)
                winObj.setProperty("document", document)
                winObj.setProperty("navigator", navigator)
                winObj.setProperty("performance", performance)
                winObj.setProperty("requestAnimationFrame", nothingFun)
                it.getGlobalEnv().define("eval", eval, VariableKind.CONST)
                it.getGlobalEnv().define("document", document, VariableKind.CONST)
                it.getGlobalEnv().define("navigator", navigator, VariableKind.CONST)
                it.getGlobalEnv().define("performance", performance, VariableKind.CONST)
                it.getGlobalEnv().define("Function", functionConstructor, VariableKind.CONST)
            }
    }

    fun exec(code: String, interpreter: Interpreter): JSValue {
        val tokens = Lexer(code).tokenize()
        val parser = Parser(tokens)
        val statements = parser.parse()
        return interpreter.interpret(statements)
    }
}