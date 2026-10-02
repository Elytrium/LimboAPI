dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "papermc-repo"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            name = "local-repo"
            url = uri("${rootDir}/local-repo")
        }
        maven {
            name = "elytrium-repo"
            url = uri("https://maven.elytrium.net/repo/")
            content {
                includeGroupByRegex("net\\.elytrium.*")
            }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "limboapi"

include("api")
include("plugin")
