plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization) // kotlin 序列化
  id("kotlin-kapt") // brv 必须引入此插件
  alias(libs.plugins.google.ksp) // ksp
  alias(libs.plugins.navigation.safeargs)
}

android {
  signingConfigs {
    getByName("debug") {
      storeFile = file("D:\\jdy2002\\appkey\\jdy.jks")
      storePassword = "jdy200255"
      keyAlias = "jdy2002"
      keyPassword = "jdy200255"
      enableV1Signing = true
      enableV2Signing = true
    }
    create("release") {
      storeFile = file("D:\\jdy2002\\appkey\\jdy.jks")
      storePassword = "jdy200255"
      keyAlias = "jdy2002"
      keyPassword = "jdy200255"
      enableV1Signing = true
      enableV2Signing = true
    }
  }
  namespace = "xyz.jdynb.music"
  compileSdk = 36

  defaultConfig {
    applicationId = "xyz.jdynb.music"
    minSdk = 24
    targetSdk = 36
    versionCode = 4
    versionName = "1.0.3"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    signingConfig = signingConfigs.getByName("debug")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    buildConfig = true // 必须 true，否则没有 BuildConfig 文件
    viewBinding = true
    dataBinding = true
  }

  externalNativeBuild {
    cmake {
      path = file("src/main/cpp/CMakeLists.txt")
      version = "4.1.2"
    }
  }
  ndkVersion = "27.0.12077973"

  splits {
    abi {
      isUniversalApk = true
      reset()
      include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    }
  }


}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)

  implementation(libs.androidx.media3.exoplayer)
  // implementation(libs.androidx.media3.exoplayer.midi)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.common)

  implementation(libs.engine)
  implementation(libs.brv)
  implementation(libs.okhttp)
  implementation(libs.net)
  implementation(libs.kotlin.serialization.json)
  implementation(libs.glide)
  implementation(libs.litepal)
  implementation(libs.tooltip)
  // 权限请求
  implementation(libs.xxpermissions)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.androidx.localbroadcastmanager)
  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.navigation.ui.ktx)
  implementation("androidx.palette:palette-ktx:1.0.0")
  implementation("com.github.Moriafly:LyricViewX:1.4.0-alpha02")
  implementation(libs.androidx.preference.ktx)
  // https://mvnrepository.com/artifact/com.intuit.sdp/sdp-android
  runtimeOnly("com.intuit.sdp:sdp-android:1.1.1")

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
}