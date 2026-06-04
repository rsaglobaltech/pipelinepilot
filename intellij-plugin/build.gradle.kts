import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij.platform")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Build against a locally installed IDE when -PlocalIdePath is given
        // (exact binary match, no SDK download); otherwise download by type/version.
        val localIdePath = providers.gradleProperty("localIdePath")
        if (localIdePath.isPresent) {
            local(localIdePath)
        } else {
            create(
                type = providers.gradleProperty("platformType").orElse("IC"),
                version = providers.gradleProperty("platformVersion").orElse("2024.2"),
            )
        }
        // Reuse the bundled Groovy support for PSI / highlighting of Jenkinsfiles.
        bundledPlugin("org.intellij.groovy")
        testFramework(TestFrameworkType.Platform)
    }

    implementation(project(":common-api"))
    // Bundle okhttp into the plugin (NOT provided by the plugin classloader at runtime).
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // jackson-module-kotlin is bundled transitively via :common-api (api dependency).
    compileOnly("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.pipelinepilot"
        name = "PipelinePilot for Jenkins"
        version = project.version.toString()

        ideaVersion {
            sinceBuild = "261"
            untilBuild = provider { null }
        }
    }

    // Optional but recommended by JetBrains: sign the plugin before publishing.
    // Provide the cert/key/password via env vars (generated per the docs:
    // https://plugins.jetbrains.com/docs/intellij/plugin-signing.html).
    signing {
        certificateChainFile = providers.environmentVariable("CERTIFICATE_CHAIN").map { file(it) }
        privateKeyFile = providers.environmentVariable("PRIVATE_KEY").map { file(it) }
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    // `./gradlew publishPlugin` uploads to the Marketplace using your permanent token
    // (Marketplace ▸ My Profile ▸ My Tokens). Set JETBRAINS_MARKETPLACE_TOKEN first.
    publishing {
        token = providers.environmentVariable("JETBRAINS_MARKETPLACE_TOKEN")
        // channels = listOf("beta")  // uncomment to publish to a pre-release channel
    }
}

kotlin {
    jvmToolchain(21)
}
