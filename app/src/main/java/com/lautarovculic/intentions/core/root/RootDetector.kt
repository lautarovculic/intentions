package com.lautarovculic.intentions.core.root

import com.lautarovculic.intentions.core.model.RootProvider
import com.lautarovculic.intentions.core.model.RootStatus

// Detects root availability and, best-effort, the provider.
class RootDetector(
    private val executor: RootExecutor,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val suSearchPaths = listOf(
        "/system/bin/su", "/system/xbin/su", "/sbin/su",
        "/product/bin/su", "/vendor/bin/su", "/data/adb/magisk/su",
    )

    suspend fun detect(): RootStatus {
        val now = clock()
        val id = executor.exec("id", timeoutMs = 4_000L)
        val available = id.success && id.stdout.contains("uid=0")
        if (!available) return RootStatus.unavailable(now)

        val suPath = findSuPath()
        val provider = detectProvider(suPath)
        return RootStatus(
            available = true,
            provider = provider,
            suPath = suPath,
            idOutput = id.stdout,
            checkedAt = now,
        )
    }

    private suspend fun findSuPath(): String? {
        val which = executor.exec("which su", timeoutMs = 4_000L)
        if (which.success && which.stdout.isNotBlank()) return which.stdout.lines().first().trim()
        val ls = executor.exec("ls ${suSearchPaths.joinToString(" ")} 2>/dev/null", timeoutMs = 4_000L)
        return ls.stdout.lines().firstOrNull { it.isNotBlank() }?.trim()
    }

    private suspend fun detectProvider(suPath: String?): RootProvider {
        // probe provider dirs/binaries
        val probe = executor.exec(
            "ls -d /data/adb/magisk /data/adb/ksu /data/adb/ap /data/adb/apd 2>/dev/null; " +
                "magisk -v 2>/dev/null; ksud -V 2>/dev/null",
            timeoutMs = 5_000L,
        )
        val out = probe.combinedOutput.lowercase()
        return when {
            out.contains("ap") && out.contains("/data/adb/ap") -> RootProvider.APATCH
            out.contains("ksu") || out.contains("kernelsu") -> RootProvider.KERNELSU
            out.contains("magisk") -> RootProvider.MAGISK
            suPath?.contains("supersu", ignoreCase = true) == true -> RootProvider.SUPERSU
            suPath != null -> RootProvider.GENERIC_SU
            else -> RootProvider.GENERIC_SU
        }
    }
}
