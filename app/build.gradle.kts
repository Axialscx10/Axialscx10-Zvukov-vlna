plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.example"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.aistudio.soundwave.nctrn"
    minSdk = 24
    targetSdk = 36
    versionCode = 33
    versionName = "33.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      val ksFile = file(keystorePath)
      val storePwd = System.getenv("STORE_PASSWORD")
      val keyPwd = System.getenv("KEY_PASSWORD")
      if (ksFile.exists() && !storePwd.isNullOrBlank() && !keyPwd.isNullOrBlank()) {
        storeFile = ksFile
        storePassword = storePwd
        keyAlias = "upload"
        keyPassword = keyPwd
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      val releaseSign = signingConfigs.getByName("release")
      if (releaseSign.storeFile != null) {
        signingConfig = releaseSign
      }
    }
    debug {
      // Uses Android SDK's default debug keystore (~/.android/debug.keystore) so clean clones build immediately
    }
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }

  configurations.matching { it.name.contains("UnitTest") }.configureEach {
    resolutionStrategy.eachDependency {
      if (requested.group == "org.ow2.asm") {
        useVersion("9.9")
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.material.icons.extended)

  // Media3 & Audio
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.datasource.okhttp)
  implementation(libs.androidx.media3.database)

  // Networking & Image loading
  implementation(libs.okhttp)
  implementation(libs.coil.compose)

  testImplementation(libs.junit)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.robolectric)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.register<Copy>("exportDebugApk") {
  from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
  into("${rootDir}/store-assets")
  rename { "SoundWave-v${android.defaultConfig.versionName}.apk" }
  doFirst {
    file("${rootDir}/store-assets").listFiles()?.filter { it.extension == "apk" }?.forEach { it.delete() }
  }
}

afterEvaluate {
  tasks.findByName("assembleDebug")?.finalizedBy("exportDebugApk")
}
