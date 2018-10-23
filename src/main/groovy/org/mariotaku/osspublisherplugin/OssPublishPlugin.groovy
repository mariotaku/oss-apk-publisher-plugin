package org.mariotaku.osspublisherplugin

import com.android.build.gradle.api.BaseVariantOutput
import okhttp3.*
import okio.ByteString
import okio.HashingSink
import okio.Okio
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.text.SimpleDateFormat

class OssPublishPlugin implements Plugin<Project> {

    static SimpleDateFormat rfc2616Format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).with {
        timeZone = TimeZone.getTimeZone("GMT")
        return it
    }

    @Override
    void apply(Project project) {
        if (!project.hasProperty("android")) {
            throw IllegalArgumentException("Project ${project.name} is not an Android project")
        }
        def config = project.extensions.create("ossPublish", OssPublisherExtensions)
        project.afterEvaluate { p ->
            p.android.applicationVariants.forEach { variant ->
                BaseVariantOutput output = variant.outputs.first()

                // Bundle task name for variant
                def ossPublishTaskName = "ossPublish${variant.name.capitalize()}"

                p.task(ossPublishTaskName) {
                    group = "oss-publish"
                    description = "Publish ${variant.name} apk to oss."

                    doLast {
                        try {
                            putObject(config, output.outputFile, apkKey(config, output.outputFile))
                        } catch (e) {
                            it.logger.error("Failed to upload APK", e)
                        }

                        def mappingFile = variant.mappingFile
                        if (config.uploadMapping && mappingFile.exists()) {
                            try {
                                putObject(config, mappingFile, mappingKey(config, mappingFile))
                            } catch (e) {
                                it.logger.error("Failed to upload mapping", e)
                            }
                        }
                    }

                    dependsOn(variant.assemble)
                }
            }
        }
    }


    static String apkKey(OssPublisherExtensions config, File file) {
        def uploadName = config.overrideKey
        if (uploadName != null) return uploadName
        def prefix = config.keyPrefix ?: ""
        def suffix = config.keySuffix ?: ""
        return "$prefix${nameWithoutExtension(file)}$suffix.${extension(file)}"
    }

    static String mappingKey(OssPublisherExtensions config, File file) {
        def uploadName = config.overrideMappingKey
        if (uploadName != null) return uploadName
        def prefix = config.keyPrefix ?: ""
        def suffix = config.keySuffix ?: ""
        return "${prefix}mapping-${nameWithoutExtension(file)}$uploadName$suffix.txt"
    }

    static String mediaType(File file) {
        switch (extension(file).toLowerCase(Locale.US)) {
            case "apk": return "application/vnd.android.package-archive"
            case "txt": return "text/plain"
            default: return "application/octet-stream"
        }
    }

    static String extension(File file) {
        def index = file.name.lastIndexOf('.')
        if (index < 0) return ''
        return file.name.substring(index + 1)
    }

    static String nameWithoutExtension(File file) {
        def index = file.name.lastIndexOf('.')
        if (index < 0) return file.name
        return file.name.substring(0, index)
    }

    static ByteString md5(RequestBody body) {
        def sink = HashingSink.md5(Okio.blackhole())
        def buffer = Okio.buffer(sink)
        body.writeTo(buffer)
        buffer.flush()
        return sink.hash()
    }

    static void putObject(OssPublisherExtensions config, File file, String key) {
        HttpUrl endpointUrl = HttpUrl.parse(config.endpoint)
        HttpUrl url = endpointUrl.newBuilder().host("${config.bucket}.${endpointUrl.host()}").addPathSegments(key).build()
        OkHttpClient client = new OkHttpClient()
        RequestBody body = RequestBody.create(MediaType.parse(mediaType(file)), file)
        String date = rfc2616Format.format(new Date())
        String md5 = md5(body).base64()
        String signature = generateSignature(config.keySecret, "PUT", md5, body.contentType(),
                date, [], "/${config.bucket}/$key")

        Request request = new Request.Builder().url(url).method("PUT", body)
                .header("Host", "${config.bucket}.oss-cn-hangzhou.aliyuncs.com")
                .header("Content-MD5", md5)
                .header("Date", date)
                .header("Authorization", "OSS ${config.keyId}:$signature")
                .build()

        client.newCall(request).execute().withCloseable {
            if (!it.successful) throw IOException("HTTP ${it.code()}")
        }
    }

    static String generateSignature(String secret, String verb, String contentMd5, MediaType contentType,
                                    String date, List<Tuple2<String, String>> ossHeaders, String ossResource) {
        def sigKey = ByteString.encodeUtf8(secret)
        def msgBuilder = new StringBuilder(verb)
        msgBuilder.append('\n')
        msgBuilder.append(contentMd5)
        msgBuilder.append('\n')
        msgBuilder.append(contentType)
        msgBuilder.append('\n')
        msgBuilder.append(date)
        ossHeaders.toSorted { a, b -> (a.first <=> b.first) }.forEach { k, v ->
            msgBuilder.append('\n')
            msgBuilder.append("${k.toLowerCase(Locale.US)}:$v")
        }
        msgBuilder.append('\n')
        msgBuilder.append(ossResource)

        return ByteString.encodeUtf8(msgBuilder.toString()).hmacSha1(sigKey).base64()
    }

}