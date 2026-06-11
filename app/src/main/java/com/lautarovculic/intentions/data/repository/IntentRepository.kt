package com.lautarovculic.intentions.data.repository

import com.lautarovculic.intentions.core.intent.IntentDispatcher
import com.lautarovculic.intentions.core.intent.IntentExporter
import com.lautarovculic.intentions.core.model.DispatchOutcome
import com.lautarovculic.intentions.core.model.ExecutionBackend
import com.lautarovculic.intentions.core.model.IntentSpec
import com.lautarovculic.intentions.core.storage.IntentRunDao
import com.lautarovculic.intentions.core.storage.IntentRunEntity
import com.lautarovculic.intentions.core.storage.PresetDao
import com.lautarovculic.intentions.core.storage.PresetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Sends Intents and persists the full outcome to history; preset save/load.
class IntentRepository(
    private val dispatcher: IntentDispatcher,
    private val runDao: IntentRunDao,
    private val presetDao: PresetDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun dispatchAndRecord(spec: IntentSpec, backend: ExecutionBackend): DispatchOutcome {
        val outcome = dispatcher.dispatch(spec, backend)
        runDao.insert(outcome.toEntity())
        return outcome
    }

    val history: Flow<List<HistoryItem>> = runDao.observeRecent().map { list -> list.map { it.toHistoryItem() } }
    val historyCount: Flow<Int> = runDao.count()

    suspend fun deleteRun(id: Long) = runDao.delete(id)
    suspend fun clearHistory() = runDao.clear()

    // presets
    val presets: Flow<List<PresetItem>> = presetDao.observeAll().map { list -> list.map { it.toPresetItem() } }
    val presetCount: Flow<Int> = presetDao.count()

    suspend fun savePreset(name: String, spec: IntentSpec, note: String? = null) {
        presetDao.upsert(
            PresetEntity(
                name = name,
                specJson = IntentExporter.json.encodeToString(IntentSpec.serializer(), spec),
                note = note,
                createdAt = clock(),
            )
        )
    }

    suspend fun deletePreset(id: Long) = presetDao.delete(id)

    // mapping
    private fun DispatchOutcome.toEntity() = IntentRunEntity(
        timestamp = timestamp,
        dispatchType = spec.dispatchType.name,
        backend = backend.name,
        targetSummary = spec.componentShortString() ?: spec.packageName ?: spec.action ?: spec.dataUri ?: "(implicit)",
        specJson = IntentExporter.json.encodeToString(IntentSpec.serializer(), spec),
        success = success,
        summary = summary,
        generatedCommand = generatedCommand,
        stdout = shellResult?.stdout,
        stderr = shellResult?.stderr,
        exitCode = shellResult?.exitCode,
        durationMs = shellResult?.durationMs,
        errorMessage = errorMessage,
    )

    private fun IntentRunEntity.toHistoryItem(): HistoryItem {
        val spec = runCatching {
            IntentExporter.json.decodeFromString(IntentSpec.serializer(), specJson)
        }.getOrNull()
        return HistoryItem(
            id = id,
            timestamp = timestamp,
            dispatchType = dispatchType,
            backend = backend,
            targetSummary = targetSummary,
            success = success,
            summary = summary,
            generatedCommand = generatedCommand,
            stdout = stdout,
            stderr = stderr,
            exitCode = exitCode,
            durationMs = durationMs,
            errorMessage = errorMessage,
            spec = spec,
        )
    }

    private fun PresetEntity.toPresetItem(): PresetItem {
        val spec = runCatching {
            IntentExporter.json.decodeFromString(IntentSpec.serializer(), specJson)
        }.getOrNull()
        return PresetItem(id = id, name = name, note = note, createdAt = createdAt, spec = spec)
    }
}

data class HistoryItem(
    val id: Long,
    val timestamp: Long,
    val dispatchType: String,
    val backend: String,
    val targetSummary: String,
    val success: Boolean,
    val summary: String,
    val generatedCommand: String?,
    val stdout: String?,
    val stderr: String?,
    val exitCode: Int?,
    val durationMs: Long?,
    val errorMessage: String?,
    val spec: IntentSpec?,
)

data class PresetItem(
    val id: Long,
    val name: String,
    val note: String?,
    val createdAt: Long,
    val spec: IntentSpec?,
)
