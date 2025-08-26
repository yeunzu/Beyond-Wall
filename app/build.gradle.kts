plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

android {
    namespace = "com.example.newpractice_jetpack_compose"
    compileSdk = 35 // 기존 36

    defaultConfig {
        applicationId = "com.example.newpractice_jetpack_compose"
        minSdk = 29
        targetSdk = 35 // 기존 36
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
        sourceCompatibility = JavaVersion.VERSION_17 // 원랜 VERSION_11
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17" // 원랜 11
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.navigation:navigation-compose:2.7.0") // dependency for crazy navigation
    implementation("androidx.appcompat:appcompat:1.7.1") // dependency for permission management
    implementation("androidx.appcompat:appcompat-resources:1.7.1") // dependency for permission management
    implementation("androidx.datastore:datastore-preferences:1.1.7") // DataStore Preferences
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2") // 코루틴 스코프 (ViewModel에서 사용 시)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.room:room-runtime:2.7.2") // room db
    kapt("androidx.room:room-compiler:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")


    // --- Hilt 의존성 추가 시작 ---
    val hiltVersion = "2.51.1"
    implementation("com.google.dagger:hilt-android:$hiltVersion") // Hilt 코어 라이브러리
    kapt("com.google.dagger:hilt-android-compiler:$hiltVersion") // Hilt 어노테이션 프로세서

    // Jetpack Compose에서 Hilt를 사용한다면 다음 의존성도 필요합니다.
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    // --- Hilt 의존성 추가 끝 ---

    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("androidx.compose.material:material-icons-extended:1.7.8") // 전체 Material 아이콘 세트를 사용하기 위한 의존성

    // kotlinx-serialization 라이브러리
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}


kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}