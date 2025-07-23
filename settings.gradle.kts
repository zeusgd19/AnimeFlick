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
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url ="https://artifactory.appodeal.com/appodeal-public/")
        maven(url ="https://jcenter.bintray.com/")
        maven(url ="https://android-sdk.is.com")
        maven { url = uri("https://jitpack.io") }
        maven(url ="https://repo.gradle.org/gradle/libs-releases/")
        maven(url ="https://maven.scijava.org/content/repositories/public/")
    }
}

rootProject.name = "AnimeFlick"
include(":app")
 