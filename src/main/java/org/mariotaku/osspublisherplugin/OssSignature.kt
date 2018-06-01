package org.mariotaku.osspublisherplugin

import okio.ByteString
import java.util.*

object OssSignature {

    fun generate(secret: String, verb: String, contentMd5: String, contentType: String,
                 date: String, ossHeaders: List<Pair<String, String>>, ossResource: String): String {
        val sigKey = ByteString.encodeUtf8(secret)
        val msgBuilder = StringBuilder(verb)
        msgBuilder.append('\n')
        msgBuilder.append(contentMd5)
        msgBuilder.append('\n')
        msgBuilder.append(contentType)
        msgBuilder.append('\n')
        msgBuilder.append(date)
        ossHeaders.sortedBy { it.first }.forEach { (k, v) ->
            msgBuilder.append('\n')
            msgBuilder.append("${k.toLowerCase(Locale.US)}:$v")
        }
        msgBuilder.append('\n')
        msgBuilder.append(ossResource)

        return ByteString.encodeUtf8(msgBuilder.toString()).hmacSha1(sigKey).base64()
    }

}
