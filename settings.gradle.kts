dependencyResolutionManagement {
    repositories {
        maven("https://maven.msdnicrosoft.work/velocity/") {
            content {
                includeGroup("com.velocitypowered")
            }
        }
        mavenCentral()
        maven {
            name = "elytrium-repo"
            url = uri("https://maven.elytrium.net/repo/")
        }
        maven {
            name = "papermc-repo"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "limboapi"

include("api")
include("plugin")
