plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.FinalProject.mainActivity"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.FinalProject.mainActivity"
        minSdk = 26
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
    implementation(project(":feature_booking"))
    implementation(project(":core"))
    implementation(project(":feature_home_organizer"))


    // AndroidX cơ bản (KHÔNG dùng compose ở đây để tránh yêu cầu plugin/feature)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment:1.8.3")
    implementation("androidx.activity:activity:1.9.2")

    // Navigation (Java-friendly, non-KTX)
    implementation("androidx.navigation:navigation-fragment:2.8.3")
    implementation("androidx.navigation:navigation-ui:2.8.3")

    // GridLayout AndroidX (cho app:layout_columnWeight trong fragment_seat_selection)
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation(libs.firebase.firestore)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

}
