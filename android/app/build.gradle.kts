import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.services)
}

val googleWebClientId = providers.gradleProperty("GHOST_CART_GOOGLE_WEB_CLIENT_ID")
    .orElse(providers.environmentVariable("GHOST_CART_GOOGLE_WEB_CLIENT_ID"))
    .getOrElse("")

// Release signing reads from keystore.properties (gitignored, never
// committed - see android/.gitignore). Absent on any machine that doesn't
// have the real keystore, so a release build only works where the keystore
// actually lives; debug builds are unaffected either way.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.ghostcart.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.ghostcart.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 65
        versionName = "2.7.36"
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Temporarily disabled - the first-ever minified/shrunk release
            // build crashed on launch on a real device, before any UI ever
            // showed. Firebase (Analytics + FCM, wired via the google-services
            // plugin) reads its config from generated string resources
            // (google_app_id, gcm_defaultSenderId, etc.) via a raw
            // resource-name lookup in its startup ContentProvider, not a
            // static R.string reference - isShrinkResources can't see that
            // usage and is the prime suspect for a crash this early. Ship
            // unminified now to unblock launch; re-enable once verified on a
            // real device (either add an explicit tools:keep resource entry
            // for the Firebase-generated strings, or bisect minify vs
            // shrinkResources independently) - do not just flip both back on
            // without a real device test.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // WorkManager for background delivery simulation steps & notifications
  implementation("androidx.work:work-runtime-ktx:2.9.0")
  implementation("androidx.credentials:credentials:1.6.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
  implementation("io.coil-kt.coil3:coil-compose:3.5.0")
  implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")

  // Firebase Analytics (google-services.json is project config, not a secret -
  // see ApiConfig.kt-adjacent conventions in this repo for why it's committed)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.analytics)
  implementation(libs.firebase.messaging)
  // Firebase In-App Messaging - added with zero campaigns configured in
  // Firebase Console on purpose. With nothing to display, this SDK just
  // initializes and sits idle; it will not show anything until a real
  // campaign is created there. Deliberately shipped inert first so this
  // version can be verified stable before any actual message content goes
  // live - do not create a Firebase Console campaign until that's confirmed.
  implementation(libs.firebase.inappmessaging.display)

  // Video playback for Ghost Cart Stories (admin can now upload MP4s, not
  // just images)
  implementation(libs.media3.exoplayer)
  implementation(libs.media3.ui)
}
