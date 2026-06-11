package com.lautarovculic.intentions.ui.screens.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lautarovculic.intentions.core.model.CapturedIntentEvent
import com.lautarovculic.intentions.data.repository.CaptureRepository
import com.lautarovculic.intentions.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CaptureViewModel(private val repo: CaptureRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    // search tokens: scheme: host: action: source: target: api: extra: (bare = anywhere)
    val events: StateFlow<List<CapturedIntentEvent>> =
        combine(repo.events, _query) { events, q -> events.filter { it.matches(q) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(q: String) { _query.value = q }
    fun clear() = viewModelScope.launch { repo.clearEvents() }

    private fun CapturedIntentEvent.matches(q: String): Boolean {
        val query = q.trim()
        if (query.isEmpty()) return true
        val colon = query.indexOf(':')
        if (colon in 1 until query.length - 0) {
            val key = query.substring(0, colon).lowercase()
            val value = query.substring(colon + 1)
            val field = when (key) {
                "scheme" -> scheme
                "host" -> host
                "action" -> action
                "source" -> sourcePackage
                "target" -> targetComponent ?: targetPackage
                "api" -> dispatchApi
                "extra" -> extras.joinToString(",") { it.key + "=" + it.value }
                else -> null
            }
            if (field != null) return field.contains(value, ignoreCase = true)
        }
        return rawEvidence.contains(query, true) ||
            (action?.contains(query, true) == true) ||
            (dataUri?.contains(query, true) == true) ||
            (targetComponent?.contains(query, true) == true)
    }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer { CaptureViewModel(c.captureRepository) }
        }
    }
}
