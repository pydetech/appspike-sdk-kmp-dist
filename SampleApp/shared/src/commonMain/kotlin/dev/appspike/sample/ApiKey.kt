package dev.appspike.sample

/**
 * The one place this sample stores its AppSpike API key — shared by every target, so the
 * Android app and an Xcode host read the same constant.
 *
 * Replace the placeholder with the `pk_live_…` key for your app from
 * [console.appspike.dev](https://console.appspike.dev). The sample has no runtime key-entry
 * field on purpose: a text field models an integration nobody ships — real apps pass the key
 * at build time, so the sample does the same.
 *
 * While the placeholder is unchanged the sample does not initialize and shows a setup
 * message on screen instead of failing silently.
 */
const val APPSPIKE_API_KEY = "YOUR_API_KEY"

/** The placeholder shipped in this repo; [APPSPIKE_API_KEY] must be replaced to initialize. */
const val API_KEY_PLACEHOLDER = "YOUR_API_KEY"

/** Where the reader has to go to fix an unset key — shown on screen by every target. */
const val API_KEY_LOCATION = "SampleApp/shared/src/commonMain/kotlin/dev/appspike/sample/ApiKey.kt"
