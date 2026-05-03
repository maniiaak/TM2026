rootProject.name = "KMP-App-Template"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
                maven("https://cache-redirector.jetbrains.com/maven-central")
                maven("https://www.jetbrains.com/intellij-repository/releases")
            }
        }
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }
}

include(":composeApp")
