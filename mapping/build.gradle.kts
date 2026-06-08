@file:Suppress("UnstableApiUsage")

import net.minecraftforge.licenser.LicenseExtension
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.JavaExec

plugins {
    java
}

tasks.withType<JavaCompile> {
    options.release.set(21)
    options.encoding = "UTF-8"
}

dependencies {
    implementation(libs.tool.commons.io)
    implementation(libs.tool.google.guava)
    implementation(libs.tool.google.gson)

    compileOnly(libs.tool.spotbugs.annotations)
}

extensions.configure<LicenseExtension> {
    setHeader(rootProject.file("HEADER.txt"))
}

val generatedMappingsDir = layout.buildDirectory.dir("generated/resources/mapping")
val minecraftCacheDir = layout.buildDirectory.dir("minecraft")
val seedsDir = layout.projectDirectory.dir("src/main/seeds")
val manifestUrl = providers.gradleProperty("manifestUrl")
val cacheValidMillis = providers.gradleProperty("cacheValidMillis")
val gameVersion = providers.gradleProperty("gameVersion")

val generateMappings by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Generates Minecraft mapping resources for the plugin module."

    dependsOn(tasks.named("compileJava"))

    mainClass.set("net.elytrium.limboapi.mapping.MappingGeneratorMain")
    classpath = sourceSets.main.get().runtimeClasspath

    inputs.dir(seedsDir)
    inputs.property("manifestUrl", manifestUrl)
    inputs.property("cacheValidMillis", cacheValidMillis)
    inputs.property("gameVersion", gameVersion)
    inputs.property("cacheRefreshBucket", cacheValidMillis.map { System.currentTimeMillis() / it.toLong() })
    outputs.dir(generatedMappingsDir)

    doFirst {
        args = listOf(
            generatedMappingsDir.get().asFile.absolutePath,
            minecraftCacheDir.get().asFile.absolutePath,
            seedsDir.asFile.absolutePath,
            manifestUrl.get(),
            cacheValidMillis.get(),
        )
    }
}

val mappingResourcesJar by tasks.registering(Jar::class) {
    group = LifecycleBasePlugin.BUILD_GROUP
    description = "Packages generated mapping resources for consumption by the plugin module."

    archiveClassifier.set("resources")
    dependsOn(generateMappings)
    from(generatedMappingsDir)
}

val mappingResourcesElements by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false

    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

artifacts {
    add(mappingResourcesElements.name, mappingResourcesJar)
}

