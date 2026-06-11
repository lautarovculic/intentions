package com.lautarovculic.intentions.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lautarovculic.intentions.core.intent.IntentExporter
import com.lautarovculic.intentions.data.repository.HistoryItem
import com.lautarovculic.intentions.data.repository.PresetItem
import com.lautarovculic.intentions.ui.components.KeyValueRow
import com.lautarovculic.intentions.ui.components.MonoBlock
import com.lautarovculic.intentions.ui.components.Pill
import com.lautarovculic.intentions.ui.components.SectionCard
import com.lautarovculic.intentions.ui.components.copyToClipboard
import com.lautarovculic.intentions.ui.navigation.Routes
import com.lautarovculic.intentions.ui.rememberAppContainer
import com.lautarovculic.intentions.ui.theme.AccentGreen
import com.lautarovculic.intentions.ui.theme.AccentRed
import com.lautarovculic.intentions.ui.theme.MonoSmall

@Composable
fun HistoryScreen(onNavigate: (String) -> Unit) {
    val container = rememberAppContainer()
    val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
    var tab by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(tab == 0, { tab = 0 }, text = { Text("Runs") })
            Tab(tab == 1, { tab = 1 }, text = { Text("Presets") })
        }
        if (tab == 0) RunsTab(vm, onNavigate, container) else PresetsTab(vm, onNavigate, container)
    }
}

@Composable
private fun RunsTab(vm: HistoryViewModel, onNavigate: (String) -> Unit, container: com.lautarovculic.intentions.di.AppContainer) {
    val runs by vm.history.collectAsState(initial = emptyList())
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${runs.size} runs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                if (runs.isNotEmpty()) TextButton(onClick = { vm.clearHistory() }) { Text("Clear") }
            }
        }
        items(runs, key = { it.id }) { run -> RunCard(run, vm, onNavigate, container, ctx) }
    }
}

@Composable
private fun RunCard(
    run: HistoryItem,
    vm: HistoryViewModel,
    onNavigate: (String) -> Unit,
    container: com.lautarovculic.intentions.di.AppContainer,
    ctx: android.content.Context,
) {
    SectionCard(run.dispatchType, trailing = {
        if (run.success) Pill("ok", AccentGreen) else Pill("fail", AccentRed)
    }) {
        Text(run.targetSummary, style = MonoSmall, color = MaterialTheme.colorScheme.onSurface)
        KeyValueRow("Backend", run.backend, mono = false)
        KeyValueRow("Summary", run.summary, mono = false)
        run.generatedCommand?.let { KeyValueRow("Command", it, copyable = true) }
        run.exitCode?.let { KeyValueRow("Exit", "$it (${run.durationMs ?: 0} ms)") }
        if (!run.stdout.isNullOrBlank()) MonoBlock(run.stdout!!, label = "stdout")
        if (!run.stderr.isNullOrBlank()) MonoBlock(run.stderr!!, label = "stderr")
        run.errorMessage?.let { KeyValueRow("Error", it) }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            run.spec?.let { spec ->
                TextButton(onClick = { container.sendToBuilder(spec); onNavigate(Routes.BUILDER) }) { Text("→ Repeater") }
                TextButton(onClick = { copyToClipboard(ctx, "adb", IntentExporter.adb(spec)) }) { Text("adb") }
            }
            TextButton(onClick = { vm.deleteRun(run.id) }) { Text("delete") }
        }
    }
}

@Composable
private fun PresetsTab(vm: HistoryViewModel, onNavigate: (String) -> Unit, container: com.lautarovculic.intentions.di.AppContainer) {
    val presets by vm.presets.collectAsState(initial = emptyList())
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("${presets.size} presets", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(presets, key = { it.id }) { preset -> PresetCard(preset, vm, onNavigate, container) }
    }
}

@Composable
private fun PresetCard(preset: PresetItem, vm: HistoryViewModel, onNavigate: (String) -> Unit, container: com.lautarovculic.intentions.di.AppContainer) {
    SectionCard(preset.name) {
        preset.note?.let { KeyValueRow("Note", it, mono = false) }
        preset.spec?.let { spec ->
            KeyValueRow("Dispatch", spec.dispatchType.label, mono = false)
            KeyValueRow("Target", spec.componentShortString() ?: spec.packageName ?: spec.dataUri)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            preset.spec?.let { spec ->
                OutlinedButton(onClick = { container.sendToBuilder(spec); onNavigate(Routes.BUILDER) }) { Text("Load") }
            }
            TextButton(onClick = { vm.deletePreset(preset.id) }) { Text("delete") }
        }
    }
}
