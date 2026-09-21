package dev.appspike.sample.wasm

import dev.appspike.sample.RemoteConfigDemo
import dev.appspike.sample.platformName
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement

/**
 * Browser sample: the same `:shared` commonMain the Android and iOS apps use, compiled to
 * Kotlin/Wasm and rendered with plain DOM elements — no UI framework, so what stays on
 * screen is the SDK integration rather than the widget toolkit.
 */

private val scope: CoroutineScope = MainScope()

// Main view
private lateinit var statusText: HTMLElement
private lateinit var fetchStatusText: HTMLElement
private lateinit var settingsText: HTMLElement
private lateinit var intervalInput: HTMLInputElement
private lateinit var timeoutInput: HTMLInputElement
private lateinit var signalKeyInput: HTMLInputElement
private lateinit var signalValueInput: HTMLInputElement
private lateinit var signalStatusText: HTMLElement
private lateinit var infoText: HTMLElement
private lateinit var keyInput: HTMLInputElement
private lateinit var keyResults: HTMLElement

// All Key/Values view
private lateinit var mainView: HTMLDivElement
private lateinit var allValuesView: HTMLDivElement
private lateinit var prefixInput: HTMLInputElement
private lateinit var allValuesList: HTMLElement
private lateinit var allValuesStatus: HTMLElement

fun main() {
    // Defaults go in before initialize so the first render already has values — they read
    // as (default) in the All Key/Values view until a fetch is activated.
    RemoteConfigDemo.setDefaults()

    buildUi()

    // The API key is a build-time constant in the shared module; there is no key-entry
    // field. An unreplaced placeholder is reported here rather than failing silently.
    RemoteConfigDemo.initialize(null) { result ->
        statusText.textContent = "Setup: $result"
        if (result == "Initialized") {
            scope.launch { statusText.textContent = "Setup: " + RemoteConfigDemo.ensureInitializedSummary() }
        }
    }
    refreshSettings()
}

private fun buildUi() {
    val app = document.getElementById("app") as HTMLElement
    mainView = div("view")
    allValuesView = div("view")
    allValuesView.hidden = true
    app.appendChild(mainView)
    app.appendChild(allValuesView)

    buildMainView()
    buildAllValuesView()
}

private fun buildMainView() = with(mainView) {
    appendChild(heading("AppSpike KMP Sample (${platformName()})"))

    statusText = note("Setup: Not initialized")
    statusText.className = "note accent"
    appendChild(statusText)

    // fetch() honours the minimum fetch interval; throttling is shown, not hidden.
    appendChild(
        row(
            button("Fetch (respect cache)") {
                fetchStatusText.textContent = "Fetching (respecting cache)..."
                scope.launch { fetchStatusText.textContent = RemoteConfigDemo.fetchRespectingCache() }
            },
            button("Activate") {
                scope.launch {
                    val changed = RemoteConfigDemo.activate()
                    fetchStatusText.textContent =
                        if (changed) "Activated new config" else "Nothing new to activate"
                }
            },
        ),
    )
    appendChild(
        row(
            button("Fetch & Activate") {
                fetchStatusText.textContent = "Fetching..."
                scope.launch {
                    fetchStatusText.textContent = try {
                        if (RemoteConfigDemo.fetchAndActivate()) "Config updated" else "No changes"
                    } catch (exception: Exception) {
                        "Error: ${exception.message}"
                    }
                }
            },
            button("Bypass Cache & Activate") {
                fetchStatusText.textContent = "Fetching (cache bypassed)..."
                scope.launch {
                    fetchStatusText.textContent = try {
                        if (RemoteConfigDemo.bypassCacheAndActivate()) {
                            "Fresh config activated"
                        } else {
                            "No changes to activate"
                        }
                    } catch (exception: Exception) {
                        "Error: ${exception.message}"
                    }
                }
            },
        ),
    )
    appendChild(
        row(
            button("Show All Values") { showAllValues(true) },
            // reset() then re-apply the sample defaults, so the all-values view shows the
            // defaults set with (default) sources rather than nothing at all.
            button("Reset") {
                RemoteConfigDemo.resetAndReapplyDefaults()
                refreshSettings()
                infoText.textContent = RemoteConfigDemo.infoSummary()
                fetchStatusText.textContent = "Reset — sample defaults re-applied"
                renderAllValues()
            },
        ),
    )
    fetchStatusText = note("")
    appendChild(fetchStatusText)

    appendChild(divider())
    appendChild(sectionTitle("Config settings"))
    settingsText = note("")
    appendChild(settingsText)
    intervalInput = input("Min interval (s)")
    timeoutInput = input("Timeout (s)")
    appendChild(row(intervalInput, timeoutInput))
    appendChild(
        row(
            button("Apply Settings") {
                val interval = intervalInput.value.trim().toLongOrNull()
                val timeout = timeoutInput.value.trim().toLongOrNull()
                if (interval != null && timeout != null) {
                    RemoteConfigDemo.applySettings(interval, timeout)
                    renderSettings()
                    infoText.textContent = RemoteConfigDemo.infoSummary()
                    fetchStatusText.textContent = "Settings applied"
                } else {
                    fetchStatusText.textContent = "Settings must be numbers"
                }
            },
            button("Restore Defaults") {
                RemoteConfigDemo.restoreDefaultSettings()
                refreshSettings()
                infoText.textContent = RemoteConfigDemo.infoSummary()
                fetchStatusText.textContent = "Settings restored to defaults"
            },
        ),
    )

    appendChild(divider())
    appendChild(sectionTitle("Custom signals"))
    appendChild(
        note(
            "Targeting attributes for custom_signal conditions, evaluated on-device and " +
                "never transmitted.",
        ),
    )
    signalKeyInput = input("Signal key")
    signalValueInput = input("Signal value")
    appendChild(row(signalKeyInput, signalValueInput))
    appendChild(
        row(
            button("Apply Signal") {
                val entered = signalKeyInput.value.trim()
                if (entered.isEmpty()) {
                    signalStatusText.textContent = "Enter a signal key"
                } else {
                    val value = signalValueInput.value
                    signalStatusText.textContent = "Applying $entered..."
                    scope.launch {
                        signalStatusText.textContent = try {
                            RemoteConfigDemo.applySignal(entered, value)
                            "Applied $entered=$value, fetched and activated"
                        } catch (exception: Exception) {
                            "Error: ${exception.message}"
                        }
                    }
                }
            },
            button("Remove Signal") {
                val entered = signalKeyInput.value.trim()
                if (entered.isEmpty()) {
                    signalStatusText.textContent = "Enter a signal key"
                } else {
                    scope.launch {
                        signalStatusText.textContent = try {
                            RemoteConfigDemo.removeSignal(entered)
                            "Signal \"$entered\" removed (null value)"
                        } catch (exception: Exception) {
                            "Error: ${exception.message}"
                        }
                    }
                }
            },
            // Convenience preset, in addition to the free-form entry above.
            button("Preset: tier=gold") {
                scope.launch {
                    signalStatusText.textContent = try {
                        RemoteConfigDemo.setSampleSignals()
                        "Preset set: tier, session_count, day_of_trial, spend"
                    } catch (exception: Exception) {
                        "Error: ${exception.message}"
                    }
                }
            },
        ),
    )
    signalStatusText = note("")
    appendChild(signalStatusText)

    appendChild(divider())
    appendChild(sectionTitle("Info"))
    appendChild(row(button("Refresh Info") { infoText.textContent = RemoteConfigDemo.infoSummary() }))
    infoText = note("")
    appendChild(infoText)

    appendChild(divider())
    keyInput = input("Key")
    keyInput.className = "wide"
    keyInput.oninput = { renderKeyResults() }
    appendChild(row(keyInput))
    keyResults = div("results")
    appendChild(keyResults)
}

private fun buildAllValuesView() = with(allValuesView) {
    appendChild(heading("All Key/Values"))
    appendChild(
        row(
            button("Back") { showAllValues(false) },
            // Clears activated values, defaults, custom signals, and settings — then puts
            // the sample defaults back, so this view shows them with (default) sources.
            button("Reset") {
                RemoteConfigDemo.resetAndReapplyDefaults()
                allValuesStatus.textContent = "Reset — sample defaults re-applied"
                refreshSettings()
                renderAllValues()
            },
        ),
    )
    prefixInput = input("Filter by key prefix")
    prefixInput.className = "wide"
    prefixInput.oninput = { renderAllValues() }
    appendChild(row(prefixInput))
    allValuesStatus = note("")
    appendChild(allValuesStatus)
    allValuesList = div("results")
    appendChild(allValuesList)
}

private fun showAllValues(show: Boolean) {
    mainView.hidden = show
    allValuesView.hidden = !show
    if (show) renderAllValues()
}

private fun refreshSettings() {
    renderSettings()
    val (interval, timeout) = RemoteConfigDemo.configSettings()
    intervalInput.value = interval.toString()
    timeoutInput.value = timeout.toString()
}

private fun renderSettings() {
    val (interval, timeout) = RemoteConfigDemo.configSettings()
    settingsText.textContent =
        "Current: minimum fetch interval = ${interval}s, fetch timeout = ${timeout}s"
}

private fun renderKeyResults() {
    keyResults.textContent = ""
    val key = keyInput.value
    if (key.isEmpty()) return
    keyResults.appendChild(line("getString: ${RemoteConfigDemo.getString(key)}", "accent"))
    keyResults.appendChild(line("getBoolean: ${RemoteConfigDemo.getBoolean(key)}"))
    keyResults.appendChild(line("getLong: ${RemoteConfigDemo.getLong(key)}"))
    keyResults.appendChild(line("getDouble: ${RemoteConfigDemo.getDouble(key)}"))
    keyResults.appendChild(line("getValue accessors:", "strong"))
    RemoteConfigDemo.inspectValue(key).forEach { (accessor, result) ->
        keyResults.appendChild(line("  $accessor = $result", "note"))
    }
}

private fun renderAllValues() {
    allValuesList.textContent = ""
    val prefix = prefixInput.value
    val allowed = if (prefix.isEmpty()) null else RemoteConfigDemo.keysWithPrefix(prefix).toSet()
    val visible = RemoteConfigDemo.getAllEntries()
        .filter { (key, _, _) -> allowed == null || key in allowed }
    if (visible.isEmpty()) {
        allValuesList.appendChild(line("No values yet — set defaults or fetch and activate first."))
        return
    }
    visible.forEach { (key, value, source) ->
        val entry = div("entry")
        entry.appendChild(line(key, "strong"))
        entry.appendChild(line("$value  $source", "accent"))
        allValuesList.appendChild(entry)
    }
}

// --- Tiny DOM helpers ------------------------------------------------------------------

private fun div(className: String): HTMLDivElement =
    (document.createElement("div") as HTMLDivElement).also { it.className = className }

private fun heading(text: String): HTMLElement =
    (document.createElement("h1") as HTMLElement).also { it.textContent = text }

private fun sectionTitle(text: String): HTMLElement =
    (document.createElement("h2") as HTMLElement).also { it.textContent = text }

private fun note(text: String): HTMLElement =
    (document.createElement("p") as HTMLElement).also {
        it.className = "note"
        it.textContent = text
    }

private fun line(text: String, className: String = ""): HTMLElement =
    (document.createElement("div") as HTMLElement).also {
        it.className = className
        it.textContent = text
    }

private fun divider(): HTMLElement = document.createElement("hr") as HTMLElement

private fun row(vararg children: HTMLElement): HTMLDivElement =
    div("row").also { row -> children.forEach { row.appendChild(it) } }

private fun button(label: String, onClick: () -> Unit): HTMLButtonElement =
    (document.createElement("button") as HTMLButtonElement).also {
        it.textContent = label
        it.onclick = { onClick() }
    }

private fun input(placeholder: String): HTMLInputElement =
    (document.createElement("input") as HTMLInputElement).also {
        it.type = "text"
        it.placeholder = placeholder
    }
