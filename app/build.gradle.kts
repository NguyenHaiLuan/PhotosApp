plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-parcelize")
}


android {
    namespace = "com.example.photosapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.photosapp"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
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

    // Lottie
    implementation(libs.lottie)
    //Glide
    implementation(libs.glide)
    //Styleable Toast
    implementation (libs.styleabletoast)
    //view pager2
    implementation (libs.androidx.viewpager2)

    implementation ("com.soundcloud.android:android-crop:1.0.1@aar")

    //camera x
    dependencies {
        val cameraxVersion = "1.4.0"

        implementation(libs.androidx.camera.core)
        implementation(libs.androidx.camera.camera2)
        implementation(libs.androidx.camera.lifecycle)
        implementation(libs.androidx.camera.video)
        implementation(libs.androidx.camera.view)
        implementation(libs.androidx.camera.extensions)
    }

}