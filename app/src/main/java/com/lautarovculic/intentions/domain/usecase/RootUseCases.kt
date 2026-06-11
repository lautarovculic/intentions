package com.lautarovculic.intentions.domain.usecase

import com.lautarovculic.intentions.core.model.RootStatus
import com.lautarovculic.intentions.core.model.ShellResult
import com.lautarovculic.intentions.data.repository.RootRepository

class RefreshRootStatusUseCase(private val repo: RootRepository) {
    suspend operator fun invoke(): RootStatus = repo.refreshStatus()
}

// run an arbitrary root command (rejects empty)
class RunRootCommandUseCase(private val repo: RootRepository) {
    suspend operator fun invoke(command: String, timeoutMs: Long): ShellResult {
        val trimmed = command.trim()
        require(trimmed.isNotEmpty()) { "Command is empty" }
        return repo.exec(trimmed, timeoutMs)
    }
}
