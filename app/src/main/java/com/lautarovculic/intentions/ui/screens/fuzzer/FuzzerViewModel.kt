package com.lautarovculic.intentions.ui.screens.fuzzer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lautarovculic.intentions.core.model.DispatchType
import com.lautarovculic.intentions.core.model.ExecutionBackend
import com.lautarovculic.intentions.core.model.IntentSpec
import com.lautarovculic.intentions.di.AppContainer
import com.lautarovculic.intentions.domain.fuzz.FuzzEngine
import com.lautarovculic.intentions.domain.usecase.SendIntentUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class FuzzMode(val label: String) { DEEP_LINK("Deep link"), EXTRA("Extras") }

data class FuzzResult(val label: String, val success: Boolean, val summary: String)

data class FuzzerUi(
    val base: IntentSpec = IntentSpec.forDeepLink("scheme://host/path?id=1"),
    val mode: FuzzMode = FuzzMode.DEEP_LINK,
    val backend: ExecutionBackend = ExecutionBackend.IN_APP,
    val delayMs: Long = 150,
    val stopOnError: Boolean = false,
    val cases: List<FuzzEngine.FuzzCase> = emptyList(),
    val running: Boolean = false,
    val progress: Int = 0,
    val results: List<FuzzResult> = emptyList(),
)

class FuzzerViewModel(private val sendIntent: SendIntentUseCase) : ViewModel() {
    private val _ui = MutableStateFlow(FuzzerUi())
    val ui = _ui.asStateFlow()
    private var job: Job? = null

    fun load(spec: IntentSpec) {
        val seed = if (spec.dataUri.isNullOrBlank()) spec.copy(dataUri = "scheme://host/path?id=1") else spec
        _ui.value = _ui.value.copy(base = seed, cases = emptyList(), results = emptyList(), progress = 0)
    }

    fun setBaseUri(uri: String) = update { it.copy(base = it.base.copy(dataUri = uri, dispatchType = DispatchType.START_ACTIVITY)) }
    fun setMode(m: FuzzMode) = update { it.copy(mode = m) }
    fun setBackend(b: ExecutionBackend) = update { it.copy(backend = b) }
    fun setDelay(ms: Long) = update { it.copy(delayMs = ms) }
    fun toggleStopOnError() = update { it.copy(stopOnError = !it.stopOnError) }

    fun generate() = update {
        val cases = when (it.mode) {
            FuzzMode.DEEP_LINK -> FuzzEngine.deepLinkCases(it.base)
            FuzzMode.EXTRA -> FuzzEngine.extraCases(it.base)
        }
        it.copy(cases = cases, results = emptyList(), progress = 0)
    }

    fun runAll() {
        val start = _ui.value
        if (start.cases.isEmpty() || start.running) return
        _ui.value = start.copy(running = true, results = emptyList(), progress = 0)
        job = viewModelScope.launch {
            val results = mutableListOf<FuzzResult>()
            for ((index, case) in start.cases.withIndex()) {
                if (!isActive) break
                val outcome = sendIntent(case.spec, start.backend)
                results.add(FuzzResult(case.label, outcome.success, outcome.summary))
                _ui.value = _ui.value.copy(progress = index + 1, results = results.toList())
                if (start.stopOnError && !outcome.success) break
                delay(start.delayMs)
            }
            _ui.value = _ui.value.copy(running = false)
        }
    }

    fun stop() { job?.cancel(); _ui.value = _ui.value.copy(running = false) }

    private fun update(block: (FuzzerUi) -> FuzzerUi) { _ui.value = block(_ui.value) }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer { FuzzerViewModel(SendIntentUseCase(c.intentRepository)) }
        }
    }
}
