# iOS Sample

A SwiftUI app that links the `:shared` KMP module — the same `RemoteConfigDemo` the Android
and wasm apps drive, so all three show the same behaviour in their own UI idiom.

## Prerequisites

- Xcode 16 or later, with an iOS 15+ simulator or device
- JDK 17+ (the Gradle build that produces the framework runs as an Xcode build phase)
- [Tuist](https://tuist.dev) — `brew install tuist` (or `mise install tuist`)

## Run it

```bash
cd SampleApp/iosApp
tuist generate          # writes SampleApp.xcworkspace and opens it in Xcode
```

Then select the `SampleApp` scheme and an iOS 15+ simulator and press Run (⌘R).

To build without opening Xcode:

```bash
cd SampleApp/iosApp
tuist generate --no-open
xcodebuild -workspace SampleApp.xcworkspace -scheme SampleApp \
  -destination 'platform=iOS Simulator,name=iPhone 15' build
```

The generated `SampleApp.xcodeproj` / `.xcworkspace` are build output and are not committed —
`Project.swift` is the source of truth, so regenerate them after pulling.

## How the shared framework gets in

`Project.swift` declares a `Compile Kotlin Framework` pre-build phase that runs the standard
Kotlin Multiplatform Xcode integration:

```
cd "$SRCROOT/.." && ./gradlew :shared:embedAndSignAppleFrameworkForXcode
```

That builds the static `shared` framework for whatever configuration, SDK, and architecture
Xcode is currently building and places it in
`shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`, which the target's
`FRAMEWORK_SEARCH_PATHS` points at (it links with `-framework shared`). Swift code just does
`import shared`.

The published SDK ships `iosArm64` and `iosSimulatorArm64` klibs, so the simulator build needs
an Apple silicon Mac (or an arm64 simulator slice); Intel simulators (`iosX64`) are not covered.

## API key

The key is a build-time constant shared by every target — put it in
**`SampleApp/shared/src/commonMain/kotlin/dev/appspike/sample/ApiKey.kt`** (`APPSPIKE_API_KEY`),
not in Swift. Get a `pk_live_…` key from the
[AppSpike console](https://console.appspike.dev).

There is no runtime key-entry field on purpose: real apps pass the key at build time. While
`YOUR_API_KEY` is unchanged the app launches but does not initialize, and the Setup section
says so, naming the file to edit.

## What it covers

Setup status, fetch controls (`Fetch & Activate`, `Bypass Cache & Activate`, `Reset`), config
settings (current minimum fetch interval and fetch timeout, apply a pair, restore defaults),
free-form custom signals (any key/value, apply and remove, plus a `tier=gold` preset), an
`All Key/Values` screen with a `Filter by key prefix` field and `(remote)` / `(default)` /
`(static)` labels, fetch info, and typed getters with a `getValue` accessor inspector.

## Pure Swift projects

If your app is not KMP, use the [iOS-native SDK](https://github.com/pydetech/appspike-sdk-ios-dist)
instead — Swift-native APIs including AsyncSequence, property wrappers, and Codable support.
