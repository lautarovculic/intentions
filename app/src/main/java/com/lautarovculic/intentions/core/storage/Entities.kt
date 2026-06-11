package com.lautarovculic.intentions.core.storage

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lautarovculic.intentions.core.model.ExtraSpec

// Sent-Intent history: full serialized spec + raw shell result.
@Entity(tableName = "intent_runs", indices = [Index("timestamp")])
data class IntentRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val dispatchType: String,
    val backend: String,
    val targetSummary: String,
    val specJson: String,
    val success: Boolean,
    val summary: String,
    val generatedCommand: String?,
    val stdout: String?,
    val stderr: String?,
    val exitCode: Int?,
    val durationMs: Long?,
    val errorMessage: String?,
)

// a saved Intent template
@Entity(tableName = "presets", indices = [Index("createdAt")])
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val specJson: String,
    val note: String? = null,
    val createdAt: Long,
)

// notes / findings, optionally linked to a run
@Entity(tableName = "notes", indices = [Index("createdAt")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val tag: String? = null,
    val linkedRunId: Long? = null,
    val createdAt: Long,
)

// a capture session
@Entity(tableName = "capture_sessions")
data class CaptureSessionEntity(
    @PrimaryKey val id: String,
    val label: String,
    val backend: String,
    val startedAt: Long,
    val stoppedAt: Long? = null,
)

// a captured IPC event; mirrors CapturedIntentEvent
@Entity(
    tableName = "captured_intents",
    indices = [Index("sessionId"), Index("timestamp")],
)
data class CapturedIntentEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val timestamp: Long,
    val backend: String,
    val dispatchType: String?,
    val dispatchApi: String?,
    val sourcePackage: String?,
    val sourceUid: Int?,
    val sourcePid: Int?,
    val targetPackage: String?,
    val targetComponent: String?,
    val action: String?,
    val dataUri: String?,
    val scheme: String?,
    val host: String?,
    val path: String?,
    val mimeType: String?,
    val categories: List<String>,
    val flags: Int,
    val flagsDecoded: List<String>,
    val extras: List<ExtraSpec>,
    val resultNote: String?,
    val rawEvidence: String,
)
