package com.lautarovculic.intentions.capture

import android.content.Intent
import android.net.Uri
import android.os.Build
import com.lautarovculic.intentions.core.intent.IntentFlag
import com.lautarovculic.intentions.core.model.CaptureBackend
import com.lautarovculic.intentions.core.model.CapturedIntentEvent
import com.lautarovculic.intentions.core.model.ExtraSpec
import com.lautarovculic.intentions.core.model.ExtraType
import java.util.UUID

// Reverse of IntentFactory: reads an incoming Intent into a CapturedIntentEvent.
object AndroidIntentReader {

    fun read(
        intent: Intent,
        sessionId: String,
        timestamp: Long,
        backend: CaptureBackend = CaptureBackend.RESOLVER_SINK,
        referrer: String? = null,
    ): CapturedIntentEvent {
        val uri = intent.data
        val extras = readExtras(intent)
        return CapturedIntentEvent(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestamp = timestamp,
            backend = backend,
            dispatchApi = "IntentCaptureActivity.onCreate",
            sourcePackage = referrer,
            targetComponent = intent.component?.flattenToShortString(),
            targetPackage = intent.`package` ?: intent.component?.packageName,
            action = intent.action,
            dataUri = intent.dataString,
            scheme = uri?.scheme,
            host = uri?.host,
            path = uri?.path,
            mimeType = intent.type,
            categories = intent.categories?.toList().orEmpty(),
            flags = intent.flags,
            flagsDecoded = IntentFlag.decode(intent.flags),
            extras = extras,
            resultNote = "Captured at resolver sink (user routed this Intent to Intentions)",
            rawEvidence = buildRawEvidence(intent, extras),
        )
    }

    @Suppress("DEPRECATION")
    private fun readExtras(intent: Intent): List<ExtraSpec> {
        val bundle = intent.extras ?: return emptyList()
        return bundle.keySet().mapNotNull { key ->
            val value = bundle.get(key) ?: return@mapNotNull ExtraSpec(key, ExtraType.NULL)
            when (value) {
                is String -> ExtraSpec(key, ExtraType.STRING, value)
                is Boolean -> ExtraSpec(key, ExtraType.BOOLEAN, value.toString())
                is Int -> ExtraSpec(key, ExtraType.INT, value.toString())
                is Long -> ExtraSpec(key, ExtraType.LONG, value.toString())
                is Float -> ExtraSpec(key, ExtraType.FLOAT, value.toString())
                is Double -> ExtraSpec(key, ExtraType.DOUBLE, value.toString())
                is Uri -> ExtraSpec(key, ExtraType.URI, value.toString())
                is IntArray -> ExtraSpec(key, ExtraType.INT_ARRAY, value.joinToString(","))
                is LongArray -> ExtraSpec(key, ExtraType.LONG_ARRAY, value.joinToString(","))
                is FloatArray -> ExtraSpec(key, ExtraType.FLOAT_ARRAY, value.joinToString(","))
                is BooleanArray -> ExtraSpec(key, ExtraType.BOOLEAN_ARRAY, value.joinToString(","))
                is Array<*> -> ExtraSpec(key, ExtraType.STRING_ARRAY, value.joinToString(",") { it.toString() })
                else -> ExtraSpec(key, ExtraType.STRING, "$value  (${value.javaClass.simpleName})")
            }
        }
    }

    private fun buildRawEvidence(intent: Intent, extras: List<ExtraSpec>): String = buildString {
        appendLine("action=${intent.action}")
        appendLine("data=${intent.dataString}")
        appendLine("type=${intent.type}")
        appendLine("component=${intent.component?.flattenToString()}")
        appendLine("package=${intent.`package`}")
        appendLine("categories=${intent.categories}")
        appendLine("flags=0x${Integer.toHexString(intent.flags)}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appendLine("identifier=${intent.identifier}")
        appendLine("toUri=${intent.toUri(Intent.URI_INTENT_SCHEME)}")
        if (extras.isNotEmpty()) {
            appendLine("extras:")
            extras.forEach { appendLine("  ${it.key} (${it.type.label}) = ${it.value}") }
        }
    }
}
