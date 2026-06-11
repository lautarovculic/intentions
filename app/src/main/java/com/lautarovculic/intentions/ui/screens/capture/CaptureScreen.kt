package com.lautarovculic.intentions.ui.screens.capture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lautarovculic.intentions.capture.LogcatCaptureService
import com.lautarovculic.intentions.core.model.CapturedIntentEvent
import com.lautarovculic.intentions.ui.components.CapturedEventDetail
import com.lautarovculic.intentions.ui.components.Pill
import com.lautarovculic.intentions.ui.navigation.Routes
import com.lautarovculic.intentions.ui.rememberAppContainer
import com.lautarovculic.intentions.ui.theme.AccentCyan
import com.lautarovculic.intentions.ui.theme.AccentGreen
import com.lautarovculic.intentions.ui.theme.AccentRed
import com.lautarovculic.intentions.ui.theme.MonoSmall
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@Composable
fun CaptureScreen(onNavigate: (String) -> Unit) {
    val container = rememberAppContainer()
    val vm: CaptureViewModel = viewModel(factory = CaptureViewModel.factory(container))
    val events by vm.events.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val running by container.captureRunning.collectAsState()
    val ctx = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Live Capture", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "${events.size} events · sink + root logcat",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (running) {
                Button(onClick = { LogcatCaptureService.stop(ctx) }, colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) { Text("Stop") }
            } else {
                Button(onClick = { LogcatCaptureService.start(ctx) }) { Text("Start") }
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            label = { Text("filter — e.g. scheme:https  host:app.target  extra:token") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(if (running) "logcat: running" else "logcat: stopped", if (running) AccentGreen else AccentRed)
            Text(" sink always-on", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(start = 8.dp))
            if (events.isNotEmpty()) TextButton(onClick = vm::clear) { Text("Clear") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        if (events.isEmpty()) {
            Text(
                "No events yet. Start the root logcat observer, or route a link/share to \"Capture with Intentions\" from any app's chooser.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(events, key = { it.id }) { event ->
                EventRow(event, onNavigate, container)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            }
        }
    }
}

@Composable
private fun EventRow(event: CapturedIntentEvent, onNavigate: (String) -> Unit, container: com.lautarovculic.intentions.di.AppContainer) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(timeFmt.format(Date(event.timestamp)), style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Pill(event.backend.name.take(4), AccentCyan)
            Text(
                event.action?.substringAfterLast('.') ?: event.dispatchApi ?: "event",
                style = MonoSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
        }
        Text(
            event.dataUri ?: event.targetComponent ?: event.resultNote ?: event.rawEvidence.lineSequence().firstOrNull().orEmpty(),
            style = MonoSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CapturedEventDetail(event)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { container.sendToBuilder(event.toIntentSpec()); onNavigate(Routes.BUILDER) }) { Text("→ Repeater") }
                    OutlinedButton(onClick = { container.sendToFuzzer(event.toIntentSpec()); onNavigate(Routes.FUZZER) }) { Text("→ Fuzzer") }
                }
            }
        }
    }
}
