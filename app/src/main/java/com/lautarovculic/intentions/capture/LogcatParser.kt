package com.lautarovculic.intentions.capture

import com.lautarovculic.intentions.core.intent.IntentFlag
import com.lautarovculic.intentions.core.model.CaptureBackend
import com.lautarovculic.intentions.core.model.CapturedIntentEvent
import com.lautarovculic.intentions.core.model.DispatchType
import java.util.UUID

// Parses ActivityTaskManager/ActivityManager logcat lines into capture events.
class LogcatParser {

    // START u0 {act=android.intent.action.VIEW dat=scheme://host cmp=pkg/.Cls flg=0x10000000 (has extras)} from uid 10123
    private val startRegex = Regex("""START\s+u(\d+)\s+\{([^}]*)\}(?:.*?from uid (\d+))?""")
    private val permDenialRegex = Regex("""Permission Denial: (.+)""")
    private val fatalRegex = Regex("""FATAL EXCEPTION""")

    fun parse(line: String, sessionId: String, timestamp: Long): CapturedIntentEvent? {
        startRegex.find(line)?.let { return parseStart(it, line, sessionId, timestamp) }
        permDenialRegex.find(line)?.let { m ->
            if (!line.contains("Intent", ignoreCase = true)) return null
            return baseEvent(sessionId, timestamp, line).copy(
                resultNote = "Permission denied: ${m.groupValues[1].take(160)}",
            )
        }
        if (fatalRegex.containsMatchIn(line)) {
            return baseEvent(sessionId, timestamp, line).copy(resultNote = "Crash / FATAL EXCEPTION near dispatch")
        }
        return null
    }

    private fun parseStart(m: MatchResult, line: String, sessionId: String, timestamp: Long): CapturedIntentEvent {
        val body = m.groupValues[2]
        val sourceUid = m.groupValues.getOrNull(3)?.toIntOrNull()
        val fields = parseBraceFields(body)
        val flags = fields["flg"]?.let { runCatching { Integer.decode(it) }.getOrNull() } ?: 0
        val cmp = fields["cmp"]
        val dat = fields["dat"]
        val uri = dat?.let { runCatching { android.net.Uri.parse(it) }.getOrNull() }
        return CapturedIntentEvent(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestamp = timestamp,
            backend = CaptureBackend.ROOT_LOGCAT,
            dispatchType = DispatchType.START_ACTIVITY,
            dispatchApi = "ActivityTaskManager.START",
            sourceUid = sourceUid,
            targetPackage = cmp?.substringBefore('/'),
            targetComponent = cmp,
            action = fields["act"],
            dataUri = dat,
            scheme = uri?.scheme,
            host = uri?.host,
            path = uri?.path,
            mimeType = fields["typ"],
            categories = fields["cat"]?.let { listOf(it) } ?: emptyList(),
            flags = flags,
            flagsDecoded = IntentFlag.decode(flags),
            resultNote = if (body.contains("has extras")) "has extras (values not in logcat)" else null,
            rawEvidence = line,
        )
    }

    private fun parseBraceFields(body: String): Map<String, String> {
        // space-separated key=value pairs
        val result = LinkedHashMap<String, String>()
        val tokenRegex = Regex("""(\w+)=(\S+)""")
        tokenRegex.findAll(body).forEach { result[it.groupValues[1]] = it.groupValues[2] }
        return result
    }

    private fun baseEvent(sessionId: String, timestamp: Long, line: String) = CapturedIntentEvent(
        id = UUID.randomUUID().toString(),
        sessionId = sessionId,
        timestamp = timestamp,
        backend = CaptureBackend.ROOT_LOGCAT,
        rawEvidence = line,
    )
}
