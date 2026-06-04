plugins {
    kotlin("jvm") version "2.3.10" apply false
    id("org.jetbrains.intellij.platform") version "2.5.0" apply false
}

allprojects {
    group = "com.pipelinepilot"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}
