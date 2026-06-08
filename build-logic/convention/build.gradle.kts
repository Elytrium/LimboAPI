plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.gradle.spotbugs)
    implementation(libs.gradle.licenser)
}

gradlePlugin {
    plugins {
        create("net.elytrium.java.version") {
            id = name
            implementationClass = "net.elytrium.gradle.plugin.JavaVersionPlugin"
        }

        create("net.elytrium.module.info") {
            id = name
            implementationClass = "net.elytrium.gradle.plugin.ModuleInfoPlugin"
        }
        create("net.elytrium.revision") {
            id = name
            implementationClass = "net.elytrium.gradle.plugin.revision.CurrentRevisionPlugin"
        }
        create("net.elytrium.spotbugs") {
            id = name
            implementationClass = "net.elytrium.gradle.plugin.SpotBugsConfigurationPlugin"
        }
        create("net.elytrium.checkstyle") {
            id = name
            implementationClass = "net.elytrium.gradle.plugin.CheckstyleConfigurationPlugin"
        }
        create("net.elytrium.licence") {
            id = name
            implementationClass = "net.elytrium.gradle.plugin.LicenceConfigurationPlugin"
        }
    }
}
