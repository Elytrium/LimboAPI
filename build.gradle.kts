import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsTask

plugins {
    java
    checkstyle
    alias(libs.plugins.gradle.spotbugs) apply false
    alias(libs.plugins.gradle.licenser) apply false
    id("net.elytrium.java.version") apply false
    id("net.elytrium.module.info") apply false
    id("net.elytrium.revision") apply false
    id("net.elytrium.spotbugs") apply false
    id("net.elytrium.checkstyle") apply false
    id("net.elytrium.licence") apply false
}


