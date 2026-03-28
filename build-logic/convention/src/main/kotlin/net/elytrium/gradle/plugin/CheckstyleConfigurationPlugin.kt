package net.elytrium.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.kotlin.dsl.configure

class CheckstyleConfigurationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("checkstyle")
        target.pluginManager.withPlugin("checkstyle") {
            target.configure<CheckstyleExtension> {
                toolVersion = "10.12.1"
                configFile = target.file("${target.rootDir}/config/checkstyle/checkstyle.xml")
                configProperties = mapOf("configDirectory" to "${target.rootDir}/config/checkstyle")
                maxErrors = 0
                maxWarnings = 0
            }
        }
    }
}