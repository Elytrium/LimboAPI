package net.elytrium.gradle.plugin.revision

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

class CurrentRevisionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create(
            "currentShortRevisionProvider",
            CurrentRevisionExtensionProvider::class
        )
    }
}