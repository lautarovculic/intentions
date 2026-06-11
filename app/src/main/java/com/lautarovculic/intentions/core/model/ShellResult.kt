package com.lautarovculic.intentions.core.model

import kotlinx.serialization.Serializable

// Raw outcome of a shell/root command: command, stdout, stderr, exit, duration.
@Serializable
data class ShellResult(
    val command: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val durationMs: Long,
    val timestamp: Long,
    val timedOut: Boolean = false,
) {
    val success: Boolean get() = exitCode == 0 && !timedOut
    val combinedOutput: String
        get() = buildString {
            if (stdout.isNotEmpty()) append(stdout)
            if (stderr.isNotEmpty()) {
                if (isNotEmpty()) append("\n")
                append(stderr)
            }
        }

    companion object {
        const val EXIT_TIMEOUT = -1
        const val EXIT_NO_ROOT = -2
        const val EXIT_EXCEPTION = -3

        fun failure(command: String, message: String, exitCode: Int, durationMs: Long = 0, timestamp: Long) =
            ShellResult(command, "", message, exitCode, durationMs, timestamp, timedOut = exitCode == EXIT_TIMEOUT)
    }
}
