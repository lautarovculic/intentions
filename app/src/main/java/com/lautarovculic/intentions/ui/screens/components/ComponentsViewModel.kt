package com.lautarovculic.intentions.ui.screens.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lautarovculic.intentions.core.model.ComponentRecord
import com.lautarovculic.intentions.core.model.IntentFilterModel
import com.lautarovculic.intentions.di.AppContainer
import com.lautarovculic.intentions.domain.usecase.ScanComponentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ComponentsUi(
    val loading: Boolean = true,
    val packageName: String = "",
    val components: List<ComponentRecord> = emptyList(),
    val rootDeepLinks: List<IntentFilterModel> = emptyList(),
    val rawDump: String? = null,
    val exportedOnly: Boolean = false,
    val rootEnriched: Boolean = false,
) {
    val visible get() = if (exportedOnly) components.filter { it.exported } else components
}

class ComponentsViewModel(
    private val packageName: String,
    private val scanComponents: ScanComponentsUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(ComponentsUi(packageName = packageName))
    val ui = _ui.asStateFlow()

    init { load(enrich = false) }

    fun load(enrich: Boolean) {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            val result = runCatching { scanComponents(packageName, enrich) }.getOrNull()
            _ui.value = _ui.value.copy(
                loading = false,
                components = result?.components ?: emptyList(),
                rootDeepLinks = result?.rootDeepLinks ?: emptyList(),
                rawDump = result?.rawDump,
                rootEnriched = enrich,
            )
        }
    }

    fun toggleExportedOnly() { _ui.value = _ui.value.copy(exportedOnly = !_ui.value.exportedOnly) }

    companion object {
        fun factory(c: AppContainer, packageName: String) = viewModelFactory {
            initializer { ComponentsViewModel(packageName, ScanComponentsUseCase(c.appRepository)) }
        }
    }
}
