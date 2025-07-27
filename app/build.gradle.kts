plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.protobuf") version "0.9.4"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.24.4"
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
                register("java") {}
            }
        }
    }
}

android {
    namespace = "com.zeusgd.AnimeFlick"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zeusgd.AnimeFlick"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-okhttp:2.3.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    implementation("androidx.compose.ui:ui:1.5.0")
    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.6.0")
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("androidx.datastore:datastore:1.0.0")
    implementation("androidx.datastore:datastore-core:1.0.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.30.1")
    implementation("com.google.protobuf:protobuf-java:3.25.1")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation(libs.androidx.material3.android)
    implementation(libs.ads.mobile.sdk)
    implementation(libs.androidx.appcompat)
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation(libs.volley)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation("org.testng:testng:6.9.6")
    kapt("androidx.room:room-compiler:2.5.2")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("com.google.android.exoplayer:exoplayer:2.18.1")
    implementation("com.github.jordyamc.oasis-jsbridge-android:oasis-jsbridge-duktape:1.0.2")
    implementation("androidx.compose.material:material-icons-extended:1.6.1")
}

