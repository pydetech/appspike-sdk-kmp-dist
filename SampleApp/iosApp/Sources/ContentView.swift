import SwiftUI

struct ContentView: View {
    @StateObject private var model = RemoteConfigViewModel()
    @State private var started = false

    var body: some View {
        NavigationView {
            Form {
                setupSection
                fetchSection
                settingsSection
                signalsSection
                infoSection
                inspectorSection
            }
            .navigationTitle("AppSpike KMP Sample")
        }
        .navigationViewStyle(.stack)
        .onAppear {
            guard !started else { return }
            started = true
            model.start()
        }
    }

    // MARK: - Setup

    private var setupSection: some View {
        Section("Setup") {
            Text(model.status)
                .font(.footnote)
                .foregroundColor(.accentColor)
            Text("Platform: \(model.platformName)")
                .font(.footnote)
                .foregroundColor(.secondary)
        }
    }

    // MARK: - Fetch controls

    private var fetchSection: some View {
        Section("Fetch controls") {
            Button("Fetch (respect cache)") { Task { await model.fetchRespectingCache() } }
            Button("Activate") { Task { await model.activate() } }
            Button("Fetch & Activate") { Task { await model.fetchAndActivate() } }
            Button("Bypass Cache & Activate") { Task { await model.bypassCacheAndActivate() } }
            NavigationLink("Show All Values", destination: AllValuesView(model: model))
            Button("Reset") { model.reset() }
            if !model.fetchStatus.isEmpty {
                Text(model.fetchStatus)
                    .font(.footnote)
                    .foregroundColor(.secondary)
            }
        }
    }

    // MARK: - Config settings

    private var settingsSection: some View {
        Section("Config settings") {
            Text(
                "Current: minimum fetch interval = "
                    + String(model.minimumFetchIntervalSeconds) + "s, fetch timeout = "
                    + String(model.fetchTimeoutSeconds) + "s"
            )
            .font(.footnote)
            .foregroundColor(.secondary)

            HStack {
                Text("Min interval (s)")
                Spacer()
                TextField("Min interval (s)", text: $model.intervalField)
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: 110)
            }
            HStack {
                Text("Timeout (s)")
                Spacer()
                TextField("Timeout (s)", text: $model.timeoutField)
                    .keyboardType(.numberPad)
                    .multilineTextAlignment(.trailing)
                    .frame(maxWidth: 110)
            }
            Button("Apply Settings") { model.applySettings() }
            Button("Restore Defaults") { model.restoreDefaultSettings() }
        }
    }

    // MARK: - Custom signals

    private var signalsSection: some View {
        Section("Custom signals") {
            Text(
                "Targeting attributes for custom_signal conditions, evaluated on-device "
                    + "and never transmitted."
            )
            .font(.footnote)
            .foregroundColor(.secondary)

            TextField("Signal key", text: $model.signalKey)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
            TextField("Signal value", text: $model.signalValue)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
            Button("Apply Signal") { Task { await model.applySignal() } }
            Button("Remove Signal") { Task { await model.removeSignal() } }
            // Convenience preset, in addition to the free-form entry above.
            Button("Preset: tier=gold") { Task { await model.applyPresetSignals() } }
            if !model.signalStatus.isEmpty {
                Text(model.signalStatus)
                    .font(.footnote)
                    .foregroundColor(.secondary)
            }
        }
    }

    // MARK: - Info

    private var infoSection: some View {
        Section("Info") {
            Button("Refresh Info") { model.refreshInfo() }
            if !model.infoText.isEmpty {
                Text(model.infoText)
                    .font(.footnote)
                    .foregroundColor(.secondary)
            }
        }
    }

    // MARK: - Typed getters / value inspection

    private var inspectorSection: some View {
        Section("Value inspection") {
            TextField("Key", text: $model.lookupKey)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
            if !model.lookupKey.isEmpty {
                Text("getString: \(model.getString(model.lookupKey))")
                    .foregroundColor(.accentColor)
                Text("getBoolean: " + String(model.getBoolean(model.lookupKey)))
                Text("getLong: " + String(model.getLong(model.lookupKey)))
                Text("getDouble: " + String(model.getDouble(model.lookupKey)))
                Text("getValue accessors:").font(.subheadline.bold())
                ForEach(model.inspectValue(model.lookupKey), id: \.0) { accessor, result in
                    Text("  \(accessor) = \(result)")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
            }
        }
    }
}
