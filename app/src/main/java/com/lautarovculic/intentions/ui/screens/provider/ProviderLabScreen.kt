package com.lautarovculic.intentions.ui.screens.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.lautarovculic.intentions.core.model.DispatchType
import com.lautarovculic.intentions.core.model.ExecutionBackend
import com.lautarovculic.intentions.core.model.ExtraType
import com.lautarovculic.intentions.ui.components.KeyValueRow
import com.lautarovculic.intentions.ui.components.MonoBlock
import com.lautarovculic.intentions.ui.components.Pill
import com.lautarovculic.intentions.ui.components.SectionCard
import com.lautarovculic.intentions.ui.rememberAppContainer
import com.lautarovculic.intentions.ui.screens.builder.EnumDropdown
import com.lautarovculic.intentions.ui.theme.AccentGreen
import com.lautarovculic.intentions.ui.theme.AccentRed

@Composable
fun ProviderLabScreen() {
    val container = rememberAppContainer()
    val vm: ProviderLabViewModel = viewModel(factory = ProviderLabViewModel.factory(container))
    val ui by vm.ui.collectAsStateWithLifecycle()
    val handoff by container.providerHandoff.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(handoff) {
        handoff?.let { vm.load(it); container.providerHandoff.value = null }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Provider Lab", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

        SectionCard("Target") {
            OutlinedTextField(
                ui.spec.dataUri ?: "", vm::setUri,
                label = { Text("content:// URI") }, singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            EnumDropdown(
                label = "Operation",
                options = ui.contentOps,
                selected = ui.spec.dispatchType,
                optionLabel = { it.label },
                onSelected = vm::setOperation,
            )
            Text("Backend", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ExecutionBackend.entries.forEach { b ->
                    FilterChip(ui.backend == b, { vm.setBackend(b) }, label = { Text(b.label) })
                }
            }
        }

        if (ui.spec.dispatchType == DispatchType.CONTENT_INSERT || ui.spec.dispatchType == DispatchType.CONTENT_UPDATE) {
            SectionCard("Binds (--bind col:type:value)", trailing = { AssistChip(onClick = vm::addBind, label = { Text("+ add") }) }) {
                ui.spec.extras.forEachIndexed { i, bind ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(bind.key, { vm.updateBind(i, bind.copy(key = it)) }, label = { Text("col") }, singleLine = true, modifier = Modifier.weight(1f))
                        Box(Modifier.weight(1f)) {
                            EnumDropdown("type", listOf(ExtraType.STRING, ExtraType.INT, ExtraType.LONG, ExtraType.FLOAT, ExtraType.BOOLEAN, ExtraType.NULL), bind.type, { it.label }) { vm.updateBind(i, bind.copy(type = it)) }
                        }
                        OutlinedTextField(bind.value, { vm.updateBind(i, bind.copy(value = it)) }, label = { Text("value") }, singleLine = true, modifier = Modifier.weight(1f))
                        IconButton(onClick = { vm.removeBind(i) }) { Icon(Icons.Filled.Delete, contentDescription = "Remove") }
                    }
                }
            }
        }

        SectionCard("Command preview") { MonoBlock(ui.generatedCommand, label = "content") }

        Button(onClick = vm::run, enabled = !ui.busy, modifier = Modifier.fillMaxWidth()) {
            if (ui.busy) CircularProgressIndicator(Modifier.padding(2.dp), strokeWidth = 2.dp) else Text("Run (${ui.backend.label})")
        }

        ui.outcome?.let { o ->
            SectionCard("Result", trailing = { if (o.success) Pill("ok", AccentGreen) else Pill("fail", AccentRed) }) {
                KeyValueRow("Summary", o.summary, mono = false)
                o.generatedCommand?.let { KeyValueRow("Command", it, copyable = true) }
                o.errorMessage?.let { KeyValueRow("Error", it) }
                o.shellResult?.let { sr ->
                    KeyValueRow("Exit", "${sr.exitCode} (${sr.durationMs} ms)")
                    if (sr.stdout.isNotBlank()) MonoBlock(sr.stdout, label = "stdout")
                    if (sr.stderr.isNotBlank()) MonoBlock(sr.stderr, label = "stderr")
                }
            }
        }
        Box(Modifier.padding(16.dp)) {}
    }
}
