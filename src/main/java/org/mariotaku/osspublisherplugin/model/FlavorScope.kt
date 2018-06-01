package org.mariotaku.osspublisherplugin.model

import com.android.build.gradle.AndroidConfig
import org.mariotaku.osspublisherplugin.combinations
import java.util.*

data class FlavorScope(val flavors: List<String> = listOf("")) {
    val camelCaseName: String
        get() = flavors.mapIndexed { index, s ->
            return@mapIndexed if (index > 0) s.capitalize() else s.decapitalize()
        }.joinToString("")

    fun camelCaseName(buildTypeName: String, prefix: String = "", suffix: String = ""): String {
        val segs = LinkedList<String>()
        if (prefix.isNotEmpty()) {
            segs += prefix
        }
        segs += flavors
        segs += buildTypeName
        if (suffix.isNotEmpty()) {
            segs += suffix
        }
        return segs.mapIndexed { index, s ->
            if (index > 0) s.capitalize() else s.decapitalize()
        }.joinToString("")
    }

    fun snakeCaseName(buildTypeName: String, prefix: String = "", suffix: String = ""): String {
        val segs = LinkedList<String>()
        if (prefix.isNotEmpty()) {
            segs += prefix
        }
        segs += flavors
        segs += buildTypeName
        if (suffix.isNotEmpty()) {
            segs += suffix
        }
        return segs.filter { it.isNotEmpty() }.mapIndexed { index, s ->
            s.decapitalize()
        }.joinToString("-")
    }

    fun isEmpty(): Boolean {
        if (flavors.isEmpty()) return true
        val single = flavors.singleOrNull() ?: return false
        return single.isEmpty()
    }

    companion object {

        fun list(config: AndroidConfig): List<FlavorScope> {
            val dimensions = config.flavorDimensionList?.takeIf(Collection<*>::isNotEmpty)
                    ?: return listOf(FlavorScope())
            val flavors = config.productFlavors?.takeIf(Collection<*>::isNotEmpty)
                    ?: return listOf(FlavorScope())
            return dimensions.map { dimension ->
                flavors.filter { flavor ->
                    flavor.dimension == dimension
                }.map { flavor ->
                    flavor.name
                }
            }.combinations().map(::FlavorScope)
        }

        fun noVariants(list: List<FlavorScope>): Boolean {
            if (list.isEmpty()) return true
            val single = list.singleOrNull() ?: return false
            return single.isEmpty()
        }

    }
}