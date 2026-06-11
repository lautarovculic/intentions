package com.lautarovculic.intentions.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lautarovculic.intentions.core.model.CapturedIntentEvent
import com.lautarovculic.intentions.ui.theme.AccentCyan
import com.lautarovculic.intentions.ui.theme.AccentGreen
import com.lautarovculic.intentions.ui.theme.AccentYellow

// Renders a captured event's fields, extras, and raw evidence.
@Composable
fun CapturedEventDetail(event: CapturedIntentEvent, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard("Parsed") {
            KeyValueRow("Backend", event.backend.label, mono = false)
            KeyValueRow("Dispatch API", event.dispatchApi)
            KeyValueRow("Source pkg", event.sourcePackage, copyable = true)
            KeyValueRow("Source uid", event.sourceUid?.toString())
            KeyValueRow("Action", event.action, copyable = true)
            KeyValueRow("Data URI", event.dataUri, copyable = true)
            KeyValueRow("Scheme", event.scheme)
            KeyValueRow("Host", event.host, copyable = true)
            KeyValueRow("Path", event.path)
            KeyValueRow("MIME", event.mimeType)
            KeyValueRow("Target", event.targetComponent ?: event.targetPackage, copyable = true)
            KeyValueRow("Categories", event.categories.joinToString(", ").ifEmpty { null })
            KeyValueRow("Flags", if (event.flags != 0) "0x${Integer.toHexString(event.flags)}" else null)
            if (event.flagsDecoded.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    event.flagsDecoded.forEach { Pill(it, AccentCyan) }
                }
            }
            event.resultNote?.let { KeyValueRow("Note", it, mono = false) }
        }
        if (event.extras.isNotEmpty()) {
            SectionCard("Extras (${event.extras.size})") {
                event.extras.forEach { extra ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(extra.type.label, AccentYellow)
                        KeyValueRow(extra.key, extra.value, copyable = true)
                    }
                }
            }
        }
        SectionCard("Raw evidence", trailing = { Pill("verbatim", AccentGreen) }) {
            MonoBlock(event.rawEvidence, label = "raw")
        }
    }
}
