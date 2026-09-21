import Foundation
import shared

/// One key/value row of the `All Key/Values` view.
struct ConfigEntry: Identifiable {
    let id: String
    let key: String
    let value: String
    /// Already formatted by the shared module as `(remote)` / `(default)` / `(static)`.
    let source: String
}

/// A thin SwiftUI-facing wrapper over `RemoteConfigDemo` in the shared KMP module — the
/// same code the Android and wasm apps drive, so every platform shows the same behaviour.
@MainActor
final class RemoteConfigViewModel: ObservableObject {
    @Published var status = "Not initialized"
    @Published var fetchStatus = ""
    @Published var signalStatus = ""
    @Published var infoText = ""

    /// The live settings pair, shown read-only above the editable fields.
    @Published var minimumFetchIntervalSeconds: Int64 = 0
    @Published var fetchTimeoutSeconds: Int64 = 0
    @Published var intervalField = ""
    @Published var timeoutField = ""

    @Published var signalKey = ""
    @Published var signalValue = ""

    @Published var lookupKey = ""

    /// Bumped whenever config state changes, so the all-values view re-reads `getAll()`.
    @Published var revision = 0

    private let demo = RemoteConfigDemo.shared

    var platformName: String { Platform_iosKt.platformName() }

    func start() {
        // Defaults go in before initialize so the first render already has values — they
        // read as (default) in the all-values view until a fetch is activated.
        demo.setDefaults()
        // The API key is a build-time constant in the shared module; there is no key-entry
        // field. An unreplaced placeholder is reported here rather than failing silently.
        demo.initialize(context: nil) { [weak self] result in
            Task { @MainActor [weak self] in
                guard let self else { return }
                self.status = result
                if result == "Initialized",
                   let summary = try? await self.demo.ensureInitializedSummary() {
                    self.status = summary
                }
            }
        }
        refreshSettings()
    }

    // MARK: - Fetch controls

    /// `fetch()` honours the minimum fetch interval; throttling is shown, not hidden.
    func fetchRespectingCache() async {
        fetchStatus = "Fetching (respecting cache)..."
        do {
            fetchStatus = try await demo.fetchRespectingCache()
        } catch {
            fetchStatus = "Error: \(error.localizedDescription)"
        }
        revision += 1
    }

    func activate() async {
        do {
            let changed = try await demo.activate()
            fetchStatus = changed.boolValue ? "Activated new config" : "Nothing new to activate"
        } catch {
            fetchStatus = "Error: \(error.localizedDescription)"
        }
        revision += 1
    }

    func fetchAndActivate() async {
        fetchStatus = "Fetching..."
        do {
            let changed = try await demo.fetchAndActivate()
            fetchStatus = changed.boolValue ? "Config updated" : "No changes"
        } catch {
            fetchStatus = "Error: \(error.localizedDescription)"
        }
        revision += 1
    }

    func bypassCacheAndActivate() async {
        fetchStatus = "Fetching (cache bypassed)..."
        do {
            let changed = try await demo.bypassCacheAndActivate()
            fetchStatus = changed.boolValue ? "Fresh config activated" : "No changes to activate"
        } catch {
            fetchStatus = "Error: \(error.localizedDescription)"
        }
        revision += 1
    }

    /// `reset()` then re-apply the sample defaults, so the all-values view shows the
    /// defaults set with (default) sources rather than nothing at all.
    func reset() {
        demo.resetAndReapplyDefaults()
        refreshSettings()
        infoText = demo.infoSummary()
        fetchStatus = "Reset — sample defaults re-applied"
        revision += 1
    }

    // MARK: - Config settings

    func refreshSettings() {
        readSettings()
        intervalField = String(minimumFetchIntervalSeconds)
        timeoutField = String(fetchTimeoutSeconds)
    }

    private func readSettings() {
        let settings = demo.configSettings()
        minimumFetchIntervalSeconds = settings.first?.int64Value ?? 0
        fetchTimeoutSeconds = settings.second?.int64Value ?? 0
    }

    func applySettings() {
        guard let interval = Int64(intervalField.trimmingCharacters(in: .whitespaces)),
              let timeout = Int64(timeoutField.trimmingCharacters(in: .whitespaces)) else {
            fetchStatus = "Settings must be numbers"
            return
        }
        demo.applySettings(minimumFetchIntervalSeconds: interval, fetchTimeoutSeconds: timeout)
        readSettings()
        infoText = demo.infoSummary()
        fetchStatus = "Settings applied"
    }

    func restoreDefaultSettings() {
        demo.restoreDefaultSettings()
        refreshSettings()
        infoText = demo.infoSummary()
        fetchStatus = "Settings restored to defaults"
    }

    // MARK: - Custom signals

    func applySignal() async {
        let entered = signalKey.trimmingCharacters(in: .whitespaces)
        guard !entered.isEmpty else {
            signalStatus = "Enter a signal key"
            return
        }
        let value = signalValue
        signalStatus = "Applying \(entered)..."
        do {
            try await demo.applySignal(key: entered, value: value)
            signalStatus = "Applied \(entered)=\(value), fetched and activated"
        } catch {
            signalStatus = "Error: \(error.localizedDescription)"
        }
        revision += 1
    }

    func removeSignal() async {
        let entered = signalKey.trimmingCharacters(in: .whitespaces)
        guard !entered.isEmpty else {
            signalStatus = "Enter a signal key"
            return
        }
        do {
            try await demo.removeSignal(key: entered)
            signalStatus = "Signal \"\(entered)\" removed (null value)"
        } catch {
            signalStatus = "Error: \(error.localizedDescription)"
        }
        revision += 1
    }

    /// Convenience preset, in addition to the free-form entry.
    func applyPresetSignals() async {
        do {
            try await demo.setSampleSignals()
            signalStatus = "Preset set: tier, session_count, day_of_trial, spend"
        } catch {
            signalStatus = "Error: \(error.localizedDescription)"
        }
        revision += 1
    }

    // MARK: - Info

    func refreshInfo() {
        infoText = demo.infoSummary()
    }

    // MARK: - Typed getters / value inspection

    func getString(_ key: String) -> String { demo.getString(key: key) }
    func getBoolean(_ key: String) -> Bool { demo.getBoolean(key: key) }
    func getLong(_ key: String) -> Int64 { demo.getLong(key: key) }
    func getDouble(_ key: String) -> Double { demo.getDouble(key: key) }

    /// Every `RemoteConfigValue` accessor plus its source, as (accessor, result) rows.
    func inspectValue(_ key: String) -> [(String, String)] {
        demo.inspectValue(key: key).map { pair in
            ((pair.first as String?) ?? "", (pair.second as String?) ?? "")
        }
    }

    // MARK: - All values

    /// `getAll()` rows, optionally narrowed by the SDK's own `getKeysByPrefix`.
    func entries(prefix: String) -> [ConfigEntry] {
        let trimmed = prefix.trimmingCharacters(in: .whitespaces)
        let allowed: Set<String>? = trimmed.isEmpty
            ? nil
            : Set(demo.keysWithPrefix(prefix: trimmed))
        return demo.getAllEntries().compactMap { triple in
            let key = (triple.first as String?) ?? ""
            if let allowed, !allowed.contains(key) { return nil }
            return ConfigEntry(
                id: key,
                key: key,
                value: (triple.second as String?) ?? "",
                source: (triple.third as String?) ?? ""
            )
        }
    }
}
