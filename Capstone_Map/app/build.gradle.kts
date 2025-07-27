plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "1.9.0"

}


android {
    namespace = "com.example.capstone_map"
    compileSdk = 34


    sourceSets {
        getByName("main").java.srcDirs("src/main/java")
    }

    defaultConfig {
        applicationId = "com.example.capstone_map"
        minSdk = 27
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {


    implementation ("com.google.code.gson:gson:2.13.1") // GSon 라이브러리 추가   JSON파싱 라이브러리
    // ViewModel + LiveData + lifecycle
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation ("androidx.activity:activity-ktx:1.7.2") // ← 이것이 핵심!

    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(files("libs\\com.skt.Tmap_1.76.jar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}