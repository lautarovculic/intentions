package com.lautarovculic.intentions.domain.usecase

import com.lautarovculic.intentions.core.model.DispatchOutcome
import com.lautarovculic.intentions.core.model.ExecutionBackend
import com.lautarovculic.intentions.core.model.IntentSpec
import com.lautarovculic.intentions.data.repository.IntentRepository

// dispatch an IntentSpec and log it to history
class SendIntentUseCase(private val repo: IntentRepository) {
    suspend operator fun invoke(spec: IntentSpec, backend: ExecutionBackend): DispatchOutcome =
        repo.dispatchAndRecord(spec, backend)
}

class SavePresetUseCase(private val repo: IntentRepository) {
    suspend operator fun invoke(name: String, spec: IntentSpec, note: String? = null) =
        repo.savePreset(name, spec, note)
}
