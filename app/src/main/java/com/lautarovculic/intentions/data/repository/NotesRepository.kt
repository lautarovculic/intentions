package com.lautarovculic.intentions.data.repository

import com.lautarovculic.intentions.core.storage.NoteDao
import com.lautarovculic.intentions.core.storage.NoteEntity
import kotlinx.coroutines.flow.Flow

class NotesRepository(
    private val dao: NoteDao,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val notes: Flow<List<NoteEntity>> = dao.observeAll()

    suspend fun add(title: String, body: String, tag: String? = null, linkedRunId: Long? = null) {
        dao.insert(NoteEntity(title = title, body = body, tag = tag, linkedRunId = linkedRunId, createdAt = clock()))
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
