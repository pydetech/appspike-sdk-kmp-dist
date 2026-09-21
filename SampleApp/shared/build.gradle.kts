import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    android {
        namespace = "dev.appspike.sample.shared"
        compileSdk = 36
        minSdk = 23
    }

    // One static framework named `shared`, consumed by iosApp through the standard
    // embedAndSignAppleFrameworkForXcode build phase (see iosApp/Project.swift).
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    // The browser target for :wasmApp — the same commonMain compiles to Kotlin/Wasm.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation("dev.appspike:remote-config:1.4.5")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
    }
}
