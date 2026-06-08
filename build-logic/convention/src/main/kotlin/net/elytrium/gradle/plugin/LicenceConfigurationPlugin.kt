package net.elytrium.gradle.plugin

import net.minecraftforge.licenser.LicenseExtension
import net.minecraftforge.licenser.LicenseProperties
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.closureOf
import org.gradle.kotlin.dsl.configure
import java.io.File

class LicenceConfigurationPlugin : Plugin<Project> {

    private fun getHeaderFile(target: Project): File {
        return target.file("HEADER.txt")
            .takeIf(File::exists)
            ?: target.rootProject.file("HEADER.txt")
    }

    override fun apply(target: Project) {
        target.pluginManager.withPlugin("net.minecraftforge.licenser") {
            target.extensions.configure<LicenseExtension> {
                mapOf(
                    "**/mcprotocollib/**" to "HEADER_MCPROTOCOLLIB.txt",
                    "**/LoginListener.java" to "HEADER_MIXED.txt",
                    "**/KickListener.java" to "HEADER_MIXED.txt",
                    "**/LoginTasksQueue.java" to "HEADER_MIXED.txt",
                    "**/MinecraftLimitedCompressDecoder.java" to "HEADER_MIXED.txt"
                ).forEach { (pattern, licenceFile) ->
                    matching(
                        pattern,
                        closureOf<LicenseProperties> {
                            setHeader(target.rootProject.file(licenceFile))
                        }
                    )
                }
                setHeader(getHeaderFile(target))
            }

        }
    }
}