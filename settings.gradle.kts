pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "9.0.1"
        id("com.android.library") version "9.0.1"
        id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
        id("com.google.dagger.hilt.android") version "2.59.2"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
        id("com.google.devtools.ksp") version "2.1.20-1.0.31"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ChatAgent"
include(":app")
include(":backdrop")
