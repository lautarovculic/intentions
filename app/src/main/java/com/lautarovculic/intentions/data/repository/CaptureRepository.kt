package com.lautarovculic.intentions.data.repository

import com.lautarovculic.intentions.core.model.CaptureBackend
import com.lautarovculic.intentions.core.model.CapturedIntentEvent
import com.lautarovculic.intentions.core.model.DispatchType
import com.lautarovculic.intentions.core.storage.CaptureDao
import com.lautarovculic.intentions.core.storage.CaptureSessionEntity
import com.lautarovculic.intentions.core.storage.CapturedIntentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CaptureRepository(
    private val dao: CaptureDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val events: Flow<List<CapturedIntentEvent>> =
        dao.observeEvents().map { list -> list.map { it.toEvent() } }

    val eventCount: Flow<Int> = dao.count()
    val sessions = dao.observeSessions()

    suspend fun record(event: CapturedIntentEvent) = dao.insertEvent(event.toEntity())

    suspend fun startSession(id: String, label: String, backend: CaptureBackend) {
        dao.insertSession(CaptureSessionEntity(id, label, backend.name, clock()))
    }

    suspend fun stopSession(id: String) = dao.stopSession(id, clock())

    suspend fun eventById(id: String): CapturedIntentEvent? = dao.eventById(id)?.toEvent()

    suspend fun clearEvents() = dao.clearEvents()

    private fun CapturedIntentEvent.toEntity() = CapturedIntentEntity(
        id = id,
        sessionId = sessionId,
        timestamp = timestamp,
        backend = backend.name,
        dispatchType = dispatchType?.name,
        dispatchApi = dispatchApi,
        sourcePackage = sourcePackage,
        sourceUid = sourceUid,
        sourcePid = sourcePid,
        targetPackage = targetPackage,
        targetComponent = targetComponent,
        action = action,
        dataUri = dataUri,
        scheme = scheme,
        host = host,
        path = path,
        mimeType = mimeType,
        categories = categories,
        flags = flags,
        flagsDecoded = flagsDecoded,
        extras = extras,
        resultNote = resultNote,
        rawEvidence = rawEvidence,
    )

    private fun CapturedIntentEntity.toEvent() = CapturedIntentEvent(
        id = id,
        sessionId = sessionId,
        timestamp = timestamp,
        backend = runCatching { CaptureBackend.valueOf(backend) }.getOrDefault(CaptureBackend.ROOT_LOGCAT),
        dispatchType = dispatchType?.let { runCatching { DispatchType.valueOf(it) }.getOrNull() },
        dispatchApi = dispatchApi,
        sourcePackage = sourcePackage,
        sourceUid = sourceUid,
        sourcePid = sourcePid,
        targetPackage = targetPackage,
        targetComponent = targetComponent,
        action = action,
        dataUri = dataUri,
        scheme = scheme,
        host = host,
        path = path,
        mimeType = mimeType,
        categories = categories,
        flags = flags,
        flagsDecoded = flagsDecoded,
        extras = extras,
        resultNote = resultNote,
        rawEvidence = rawEvidence,
    )
}
