plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.FinalProject.mainActivity"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.FinalProject.mainActivity"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
        // ... other configurations like compileSdk, defaultConfig, etc.

    packaging {
        resources {
            excludes += "/META-INF/INDEX.LIST"
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
        }
    }// ... other configurations like buildTypes, compileOptions, etc.

}

dependencies {
    implementation(project(":feature_auth"))
    implementation(project(":feature_home_attendee"))
    implementation(project(":core"))
    implementation(project(":feature_home_organizer"))


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
}
