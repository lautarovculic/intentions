package com.lautarovculic.intentions.domain.usecase

import com.lautarovculic.intentions.core.model.ComponentRecord
import com.lautarovculic.intentions.core.model.InstalledAppRecord
import com.lautarovculic.intentions.data.repository.AppRepository

class ScanAppsUseCase(private val repo: AppRepository) {
    suspend operator fun invoke(includeSystem: Boolean): List<InstalledAppRecord> = repo.listApps(includeSystem)
}

// components for a package, optionally enriched via root dumpsys
class ScanComponentsUseCase(private val repo: AppRepository) {
    data class Result(
        val components: List<ComponentRecord>,
        val rootDeepLinks: List<com.lautarovculic.intentions.core.model.IntentFilterModel> = emptyList(),
        val rawDump: String? = null,
    )

    suspend operator fun invoke(packageName: String, enrichWithRoot: Boolean): Result {
        val components = repo.listComponents(packageName)
        if (!enrichWithRoot) return Result(components)
        val (filters, dump) = repo.deepLinkFiltersFromRoot(packageName)
        return Result(components, filters, dump)
    }
}
