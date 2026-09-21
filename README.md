# AppSpike SDK for Kotlin Multiplatform

**The free Firebase Remote Config alternative.**

> **Firebase Remote Config is going paid.** Google's usage-based pricing took effect on
> September 1, 2026. Existing free-plan (Spark) projects hit enforcement on
> **December 1, 2026**: past 100K daily fetches they get a 30-day grace period and are
> then throttled. Existing Blaze projects are billed automatically from
> **February 1, 2027**. The dates come from
> [Firebase's own pricing schedule](https://firebase.google.com/docs/remote-config/pricing).
> The [migration schedule below](#when-to-migrate) fits inside that window.


Kotlin Multiplatform SDK for [AppSpike Remote Config](https://appspike.dev/remote-config). **Free** remote configuration, feature flags, and staged rollouts from one shared codebase targeting Android, iOS, and wasmJs, with every condition evaluated **locally on-device**. AppSpike Remote Config is also a drop-in replacement for Firebase Remote Config: no fetch limits, no usage fees, no per-platform SDK differences.

[Product](https://appspike.dev/remote-config) · [Docs](https://appspike.dev/docs/remote-config)

> **iOS-native apps:** If your project is pure Swift/SwiftUI (not KMP), use the [iOS-native SDK](https://github.com/pydetech/appspike-sdk-ios-dist) instead for Swift-native APIs including AsyncSequence, property wrappers, and Codable support.

## Installation

### Kotlin Multiplatform (Gradle)

Add the dependency. Artifacts are published on Maven Central:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

```kotlin
// shared/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("dev.appspike:remote-config:1.4.5")
        }
    }
}
```

### Supported Targets

| Target | Artifact |
|--------|----------|
| Android | AAR |
| iOS (`iosArm64`, `iosSimulatorArm64`) | Kotlin/Native klib |
| wasmJs | Kotlin/Wasm klib |

## Quick Start

**1. Register your app.** Create your app at [console.appspike.dev](https://console.appspike.dev) and copy its `pk_live_…` API key.

All code below runs in `commonMain`, shared across Android, iOS, and wasmJs.

**2. Initialize the SDK.** Use a platform-specific context on Android, `null` on iOS/wasmJs.

```kotlin
import dev.appspike.AppSpike
import dev.appspike.InitResult
import dev.appspike.remoteconfig.AppSpikeRemoteConfig
import dev.appspike.remoteconfig.CustomSignals
import dev.appspike.remoteconfig.RemoteConfigFetchException
import dev.appspike.remoteconfig.RemoteConfigSettings

AppSpike.initialize(
    context = platformContext, // Android: applicationContext, iOS/wasmJs: null
    apiKey = "your-api-key",
    modules = listOf(AppSpikeRemoteConfig),
) { result ->
    when (result) {
        is InitResult.Success -> println("AppSpike ready")
        is InitResult.Error -> println("Init failed: ${result.message}")
    }
}
```

**3. Set defaults.** These are served until a fetched config is activated.

```kotlin
AppSpikeRemoteConfig.setDefaults(mapOf(
    "welcome_message" to "Hello!",
    "feature_enabled" to false,
    "max_retries" to 3L,
    "price" to 9.99,
))
```

**4. Fetch and activate.** This is a suspend function.

```kotlin
val changed = try {
    AppSpikeRemoteConfig.fetchAndActivate()
} catch (e: RemoteConfigFetchException) {
    // A failed fetch is an ordinary outcome — offline, or a backoff window.
    // Your defaults (or the last activated config) stay in place.
    false
}
```

**5. Read values.** Typed accessors, with in-app defaults as the fallback.

```kotlin
val message = AppSpikeRemoteConfig.getString("welcome_message")
val enabled = AppSpikeRemoteConfig.getBoolean("feature_enabled")
val retries = AppSpikeRemoteConfig.getLong("max_retries")
val price = AppSpikeRemoteConfig.getDouble("price")

// Use RemoteConfigValue for source info
val value = AppSpikeRemoteConfig.getValue("welcome_message")
println("Value: ${value.asString()}, Source: ${value.getSource()}")
```

**6. Custom signals for targeting.** These are evaluated on-device.

```kotlin
AppSpikeRemoteConfig.setCustomSignals(
    CustomSignals.Builder()
        .put("tier", "gold")
        .put("level", 5)
        .build()
)
```

**7. Listen for config changes.** Get notified when activated keys change.

```kotlin
val registration = AppSpikeRemoteConfig.addOnConfigUpdateListener { configUpdate ->
    println("Config changed: ${configUpdate.updatedKeys}")
}
// Later: registration.remove()
```

**8. Config settings.** Tune the fetch interval and timeout.

```kotlin
AppSpikeRemoteConfig.setConfigSettings(
    RemoteConfigSettings(
        minimumFetchIntervalSeconds = 3600,
        fetchTimeoutSeconds = 30,
    )
)
```

**9. Get all keys or filter by prefix**

```kotlin
val allValues = AppSpikeRemoteConfig.getAll()
val featureKeys = AppSpikeRemoteConfig.getKeysByPrefix("feature_")
```

**10. Config info.** The last fetch status and time.

```kotlin
val info = AppSpikeRemoteConfig.getInfo()
println("Last fetch: ${info.lastFetchStatus}, at ${info.lastFetchTimeMillis}")
```

**11. Reset all state**

```kotlin
AppSpikeRemoteConfig.reset()
```

### Android-specific: XML defaults

On Android, you can also load defaults from XML resources:

```kotlin
// androidMain
import dev.appspike.remoteconfig.setDefaults

AppSpikeRemoteConfig.setDefaults(context, R.xml.remote_config_defaults)
```

## Why AppSpike Remote Config?

**A free, direct replacement for Firebase Remote Config.** Same fetch/activate lifecycle, same typed accessors, and no fetch metering or usage fees. Firebase Remote Config is free up to 100K fetches per day, then bills $0.06 per 10K. AppSpike Remote Config stays free at any scale.

**Same template, same result on every platform.** Firebase Remote Config ships separate native SDKs per platform. KMP wrappers like GitLive bridge them, so the same template can resolve differently on Android and iOS: different regex engines, different number parsing, different locale handling. AppSpike Remote Config's evaluator is one Kotlin implementation compiled to every target, held identical by a shared cross-platform conformance test suite. A rollout that hits 10% on Android hits the same 10% on iOS.

**Your targeting data stays on the device.** Firebase Remote Config sends custom signals to Google's servers with every fetch and evaluates conditions there. AppSpike Remote Config downloads the template once and evaluates locally. Signals never leave the device, which is the answer your privacy review needs.

**Full API from `commonMain`.** GitLive's wrapper covers ~20% of Firebase's Remote Config API and inherits every platform quirk underneath. AppSpike Remote Config's entire surface (typed accessors, custom signals, update listeners, `getKeysByPrefix`, `reset`) is common code. There is no platform it silently doesn't work on.

**Battle tested.** It already serves millions of users in PokeRaid and PokeTrade.

What Firebase Remote Config still does that we don't: Google Analytics audience targeting (use custom signals instead) and managed A/B experiment dashboards (run A/B tests with percentage conditions). If Analytics audiences are load-bearing for you today, the two SDKs coexist in one app so you can migrate everything else first. Everything else is covered:

## Migrating from GitLive Firebase KMP SDK

If you're using [GitLive's firebase-kotlin-sdk](https://github.com/GitLiveApp/firebase-kotlin-sdk) (`dev.gitlive:firebase-config`), the migration is straightforward. Both are KMP libraries with suspend-based APIs and similar naming. Move your config template in the console first, then work through the code.

### Feature comparison

| Feature | Firebase Remote Config | AppSpike Remote Config |
|---------|----------------------|----------------------|
| Fetch & activate lifecycle | ✅ | ✅ |
| Typed value access (string, bool, long, double, byte array) | ✅ | ✅ |
| In-app defaults | ✅ | ✅ |
| XML resource defaults (Android) | ✅ | ✅ |
| Custom signals / targeting | ✅ | ✅ |
| Percent rollout | ✅ | ✅ |
| Country / language targeting | ✅ | ✅ |
| App version / build targeting | ✅ | ✅ |
| Date/time conditions | ✅ | ✅ |
| Regex matching | ✅ | ✅ |
| Config update listeners | ✅ | ✅ |
| `getKeysByPrefix` | ✅ | ✅ |
| `reset()` | ✅ | ✅ |
| `ensureInitialized()` | ✅ | ✅ |
| Minimum fetch interval | ✅ | ✅ |
| Exponential backoff on failure | ✅ | ✅ |
| Kotlin Multiplatform | ❌ | ✅ |
| Shared evaluator across all targets | ❌ | ✅ |
| Price at scale | 100K fetches/day free, then $0.06 per 10K | Free, no fetch metering |
| Config import | ❌ No import path from other providers | ✅ One-click import from Firebase |
| Version history & rollback | ✅ | ✅ |
| Real-time config updates | ✅ Real-time Remote Config | ✅ (push setup required) |
| A/B testing | ✅ Firebase A/B Testing | ✅ Via percentage conditions |
| Analytics audience targeting | ✅ Google Analytics audiences | ❌ Use custom signals instead |
| Device targeting identity | Google Installation ID | AppSpike device ID |
| Custom signals stay on-device | ❌ (sent for server-side evaluation) | ✅ (never transmitted) |

### When to migrate

The two SDKs run side by side in the same app, so nothing forces a single cutover day. Two dates bound the plan: existing Spark projects face throttling enforcement from December 1, 2026, and existing Blaze projects are billed from February 1, 2027.

1. **Today.** Register your app at [console.appspike.dev](https://console.appspike.dev), import your Firebase Remote Config template, and publish. Nothing in your app changes yet.
2. **Next development cycle.** Make the code changes below in a branch. Debug builds can run both SDKs together and compare values.
3. **Before the cutover release.** Finish any in-flight percentage rollouts and experiments on Firebase Remote Config. Rollout groups are re-randomized on AppSpike, so a mid-rollout user can change groups. If your template changed since step 1, import it again.
4. **The cutover release.** Ship the swap as a normal app release. Keep your in-app defaults registered. They cover every device that has not fetched yet.
5. **After the rollout.** Once the release has reached most of your fleet, remove the `dev.gitlive:firebase-config` dependency.

### Step-by-step

**1. Move your config template.** In the [AppSpike console](https://console.appspike.dev), register your app, import your Firebase Remote Config template (Firebase export upload is supported), review it, and publish. Your parameters and conditions exist on the AppSpike side before the shared code changes.

**2. Replace the dependency.** In `commonMain`:

```kotlin
// Remove
implementation("dev.gitlive:firebase-config:2.7.0")

// Add
implementation("dev.appspike:remote-config:1.4.5")
```

**3. Update imports.** These cover every step below.

```kotlin
// Before
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfig
import dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfigValue
import dev.gitlive.firebase.remoteconfig.remoteConfig

// After
import dev.appspike.AppSpike
import dev.appspike.InitResult
import dev.appspike.remoteconfig.AppSpikeRemoteConfig
import dev.appspike.remoteconfig.CustomSignals
import dev.appspike.remoteconfig.RemoteConfigFetchException
import dev.appspike.remoteconfig.RemoteConfigSettings
import dev.appspike.remoteconfig.RemoteConfigValue

// The GitLive-compatible spellings are extensions, so import the ones you keep:
import dev.appspike.remoteconfig.all
import dev.appspike.remoteconfig.info
import dev.appspike.remoteconfig.setDefaults
import dev.appspike.remoteconfig.settings
```

**4. Add initialization.** `AppSpike.initialize` is added once at app startup. After it, the
`AppSpikeRemoteConfig` object takes the place of every `Firebase.remoteConfig` receiver.

```kotlin
// Before
val config = Firebase.remoteConfig

// After
AppSpike.initialize(
    context = platformContext, // Android: applicationContext, iOS/wasmJs: null
    apiKey = "your-api-key",
    modules = listOf(AppSpikeRemoteConfig),
) { result ->
    when (result) {
        is InitResult.Success -> { /* ready */ }
        is InitResult.Error -> { /* handle error */ }
    }
}
```

**5. Defaults.** The GitLive call compiles verbatim.

```kotlin
// Before
config.setDefaults("key1" to "value1", "key2" to 42)

// After — same vararg of pairs; only the receiver changed
AppSpikeRemoteConfig.setDefaults("key1" to "value1", "key2" to 42)

// The canonical AppSpike spelling takes a Map, if you'd rather write that
AppSpikeRemoteConfig.setDefaults(mapOf("key1" to "value1", "key2" to 42))
```

**6. Settings.** The GitLive block compiles verbatim.

```kotlin
// Before
config.settings {
    minimumFetchInterval = 0.seconds
    fetchTimeout = 60.seconds
}

// After — same block, same Duration properties
AppSpikeRemoteConfig.settings {
    minimumFetchInterval = 0.seconds
    fetchTimeout = 60.seconds
}

// The canonical AppSpike spelling is a data class, if you'd rather write that
AppSpikeRemoteConfig.setConfigSettings(
    RemoteConfigSettings(
        minimumFetchIntervalSeconds = 0,
        fetchTimeoutSeconds = 60,
    )
)
```

**7. Fetch / activate.** The same suspend functions.

```kotlin
// Before
config.fetch()
config.activate()
config.fetchAndActivate()

// After — identical names and shapes
AppSpikeRemoteConfig.fetch()
AppSpikeRemoteConfig.activate()
AppSpikeRemoteConfig.fetchAndActivate()
```

Keep whatever `try`/`catch` you had around the fetch. A failed fetch is an ordinary outcome
(offline, or a backoff window) and throws `RemoteConfigFetchException`, so an unguarded call
inside a `launch { }` takes the app down with it:

```kotlin
val changed = try {
    AppSpikeRemoteConfig.fetchAndActivate()
} catch (e: RemoteConfigFetchException) {
    false   // keep serving the current values
}
```

**8. Read values.** Replace the receiver, keep the calls.

```kotlin
// Before
config.getValue("key").asString()
config.getValue("key").asBoolean()
config.getValue("key").asLong()
config.getValue("key").asDouble()
config.all
config.info

// After
AppSpikeRemoteConfig.getValue("key").asString()
AppSpikeRemoteConfig.getValue("key").asBoolean()
AppSpikeRemoteConfig.getValue("key").asLong()
AppSpikeRemoteConfig.getValue("key").asDouble()
AppSpikeRemoteConfig.all    // getAll() is the canonical spelling
AppSpikeRemoteConfig.info   // getInfo() is the canonical spelling
```

**9. Custom signals.** New capability, no GitLive equivalent.

```kotlin
// GitLive has no custom signals; AppSpike adds targeting signals evaluated locally
// on-device, from commonMain. Suspend, so call it from a coroutine.
AppSpikeRemoteConfig.setCustomSignals(
    CustomSignals.Builder()
        .put("tier", "gold")
        .put("level", 5)
        .build()
)
```

### API mapping reference

AppSpike keeps compile-compatible aliases for the member spellings GitLive and Firebase use, so once the **types** are renamed most call sites compile unchanged. The aliases delegate to the canonical AppSpike names shown in [Public API Reference](#public-api-reference). Rows marked "Same" need no edit beyond the receiver.

#### GitLive → AppSpike

| GitLive (`dev.gitlive:firebase-config`) | AppSpike (`dev.appspike:remote-config`) | Notes |
|---|---|---|
| `Firebase.remoteConfig` | `AppSpikeRemoteConfig` | Object singleton |
| `config.getValue(key)` | `AppSpikeRemoteConfig.getValue(key)` | Replace receiver |
| `value.asString()` | `value.asString()` | Same |
| `value.asBoolean()` | `value.asBoolean()` | Same |
| `value.asLong()` | `value.asLong()` | Same |
| `value.asDouble()` | `value.asDouble()` | Same |
| `value.asByteArray()` | `value.asByteArray()` | Same |
| `value.getSource()` | `value.getSource()` | Same |
| `config.all` | `AppSpikeRemoteConfig.all` | Same (`getAll()` is the canonical spelling) |
| `config.getKeysByPrefix(p)` | `AppSpikeRemoteConfig.getKeysByPrefix(p)` | Same |
| `config.info` | `AppSpikeRemoteConfig.info` | Same (`getInfo()` is the canonical spelling) |
| `info.fetchTime` | `info.fetchTime` | Same (`Instant`) |
| `info.lastFetchStatus` | `info.lastFetchStatus` | Same. `FetchStatus.Success` etc. resolve |
| `config.settings { ... }` | `AppSpikeRemoteConfig.settings { ... }` | Same. `minimumFetchInterval` / `fetchTimeout` (`Duration`) and the `…InSeconds` forms both work |
| `settings.minimumFetchInterval` | `settings.minimumFetchInterval` | Same (`Duration`) |
| `config.setDefaults(pairs)` | `AppSpikeRemoteConfig.setDefaults(pairs)` | Same. A `Map` overload is the canonical one |
| `config.fetch(duration?)` | `AppSpikeRemoteConfig.fetch(duration?)` | Same. A `Long` seconds overload is the canonical one |
| `config.activate()` | `AppSpikeRemoteConfig.activate()` | Same |
| `config.fetchAndActivate()` | `AppSpikeRemoteConfig.fetchAndActivate()` | Same |
| `config.reset()` | `AppSpikeRemoteConfig.reset()` | Suspend → synchronous (still compiles) |
| `config.ensureInitialized()` | `AppSpikeRemoteConfig.ensureInitialized()` | Same. Returns `RemoteConfigInfo` |
| `config.get<T>(key)`, `config[key]` | `AppSpikeRemoteConfig.get<T>(key)`, `[key]` | Same. `Boolean`, `Double`, `Long`, `String`, `RemoteConfigValue` |
| `ValueSource.Remote` / `.Default` / `.Static` | same spellings | Enum entries are `REMOTE` / `DEFAULT` / `STATIC`. The GitLive casing resolves too |
| `FirebaseRemoteConfigException` | `RemoteConfigException` | Base type. Drop the `Firebase` prefix |
| `FirebaseRemoteConfigFetchThrottledException` | `RemoteConfigThrottledException` | Adds `throttleEndTimeMillis` |
| N/A | `AppSpikeRemoteConfig.setCustomSignals(s)` | AppSpike-only |
| N/A | `AppSpikeRemoteConfig.addOnConfigUpdateListener(l)` | AppSpike-only |

#### Firebase Remote Config → AppSpike

Coming from Firebase Android used from Kotlin (`com.google.firebase:firebase-config`), including the KTX idioms (`remoteConfigSettings { }`, `customSignals { }`, `configUpdates`):

| Firebase Remote Config (Kotlin) | AppSpike (`dev.appspike:remote-config`) | Notes |
|---|---|---|
| `FirebaseRemoteConfig.getInstance()`, `Firebase.remoteConfig` | `AppSpikeRemoteConfig` | Object singleton. Replace the receiver |
| `config.getString/getBoolean/getLong/getDouble(key)` | same | Same |
| `config.getValue(key)` | same | Same |
| `config.getAll()`, `config.all` | `getAll()`, `all` | Same |
| `config.getKeysByPrefix(p)` | same | Same |
| `config.getInfo()`, `config.info` | `getInfo()`, `info` | Same |
| `value.asString/asBoolean/asLong/asDouble/asByteArray()` | same | Same, but total. An unconvertible value reads as the zero value instead of throwing `IllegalArgumentException` |
| `value.source` | `value.source` | Same spelling. The type is the `ValueSource` enum, not an `Int` |
| `VALUE_SOURCE_*`, `LAST_FETCH_STATUS_*` | `ValueSource.*`, `FetchStatus.*` | **Edit required.** Enums, not `Int` constants |
| `info.fetchTimeMillis` | `info.fetchTimeMillis` | Same (`lastFetchTimeMillis` is the canonical spelling) |
| `info.lastFetchStatus`, `info.configSettings` | same | Same |
| `remoteConfigSettings { minimumFetchIntervalInSeconds = … }` | same | Same builder function and property names |
| `FirebaseRemoteConfigSettings.Builder().setFetchTimeoutInSeconds(…)` | `RemoteConfigSettings.Builder()…` | Same setter names |
| `config.setConfigSettingsAsync(s)` | same | Returns `Unit`, not `Task<Void>` |
| `config.setDefaultsAsync(map)` | same | Returns `Unit`, not `Task<Void>` |
| `config.setDefaultsAsync(R.xml.defaults)` | `setDefaults(context, resId)` | **Edit required.** Android source set, and it needs a `Context` |
| `customSignals { put(k, v) }` | same | Same builder function |
| `config.setCustomSignals(s)` | same | `suspend` instead of `Task` |
| `config.fetch()`, `config.fetch(seconds)` | same | `suspend` instead of `Task` |
| `config.activate()`, `config.fetchAndActivate()` | same | `suspend` instead of `Task` |
| `config.ensureInitialized()` | same | `suspend`. Returns `RemoteConfigInfo` |
| `config.reset()` | same | Synchronous |
| `config.addOnConfigUpdateListener(listener)` | same | `ConfigUpdateListener` has the same two methods |
| `ConfigUpdateListener.onUpdate(ConfigUpdate)` | same | `configUpdate.updatedKeys` |
| `ConfigUpdateListener.onError(e)` | `onError(error: RemoteConfigException)` | Same shape |
| `registration.remove()` | same | Same |
| `config.configUpdates` | same | `Flow<ConfigUpdate>` |
| `config[key]` (KTX) | `config[key]` | Resolves where the expected type is known (`val v: RemoteConfigValue = config["k"]`). For the chained form use `getValue("k").asString()` |
| `FirebaseRemoteConfigException` | `RemoteConfigException` | Base type |
| `FirebaseRemoteConfigFetchThrottledException` | `RemoteConfigThrottledException` | `e.throttleEndTimeMillis`, same spelling |
| `FirebaseRemoteConfigClientException` / `ServerException` | `RemoteConfigFetchException` | One fetch-failure type |
| `task.addOnCompleteListener { }`, `task.await()` | N/A | **Not supported.** AppSpike never returns a Play Services `Task`, so use the `suspend` functions |
| `FirebaseRemoteConfigException.Code`, `e.code` | N/A | Not implemented |

### AI-Assisted Migration

Copy the prompt below into your AI coding assistant (Claude, Cursor, Copilot, etc.) to migrate automatically:

<details>
<summary>Migration prompt</summary>

```
Migrate this Kotlin Multiplatform project from GitLive Firebase KMP SDK (dev.gitlive:firebase-config) to AppSpike Remote Config (dev.appspike:remote-config).

Rules:
1. Replace the dependency:
   - Remove: dev.gitlive:firebase-config
   - Add: dev.appspike:remote-config:1.4.5

2. Replace imports:
   - dev.gitlive.firebase.Firebase → dev.appspike.AppSpike
   - dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfig → dev.appspike.remoteconfig.AppSpikeRemoteConfig
   - dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfigValue → dev.appspike.remoteconfig.RemoteConfigValue
   - dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfigSettings → dev.appspike.remoteconfig.RemoteConfigSettings
   - dev.gitlive.firebase.remoteconfig.FirebaseRemoteConfigInfo → dev.appspike.remoteconfig.RemoteConfigInfo
   - dev.gitlive.firebase.remoteconfig.remoteConfig → (not needed, AppSpikeRemoteConfig is an object)
   - Add: import dev.appspike.InitResult

3. Add initialization:
   - Add: AppSpike.initialize(context = platformContext, apiKey = "API_KEY", modules = listOf(AppSpikeRemoteConfig)) { result -> }
     (platformContext: applicationContext on Android, null on iOS/wasmJs)

4. Replace Firebase.remoteConfig with AppSpikeRemoteConfig everywhere

5. Replace value access — method signatures are identical:
   - config.getValue(key).asString() → AppSpikeRemoteConfig.getValue(key).asString()
   - Same for asBoolean(), asLong(), asDouble(), asByteArray(), getSource()

6. Prefer the canonical AppSpikeRemoteConfig.getAll() over config.all (the `all` property
   spelling also compiles, as an extension — import dev.appspike.remoteconfig.all)

7. Prefer the canonical AppSpikeRemoteConfig.getInfo() over config.info (same: the `info`
   property spelling compiles via import dev.appspike.remoteconfig.info)

8. Prefer the canonical settings data class (the GitLive settings { } block also compiles,
   as an extension — import dev.appspike.remoteconfig.settings):
   - Before: config.settings { minimumFetchInterval = 0.seconds; fetchTimeout = 60.seconds }
   - After: AppSpikeRemoteConfig.setConfigSettings(RemoteConfigSettings(minimumFetchIntervalSeconds = 0, fetchTimeoutSeconds = 60))

9. Prefer the canonical Map overload for defaults (the GitLive vararg-of-pairs form also
   compiles, as an extension — import dev.appspike.remoteconfig.setDefaults):
   - Before: config.setDefaults("key1" to "value1", "key2" to 42)
   - After: AppSpikeRemoteConfig.setDefaults(mapOf("key1" to "value1", "key2" to 42))

10. Fetch/activate suspend functions are identical — only change the receiver:
    - config.fetch() → AppSpikeRemoteConfig.fetch()
    - config.activate() → AppSpikeRemoteConfig.activate()
    - config.fetchAndActivate() → AppSpikeRemoteConfig.fetchAndActivate()
    - config.reset() → AppSpikeRemoteConfig.reset() (note: synchronous, not suspend)
    - config.ensureInitialized() → AppSpikeRemoteConfig.ensureInitialized()

11. Replace config.get<T>(key) (reified inline) with typed accessors:
    - config.get<String>(key) → AppSpikeRemoteConfig.getString(key)
    - config.get<Boolean>(key) → AppSpikeRemoteConfig.getBoolean(key)
    - config.get<Long>(key) → AppSpikeRemoteConfig.getLong(key)
    - config.get<Double>(key) → AppSpikeRemoteConfig.getDouble(key)

12. Replace exception types:
    - FirebaseRemoteConfigException → RemoteConfigException (the base type)
    - FirebaseRemoteConfigFetchThrottledException → RemoteConfigThrottledException
    - Catch RemoteConfigFetchException around fetch calls — a failed fetch throws

Apply these changes to every file in the project. After migrating, verify the project builds on all targets.
```

</details>

### Key differences from GitLive

- **No wrapper layer.** AppSpike Remote Config is one Kotlin implementation, not a KMP bridge over the native Remote Config SDKs.
- **Custom signals.** Target users by app-defined attributes. Evaluation runs locally, so the signals never leave the device.
- **Config update listeners.** You get notified when activated values change.
- **Local evaluation.** Conditions are evaluated on-device, not on the server.
- **Time conditions hold.** Changing the device clock will not unlock a time-gated config.

## Architecture

AppSpike Remote Config evaluates all conditions **locally on-device**. Custom signals never leave the device. They're matched against the template's conditions in the SDK, not sent to a server. This provides:

- **Privacy**: targeting signals stay on-device
- **Offline re-evaluation**: changed signals or crossed date/time boundaries apply without a network call
- **Cross-platform consistency**: the same evaluator runs on Android, iOS, and wasmJs, proven by shared test vectors

The SDK fetches a template from the CDN, evaluates it locally against the device context (platform, app version, country, language, custom signals, percent bucket, date/time), and stores the resolved values. The fetch/activate lifecycle mirrors Firebase Remote Config:

1. `fetch()` downloads and evaluates the template
2. `activate()` promotes fetched values to the live config
3. `fetchAndActivate()` does both in one call

## Modules

| Module | Coordinates | Description |
|--------|-------------|-------------|
| SDK Core | `dev.appspike:sdk-core` | Session management, authentication, device context, module contract |
| Remote Config | `dev.appspike:remote-config` | Remote config with local evaluation of all condition types |

## Public API Reference

### `AppSpikeRemoteConfig`

| Method | Description |
|--------|-------------|
| `getString(key)` | String value (empty if absent) |
| `getBoolean(key)` | Boolean value (false if absent) |
| `getLong(key)` | Long value (0 if absent) |
| `getDouble(key)` | Double value (0.0 if absent) |
| `getValue(key)` | `RemoteConfigValue` with source info |
| `getAll()` | All key-value pairs |
| `getKeysByPrefix(prefix)` | Keys matching a prefix |
| `getInfo()` | Last fetch status, time, and settings |
| `setDefaults(map)` | Set in-app defaults |
| `setConfigSettings(settings)` | Set fetch interval and timeout |
| `setCustomSignals(signals)` | Set targeting signals (suspend) |
| `fetch(interval?)` | Fetch config from server (suspend) |
| `activate()` | Promote fetched values to live (suspend) |
| `fetchAndActivate()` | Fetch and activate in one call (suspend) |
| `addOnConfigUpdateListener(listener)` | Listen for config changes. `ConfigUpdateListener` has `onUpdate(ConfigUpdate)` and `onError(RemoteConfigException)`. A lambda overload takes the update alone |
| `configUpdates` | The same stream as a `Flow<ConfigUpdate>` |
| `ensureInitialized()` | Wait for SDK readiness (suspend) |
| `reset()` | Clear all state |

### `RemoteConfigValue`

| Method | Description |
|--------|-------------|
| `asString()` | Value as String |
| `asBoolean()` | Value as Boolean |
| `asLong()` | Value as Long |
| `asDouble()` | Value as Double |
| `asByteArray()` | Value as ByteArray |
| `getSource()` | `REMOTE`, `DEFAULT`, or `STATIC` (also readable as the `source` property) |

### Exceptions

| Type | Description |
|------|-------------|
| `RemoteConfigException` | Base type for every Remote Config error |
| `RemoteConfigFetchException` | A fetch failed: no template URL, a network or HTTP error, or an unparseable template |
| `RemoteConfigThrottledException` | The fetch was refused while backing off after consecutive failures. `throttleEndTimeMillis` is the epoch-millisecond instant at which the next attempt is allowed |

## Sample App

The `SampleApp/` directory contains one KMP build with three runnable apps: Android (Compose),
iOS (SwiftUI), and the browser (Kotlin/Wasm).

Set `APPSPIKE_API_KEY` in `SampleApp/shared/src/commonMain/kotlin/dev/appspike/sample/ApiKey.kt`,
the single key location for all three targets. Then:

**Android**

```bash
cd SampleApp
./gradlew :androidApp:installDebug     # or :androidApp:assembleDebug to just build
```

**iOS** (needs Xcode 16+ and [Tuist](https://tuist.dev), installed with `brew install tuist`):

```bash
cd SampleApp/iosApp
tuist generate                          # generates + opens SampleApp.xcworkspace
```

Then pick the `SampleApp` scheme and an iOS 15+ simulator and press Run (⌘R). A pre-build phase
runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode`, so the `shared` framework the Swift
code imports is always in step with the Kotlin sources.

**Browser (wasm)**

```bash
cd SampleApp
./gradlew :wasmApp:wasmJsBrowserDevelopmentRun --continuous
```

Serves the app at **http://localhost:8080**. Use `:wasmApp:wasmJsBrowserDistribution` for a
static bundle in `wasmApp/build/dist/wasmJs/productionExecutable/`.

## Requirements

- Kotlin 2.0+
- Android API 23+ (for Android target)
- iOS 15+ (for iOS target)

## License

Copyright (c) 2026 Pyde Technologies LTD. All rights reserved.

The AppSpike SDK is proprietary software, free to use with AppSpike services. Redistribution, modification, and reverse engineering are not permitted. See [LICENSE](LICENSE) for the full terms, or contact info@pyde.tech.
