package com.lautarovculic.intentions.ui.screens.provider

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lautarovculic.intentions.core.model.DispatchOutcome
import com.lautarovculic.intentions.core.model.DispatchType
import com.lautarovculic.intentions.core.model.ExecutionBackend
import com.lautarovculic.intentions.core.model.ExtraSpec
import com.lautarovculic.intentions.core.model.ExtraType
import com.lautarovculic.intentions.core.model.IntentSpec
import com.lautarovculic.intentions.core.model.TargetingMode
import com.lautarovculic.intentions.core.shell.ContentCommandBuilder
import com.lautarovculic.intentions.di.AppContainer
import com.lautarovculic.intentions.domain.usecase.SendIntentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProviderUi(
    val spec: IntentSpec = IntentSpec(dispatchType = DispatchType.CONTENT_QUERY, dataUri = "content://", targeting = TargetingMode.IMPLICIT),
    val backend: ExecutionBackend = ExecutionBackend.ROOT_SHELL,
    val busy: Boolean = false,
    val outcome: DispatchOutcome? = null,
) {
    val contentOps = listOf(
        DispatchType.CONTENT_QUERY, DispatchType.CONTENT_INSERT, DispatchType.CONTENT_UPDATE,
        DispatchType.CONTENT_DELETE, DispatchType.CONTENT_OPEN_FILE,
    )
    val generatedCommand: String get() = ContentCommandBuilder.build(ContentCommandBuilder.fromSpec(spec))
}

class ProviderLabViewModel(private val sendIntent: SendIntentUseCase) : ViewModel() {
    private val _ui = MutableStateFlow(ProviderUi())
    val ui = _ui.asStateFlow()

    private fun edit(block: (IntentSpec) -> IntentSpec) { _ui.value = _ui.value.copy(spec = block(_ui.value.spec)) }

    fun load(spec: IntentSpec) {
        val op = if (spec.dispatchType.isContent) spec.dispatchType else DispatchType.CONTENT_QUERY
        _ui.value = ProviderUi(spec = spec.copy(dispatchType = op, targeting = TargetingMode.IMPLICIT))
    }

    fun setUri(v: String) = edit { it.copy(dataUri = v) }
    fun setOperation(op: DispatchType) = edit { it.copy(dispatchType = op) }
    fun setBackend(b: ExecutionBackend) { _ui.value = _ui.value.copy(backend = b) }

    fun addBind() = edit { it.copy(extras = it.extras + ExtraSpec("col", ExtraType.STRING, "")) }
    fun updateBind(i: Int, e: ExtraSpec) = edit { it.copy(extras = it.extras.mapIndexed { idx, old -> if (idx == i) e else old }) }
    fun removeBind(i: Int) = edit { it.copy(extras = it.extras.filterIndexed { idx, _ -> idx != i }) }

    fun run() {
        val current = _ui.value
        _ui.value = current.copy(busy = true, outcome = null)
        viewModelScope.launch {
            val outcome = sendIntent(current.spec, current.backend)
            _ui.value = _ui.value.copy(busy = false, outcome = outcome)
        }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer { ProviderLabViewModel(SendIntentUseCase(c.intentRepository)) }
        }
    }
}
