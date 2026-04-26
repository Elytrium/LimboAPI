package net.elytrium.gradle.plugin

import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

class SpotBugsConfigurationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.plugins.apply("com.github.spotbugs")

        target.pluginManager.withPlugin("com.github.spotbugs") {
            target.extensions.configure<SpotBugsExtension> {
                val suppressionsFile = target.file("${target.rootDir}/config/spotbugs/suppressions.xml")
                excludeFilter.set(suppressionsFile)
            }

            target.tasks.withType<SpotBugsTask> {
                reports.create("html") {
                    required.set(true)
                    val outputFile = target.layout.buildDirectory.file("reports/spotbugs/main/spotbugs.html")
                    outputLocation.set(outputFile)
                    setStylesheet("fancy-hist.xsl")
                }
            }
        }
    }
}