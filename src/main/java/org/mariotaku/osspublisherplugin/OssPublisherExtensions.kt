package org.mariotaku.osspublisherplugin

import org.gradle.api.internal.model.DefaultObjectFactory

open class OssPublisherExtensions(factory: DefaultObjectFactory) {

    var endpoint: String = "http://oss-cn-hangzhou.aliyuncs.com"
    var keyId: String = ""
    var keySecret: String = ""
    var bucket: String = ""

    var keyPrefix: String? = null
}
