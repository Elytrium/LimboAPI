package net.elytrium.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class ModuleInfoPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.version = "1.1.27-SNAPSHOT"
        target.group = "net.elytrium.limboapi"
    }
}