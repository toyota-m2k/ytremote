plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}
android {
    compileSdkVersion(30)
    buildToolsVersion("30.0.2")

    defaultConfig {
        applicationId("com.michael.ytremote")
        minSdkVersion(26)
        targetSdkVersion(30)
        versionCode(1)
        versionName("1.0.1.0")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            minifyEnabled(false)
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_1_8)
        targetCompatibility(JavaVersion.VERSION_1_8)
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures  {
        dataBinding = true
    }
}

dependencies {
    val kotlin_version:String by project
    val exoplayer_version:String by project
    val okhttp_version:String by project
    val rx_java_version:String by project
    val rx_android_version:String by project
    val rx_kotlin_version:String by project
    val recycle_view_version:String by project
    val constraint_layout_version:String by project
    val core_ktx_version:String by project
    val appcompat_version:String by project
    val material_version:String by project
    val lifecycle_ktx_version:String by project
    val preference_version:String by project
    val androidx_navigation:String by project

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("androidx.core:core-ktx:$core_ktx_version")
    implementation("androidx.appcompat:appcompat:$appcompat_version")
    implementation("com.google.android.material:material:$material_version")
    implementation("androidx.navigation:navigation-fragment:$androidx_navigation")
    implementation("androidx.navigation:navigation-ui:$androidx_navigation")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle_ktx_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_ktx_version")
    implementation("io.reactivex.rxjava3:rxjava:$rx_java_version")
    implementation("io.reactivex.rxjava3:rxandroid:$rx_android_version")
    implementation("io.reactivex.rxjava3:rxkotlin:$rx_kotlin_version")
    implementation("androidx.constraintlayout:constraintlayout:$constraint_layout_version")
    implementation("androidx.recyclerview:recyclerview:$recycle_view_version")
    implementation("androidx.preference:preference:$preference_version")

    implementation("com.squareup.okhttp3:okhttp:$okhttp_version")
    implementation("com.google.android.exoplayer:exoplayer-core:$exoplayer_version")
//    implementation("com.google.android.exoplayer:exoplayer-hls:2.9.6")
//    implementation("com.google.android.exoplayer:exoplayer-dash:2.9.6")
    implementation("com.google.android.exoplayer:exoplayer-smoothstreaming:$exoplayer_version")
    implementation("com.google.android.exoplayer:exoplayer-ui:$exoplayer_version")
    testImplementation("junit:junit:4.13.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}