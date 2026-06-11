package com.lautarovculic.intentions.ui.screens.builder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lautarovculic.intentions.core.intent.IntentExporter
import com.lautarovculic.intentions.core.model.DispatchOutcome
import com.lautarovculic.intentions.core.model.DispatchType
import com.lautarovculic.intentions.core.model.ExecutionBackend
import com.lautarovculic.intentions.core.model.ExtraSpec
import com.lautarovculic.intentions.core.model.ExtraType
import com.lautarovculic.intentions.core.model.IntentSpec
import com.lautarovculic.intentions.core.model.TargetingMode
import com.lautarovculic.intentions.core.shell.AmCommandBuilder
import com.lautarovculic.intentions.core.intent.IntentFlag
import com.lautarovculic.intentions.di.AppContainer
import com.lautarovculic.intentions.domain.usecase.SavePresetUseCase
import com.lautarovculic.intentions.domain.usecase.SendIntentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BuilderUi(
    val spec: IntentSpec = IntentSpec(),
    val backend: ExecutionBackend = ExecutionBackend.IN_APP,
    val busy: Boolean = false,
    val outcome: DispatchOutcome? = null,
) {
    val exports: Map<IntentExporter.Format, String> get() = IntentExporter.all(spec)
    val shellWarnings: List<String>
        get() = if (spec.dispatchType.isContent) emptyList()
        else AmCommandBuilder.build(spec, IntentFlag.maskOf(spec.flags)).warnings
}

class BuilderViewModel(
    private val sendIntent: SendIntentUseCase,
    private val savePreset: SavePresetUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(BuilderUi())
    val ui = _ui.asStateFlow()

    private fun edit(block: (IntentSpec) -> IntentSpec) {
        _ui.value = _ui.value.copy(spec = block(_ui.value.spec), outcome = _ui.value.outcome)
    }

    fun load(spec: IntentSpec) { _ui.value = BuilderUi(spec = spec) }
    fun setBackend(b: ExecutionBackend) { _ui.value = _ui.value.copy(backend = b) }
    fun setDispatch(d: DispatchType) = edit { it.copy(dispatchType = d) }
    fun setTargeting(t: TargetingMode) = edit { it.copy(targeting = t) }
    fun setAction(v: String) = edit { it.copy(action = v.ifBlank { null }) }
    fun setData(v: String) = edit { it.copy(dataUri = v.ifBlank { null }) }
    fun setMime(v: String) = edit { it.copy(mimeType = v.ifBlank { null }) }
    fun setPackage(v: String) = edit { it.copy(packageName = v.ifBlank { null }) }
    fun setComponentPackage(v: String) = edit { it.copy(componentPackage = v.ifBlank { null }) }
    fun setComponentClass(v: String) = edit { it.copy(componentClass = v.ifBlank { null }) }
    fun setIdentifier(v: String) = edit { it.copy(identifier = v.ifBlank { null }) }

    fun setCategoriesText(text: String) = edit {
        it.copy(categories = text.split(",", "\n").map(String::trim).filter(String::isNotEmpty))
    }

    fun toggleFlag(name: String) = edit {
        it.copy(flags = if (name in it.flags) it.flags - name else it.flags + name)
    }

    fun addExtra() = edit { it.copy(extras = it.extras + ExtraSpec("key", ExtraType.STRING, "")) }
    fun removeExtra(index: Int) = edit { it.copy(extras = it.extras.filterIndexed { i, _ -> i != index }) }
    fun updateExtra(index: Int, extra: ExtraSpec) = edit {
        it.copy(extras = it.extras.mapIndexed { i, e -> if (i == index) extra else e })
    }

    fun send() {
        val current = _ui.value
        _ui.value = current.copy(busy = true, outcome = null)
        viewModelScope.launch {
            val outcome = sendIntent(current.spec, current.backend)
            _ui.value = _ui.value.copy(busy = false, outcome = outcome)
        }
    }

    fun savePreset(name: String) {
        viewModelScope.launch { savePreset(name.ifBlank { "preset" }, _ui.value.spec) }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer {
                BuilderViewModel(
                    SendIntentUseCase(c.intentRepository),
                    SavePresetUseCase(c.intentRepository),
                )
            }
        }
    }
}
