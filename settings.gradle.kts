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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Dghiuri"
include(":app")

// Until io.github.meko123456:markdown-blocks:0.1.0 is on Maven Central, substitute the
// sibling checkout (CI clones it next to this repo). Delete this block once published.
val markdownBlocks = file("../markdown-blocks")
if (markdownBlocks.isDirectory) {
    includeBuild(markdownBlocks) {
        dependencySubstitution {
            substitute(module("io.github.meko123456:markdown-blocks")).using(project(":markdown"))
        }
    }
}
