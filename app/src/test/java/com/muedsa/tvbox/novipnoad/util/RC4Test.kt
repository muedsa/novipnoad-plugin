package com.muedsa.tvbox.novipnoad.util

import com.muedsa.tvbox.tool.decodeBase64
import org.junit.Test

class RC4Test {

    @Test
    fun test() {
        val data = "nC/RNmH+kvgUk/EdkdJYxSH6n1b4PnmWIRhNEiLBS42MNt6/SLBD+VaRn+w8pC/u2hpuRjaGeEDp04so1Z0gk+XTMQwxx1NnOfLBdrpCZoHSWiTOz6RusiNu/dJn8nKr3zeTMQUoqou3bUoxvdMoWvweNF1D/Yw4Q96K+HuJisA4pZYxxQd0WatRqyEsIK5ExBxxGTWVIkJjFYbr90o0F5tAw/MFhr9KlC4dDHLOygau1V715Px5gf2CGFbKIP/xjSrhXo8OG+vrcWYhi9wODAWig2oF3PoBH+ACF1hDlS4PeU/nxW0Vw8JPkhv8F3sT391idBQt8bFcgplTMIWgw+66Tn3KfLLx87LZk2hBrO6687zFxTrOi0U9Fjk7Flyrf0AApMwu8w=="
        val text = RC4("e11ed29b".toByteArray(Charsets.ISO_8859_1))
            .decrypt(data.decodeBase64())
            .toString(Charsets.ISO_8859_1)
        println(text)
    }

}