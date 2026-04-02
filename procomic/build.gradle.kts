plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "eu.kanade.tachiyomi.extension.ar.procomic"
    compileSdk = 34

    defaultConfig {
        applicationId = "eu.kanade.tachiyomi.extension.ar.procomic"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.5.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
    compileOnly(project(":source-api"))
    compileOnly(project(":core:common"))
    compileOnly(libs.jsoup)
    compileOnly(libs.okhttp.core)
    compileOnly(libs.rxjava)
    compileOnly(libs.injekt)
    compileOnly(kotlinx.serialization.json)
}
