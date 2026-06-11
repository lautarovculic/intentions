package com.lautarovculic.intentions.core.root

import com.lautarovculic.intentions.core.model.ShellResult

// Single entry point for all root shell execution.
interface RootExecutor {

    suspend fun isRootAvailable(): Boolean

    // Run a command under root; never throws (failures encoded in ShellResult).
    suspend fun exec(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): ShellResult

    // Stream a long-running command line by line until onLine returns false.
    suspend fun stream(command: String, onLine: suspend (String) -> Boolean)

    companion object {
        const val DEFAULT_TIMEOUT_MS = 10_000L
    }
}
