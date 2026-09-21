package dev.appspike.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.appspike.sample.RemoteConfigDemo
import dev.appspike.sample.platformName
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold { padding ->
                    var showAllValues by remember { mutableStateOf(false) }
                    if (showAllValues) {
                        AllValuesScreen(
                            onBack = { showAllValues = false },
                            modifier = Modifier.padding(padding),
                        )
                    } else {
                        SampleScreen(
                            onShowAllValues = { showAllValues = true },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleScreen(
    onShowAllValues: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Not initialized") }
    var fetchStatus by remember { mutableStateOf("") }
    var infoText by remember { mutableStateOf("") }
    var signalStatus by remember { mutableStateOf("") }
    var settings by remember { mutableStateOf(RemoteConfigDemo.configSettings()) }
    var intervalSeconds by remember { mutableStateOf(settings.first.toString()) }
    var timeoutSeconds by remember { mutableStateOf(settings.second.toString()) }
    var signalKey by remember { mutableStateOf("") }
    var signalValue by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }

    fun refreshSettings() {
        settings = RemoteConfigDemo.configSettings()
        intervalSeconds = settings.first.toString()
        timeoutSeconds = settings.second.toString()
    }

    LaunchedEffect(Unit) {
        // Defaults go in before initialize so the first render already has values —
        // they read as (default) in the all-values screen until a fetch is activated.
        RemoteConfigDemo.setDefaults()
        // The API key is a build-time constant in the shared module; there is no key-entry
        // field. An unreplaced placeholder is reported here rather than failing silently.
        RemoteConfigDemo.initialize(context.applicationContext) { result ->
            status = result
            if (result == "Initialized") {
                scope.launch { status = RemoteConfigDemo.ensureInitializedSummary() }
            }
        }
        refreshSettings()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // Keeps the focused field above the keyboard (with adjustResize in the manifest).
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "AppSpike KMP Sample (${platformName()})",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            "Setup: $status",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )

        // fetch() honours the minimum fetch interval; throttling is shown, not hidden.
        Button(onClick = {
            fetchStatus = "Fetching (respecting cache)..."
            scope.launch { fetchStatus = RemoteConfigDemo.fetchRespectingCache() }
        }) { Text("Fetch (respect cache)") }

        Button(onClick = {
            scope.launch {
                val changed = RemoteConfigDemo.activate()
                fetchStatus = if (changed) "Activated new config" else "Nothing new to activate"
            }
        }) { Text("Activate") }

        Button(onClick = {
            fetchStatus = "Fetching..."
            scope.launch {
                try {
                    val changed = RemoteConfigDemo.fetchAndActivate()
                    fetchStatus = if (changed) "Config updated" else "No changes"
                } catch (exception: Exception) {
                    fetchStatus = "Error: ${exception.message}"
                }
            }
        }) { Text("Fetch & Activate") }

        Button(onClick = {
            fetchStatus = "Fetching (cache bypassed)..."
            scope.launch {
                try {
                    val changed = RemoteConfigDemo.bypassCacheAndActivate()
                    fetchStatus = if (changed) "Fresh config activated" else "No changes to activate"
                } catch (exception: Exception) {
                    fetchStatus = "Error: ${exception.message}"
                }
            }
        }) { Text("Bypass Cache & Activate") }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onShowAllValues) { Text("Show All Values") }
            OutlinedButton(onClick = {
                // reset() then re-apply the sample defaults, so the all-values screen shows
                // the defaults set with (default) sources rather than nothing at all.
                RemoteConfigDemo.resetAndReapplyDefaults()
                refreshSettings()
                infoText = RemoteConfigDemo.infoSummary()
                fetchStatus = "Reset — sample defaults re-applied"
            }) { Text("Reset") }
        }

        if (fetchStatus.isNotEmpty()) {
            Text(fetchStatus, style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider()

        Text("Config settings", style = MaterialTheme.typography.titleMedium)
        Text(
            "Current: minimum fetch interval = ${settings.first}s, " +
                "fetch timeout = ${settings.second}s",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = intervalSeconds,
                onValueChange = { intervalSeconds = it },
                label = { Text("Min interval (s)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = timeoutSeconds,
                onValueChange = { timeoutSeconds = it },
                label = { Text("Timeout (s)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val interval = intervalSeconds.toLongOrNull()
                val timeout = timeoutSeconds.toLongOrNull()
                if (interval != null && timeout != null) {
                    RemoteConfigDemo.applySettings(interval, timeout)
                    settings = RemoteConfigDemo.configSettings()
                    infoText = RemoteConfigDemo.infoSummary()
                    fetchStatus = "Settings applied"
                } else {
                    fetchStatus = "Settings must be numbers"
                }
            }) { Text("Apply Settings") }

            OutlinedButton(onClick = {
                RemoteConfigDemo.restoreDefaultSettings()
                refreshSettings()
                infoText = RemoteConfigDemo.infoSummary()
                fetchStatus = "Settings restored to defaults"
            }) { Text("Restore Defaults") }
        }

        HorizontalDivider()

        Text("Custom signals", style = MaterialTheme.typography.titleMedium)
        Text(
            "Targeting attributes for custom_signal conditions, evaluated on-device and " +
                "never transmitted.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = signalKey,
                onValueChange = { signalKey = it },
                label = { Text("Signal key") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = signalValue,
                onValueChange = { signalValue = it },
                label = { Text("Signal value") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val entered = signalKey.trim()
                if (entered.isEmpty()) {
                    signalStatus = "Enter a signal key"
                } else {
                    signalStatus = "Applying $entered..."
                    scope.launch {
                        signalStatus = try {
                            RemoteConfigDemo.applySignal(entered, signalValue)
                            "Applied $entered=$signalValue, fetched and activated"
                        } catch (exception: Exception) {
                            "Error: ${exception.message}"
                        }
                    }
                }
            }) { Text("Apply Signal") }

            OutlinedButton(onClick = {
                val entered = signalKey.trim()
                if (entered.isEmpty()) {
                    signalStatus = "Enter a signal key"
                } else {
                    scope.launch {
                        signalStatus = try {
                            RemoteConfigDemo.removeSignal(entered)
                            "Signal \"$entered\" removed (null value)"
                        } catch (exception: Exception) {
                            "Error: ${exception.message}"
                        }
                    }
                }
            }) { Text("Remove Signal") }
        }
        // Convenience preset, in addition to the free-form entry above.
        OutlinedButton(onClick = {
            scope.launch {
                signalStatus = try {
                    RemoteConfigDemo.setSampleSignals()
                    "Preset set: tier, session_count, day_of_trial, spend"
                } catch (exception: Exception) {
                    "Error: ${exception.message}"
                }
            }
        }) { Text("Preset: tier=gold") }
        if (signalStatus.isNotEmpty()) {
            Text(signalStatus, style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider()

        Text("Info", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { infoText = RemoteConfigDemo.infoSummary() }) {
            Text("Refresh Info")
        }
        if (infoText.isNotEmpty()) {
            Text(infoText, style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider()

        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (key.isNotEmpty()) {
            Text(
                "getString: ${RemoteConfigDemo.getString(key)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("getBoolean: ${RemoteConfigDemo.getBoolean(key)}")
            Text("getLong: ${RemoteConfigDemo.getLong(key)}")
            Text("getDouble: ${RemoteConfigDemo.getDouble(key)}")
            Text("getValue accessors:", style = MaterialTheme.typography.titleSmall)
            RemoteConfigDemo.inspectValue(key).forEach { (accessor, result) ->
                Text("  $accessor = $result", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AllValuesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var prefix by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var resetStatus by remember { mutableStateOf("") }
    val entries = remember(refresh) { RemoteConfigDemo.getAllEntries() }
    val filteredKeys = remember(prefix, refresh) {
        if (prefix.isEmpty()) null else RemoteConfigDemo.keysWithPrefix(prefix)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("All Key/Values", style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            OutlinedButton(onClick = {
                // Clears activated values, defaults, custom signals, and settings — then puts
                // the sample defaults back, so this screen shows them with (default) sources.
                RemoteConfigDemo.resetAndReapplyDefaults()
                resetStatus = "Reset — sample defaults re-applied"
                refresh++
            }) { Text("Reset") }
        }

        OutlinedTextField(
            value = prefix,
            onValueChange = { prefix = it },
            label = { Text("Filter by key prefix") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (resetStatus.isNotEmpty()) {
            Text(resetStatus, style = MaterialTheme.typography.bodySmall)
        }

        val visible = if (filteredKeys == null) {
            entries
        } else {
            entries.filter { (entryKey, _, _) -> entryKey in filteredKeys }
        }
        if (visible.isEmpty()) {
            Text(
                "No values yet — set defaults or fetch and activate first.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        visible.forEach { (entryKey, value, source) ->
            Column {
                Text(entryKey, style = MaterialTheme.typography.titleSmall)
                Text(
                    "$value  $source",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
