plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("de.undercouch.download") version "5.6.0"
    alias(libs.plugins.ksp)

}

android {
    namespace = "com.example.editphoto"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.editphoto"
        minSdk = 24
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //sdp
    implementation("com.intuit.sdp:sdp-android:1.1.0")
    //glide
    implementation(libs.glide)
    //viewmodel + livedata
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    //mediapipe
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
    //opencv
    implementation(project(":sdk"))
    //camera
    implementation ("androidx.camera:camera-core:1.3.0")
    implementation ("androidx.camera:camera-camera2:1.3.0")
    implementation ("androidx.camera:camera-lifecycle:1.3.0")
    implementation ("androidx.camera:camera-view:1.3.0")

    //croper
    implementation("com.vanniktech:android-image-cropper:4.6.0")

    //flexbox
    implementation ("com.google.android.flexbox:flexbox:3.0.0")
    //photoView
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    //RoomDatabase
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    //PhotoEditor
    implementation ("com.burhanrashid52:photoeditor:3.0.2")


}

