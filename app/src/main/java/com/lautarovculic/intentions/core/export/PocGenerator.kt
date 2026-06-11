package com.lautarovculic.intentions.core.export

import android.os.Build
import com.lautarovculic.intentions.core.intent.IntentExporter
import com.lautarovculic.intentions.core.model.ComponentRecord
import com.lautarovculic.intentions.core.model.DispatchOutcome
import com.lautarovculic.intentions.core.model.IntentSpec

// Generates a Markdown PoC, separating discovery method from exploitability.
object PocGenerator {

    data class Device(
        val androidRelease: String = Build.VERSION.RELEASE,
        val sdk: Int = Build.VERSION.SDK_INT,
        val model: String = "${Build.MANUFACTURER} ${Build.MODEL}",
        val rooted: Boolean,
    )

    fun generate(
        spec: IntentSpec,
        outcome: DispatchOutcome?,
        component: ComponentRecord?,
        device: Device,
        title: String = "Exposed IPC component",
        notes: String? = null,
    ): String = buildString {
        appendLine("### Proof of Concept — $title")
        appendLine()
        appendLine("**Device**")
        appendLine()
        appendLine("- Android version: ${device.androidRelease} (API ${device.sdk})")
        appendLine("- Model: ${device.model}")
        appendLine("- Rooted: ${if (device.rooted) "yes (discovery only)" else "no"}")
        component?.let {
            appendLine("- Target package: ${it.packageName}")
            appendLine("- Component: ${it.shortString}")
            appendLine("- Type: ${it.type.label}")
            appendLine("- Exported: ${it.exported}")
            appendLine("- Required permission: ${it.permission ?: it.readPermission ?: it.writePermission ?: "null"}")
        }
        appendLine()
        appendLine("**Validation (clean adb command — no root required if component is exported)**")
        appendLine()
        appendLine("```bash")
        appendLine(IntentExporter.adb(spec))
        appendLine("```")
        appendLine()
        appendLine("**Equivalent third-party app dispatch (Kotlin)**")
        appendLine()
        appendLine("```kotlin")
        appendLine(IntentExporter.kotlin(spec).trimEnd())
        appendLine("```")
        appendLine()
        if (outcome != null) {
            appendLine("**Observed result**")
            appendLine()
            appendLine("```txt")
            appendLine("Backend: ${outcome.backend.label}")
            appendLine("Success: ${outcome.success}")
            appendLine(outcome.summary)
            outcome.shellResult?.let { sr ->
                appendLine("exit=${sr.exitCode} durationMs=${sr.durationMs}")
                if (sr.combinedOutput.isNotBlank()) {
                    appendLine("--- output ---")
                    appendLine(sr.combinedOutput.take(2000))
                }
            }
            outcome.errorMessage?.let { appendLine("error: $it") }
            appendLine("```")
            appendLine()
        }
        if (!notes.isNullOrBlank()) {
            appendLine("**Notes**")
            appendLine()
            appendLine(notes)
            appendLine()
        }
        appendLine("**Reproducibility**")
        appendLine()
        appendLine("- Discovery method: ${if (device.rooted) "rooted local research (dumpsys / PackageManager)" else "PackageManager enumeration"}")
        appendLine("- Exploitability: " + if (component?.isUnprotectedExported == true)
            "reproducible from a normal third-party app (exported, no permission gate)"
        else "requires the conditions noted above")
    }
}
