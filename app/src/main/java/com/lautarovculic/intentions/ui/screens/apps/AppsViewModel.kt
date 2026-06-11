package com.lautarovculic.intentions.ui.screens.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lautarovculic.intentions.core.model.InstalledAppRecord
import com.lautarovculic.intentions.di.AppContainer
import com.lautarovculic.intentions.domain.usecase.ScanAppsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppsFilter(
    val query: String = "",
    val includeSystem: Boolean = false,
    val exportedOnly: Boolean = false,
    val deepLinksOnly: Boolean = false,
    val debuggableOnly: Boolean = false,
)

data class AppsUi(
    val loading: Boolean = true,
    val all: List<InstalledAppRecord> = emptyList(),
    val filter: AppsFilter = AppsFilter(),
) {
    val visible: List<InstalledAppRecord> = all.filter { app ->
        (filter.query.isBlank() ||
            app.label.contains(filter.query, true) ||
            app.packageName.contains(filter.query, true)) &&
            (filter.includeSystem || !app.isSystem) &&
            (!filter.exportedOnly || app.totalExportedComponents > 0) &&
            (!filter.deepLinksOnly || app.hasAppLinks || app.deepLinkCount > 0) &&
            (!filter.debuggableOnly || app.isDebuggable)
    }
}

class AppsViewModel(private val scanApps: ScanAppsUseCase) : ViewModel() {
    private val _ui = MutableStateFlow(AppsUi())
    val ui = _ui.asStateFlow()

    init { rescan() }

    fun rescan() {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            val apps = runCatching { scanApps(includeSystem = true) }.getOrDefault(emptyList())
            _ui.value = _ui.value.copy(loading = false, all = apps)
        }
    }

    fun updateFilter(transform: (AppsFilter) -> AppsFilter) {
        _ui.value = _ui.value.copy(filter = transform(_ui.value.filter))
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer { AppsViewModel(ScanAppsUseCase(c.appRepository)) }
        }
    }
}
