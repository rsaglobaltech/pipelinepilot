plugins {
    kotlin("jvm")
}

dependencies {
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
}

kotlin {
    jvmToolchain(21)
}
