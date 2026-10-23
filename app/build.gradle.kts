import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release signing pulls from a local, gitignored keystore.properties so no
// secrets are committed. On a fresh clone without that file, the release build
// falls back to unsigned (assembleRelease still runs; the APK just isn't signed).
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "app.mindfs.android"
    compileSdk = 36

    defaultConfig {
        // Personal build: a distinct applicationId keeps it independent of any
        // future official client and of the codeg app on the same device.
        applicationId = "app.mindfs.android.fengx"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-fengx.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Two permanent packages from one source tree. `modern` keeps the original
    // applicationId and minSdk 31 (Android 12+); `legacy` targets Android 9
    // (API 28) and uses a distinct applicationId so both can be installed on the
    // same device. Every release ships both APKs.
    flavorDimensions += "platform"
    productFlavors {
        create("modern") {
            dimension = "platform"
        }
        create("legacy") {
            dimension = "platform"
            minSdk = 28
            applicationIdSuffix = ".legacy"
            versionNameSuffix = "-legacy"
        }
    }

    // App-UI localizations: English (default) + Simplified Chinese.
    androidResources {
        localeFilters += listOf("en", "zh-rCN")
    }

    signingConfigs {
        // Created only when keystore.properties is present (local dev / release
        // machine). CI or fresh clones without it build unsigned.
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            // Scaffold milestone: R8 stays off until features land (v112+); flip it
            // together with the proguard rules for kotlinx-serialization/Ktor.
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropsFile.exists()) {
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

// Compile to JVM 17 bytecode while running the build on the local JDK 21. No
// jvmToolchain() — that would try to provision a separate JDK 17.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // --- Compose ---
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- AndroidX core / lifecycle / navigation ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // --- DI: Hilt ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // --- Coroutines / serialization ---
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // --- Networking: Ktor (OkHttp engine) ---
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.logging)

    // --- Persistence ---
    implementation(libs.androidx.datastore.preferences)

    // --- Unit tests (JVM, no device) ---
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)

    // --- Instrumentation tests ---
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
