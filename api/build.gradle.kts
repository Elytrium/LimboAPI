@file:Suppress("GroovyAssignabilityCheck")

import net.minecraftforge.licenser.LicenseExtension
import net.minecraftforge.licenser.LicenseProperties
import org.gradle.kotlin.dsl.closureOf

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.gradle.spotbugs)
    alias(libs.plugins.gradle.licenser)
    id("net.elytrium.java.version")
    id("net.elytrium.module.info")
    id("net.elytrium.revision")
    id("net.elytrium.spotbugs")
    id("net.elytrium.checkstyle")
    id("net.elytrium.licence")
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

tasks.named<Javadoc>("javadoc") {
    options.encoding = "UTF-8"
    (options as? StandardJavadocDocletOptions)?.apply {
        source = "21"
        links("https://docs.oracle.com/en/java/javase/11/docs/api/")
        addStringOption("Xdoclint:none", "-quiet")
        if (JavaVersion.current() >= JavaVersion.VERSION_1_9 && JavaVersion.current() < JavaVersion.VERSION_12) {
            addBooleanOption("-no-module-directories", true)
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

publishing {
    repositories {
        maven {
            credentials {
                username = System.getenv("ELYTRIUM_MAVEN_USERNAME")
                password = System.getenv("ELYTRIUM_MAVEN_PASSWORD")
            }
            name = "elytrium-repo"
            url = uri("https://maven.elytrium.net/repo/")
        }
    }

    publications.create<MavenPublication>("publication") {
        from(components["java"])

        artifact(javadocJar)
        artifact(sourcesJar)
    }
}

artifacts {
    archives(javadocJar)
    archives(sourcesJar)
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
