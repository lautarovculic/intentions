package com.lautarovculic.intentions.core.model

// Which backend performed a dispatch.
enum class ExecutionBackend(val label: String) {
    IN_APP("In-app caller"),
    ROOT_SHELL("Root shell (su)"),
}

// Result of sending an IntentSpec; stored verbatim in history.
data class DispatchOutcome(
    val spec: IntentSpec,
    val backend: ExecutionBackend,
    val success: Boolean,
    val summary: String,
    val generatedCommand: String? = null,
    val shellResult: ShellResult? = null,
    val errorMessage: String? = null,
    val timestamp: Long,
)
