package com.lautarovculic.intentions.core.intent

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.lautarovculic.intentions.core.model.DispatchType
import com.lautarovculic.intentions.core.model.DispatchOutcome
import com.lautarovculic.intentions.core.model.ExecutionBackend
import com.lautarovculic.intentions.core.model.ExtraType
import com.lautarovculic.intentions.core.model.IntentSpec
import com.lautarovculic.intentions.core.model.ShellResult
import com.lautarovculic.intentions.core.root.RootExecutor
import com.lautarovculic.intentions.core.shell.AmCommandBuilder
import com.lautarovculic.intentions.core.shell.ContentCommandBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Sends an IntentSpec through a backend and returns a logged DispatchOutcome.
class IntentDispatcher(
    private val appContext: Context,
    private val rootExecutor: RootExecutor,
    private val main: CoroutineDispatcher = Dispatchers.Main,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    suspend fun dispatch(spec: IntentSpec, backend: ExecutionBackend): DispatchOutcome =
        when (backend) {
            ExecutionBackend.IN_APP -> dispatchInApp(spec)
            ExecutionBackend.ROOT_SHELL -> dispatchRoot(spec)
        }

    // in-app

    private suspend fun dispatchInApp(spec: IntentSpec): DispatchOutcome {
        val now = clock()
        if (spec.dispatchType.isContent) return dispatchContentInApp(spec, now)
        return try {
            val intent = IntentFactory.create(spec)
            val summary = withContext(main) { fire(intent, spec.dispatchType) }
            DispatchOutcome(
                spec = spec,
                backend = ExecutionBackend.IN_APP,
                success = true,
                summary = summary,
                timestamp = now,
            )
        } catch (t: Throwable) {
            DispatchOutcome(
                spec = spec,
                backend = ExecutionBackend.IN_APP,
                success = false,
                summary = "Dispatch failed",
                errorMessage = "${t.javaClass.simpleName}: ${t.message}",
                timestamp = now,
            )
        }
    }

    private fun fire(intent: Intent, type: DispatchType): String = when (type) {
        DispatchType.START_ACTIVITY,
        DispatchType.START_ACTIVITY_FOR_RESULT,
        DispatchType.START_ACTIVITY_WAIT -> {
            // non-Activity context needs NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(intent)
            "startActivity dispatched (NEW_TASK auto-added for non-Activity context)"
        }
        DispatchType.START_SERVICE -> {
            val cn = appContext.startService(intent)
            if (cn != null) "Service started: ${cn.flattenToShortString()}" else "startService returned null (not found / not permitted)"
        }
        DispatchType.START_FOREGROUND_SERVICE -> {
            val cn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                appContext.startForegroundService(intent) else appContext.startService(intent)
            if (cn != null) "Foreground service started: ${cn.flattenToShortString()}" else "returned null"
        }
        DispatchType.SEND_BROADCAST -> { appContext.sendBroadcast(intent); "Broadcast sent" }
        DispatchType.SEND_ORDERED_BROADCAST -> {
            appContext.sendOrderedBroadcast(intent, null); "Ordered broadcast sent"
        }
        else -> { appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); "startActivity dispatched" }
    }

    private suspend fun dispatchContentInApp(spec: IntentSpec, now: Long): DispatchOutcome = withContext(Dispatchers.IO) {
        val uri = spec.dataUri
        if (uri.isNullOrBlank()) {
            return@withContext DispatchOutcome(spec, ExecutionBackend.IN_APP, false,
                "Content URI required", errorMessage = "dataUri is empty", timestamp = now)
        }
        try {
            val cr = appContext.contentResolver
            val summary = when (spec.dispatchType) {
                DispatchType.CONTENT_QUERY -> {
                    cr.query(Uri.parse(uri), null, null, null, null)?.use { c ->
                        buildString {
                            append("rows=${c.count} cols=${c.columnCount}\n")
                            append(c.columnNames.joinToString(" | "))
                            var n = 0
                            while (c.moveToNext() && n < 50) {
                                append("\n")
                                append((0 until c.columnCount).joinToString(" | ") { i ->
                                    runCatching { c.getString(i) }.getOrDefault("<non-text>") ?: "null"
                                })
                                n++
                            }
                        }
                    } ?: "query returned null cursor"
                }
                DispatchType.CONTENT_INSERT -> {
                    val values = spec.extras.toContentValues()
                    "inserted -> ${cr.insert(Uri.parse(uri), values)}"
                }
                DispatchType.CONTENT_UPDATE -> {
                    val values = spec.extras.toContentValues()
                    "rows updated = ${cr.update(Uri.parse(uri), values, null, null)}"
                }
                DispatchType.CONTENT_DELETE -> "rows deleted = ${cr.delete(Uri.parse(uri), null, null)}"
                DispatchType.CONTENT_OPEN_FILE -> {
                    cr.openInputStream(Uri.parse(uri))?.use { it.readBytes() }?.let { bytes ->
                        "opened ${bytes.size} bytes; head=" + bytes.take(256).toByteArray().decodeToString()
                    } ?: "openInputStream returned null"
                }
                else -> "unsupported content op"
            }
            DispatchOutcome(spec, ExecutionBackend.IN_APP, true, summary, timestamp = now)
        } catch (t: Throwable) {
            DispatchOutcome(spec, ExecutionBackend.IN_APP, false, "Provider op failed",
                errorMessage = "${t.javaClass.simpleName}: ${t.message}", timestamp = now)
        }
    }

    private fun List<com.lautarovculic.intentions.core.model.ExtraSpec>.toContentValues(): ContentValues {
        val cv = ContentValues()
        forEach { e ->
            when (e.type) {
                ExtraType.INT -> cv.put(e.key, e.value.toIntOrNull())
                ExtraType.LONG -> cv.put(e.key, e.value.toLongOrNull())
                ExtraType.FLOAT -> cv.put(e.key, e.value.toFloatOrNull())
                ExtraType.DOUBLE -> cv.put(e.key, e.value.toDoubleOrNull())
                ExtraType.BOOLEAN -> cv.put(e.key, e.value.toBooleanStrictOrNull())
                ExtraType.NULL -> cv.putNull(e.key)
                else -> cv.put(e.key, e.value)
            }
        }
        return cv
    }

    // root shell

    private suspend fun dispatchRoot(spec: IntentSpec): DispatchOutcome {
        val now = clock()
        val command = if (spec.dispatchType.isContent) {
            ContentCommandBuilder.build(ContentCommandBuilder.fromSpec(spec))
        } else {
            AmCommandBuilder.build(spec, IntentFlag.maskOf(spec.flags)).command
        }
        if (!rootExecutor.isRootAvailable()) {
            return DispatchOutcome(
                spec, ExecutionBackend.ROOT_SHELL, false, "Root not available",
                generatedCommand = command,
                shellResult = ShellResult.failure(command, "Root not available", ShellResult.EXIT_NO_ROOT, timestamp = now),
                errorMessage = "su returned no uid=0", timestamp = now,
            )
        }
        val result = rootExecutor.exec(command, timeoutMs = 15_000L)
        return DispatchOutcome(
            spec = spec,
            backend = ExecutionBackend.ROOT_SHELL,
            success = result.success,
            summary = if (result.success) "Executed (exit ${result.exitCode}, ${result.durationMs}ms)"
            else "Command failed (exit ${result.exitCode})",
            generatedCommand = command,
            shellResult = result,
            timestamp = now,
        )
    }
}
