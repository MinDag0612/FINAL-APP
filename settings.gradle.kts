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
    }
}

rootProject.name = "final_app"
include(":app")
include(":core")
include(":feature_auth")
include(":feature_home_attendee")
include(":feature_event_detail")
include(":feature_profile")
include(":feature_review_event")
include(":feature_home_organizer")
