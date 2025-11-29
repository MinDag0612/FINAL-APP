plugins { id("com.android.library") }

android {
    namespace = "com.FinalProject.feature_booking"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { viewBinding = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":core"))
    implementation(project(":core"))
    // AndroidX cơ bản
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.fragment:fragment:1.8.3")
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.gridlayout:gridlayout:1.0.0")

    // Material 3
    implementation("com.google.android.material:material:1.12.0")

    // Navigation - chọn 1 phiên bản duy nhất (Java-friendly, non-KTX)
    implementation("androidx.navigation:navigation-fragment:2.8.3")
    implementation("androidx.navigation:navigation-ui:2.8.3")

    // Firebase (BOM + modules)
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.navigation.ui)

    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
