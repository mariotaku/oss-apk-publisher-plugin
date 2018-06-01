package org.mariotaku.osspublisherplugin

import org.junit.Assert
import org.junit.Test

class SignatureTest {
    @Test
    fun testSignature() {
        val signature = OssSignature.generate("OtxrzxIsfpFjA7SwPzILwy8Bw21TLhquhboDYROV", "PUT",
                "ODBGOERFMDMzQTczRUY3NUE3NzA5QzdFNUYzMDQxNEM=", "text/html", "Thu, 17 Nov 2005 18:49:58 GMT",
                listOf("X-OSS-Meta-Author" to "foo@bar.com", "X-OSS-Magic" to "abracadabra"), "/oss-example/nelson")
        Assert.assertEquals("26NBxoKdsyly4EDv6inkoDft/yA=", signature)
    }
}
