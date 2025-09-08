plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.aieyes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aieyes"
        minSdk = 27
        targetSdk = 35
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
}

dependencies {

    // AndroidX Core & UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    implementation(project(":navigation"))

    implementation("androidx.camera:camera-core:1.2.3")
    implementation("androidx.camera:camera-camera2:1.2.3")
    implementation("androidx.camera:camera-lifecycle:1.2.3")
    implementation("androidx.camera:camera-view:1.2.3")
    implementation("androidx.camera:camera-extensions:1.2.3")


    // // TensorFlow Lite Task & Support
    // implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.0")
    // implementation("org.tensorflow:tensorflow-lite-support:0.4.0")

    // // Gson
    // implementation("com.google.code.gson:gson:2.10.1")

    // // Retrofit & OkHttp Logging
    // implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // implementation("com.squareup.retrofit2:converter-gson:2.9.0")

//=======
    // Networking
    implementation("com.google.code.gson:gson:2.10.1")
    // HTTP 통신을 위한 OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.guava:guava:31.1-android")

    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Testing
//>>>>>>> 0060d0bdbd5eae8bcae7ef7248093c7299ac94e8
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}