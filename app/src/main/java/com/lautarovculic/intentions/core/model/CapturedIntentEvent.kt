package com.lautarovculic.intentions.core.model

import kotlinx.serialization.Serializable

// Which capture backend observed the event.
@Serializable
enum class CaptureBackend(val label: String, val fidelity: String) {
    RESOLVER_SINK("Resolver / Deep-link sink", "high (only Intents routed to Intentions)"),
    ROOT_LOGCAT("Root logcat observer", "partial (extras often omitted/redacted)"),
    DUMPSYS_SAMPLER("dumpsys sampler", "partial (reconstructed state)"),
    LSPOSED("LSPosed hooks", "high (pre-dispatch object)"),
    MANUAL("Manual / sent by Intentions", "exact"),
}

// A normalized captured IPC event; most fields nullable since backends vary.
@Serializable
data class CapturedIntentEvent(
    val id: String,
    val sessionId: String,
    val timestamp: Long,
    val backend: CaptureBackend,
    val dispatchType: DispatchType? = null,
    val dispatchApi: String? = null,
    val sourcePackage: String? = null,
    val sourceUid: Int? = null,
    val sourcePid: Int? = null,
    val targetPackage: String? = null,
    val targetComponent: String? = null,
    val action: String? = null,
    val dataUri: String? = null,
    val scheme: String? = null,
    val host: String? = null,
    val path: String? = null,
    val mimeType: String? = null,
    val categories: List<String> = emptyList(),
    val flags: Int = 0,
    val flagsDecoded: List<String> = emptyList(),
    val extras: List<ExtraSpec> = emptyList(),
    val resultNote: String? = null,
    val rawEvidence: String,
) {
    // lossy reconstruction into an IntentSpec
    fun toIntentSpec(): IntentSpec {
        val pkg = targetComponent?.substringBefore('/')?.takeIf { it.isNotBlank() }
        val cls = targetComponent?.substringAfter('/', "")?.takeIf { it.isNotBlank() }
        return IntentSpec(
            dispatchType = dispatchType ?: DispatchType.START_ACTIVITY,
            targeting = when {
                pkg != null && cls != null -> TargetingMode.EXPLICIT_COMPONENT
                targetPackage != null -> TargetingMode.PACKAGE_ONLY
                else -> TargetingMode.IMPLICIT
            },
            action = action,
            dataUri = dataUri,
            mimeType = mimeType,
            packageName = targetPackage,
            componentPackage = pkg,
            componentClass = cls,
            categories = categories,
            extras = extras,
        )
    }
}
