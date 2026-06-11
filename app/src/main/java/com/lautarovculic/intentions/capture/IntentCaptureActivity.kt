package com.lautarovculic.intentions.capture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.lautarovculic.intentions.core.model.CaptureBackend
import com.lautarovculic.intentions.core.model.CapturedIntentEvent
import com.lautarovculic.intentions.di.IntentionsApp
import com.lautarovculic.intentions.ui.components.CapturedEventDetail
import com.lautarovculic.intentions.ui.components.copyToClipboard
import com.lautarovculic.intentions.ui.theme.IntentionsTheme
import kotlinx.coroutines.launch

// Capture backend A: resolver / deep-link sink. Captures Intents routed to Intentions.
class IntentCaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = IntentionsApp.from(application)
        val event = AndroidIntentReader.read(
            intent = intent,
            sessionId = SESSION_ID,
            timestamp = System.currentTimeMillis(),
            backend = CaptureBackend.RESOLVER_SINK,
            referrer = referrer?.host,
        )
        lifecycleScope.launch {
            container.captureRepository.startSession(SESSION_ID, "Resolver / deep-link sink", CaptureBackend.RESOLVER_SINK)
            container.captureRepository.record(event)
        }

        setContent {
            IntentionsTheme {
                CaptureSinkScreen(event, onClose = { finish() })
            }
        }
    }

    companion object {
        const val SESSION_ID = "resolver-sink"
    }
}

@androidx.compose.runtime.Composable
private fun CaptureSinkScreen(event: CapturedIntentEvent, onClose: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var copied by remember { mutableStateOf(false) }
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Intent captured", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "Intentions intercepted this Intent at the resolver sink and logged it to Live Capture.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CapturedEventDetail(event)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    copyToClipboard(ctx, "captured-intent", event.rawEvidence)
                    copied = true
                }) { Text(if (copied) "Copied raw evidence" else "Copy raw evidence") }
                Button(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Text("  Close")
                }
            }
        }
    }
}
