plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kemi.desklink"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kemi.desklink"
        minSdk = 26
        targetSdk = 31
        versionCode = 1
        versionName = "0.1.0-p0"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:platform"))
    implementation(project(":core:reference"))
    implementation(project(":core:workspace"))
    implementation(project(":features:media"))
}
