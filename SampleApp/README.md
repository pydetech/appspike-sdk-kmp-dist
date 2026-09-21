# AppSpike Remote Config — KMP Sample App

One Gradle build, one shared module, three runnable apps: Android (Compose), iOS (SwiftUI),
and the browser (Kotlin/Wasm + DOM). All the Remote Config logic lives in
`shared/src/commonMain` — each app is just the platform's UI on top of it, which is the point
of the sample: the integration is written once.

```
SampleApp/
├── shared/                     KMP module: android + iosArm64 + iosSimulatorArm64 + wasmJs
│   └── src/commonMain/kotlin/dev/appspike/sample/
│       ├── ApiKey.kt           APPSPIKE_API_KEY — the ONE key location for ALL targets
│       └── RemoteConfigDemo.kt every SDK call the samples make
├── androidApp/                 Jetpack Compose app
├── iosApp/                     SwiftUI app (Tuist-generated Xcode project)
└── wasmApp/                    Kotlin/Wasm browser app (plain DOM, no UI framework)
```

## API key

The key is a **build-time code constant**, shared by every target. Replace `YOUR_API_KEY` in:

**`shared/src/commonMain/kotlin/dev/appspike/sample/ApiKey.kt`**

```kotlin
const val APPSPIKE_API_KEY = "YOUR_API_KEY"
```

Get a `pk_live_…` key from the [AppSpike console](https://console.appspike.dev). There is no
runtime key-entry field on any platform on purpose: a text field models an integration nobody
ships — real apps pass the key at build time. While the placeholder is unchanged, every app
launches but does not initialize, and says so on screen, naming this file.

## Run it

### Android

```bash
cd SampleApp
./gradlew :androidApp:installDebug     # or :androidApp:assembleDebug to just build
```

Needs an emulator or device on API 23+.

### iOS

```bash
cd SampleApp/iosApp
tuist generate                          # generates + opens SampleApp.xcworkspace
```

Then pick the `SampleApp` scheme and an iOS 15+ simulator and press Run (⌘R). Requires Xcode 16+
and [Tuist](https://tuist.dev) (`brew install tuist`). The Xcode project is generated from
`iosApp/Project.swift` and is not committed; a pre-build phase runs
`./gradlew :shared:embedAndSignAppleFrameworkForXcode` so the `shared` framework is always in
step with the Kotlin sources. See [iosApp/README.md](iosApp/README.md) for details.

### Browser (wasm)

```bash
cd SampleApp
./gradlew :wasmApp:wasmJsBrowserDevelopmentRun --continuous
```

Serves the app at **http://localhost:8080** and opens it in your default browser; `--continuous`
rebuilds on source changes. Needs a browser with WebAssembly garbage collection (Chrome 119+,
Firefox 120+, Safari 18.2+).

For a static bundle to host yourself:

```bash
./gradlew :wasmApp:wasmJsBrowserDistribution
# output: wasmApp/build/dist/wasmJs/productionExecutable/
```

## What every app covers

The same content on all three platforms, each in its own UI idiom:

- **Setup** — initialization status; a clear on-screen message while the key placeholder is unset
- **Fetch controls** — `Fetch (respect cache)`, `Activate`, `Fetch & Activate`,
  `Bypass Cache & Activate`, `Reset` (which calls `reset()` and re-applies the sample defaults)
- **Config settings** — the current minimum fetch interval and fetch timeout, applying a pair,
  and restoring the SDK defaults
- **Custom signals** — free-form key/value entry (apply = set signal + bypass-cache fetch +
  activate), remove-signal, plus a `tier=gold` preset
- **All Key/Values** — every `getAll()` row with a `Filter by key prefix` field and `(remote)` /
  `(default)` / `(static)` source labels
- **Info** — last fetch status and time, plus the live settings
- **Value inspection** — typed getters and every `RemoteConfigValue` accessor for one key

## Versions

| | |
|---|---|
| Kotlin | 2.3.20 |
| AGP | 9.0.0 |
| Gradle | 9.3.0 |
| `dev.appspike:remote-config` | 1.4.5 |
