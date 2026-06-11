package com.lautarovculic.intentions.core.root

import com.lautarovculic.intentions.core.model.ShellResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.InputStreamReader

// Root backend: each call forks su -c <command>, runs on io, fully captured with a timeout.
class ShellRootExecutor(
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val suBinary: String = "su",
    private val clock: () -> Long = System::currentTimeMillis,
) : RootExecutor {

    @Volatile
    private var cachedRoot: Boolean? = null

    override suspend fun isRootAvailable(): Boolean {
        cachedRoot?.let { return it }
        val result = exec("id", timeoutMs = 4_000L)
        val ok = result.success && result.stdout.contains("uid=0")
        cachedRoot = ok
        return ok
    }

    override suspend fun exec(command: String, timeoutMs: Long): ShellResult = withContext(io) {
        val start = clock()
        var process: Process? = null
        try {
            process = ProcessBuilder(suBinary, "-c", command)
                .redirectErrorStream(false)
                .start()
            val proc = process
            val outcome = withTimeoutOrNull(timeoutMs) {
                // drain both streams concurrently to avoid deadlock
                coroutineScope {
                    val outDef = async(io) { readAll(proc.inputStream) }
                    val errDef = async(io) { readAll(proc.errorStream) }
                    val exit = runInterruptible(io) { proc.waitFor() }
                    Triple(outDef.await(), errDef.await(), exit)
                }
            }
            if (outcome == null) {
                proc.destroyForcibly()
                ShellResult.failure(
                    command = command,
                    message = "Command timed out after ${timeoutMs}ms",
                    exitCode = ShellResult.EXIT_TIMEOUT,
                    durationMs = clock() - start,
                    timestamp = start,
                )
            } else {
                val (out, err, exit) = outcome
                ShellResult(
                    command = command,
                    stdout = out.trimEnd('\n'),
                    stderr = err.trimEnd('\n'),
                    exitCode = exit,
                    durationMs = clock() - start,
                    timestamp = start,
                )
            }
        } catch (t: Throwable) {
            process?.destroyForcibly()
            val msg = t.message ?: t.javaClass.simpleName
            val code = if (msg.contains("No such file", true) || msg.contains("error=2"))
                ShellResult.EXIT_NO_ROOT else ShellResult.EXIT_EXCEPTION
            cachedRoot = if (code == ShellResult.EXIT_NO_ROOT) false else cachedRoot
            ShellResult.failure(command, "su execution failed: $msg", code, clock() - start, start)
        }
    }

    override suspend fun stream(command: String, onLine: suspend (String) -> Boolean) = withContext(io) {
        var process: Process? = null
        try {
            process = ProcessBuilder(suBinary, "-c", command)
                .redirectErrorStream(true)
                .start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            while (currentCoroutineContext().isActive) {
                val line = reader.readLine() ?: break
                if (!onLine(line)) break
            }
        } catch (t: Throwable) {
            // surface stream failures to logcat
            android.util.Log.w("Intentions", "root stream failed: $command", t)
        } finally {
            process?.destroyForcibly()
        }
        Unit
    }

    private fun readAll(stream: java.io.InputStream): String {
        val sb = StringBuilder()
        BufferedReader(InputStreamReader(stream)).use { reader ->
            reader.forEachLine { sb.append(it).append('\n') }
        }
        return sb.toString()
    }
}
