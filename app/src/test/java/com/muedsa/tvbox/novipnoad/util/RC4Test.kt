package com.muedsa.tvbox.novipnoad.util

import com.muedsa.tvbox.novipnoad.service.MediaDetailService.Companion.decodeBase64Like
import com.muedsa.tvbox.tool.decodeBase64
import org.junit.Test
import kotlin.io.encoding.ExperimentalEncodingApi

class RC4Test {

    @Test
    fun test() {
        val data = "nC/RNmH+kvgUk/EdkdJYxSH6n1b4PnmWIRhNEiLBS42MNt6/SLBD+VaRn+w8pC/u2hpuRjaGeEDp04so1Z0gk+XTMQwxx1NnOfLBdrpCZoHSWiTOz6RusiNu/dJn8nKr3zeTMQUoqou3bUoxvdMoWvweNF1D/Yw4Q96K+HuJisA4pZYxxQd0WatRqyEsIK5ExBxxGTWVIkJjFYbr90o0F5tAw/MFhr9KlC4dDHLOygau1V715Px5gf2CGFbKIP/xjSrhXo8OG+vrcWYhi9wODAWig2oF3PoBH+ACF1hDlS4PeU/nxW0Vw8JPkhv8F3sT391idBQt8bFcgplTMIWgw+66Tn3KfLLx87LZk2hBrO6687zFxTrOi0U9Fjk7Flyrf0AApMwu8w=="
        val text = RC4("e11ed29b".toByteArray(Charsets.ISO_8859_1))
            .decrypt(data.decodeBase64())
            .toString(Charsets.ISO_8859_1)
        println(text)
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)
    @Test
    fun test2() {
        val data = "FCkdhCoyx8oIW5uU".decodeBase64Like().toString(Charsets.UTF_8)
        val text = RC4("smXA".toByteArray(Charsets.ISO_8859_1))
            .decrypt(data.toByteArray(Charsets.ISO_8859_1))
            .toString(Charsets.ISO_8859_1)
        println(text)
        assert(text == "e11ed29b")
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)
    @Test
    fun test3() {
        val dataArr = listOf(
            "null", "FgTjrsjibdaOmIWi.rDPcMbIolTpmPK.JuvY7MHA==", "tfBdMH5BWQfKW5LYW5SdsCkp", "W77cKCoT", "CdnshHyFDCk8W6pdVmk3", "DCkUxL/cKSkecaFdPgJcRa", "eSkAnxXkW6RdGSk6", "W7K2WRHXWRm4dc0itq", "FCo2tSo8xa", "frm2WOZcKSkq", "FCkdhCoyx8oIW5uU", "EIRcICkrWPhdPNNcIHXBWObs", "zcBdQ8ouWPC", "W6agsSoFWQ9qW6TxWOJdNCogWRC", "DCkRwflcNCo+jIddI03cMHe", "sXCRWQzHfhhcRMHSl8k8", "gahcMKSc", "gSkiWOGLWRpcUSkVW6K", "rCokjMXJW77dM8k7CmkoW54ymIy", "DCkUnshdKCk0hZ0", "W5JdGbDfcSkpwCk0", "EtLvxCk2W6f6u2vPm8oc", "pCkFpsaekNBcLbPSW5xdPG", "gmkoW6LwW7xdUSkUW6C6hrKX", "mwatwuu", "CmonWRVdGZP0oW4", "nSo0W61vuq", "FIBdOSkIBSo0pW", "W6CSWQzKWQZdHmonEvBdN8oUkG", "WQNdICk/gCk+W5RdT8o0iSkQ", "W6hcIHGBtCkE", "qmkGW7WNW7FcQ8oXaa", "iMiFea", "cSoJgmkDDmkWbIW", "se5QW5RdLmojymkPWROUW6xdNSo+", "sbGOWQXUfNhcNMXhpCkL", "iCkdxCkUWRBcJZ/dMq", "E21BfCkrWQpdRq", "DciaB8ojW4FcJCoctmoLd8oFjq"
        )
        dataArr.forEachIndexed { index, item ->
            val data = item.decodeBase64Like().toString(Charsets.UTF_8)
            val text = RC4("smXA".toByteArray(Charsets.ISO_8859_1))
                .decrypt(data.toByteArray(Charsets.ISO_8859_1))
                .toString(Charsets.ISO_8859_1)
            if (text == "e11ed29b") {
                println("$index: $item")
            }
        }
    }

}