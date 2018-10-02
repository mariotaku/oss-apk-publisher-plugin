package org.mariotaku.osspublisherplugin

import org.junit.Test

class SignatureTest {
    @Test
    void testGenerate() {
        def signature = OssPublishPlugin.generateSignature("OtxrzxIsfpFjA7SwPzILwy8Bw21TLhquhboDYROV", "PUT",
                "ODBGOERFMDMzQTczRUY3NUE3NzA5QzdFNUYzMDQxNEM=", "text/html", "Thu, 17 Nov 2005 18:49:58 GMT",
                [Tuple2("X-OSS-Meta-Author", "foo@bar.com"), Tuple2("X-OSS-Magic", "abracadabra")], "/oss-example/nelson")
        Assert.assertEquals("26NBxoKdsyly4EDv6inkoDft/yA=", signature)
    }
}