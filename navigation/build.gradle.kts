plugins {
    id("com.android.library")
    // ⚠️ 참고: Kotlin 버전을 "2.2.0"에서 "1.9.0" 또는 "1.8.10"처럼
    // app 모듈과 동일한 버전으로 맞추는 것을 강력히 권장합니다.
    id("org.jetbrains.kotlin.android") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
}

android {
    namespace = "com.example.capstone_map"
    compileSdk = 34

    defaultConfig {
        minSdk = 27
        consumerProguardFiles("consumer-rules.pro")
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
    buildFeatures {
        viewBinding = true
    }

    // TFLite 모델 파일(.tflite)을 압축하지 않도록 설정
    aaptOptions {
        noCompress.add("tflite")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // === 기존 navigation 모듈 의존성 ===
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    api(files("libs/com.skt.Tmap_1.76.jar"))

    // --- ▼ ObstacleDetectionFragment가 필요로 하는 의존성 추가 ▼ ---

    // CameraX (view는 이미 있음)
    // (참고: 버전은 app 모듈에서 사용하던 버전과 통일하는 것이 좋습니다.)
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation(libs.androidx.camera.view) // (이 줄은 이미 있었습니다)

    // TensorFlow Lite (Task Vision)
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")
    // TensorFlow Lite (Support)
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Retrofit & OkHttp Logging
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3") // okhttp와 버전 맞춤

    // --- ▲ 의존성 추가 끝 ▲ ---

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
