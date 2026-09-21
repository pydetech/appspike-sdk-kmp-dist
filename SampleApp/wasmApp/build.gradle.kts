import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    // A browser application: the same shared module the Android and iOS apps use,
    // compiled to Kotlin/Wasm and driven by a plain DOM UI (no Compose needed).
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "wasmApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":shared"))
            // kotlinx.browser / org.w3c.dom for Kotlin/Wasm live in this artifact.
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
    }
}
