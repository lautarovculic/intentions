package com.lautarovculic.intentions.ui.screens.dashboard

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lautarovculic.intentions.core.model.RootStatus
import com.lautarovculic.intentions.di.AppContainer
import com.lautarovculic.intentions.domain.usecase.RefreshRootStatusUseCase
import com.lautarovculic.intentions.domain.usecase.ScanAppsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceInfo(
    val release: String = Build.VERSION.RELEASE,
    val sdk: Int = Build.VERSION.SDK_INT,
    val model: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val fingerprint: String = Build.FINGERPRINT,
)

data class DashboardUi(
    val loading: Boolean = true,
    val root: RootStatus? = null,
    val device: DeviceInfo = DeviceInfo(),
    val indexedPackages: Int? = null,
)

class DashboardViewModel(
    private val refreshRootStatus: RefreshRootStatusUseCase,
    private val scanApps: ScanAppsUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(DashboardUi())
    val ui = _ui.asStateFlow()

    init { refresh() }

    fun refresh() {
        _ui.value = _ui.value.copy(loading = true)
        viewModelScope.launch {
            val root = runCatching { refreshRootStatus() }.getOrNull()
            val count = runCatching { scanApps(includeSystem = true).size }.getOrNull()
            _ui.value = DashboardUi(loading = false, root = root, indexedPackages = count)
        }
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer {
                DashboardViewModel(
                    RefreshRootStatusUseCase(c.rootRepository),
                    ScanAppsUseCase(c.appRepository),
                )
            }
        }
    }
}
