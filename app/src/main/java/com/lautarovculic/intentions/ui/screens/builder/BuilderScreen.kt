package com.lautarovculic.intentions.ui.screens.builder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lautarovculic.intentions.core.intent.IntentExporter
import com.lautarovculic.intentions.core.intent.IntentFlag
import com.lautarovculic.intentions.core.model.DispatchType
import com.lautarovculic.intentions.core.model.ExecutionBackend
import com.lautarovculic.intentions.core.model.ExtraSpec
import com.lautarovculic.intentions.core.model.ExtraType
import com.lautarovculic.intentions.core.model.TargetingMode
import com.lautarovculic.intentions.ui.components.KeyValueRow
import com.lautarovculic.intentions.ui.components.MonoBlock
import com.lautarovculic.intentions.ui.components.Pill
import com.lautarovculic.intentions.ui.components.SectionCard
import com.lautarovculic.intentions.ui.components.copyToClipboard
import com.lautarovculic.intentions.ui.navigation.Routes
import com.lautarovculic.intentions.ui.rememberAppContainer
import com.lautarovculic.intentions.ui.theme.AccentGreen
import com.lautarovculic.intentions.ui.theme.AccentRed
import com.lautarovculic.intentions.ui.theme.AccentYellow

@Composable
fun BuilderScreen(onNavigate: (String) -> Unit) {
    val container = rememberAppContainer()
    val vm: BuilderViewModel = viewModel(factory = BuilderViewModel.factory(container))
    val ui by vm.ui.collectAsStateWithLifecycle()
    val handoff by container.builderHandoff.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    // Consume a "send to builder" handoff exactly once.
    androidx.compose.runtime.LaunchedEffect(handoff) {
        handoff?.let { vm.load(it); container.builderHandoff.value = null }
    }

    var showPresetDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf(IntentExporter.Format.ADB) }

    LazyColumn(
        Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SectionCard("Dispatch") {
                EnumDropdown(
                    label = "Type",
                    options = DispatchType.entries,
                    selected = ui.spec.dispatchType,
                    optionLabel = { it.label },
                    onSelected = vm::setDispatch,
                )
                Text("Backend", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExecutionBackend.entries.forEach { b ->
                        FilterChip(ui.backend == b, { vm.setBackend(b) }, label = { Text(b.label) })
                    }
                }
            }
        }

        if (!ui.spec.dispatchType.isContent) {
            item {
                SectionCard("Targeting") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TargetingMode.entries.forEach { t ->
                            FilterChip(ui.spec.targeting == t, { vm.setTargeting(t) }, label = { Text(t.label) })
                        }
                    }
                    if (ui.spec.targeting == TargetingMode.EXPLICIT_COMPONENT) {
                        OutlinedTextField(ui.spec.componentPackage ?: "", vm::setComponentPackage, label = { Text("Component package") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(ui.spec.componentClass ?: "", vm::setComponentClass, label = { Text("Component class (FQN)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                    if (ui.spec.targeting == TargetingMode.PACKAGE_ONLY) {
                        OutlinedTextField(ui.spec.packageName ?: "", vm::setPackage, label = { Text("Target package") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        item {
            SectionCard(if (ui.spec.dispatchType.isContent) "Provider URI" else "Intent fields") {
                if (ui.spec.dispatchType.isContent) {
                    OutlinedTextField(ui.spec.dataUri ?: "", vm::setData, label = { Text("content:// URI") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                } else {
                    OutlinedTextField(ui.spec.action ?: "", vm::setAction, label = { Text("action") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(ui.spec.dataUri ?: "", vm::setData, label = { Text("data URI") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(ui.spec.mimeType ?: "", vm::setMime, label = { Text("MIME type") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(ui.spec.categories.joinToString(", "), vm::setCategoriesText, label = { Text("categories (comma-separated)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(ui.spec.identifier ?: "", vm::setIdentifier, label = { Text("identifier") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        if (!ui.spec.dispatchType.isContent) {
            item {
                SectionCard("Flags") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        IntentFlag.entries.forEach { flag ->
                            FilterChip(flag.name in ui.spec.flags, { vm.toggleFlag(flag.name) }, label = { Text(flag.name) })
                        }
                    }
                }
            }
        }

        item {
            SectionCard("Extras (${ui.spec.extras.size})", trailing = {
                AssistChip(onClick = vm::addExtra, label = { Text("+ add") })
            }) {
                ui.spec.extras.forEachIndexed { index, extra ->
                    ExtraRow(extra, onChange = { vm.updateExtra(index, it) }, onDelete = { vm.removeExtra(index) })
                }
                if (ui.spec.extras.isEmpty()) Text("No extras", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            if (ui.shellWarnings.isNotEmpty()) {
                SectionCard("Shell warnings", trailing = { Pill("am limits", AccentYellow) }) {
                    ui.shellWarnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = vm::send, enabled = !ui.busy, modifier = Modifier.weight(1f)) {
                    if (ui.busy) CircularProgressIndicator(Modifier.padding(2.dp), strokeWidth = 2.dp)
                    else Text("Send (${ui.backend.label})")
                }
                OutlinedButton(onClick = { showPresetDialog = true }) { Text("Save") }
                OutlinedButton(onClick = { container.sendToFuzzer(ui.spec); onNavigate(Routes.FUZZER) }) { Text("Fuzz") }
            }
        }

        item {
            SectionCard("Export") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IntentExporter.Format.entries.forEach { fmt ->
                        FilterChip(exportFormat == fmt, { exportFormat = fmt }, label = { Text(fmt.label) })
                    }
                }
                MonoBlock(ui.exports[exportFormat].orEmpty(), label = exportFormat.label)
            }
        }

        ui.outcome?.let { outcome ->
            item {
                SectionCard("Result", trailing = {
                    if (outcome.success) Pill("ok", AccentGreen) else Pill("fail", AccentRed)
                }) {
                    KeyValueRow("Backend", outcome.backend.label, mono = false)
                    KeyValueRow("Summary", outcome.summary, mono = false)
                    outcome.generatedCommand?.let { KeyValueRow("Command", it, copyable = true) }
                    outcome.errorMessage?.let { KeyValueRow("Error", it) }
                    outcome.shellResult?.let { sr ->
                        KeyValueRow("Exit", sr.exitCode.toString())
                        KeyValueRow("Duration", "${sr.durationMs} ms")
                        if (sr.stdout.isNotBlank()) MonoBlock(sr.stdout, label = "stdout")
                        if (sr.stderr.isNotBlank()) MonoBlock(sr.stderr, label = "stderr")
                    }
                    OutlinedButton(onClick = {
                        copyToClipboard(ctx, "result", outcome.summary + "\n" + (outcome.shellResult?.combinedOutput ?: outcome.errorMessage ?: ""))
                    }) { Text("Copy result") }
                }
            }
        }
        item { Box(Modifier.padding(24.dp)) {} }
    }

    if (showPresetDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            confirmButton = {
                Button(onClick = { vm.savePreset(name); showPresetDialog = false }) { Text("Save") }
            },
            dismissButton = { OutlinedButton(onClick = { showPresetDialog = false }) { Text("Cancel") } },
            title = { Text("Save preset") },
            text = { OutlinedTextField(name, { name = it }, label = { Text("Preset name") }, singleLine = true) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtraRow(extra: ExtraSpec, onChange: (ExtraSpec) -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            extra.key, { onChange(extra.copy(key = it)) },
            label = { Text("key") }, singleLine = true, modifier = Modifier.weight(0.9f),
        )
        Box(Modifier.weight(1f)) {
            EnumDropdown(
                label = "type",
                options = ExtraType.selectable,
                selected = extra.type,
                optionLabel = { it.label },
                onSelected = { onChange(extra.copy(type = it)) },
            )
        }
        OutlinedTextField(
            extra.value, { onChange(extra.copy(value = it)) },
            label = { Text("value") }, singleLine = true, modifier = Modifier.weight(1.1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete extra") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(optionLabel(option)) }, onClick = { onSelected(option); expanded = false })
            }
        }
    }
}
