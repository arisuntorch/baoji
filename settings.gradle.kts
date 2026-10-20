pluginManagement {
    repositories {
        // Huawei mirrors both Maven Central and Google; official repos remain as fallback.
        maven("https://repo.huaweicloud.com/repository/maven/")
        google {
            content {
                includeGroupByRegex("com[.]android.*")
                includeGroupByRegex("com[.]google.*")
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
        maven("https://repo.huaweicloud.com/repository/maven/")
        google {
            content {
                includeGroupByRegex("com[.]android.*")
                includeGroupByRegex("com[.]google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}
rootProject.name = "baoji"
include(":app")
