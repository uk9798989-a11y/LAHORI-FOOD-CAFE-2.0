import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.security.MessageDigest
import java.time.Instant

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.lahorifoodcafe.xkmpqz"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Do not remove or modify these signingConfigs. They are necessary for building, installing,
  // and updating Android apps in AI Studio.
  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      // Do not remove or modify this signingConfig assignment. It is necessary for Android apps in
      // AI Studio.
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.messaging)
  implementation(libs.firebase.storage)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  // "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}

abstract class PackageLahoriApkTask : org.gradle.api.DefaultTask() {
  @get:org.gradle.api.tasks.InputFile
  @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.NONE)
  abstract val sourceApk: org.gradle.api.file.RegularFileProperty

  @get:org.gradle.api.tasks.OutputFile
  abstract val outputApk: org.gradle.api.file.RegularFileProperty

  @get:org.gradle.api.tasks.Input
  abstract val buildVariant: org.gradle.api.provider.Property<String>

  @get:org.gradle.api.tasks.Input
  abstract val signingSummary: org.gradle.api.provider.Property<String>

  @org.gradle.api.tasks.TaskAction
  fun packageApk() {
    val src = sourceApk.get().asFile
    val dst = outputApk.get().asFile
    if (!src.exists() || src.length() == 0L) {
      throw org.gradle.api.GradleException("Expected compiled APK not found or empty: ${src.absolutePath}")
    }
    dst.parentFile.mkdirs()
    src.copyTo(dst, overwrite = true)

    val digest = MessageDigest.getInstance("SHA-256")
    dst.inputStream().use { input ->
      val buffer = ByteArray(8192)
      var read = input.read(buffer)
      while (read != -1) {
        digest.update(buffer, 0, read)
        read = input.read(buffer)
      }
    }
    val sha256 = digest.digest().joinToString("") { b -> "%02x".format(b) }
    val sizeMb = "%.2f".format(dst.length() / (1024.0 * 1024.0))

    logger.lifecycle("============================================================")
    logger.lifecycle("Lahori Food Cafe APK Ready for Deployment")
    logger.lifecycle("Variant  : ${buildVariant.get()}")
    logger.lifecycle("Signing  : ${signingSummary.get()}")
    logger.lifecycle("Output   : ${dst.absolutePath}")
    logger.lifecycle("Size     : ${dst.length()} bytes (${sizeMb} MB)")
    logger.lifecycle("Updated  : ${Instant.ofEpochMilli(dst.lastModified())}")
    logger.lifecycle("SHA-256  : $sha256")
    logger.lifecycle("============================================================")
  }
}

val envKeystorePath = providers.environmentVariable("KEYSTORE_PATH").orNull
val envStorePassword = providers.environmentVariable("STORE_PASSWORD").orNull
val envKeyPassword = providers.environmentVariable("KEY_PASSWORD").orNull
val releaseKeystoreFile = file(envKeystorePath ?: "${rootDir}/my-upload-key.jks")
val hasReleaseSigningReady =
    releaseKeystoreFile.exists() &&
        !envStorePassword.isNullOrBlank() &&
        !envKeyPassword.isNullOrBlank()
val requireReleaseKeystore =
    providers.gradleProperty("requireReleaseKeystore").orNull.equals("true", ignoreCase = true)

tasks.register<PackageLahoriApkTask>("buildLahoriReleaseApk") {
  group = "build"
  description =
      "Builds the signed APK (release via signingConfigs.release when upload keystore & passwords are configured, otherwise signed with existing debug.keystore) and outputs Lahori-Food-Cafe.apk in app/build/outputs/apk/."

  if (hasReleaseSigningReady || requireReleaseKeystore) {
    dependsOn("assembleRelease")
    sourceApk.set(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    buildVariant.set("release")
    signingSummary.set("release signingConfig (${releaseKeystoreFile.name}, alias=upload)")
  } else {
    dependsOn("assembleDebug")
    sourceApk.set(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    buildVariant.set("debug-signed (fallback; set KEYSTORE_PATH/STORE_PASSWORD/KEY_PASSWORD or provide my-upload-key.jks for release upload keystore)")
    signingSummary.set("debugConfig (existing debug.keystore, alias=androiddebugkey)")
  }
  outputApk.set(layout.buildDirectory.file("outputs/apk/Lahori-Food-Cafe.apk"))
}

