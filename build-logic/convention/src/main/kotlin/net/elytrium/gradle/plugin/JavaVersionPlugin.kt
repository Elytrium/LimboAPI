package net.elytrium.gradle.plugin

import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType

class JavaVersionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.withPlugin("java") {
            target.tasks.withType<JavaCompile> {
                sourceCompatibility = JavaVersion.VERSION_21.toString()
                targetCompatibility = JavaVersion.VERSION_21.toString()
                options.release.set(21)
                options.encoding = "UTF-8"
            }
        }
    }
}