package com.lautarovculic.intentions.data.repository

import com.lautarovculic.intentions.core.model.ComponentRecord
import com.lautarovculic.intentions.core.model.IntentFilterModel
import com.lautarovculic.intentions.core.model.InstalledAppRecord
import com.lautarovculic.intentions.core.root.RootExecutor
import com.lautarovculic.intentions.core.scanner.DumpsysPackageParser
import com.lautarovculic.intentions.core.scanner.PackageScanner
import com.lautarovculic.intentions.core.shell.DumpsysCommands

class AppRepository(
    private val scanner: PackageScanner,
    private val rootExecutor: RootExecutor,
) {
    suspend fun listApps(includeSystem: Boolean): List<InstalledAppRecord> =
        scanner.scanInstalledApps(includeSystem)

    suspend fun listComponents(packageName: String): List<ComponentRecord> =
        scanner.scanComponents(packageName)

    // raw dumpsys package evidence (root)
    suspend fun dumpPackage(packageName: String): String? {
        if (!rootExecutor.isRootAvailable()) return null
        val result = rootExecutor.exec(DumpsysCommands.packageInfo(packageName), timeoutMs = 15_000L)
        return result.combinedOutput.takeIf { it.isNotBlank() }
    }

    // custom-scheme deep links from root dumpsys
    suspend fun deepLinkFiltersFromRoot(packageName: String): Pair<List<IntentFilterModel>, String?> {
        val dump = dumpPackage(packageName) ?: return emptyList<IntentFilterModel>() to null
        return DumpsysPackageParser.parseFilters(dump) to dump
    }
}
