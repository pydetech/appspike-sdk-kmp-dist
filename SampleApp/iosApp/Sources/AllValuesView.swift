import SwiftUI

/// The shared `All Key/Values` screen: every `getAll()` row with its lowercase source
/// label, narrowed by the SDK's own prefix filter.
struct AllValuesView: View {
    @ObservedObject var model: RemoteConfigViewModel
    @State private var prefix = ""

    var body: some View {
        let entries = model.entries(prefix: prefix)
        return List {
            Section {
                TextField("Filter by key prefix", text: $prefix)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                // Clears activated values, defaults, custom signals, and settings — then
                // puts the sample defaults back, so this screen shows them with (default)
                // sources.
                Button("Reset") { model.reset() }
            }
            Section {
                if entries.isEmpty {
                    Text("No values yet — set defaults or fetch and activate first.")
                        .foregroundColor(.secondary)
                }
                ForEach(entries) { entry in
                    VStack(alignment: .leading, spacing: 2) {
                        Text(entry.key).font(.subheadline.bold())
                        Text("\(entry.value)  \(entry.source)")
                            .foregroundColor(.accentColor)
                    }
                }
            }
        }
        .id(model.revision)
        .navigationTitle("All Key/Values")
    }
}
