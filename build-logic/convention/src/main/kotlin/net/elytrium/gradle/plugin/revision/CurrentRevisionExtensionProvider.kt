package net.elytrium.gradle.plugin.revision

open class CurrentRevisionExtensionProvider {
    fun getCurrentShortRevision(): String {
        val isWindows = System.getProperty("os.name")
            .lowercase()
            .contains("win")

        val process = if (isWindows) {
            ProcessBuilder("cmd", "/c", "git rev-parse --short HEAD")
        } else {
            ProcessBuilder("bash", "-c", "git rev-parse --short HEAD")
        }
        return process
            .start()
            .inputStream
            .bufferedReader()
            .readText()
            .trim()
    }
}