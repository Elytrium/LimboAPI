@file:Suppress("GroovyAssignabilityCheck")

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.gradle.licenser)
    alias(libs.plugins.gradle.spotbugs)
    id("net.elytrium.checkstyle")
    id("net.elytrium.java.version")
    id("net.elytrium.licence")
    id("net.elytrium.module.info")
    id("net.elytrium.publish")
    id("net.elytrium.revision")
    id("net.elytrium.spotbugs")
}

dependencies {
    compileOnly(libs.minecraft.velocity.api)
    api(libs.elytrium.commons.config)
    api(libs.elytrium.commons.utils)
    api(libs.elytrium.commons.velocity)
    api(libs.elytrium.commons.kyori)
    api(libs.minecraft.adventure.nbt)

    compileOnly(libs.tool.spotbugs.annotations)
}

val versionStringProvider = provider {
    if (version.toString().contains("-")) {
        val currentShortRevision = currentShortRevisionProvider.getCurrentShortRevision()
        "${version} (git-${currentShortRevision})"
    } else {
        version.toString()
    }
}

val copyTask by tasks.register<Copy>("generateTemplates") {
    val versionString = versionStringProvider.get()
    inputs.property("version", versionString)
    from(file("src/main/templates"))
    into(layout.buildDirectory.dir("generated/sources/templates"))
    expand("version" to versionString)
}

sourceSets.main.configure {
    java.srcDir(copyTask.outputs)
}
