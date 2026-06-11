package com.lautarovculic.intentions.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lautarovculic.intentions.core.model.RootProvider
import com.lautarovculic.intentions.ui.components.KeyValueRow
import com.lautarovculic.intentions.ui.components.Pill
import com.lautarovculic.intentions.ui.components.SectionCard
import com.lautarovculic.intentions.ui.navigation.Routes
import com.lautarovculic.intentions.ui.rememberAppContainer
import com.lautarovculic.intentions.ui.theme.AccentCyan
import com.lautarovculic.intentions.ui.theme.AccentGreen
import com.lautarovculic.intentions.ui.theme.AccentRed
import com.lautarovculic.intentions.ui.theme.AccentYellow

@Composable
fun DashboardScreen(onNavigate: (String) -> Unit) {
    val container = rememberAppContainer()
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
    val ui by vm.ui.collectAsStateWithLifecycle()
    val captureRunning by container.captureRunning.collectAsState()
    val historyCount by container.intentRepository.historyCount.collectAsState(initial = 0)
    val captureCount by container.captureRepository.eventCount.collectAsState(initial = 0)
    val presetCount by container.intentRepository.presetCount.collectAsState(initial = 0)

    var showInfo by remember { mutableStateOf(false) }
    if (showInfo) InfoDialog(onDismiss = { showInfo = false })

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Intentions", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                Text("Android IPC research workbench", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = vm::refresh) {
                if (ui.loading) CircularProgressIndicator(Modifier.padding(4.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
            IconButton(onClick = { showInfo = true }) {
                Icon(Icons.Outlined.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
            }
        }

        SectionCard("Root", trailing = {
            val root = ui.root
            when {
                root == null -> Pill("checking…", AccentYellow)
                root.available -> Pill("available", AccentGreen)
                else -> Pill("unavailable", AccentRed)
            }
        }) {
            val root = ui.root
            KeyValueRow("Available", root?.available?.toString() ?: "checking…", mono = false)
            KeyValueRow("Provider", (root?.provider ?: RootProvider.NONE).label, mono = false)
            KeyValueRow("su path", root?.suPath)
            KeyValueRow("id", root?.idOutput)
        }

        SectionCard("Capture", trailing = {
            if (captureRunning) Pill("running", AccentGreen) else Pill("stopped", AccentYellow)
        }) {
            KeyValueRow("Foreground service", if (captureRunning) "running" else "stopped", mono = false)
            KeyValueRow("Captured events", captureCount.toString(), mono = false)
            OutlinedButton(onClick = { onNavigate(Routes.CAPTURE) }) { Text("Open Live Capture") }
        }

        SectionCard("Index") {
            KeyValueRow("Indexed packages", ui.indexedPackages?.toString() ?: "—", mono = false)
            KeyValueRow("Sent Intents (history)", historyCount.toString(), mono = false)
            KeyValueRow("Saved presets", presetCount.toString(), mono = false)
        }

        SectionCard("Device") {
            KeyValueRow("Model", ui.device.model, mono = false)
            KeyValueRow("Android", "${ui.device.release} (API ${ui.device.sdk})", mono = false)
            KeyValueRow("Build", ui.device.fingerprint)
        }

        SectionCard("Tools") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { onNavigate(Routes.CONSOLE) }, Modifier.fillMaxWidth()) { Text("Root Console") }
                Button(onClick = { onNavigate(Routes.PROVIDER) }, Modifier.fillMaxWidth()) { Text("Provider Lab") }
                Button(onClick = { onNavigate(Routes.FUZZER) }, Modifier.fillMaxWidth()) { Text("Fuzzer") }
                OutlinedButton(onClick = { onNavigate(Routes.APPS) }, Modifier.fillMaxWidth()) { Text("App / Component Scanner") }
            }
        }
    }
}

@Composable
private fun InfoDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val site = "https://lautarovculic.com"
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    // X — close the pop-up
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                Text(
                    "Lautaro Villarreal Culic'",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    site,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentCyan,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW, site.toUri())
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                )
                Text(
                    "Intentions - v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
