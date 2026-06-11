package com.lautarovculic.intentions.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IntentRunDao {
    @Insert
    suspend fun insert(run: IntentRunEntity): Long

    @Query("SELECT * FROM intent_runs ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 300): Flow<List<IntentRunEntity>>

    @Query("SELECT * FROM intent_runs WHERE id = :id")
    suspend fun byId(id: Long): IntentRunEntity?

    @Query("SELECT COUNT(*) FROM intent_runs")
    fun count(): Flow<Int>

    @Query("DELETE FROM intent_runs")
    suspend fun clear()

    @Query("DELETE FROM intent_runs WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PresetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: PresetEntity): Long

    @Query("SELECT * FROM presets ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PresetEntity>>

    @Query("SELECT COUNT(*) FROM presets")
    fun count(): Flow<Int>

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface CaptureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CapturedIntentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CaptureSessionEntity)

    @Query("UPDATE capture_sessions SET stoppedAt = :stoppedAt WHERE id = :id")
    suspend fun stopSession(id: String, stoppedAt: Long)

    @Query("SELECT * FROM captured_intents ORDER BY timestamp DESC LIMIT :limit")
    fun observeEvents(limit: Int = 1000): Flow<List<CapturedIntentEntity>>

    @Query("SELECT * FROM captured_intents WHERE id = :id")
    suspend fun eventById(id: String): CapturedIntentEntity?

    @Query("SELECT * FROM capture_sessions ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<CaptureSessionEntity>>

    @Query("SELECT COUNT(*) FROM captured_intents")
    fun count(): Flow<Int>

    @Query("DELETE FROM captured_intents")
    suspend fun clearEvents()
}
