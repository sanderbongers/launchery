plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

// The release signing config exists only when all three variables are set, so CI and a fresh clone build an unsigned
// release APK. The keystore file is outside the repository and its password is never written to disk.
val releaseKeystoreFile = providers.environmentVariable("LAUNCHERY_KEYSTORE_FILE")
val releaseKeyAlias = providers.environmentVariable("LAUNCHERY_KEY_ALIAS")
val releaseKeystorePassword = providers.environmentVariable("LAUNCHERY_KEYSTORE_PASSWORD")
val hasReleaseSigning =
    releaseKeystoreFile.isPresent && releaseKeyAlias.isPresent && releaseKeystorePassword.isPresent

android {
    namespace = "rs.bonge.launchery"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        applicationId = "rs.bonge.launchery"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = File(releaseKeystoreFile.get())
                storePassword = releaseKeystorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeystorePassword.get()
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        // A second, user-installed app for trying Uninstall and live package updates on the emulator. Install it
        // with adb only; it is never part of a release.
        create("fixture") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".fixture"
            matchingFallbacks += "debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        // These compare against the latest upstream releases, so as errors they would fail an unchanged commit once a
        // new version comes out. Informational keeps them in the lint report without failing the build.
        informational += setOf("AndroidGradlePluginVersion", "GradleDependency", "NewerVersionAvailable")
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.palette)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.tv.material)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
