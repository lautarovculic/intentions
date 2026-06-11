package com.lautarovculic.intentions.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lautarovculic.intentions.data.repository.IntentRepository
import com.lautarovculic.intentions.di.AppContainer
import kotlinx.coroutines.launch

class HistoryViewModel(private val repo: IntentRepository) : ViewModel() {
    val history = repo.history
    val presets = repo.presets

    fun deleteRun(id: Long) = viewModelScope.launch { repo.deleteRun(id) }
    fun clearHistory() = viewModelScope.launch { repo.clearHistory() }
    fun deletePreset(id: Long) = viewModelScope.launch { repo.deletePreset(id) }

    companion object {
        fun factory(c: AppContainer) = viewModelFactory {
            initializer { HistoryViewModel(c.intentRepository) }
        }
    }
}
