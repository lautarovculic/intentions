package com.lautarovculic.intentions.ui.screens.console

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lautarovculic.intentions.core.model.ShellResult
import com.lautarovculic.intentions.di.AppContainer
import com.lautarovculic.intentions.domain.usecase.RunRootCommandUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConsoleUi(
    val command: String = "id",
    val running: Boolean = false,
    val result: ShellResult? = null,
)

class RootConsoleViewModel(private val run: RunRootCommandUseCase) : ViewModel() {
    private val _ui = MutableStateFlow(ConsoleUi())
    val ui = _ui.asStateFlow()

    fun setCommand(c: String) { _ui.value = _ui.value.copy(command = c) }

    fun run(timeoutMs: Long = 15_000L) {
        val cmd = _ui.value.command
        _ui.value = _ui.value.copy(running = true, result = null)
        viewModelScope.launch {
            val result = runCatching { run(cmd, timeoutMs) }.getOrElse {
                ShellResult.failure(cmd, it.message ?: "error", ShellResult.EXIT_EXCEPTION, timestamp = 0)
            }
            _ui.value = _ui.value.copy(running = false, result = result)
        }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer { RootConsoleViewModel(RunRootCommandUseCase(c.rootRepository)) }
        }
    }
}
