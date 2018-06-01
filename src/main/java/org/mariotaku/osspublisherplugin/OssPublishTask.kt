package org.mariotaku.osspublisherplugin

import okhttp3.*
import okio.ByteString
import okio.HashingSink
import okio.Okio
import org.gradle.api.DefaultTask
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

open class OssPublishTask : DefaultTask() {

    lateinit var config: OssPublisherExtensions
    var apkFile: File? = null
    var mappingFile: File? = null

    init {
        doLast {
            val apkFile = this.apkFile!!
            putObject(config.bucket, apkFile.key, apkFile)

            val mappingFile = this.mappingFile
            if (mappingFile != null) {
                putObject(config.bucket, mappingFile.key, mappingFile)
            }
        }
    }

    private val File.key: String
        get() {
            val prefix = config.keyPrefix ?: return name
            return "$prefix$name"
        }

    private val File.mediaType: MediaType
        get() = when (extension.toLowerCase(Locale.US)) {
            "apk" -> MediaType.parse("application/vnd.android.package-archive")!!
            "txt" -> MediaType.parse("text/plain")!!
            else -> MediaType.parse("application/octet-stream")!!
        }

    private fun putObject(bucket: String, key: String, file: File) {
        val endpointUrl = HttpUrl.parse(config.endpoint)!!
        val url = endpointUrl.newBuilder().host("${config.bucket}.${endpointUrl.host()}").addPathSegments(key).build()
        val client = OkHttpClient()
        val body = RequestBody.create(file.mediaType, file)
        val date = Date()
        val md5 = body.md5().base64()
        val signature = OssSignature.generate(config.keySecret, "PUT", md5, body.contentType().toString(),
                date.rfc2616(), emptyList(), "/$bucket/$key")

        val request = Request.Builder().url(url).method("PUT", body)
                .header("Host", "${config.bucket}.oss-cn-hangzhou.aliyuncs.com")
                .header("Content-MD5", md5)
                .header("Date", date.rfc2616())
                .header("Authorization", "OSS ${config.keyId}:$signature")
                .build()

        client.newCall(request).execute().use {
            if (!it.isSuccessful) throw IOException("HTTP ${it.code()}")
        }
    }


    private fun Date.rfc2616(): String {
        val calendar = Calendar.getInstance()
        calendar.time = this
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        return dateFormat.format(calendar.time)
    }


    private fun RequestBody.md5(): ByteString {
        val sink = HashingSink.md5(Okio.blackhole())
        val buffer = Okio.buffer(sink)
        writeTo(buffer)
        buffer.flush()
        return sink.hash()
    }
}
