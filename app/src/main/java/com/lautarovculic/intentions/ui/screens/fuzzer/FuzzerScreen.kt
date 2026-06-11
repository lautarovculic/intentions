package com.lautarovculic.intentions.ui.screens.fuzzer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lautarovculic.intentions.core.model.ExecutionBackend
import com.lautarovculic.intentions.ui.components.Pill
import com.lautarovculic.intentions.ui.components.SectionCard
import com.lautarovculic.intentions.ui.rememberAppContainer
import com.lautarovculic.intentions.ui.theme.AccentGreen
import com.lautarovculic.intentions.ui.theme.AccentRed
import com.lautarovculic.intentions.ui.theme.MonoSmall

@Composable
fun FuzzerScreen() {
    val container = rememberAppContainer()
    val vm: FuzzerViewModel = viewModel(factory = FuzzerViewModel.factory(container))
    val ui by vm.ui.collectAsStateWithLifecycle()
    val handoff by container.fuzzerHandoff.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(handoff) {
        handoff?.let { vm.load(it); container.fuzzerHandoff.value = null }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Fuzzer", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            }
            item {
                SectionCard("Base") {
                    OutlinedTextField(
                        ui.base.dataUri ?: "", vm::setBaseUri,
                        label = { Text("base deep link / data URI") }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FuzzMode.entries.forEach { m -> FilterChip(ui.mode == m, { vm.setMode(m) }, label = { Text(m.label) }) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ExecutionBackend.entries.forEach { b -> FilterChip(ui.backend == b, { vm.setBackend(b) }, label = { Text(b.label) }) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(ui.delayMs == 0L, { vm.setDelay(0) }, label = { Text("no delay") })
                        FilterChip(ui.delayMs == 150L, { vm.setDelay(150) }, label = { Text("150ms") })
                        FilterChip(ui.delayMs == 500L, { vm.setDelay(500) }, label = { Text("500ms") })
                        FilterChip(ui.stopOnError, { vm.toggleStopOnError() }, label = { Text("stop on error") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = vm::generate) { Text("Generate ${if (ui.cases.isNotEmpty()) "(${ui.cases.size})" else ""}") }
                        if (!ui.running) {
                            Button(onClick = vm::runAll, enabled = ui.cases.isNotEmpty()) { Text("Run ${ui.cases.size}") }
                        } else {
                            Button(onClick = vm::stop, colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) { Text("Stop") }
                        }
                    }
                }
            }
            if (ui.running || ui.results.isNotEmpty()) {
                item {
                    SectionCard("Run", trailing = { Pill("${ui.progress}/${ui.cases.size}", AccentGreen) }) {
                        if (ui.running) LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(
                            "ok=${ui.results.count { it.success }}  fail=${ui.results.count { !it.success }}",
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(ui.results) { r ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(if (r.success) "ok" else "fail", if (r.success) AccentGreen else AccentRed)
                    Text(r.label, style = MonoSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                }
            }
            item { Box(Modifier.padding(16.dp)) {} }
        }
    }
}
