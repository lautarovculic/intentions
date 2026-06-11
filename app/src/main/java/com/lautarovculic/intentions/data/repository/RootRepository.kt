package com.lautarovculic.intentions.data.repository

import com.lautarovculic.intentions.core.model.RootStatus
import com.lautarovculic.intentions.core.model.ShellResult
import com.lautarovculic.intentions.core.root.RootDetector
import com.lautarovculic.intentions.core.root.RootExecutor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RootRepository(
    private val executor: RootExecutor,
    private val detector: RootDetector,
) {
    private val _status = MutableStateFlow<RootStatus?>(null)
    val status: StateFlow<RootStatus?> = _status.asStateFlow()

    suspend fun refreshStatus(): RootStatus = detector.detect().also { _status.value = it }

    suspend fun isRootAvailable(): Boolean = executor.isRootAvailable()

    suspend fun exec(command: String, timeoutMs: Long = RootExecutor.DEFAULT_TIMEOUT_MS): ShellResult =
        executor.exec(command, timeoutMs)
}
