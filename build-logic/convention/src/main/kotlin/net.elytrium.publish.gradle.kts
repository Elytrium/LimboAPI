plugins {
    id("java")
    id("java-library")
    id("maven-publish")
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