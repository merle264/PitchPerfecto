pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io") // Add JitPack repository for plugins
        maven("https://mvn.0110.be/releases") // Add TarsosDSP repository
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io") // Add JitPack repository for dependencies
        maven("https://mvn.0110.be/releases") // Add TarsosDSP repository
    }
}

rootProject.name = "KaraokeApp"
include(":app")
