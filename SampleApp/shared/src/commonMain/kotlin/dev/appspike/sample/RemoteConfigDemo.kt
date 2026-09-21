package dev.appspike.sample

import dev.appspike.AppSpike
import dev.appspike.InitResult
import dev.appspike.remoteconfig.AppSpikeRemoteConfig
import dev.appspike.remoteconfig.CustomSignals
import dev.appspike.remoteconfig.RemoteConfigSettings
import dev.appspike.remoteconfig.RemoteConfigThrottledException
import dev.appspike.remoteconfig.ValueSource

object RemoteConfigDemo {
    /**
     * In-app defaults are served until a fetched template is activated, and as fallback for
     * keys the template does not define. Call before initialize so the first render already
     * has values — they show as `(default)` in the all-values screen until a fetch is
     * activated, and [resetAndReapplyDefaults] puts them back after a reset.
     */
    fun setDefaults() {
        AppSpikeRemoteConfig.setDefaults(
            mapOf(
                "welcome_message" to "Hello from defaults",
                "feature_enabled" to false,
                "max_retries" to 3L,
                "price_multiplier" to 1.0,
                // Platform-specific extra: Android/KMP accept ByteArray defaults, which the
                // SDK preserves byte for byte and hands back via getValue().asByteArray().
                "binary_payload" to byteArrayOf(0x41, 0x70, 0x70, 0x53, 0x70, 0x69, 0x6B, 0x65),
            ),
        )
    }

    /**
     * Initializes with [APPSPIKE_API_KEY]. While that constant still holds the placeholder,
     * initialization is skipped and the reason — naming the file to edit — is reported to
     * [onResult] so the UI can show it.
     */
    fun initialize(context: Any?, onResult: (String) -> Unit) {
        if (APPSPIKE_API_KEY == API_KEY_PLACEHOLDER) {
            onResult("Not initialized — set your API key in $API_KEY_LOCATION")
            return
        }
        AppSpike.initialize(
            context = context,
            apiKey = APPSPIKE_API_KEY,
            modules = listOf(AppSpikeRemoteConfig),
        ) { result ->
            when (result) {
                is InitResult.Success -> onResult("Initialized")
                is InitResult.Error -> onResult("Init failed: ${result.message}")
            }
        }
    }

    /** Suspends until the first activated-or-defaults state is loaded, then reports it. */
    suspend fun ensureInitializedSummary(): String {
        val info = AppSpikeRemoteConfig.ensureInitialized()
        return "Ready — last fetch: ${info.lastFetchStatus}"
    }

    fun getString(key: String): String = AppSpikeRemoteConfig.getString(key)
    fun getBoolean(key: String): Boolean = AppSpikeRemoteConfig.getBoolean(key)
    fun getLong(key: String): Long = AppSpikeRemoteConfig.getLong(key)
    fun getDouble(key: String): Double = AppSpikeRemoteConfig.getDouble(key)

    /**
     * Source labels are rendered lowercase in parentheses — `(remote)`, `(default)`,
     * `(static)` — rather than the raw enum name, so every AppSpike sample reads the same way.
     */
    private fun sourceLabel(source: ValueSource): String = "(${source.name.lowercase()})"

    /** Every RemoteConfigValue accessor plus its source, for the value-inspector card. */
    fun inspectValue(key: String): List<Pair<String, String>> {
        val value = AppSpikeRemoteConfig.getValue(key)
        return listOf(
            "asString" to value.asString(),
            "asBoolean" to value.asBoolean().toString(),
            "asLong" to value.asLong().toString(),
            "asDouble" to value.asDouble().toString(),
            "asByteArray" to "${value.asByteArray().size} bytes",
            "source" to sourceLabel(value.getSource()),
        )
    }

    /** Plain fetch honours the minimum fetch interval — throttling is surfaced, not hidden. */
    suspend fun fetchRespectingCache(): String = try {
        AppSpikeRemoteConfig.fetch()
        "Fetched (call Activate to apply)"
    } catch (throttled: RemoteConfigThrottledException) {
        "Throttled: ${throttled.message} — use Bypass Cache to force"
    }

    suspend fun activate(): Boolean = AppSpikeRemoteConfig.activate()

    suspend fun fetchAndActivate(): Boolean = AppSpikeRemoteConfig.fetchAndActivate()

    /** Interval 0 ignores the minimum fetch interval, so the fetch always hits the server. */
    suspend fun bypassCacheAndActivate(): Boolean {
        AppSpikeRemoteConfig.fetch(minimumFetchIntervalSeconds = 0)
        return AppSpikeRemoteConfig.activate()
    }

    fun applySettings(minimumFetchIntervalSeconds: Long, fetchTimeoutSeconds: Long) {
        AppSpikeRemoteConfig.setConfigSettings(
            RemoteConfigSettings(
                minimumFetchIntervalSeconds = minimumFetchIntervalSeconds,
                fetchTimeoutSeconds = fetchTimeoutSeconds,
            ),
        )
    }

    /** The no-argument constructor carries the SDK defaults (43200s interval, 60s timeout). */
    fun restoreDefaultSettings() {
        AppSpikeRemoteConfig.setConfigSettings(RemoteConfigSettings())
    }

    /** The live (minimum fetch interval, fetch timeout) pair, in seconds. */
    fun configSettings(): Pair<Long, Long> {
        val settings = AppSpikeRemoteConfig.getInfo().configSettings
        return settings.minimumFetchIntervalSeconds to settings.fetchTimeoutSeconds
    }

    fun infoSummary(): String {
        val info = AppSpikeRemoteConfig.getInfo()
        val settings = info.configSettings
        return "lastFetchStatus=${info.lastFetchStatus}, lastFetchTimeMillis=${info.lastFetchTimeMillis}, " +
            "interval=${settings.minimumFetchIntervalSeconds}s, timeout=${settings.fetchTimeoutSeconds}s"
    }

    /**
     * Free-form signal entry: sets [key] to [value], then re-evaluates immediately by
     * bypassing the fetch cache and activating, because signals change targeting.
     */
    suspend fun applySignal(key: String, value: String) {
        AppSpikeRemoteConfig.setCustomSignals(
            CustomSignals.Builder().put(key, value).build(),
        )
        AppSpikeRemoteConfig.fetch(minimumFetchIntervalSeconds = 0)
        AppSpikeRemoteConfig.activate()
    }

    /** Convenience preset, in addition to the free-form entry: every builder put type. */
    suspend fun setSampleSignals() {
        AppSpikeRemoteConfig.setCustomSignals(
            CustomSignals.Builder()
                .put("tier", "gold")
                .put("session_count", 12L)
                .put("day_of_trial", 3)
                .put("spend", 9.99)
                .build(),
        )
    }

    /** A null value removes the signal (Firebase semantics). */
    suspend fun removeSignal(key: String) {
        AppSpikeRemoteConfig.setCustomSignals(
            CustomSignals.Builder().put(key, null as String?).build(),
        )
    }

    /** All known key/values as (key, value, source label) triples, sorted by key. */
    fun getAllEntries(): List<Triple<String, String, String>> =
        AppSpikeRemoteConfig.getAll()
            .toList()
            .sortedBy { (key, _) -> key }
            .map { (key, value) -> Triple(key, value.asString(), sourceLabel(value.getSource())) }

    fun keysWithPrefix(prefix: String): List<String> =
        AppSpikeRemoteConfig.getKeysByPrefix(prefix).sorted()

    /**
     * Clears activated values, defaults, custom signals, and settings — then re-applies the
     * sample defaults, so the all-values screen afterwards shows them with `(default)`
     * sources and no remote rows.
     */
    fun resetAndReapplyDefaults() {
        AppSpikeRemoteConfig.reset()
        setDefaults()
    }
}
