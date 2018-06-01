package org.mariotaku.osspublisherplugin

import com.android.build.gradle.AndroidConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskContainer
import org.mariotaku.osspublisherplugin.model.FlavorScope
import java.io.File

class OssPublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (!project.hasProperty("android")) {
            throw IllegalArgumentException("Project ${project.name} is not an Android project")
        }
        val config = project.extensions.create("ossPublish",
                OssPublisherExtensions::class.java, project.objects)
        project.afterEvaluate {
            setupTasks(it, config)
        }
    }

    private fun setupTasks(project: Project, config: OssPublisherExtensions) {
        val android = project.property("android") as AndroidConfig
        val buildTypeNames = android.buildTypes.map { type -> type.name }
        val buildVariants = FlavorScope.list(android)

        buildTypeNames.forEach { buildTypeName ->
            val typeTasks = buildVariants.map { buildVariant ->
                val targetName = buildVariant.camelCaseName(buildTypeName)

                val apkName = "${buildVariant.snakeCaseName(buildTypeName, project.name)}.apk"
                val apkPath = arrayOf(project.buildDir, "outputs", "apk", buildVariant.camelCaseName,
                        buildTypeName, apkName).joinToString(File.separator)
                val mappingPath = arrayOf(project.buildDir, "outputs", "mapping", buildVariant.camelCaseName,
                        buildTypeName, "mapping.txt").joinToString(File.separator)

                // Bundle task name for variant
                val ossPublishTaskName = buildVariant.camelCaseName(buildTypeName, "ossPublish")
                val assembleTaskName = buildVariant.camelCaseName(buildTypeName, "assemble")

                return@map project.tasks.create(ossPublishTaskName, OssPublishTask::class.java) {
                    it.group = "oss-publish"
                    it.description = "Publish $targetName apk to OSS."
                    it.config = config
                    it.apkFile = apkPath.fileIfExists()
                    it.mappingFile = mappingPath.fileIfExists()

                    it.dependsOn(assembleTaskName)
                }

            }
            if (!FlavorScope.noVariants(buildVariants)) {
                project.tasks.create("ossPublish${buildTypeName.capitalize()}") {
                    it.group = "oss-publish"
                    it.dependsOn(typeTasks)
                }
            }
        }
    }

    companion object {

        fun TaskContainer.injectDependency(path: String, dependsOn: Task) {
            findByPath(path)?.dependsOn(dependsOn)
        }

        fun String.fileIfExists(): File? {
            return File(this).takeIf { it.exists() }
        }

    }

}
