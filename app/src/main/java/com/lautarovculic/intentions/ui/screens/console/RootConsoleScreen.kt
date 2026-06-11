package com.lautarovculic.intentions.ui.screens.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lautarovculic.intentions.core.shell.DumpsysCommands
import com.lautarovculic.intentions.core.shell.LogcatCommands
import com.lautarovculic.intentions.core.shell.PmCommands
import com.lautarovculic.intentions.ui.components.KeyValueRow
import com.lautarovculic.intentions.ui.components.MonoBlock
import com.lautarovculic.intentions.ui.components.Pill
import com.lautarovculic.intentions.ui.components.SectionCard
import com.lautarovculic.intentions.ui.rememberAppContainer
import com.lautarovculic.intentions.ui.theme.AccentGreen
import com.lautarovculic.intentions.ui.theme.AccentRed

@Composable
fun RootConsoleScreen() {
    val container = rememberAppContainer()
    val vm: RootConsoleViewModel = viewModel(factory = RootConsoleViewModel.factory(container))
    val ui by vm.ui.collectAsStateWithLifecycle()

    val quick = listOf(
        "id" to "id",
        "packages -3" to PmCommands.listPackages(includePaths = false, userApps = true),
        "top activity" to DumpsysCommands.topActivity(),
        "logcat -t 200" to LogcatCommands.dump(200),
        "running services" to DumpsysCommands.services(),
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Root Console", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            "Every command runs through the RootExecutor (su -c), off the main thread, fully captured.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = ui.command,
            onValueChange = vm::setCommand,
            label = { Text("su -c …") },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            quick.forEach { (label, cmd) ->
                AssistChip(onClick = { vm.setCommand(cmd) }, label = { Text(label) })
            }
        }
        Button(onClick = { vm.run() }, enabled = !ui.running, modifier = Modifier.fillMaxWidth()) {
            if (ui.running) CircularProgressIndicator(Modifier.padding(2.dp), strokeWidth = 2.dp) else Text("Run")
        }
        ui.result?.let { r ->
            SectionCard("Result", trailing = {
                if (r.success) Pill("exit ${r.exitCode}", AccentGreen) else Pill("exit ${r.exitCode}", AccentRed)
            }) {
                KeyValueRow("Command", r.command, copyable = true)
                KeyValueRow("Duration", "${r.durationMs} ms")
                if (r.timedOut) KeyValueRow("Timed out", "true")
                if (r.stdout.isNotBlank()) MonoBlock(r.stdout, label = "stdout")
                if (r.stderr.isNotBlank()) MonoBlock(r.stderr, label = "stderr")
                if (r.stdout.isBlank() && r.stderr.isBlank()) Text("(no output)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
