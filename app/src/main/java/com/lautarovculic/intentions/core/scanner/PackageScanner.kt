package com.lautarovculic.intentions.core.scanner

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.content.pm.ServiceInfo
import android.net.Uri
import com.lautarovculic.intentions.core.model.ComponentRecord
import com.lautarovculic.intentions.core.model.ComponentType
import com.lautarovculic.intentions.core.model.IntentFilterModel
import com.lautarovculic.intentions.core.model.InstalledAppRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Enumerates installed apps and components via normal PackageManager APIs.
@Suppress("DEPRECATION")
class PackageScanner(
    context: Context,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val pm = context.packageManager

    private val componentFlags = PackageManager.GET_ACTIVITIES or
        PackageManager.GET_SERVICES or
        PackageManager.GET_RECEIVERS or
        PackageManager.GET_PROVIDERS or
        PackageManager.GET_PERMISSIONS or
        PackageManager.MATCH_DISABLED_COMPONENTS

    suspend fun scanInstalledApps(includeSystem: Boolean = true): List<InstalledAppRecord> = withContext(io) {
        val appLinkPackages = appLinkHandlerPackages()
        pm.getInstalledPackages(componentFlags).mapNotNull { pkg ->
            val app = pkg.applicationInfo ?: return@mapNotNull null
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystem && isSystem) return@mapNotNull null
            toAppRecord(pkg, app, isSystem, appLinkPackages.contains(pkg.packageName))
        }.sortedBy { it.label.lowercase() }
    }

    private fun toAppRecord(
        pkg: PackageInfo,
        app: ApplicationInfo,
        isSystem: Boolean,
        hasAppLinks: Boolean,
    ): InstalledAppRecord {
        val exportedActivities = pkg.activities?.count { it.exported } ?: 0
        val exportedServices = pkg.services?.count { it.exported } ?: 0
        val exportedReceivers = pkg.receivers?.count { it.exported } ?: 0
        val exportedProviders = pkg.providers?.count { it.exported } ?: 0
        return InstalledAppRecord(
            packageName = pkg.packageName,
            label = runCatching { pm.getApplicationLabel(app).toString() }.getOrDefault(pkg.packageName),
            versionName = pkg.versionName,
            versionCode = pkg.longVersionCode,
            uid = app.uid,
            minSdk = app.minSdkVersion,
            targetSdk = app.targetSdkVersion,
            isSystem = isSystem,
            isDebuggable = (app.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
            enabled = app.enabled,
            sourceDir = app.sourceDir,
            dataDir = app.dataDir,
            sharedUserId = pkg.sharedUserId,
            installerPackage = runCatching { pm.getInstallerPackageName(pkg.packageName) }.getOrNull(),
            requestedPermissions = pkg.requestedPermissions?.toList().orEmpty(),
            exportedActivityCount = exportedActivities,
            exportedServiceCount = exportedServices,
            exportedReceiverCount = exportedReceivers,
            exportedProviderCount = exportedProviders,
            deepLinkCount = if (hasAppLinks) 1 else 0,
            hasAppLinks = hasAppLinks,
        )
    }

    suspend fun scanComponents(packageName: String): List<ComponentRecord> = withContext(io) {
        val pkg = runCatching { pm.getPackageInfo(packageName, componentFlags) }.getOrNull()
            ?: return@withContext emptyList()
        val filtersByClass = appLinkFiltersFor(packageName)
        buildList {
            pkg.activities?.forEach { add(it.toRecord(packageName, filtersByClass[it.name].orEmpty())) }
            pkg.services?.forEach { add(it.toRecord(packageName)) }
            pkg.receivers?.forEach { add(it.toRecord(packageName, ComponentType.RECEIVER)) }
            pkg.providers?.forEach { add(it.toRecord(packageName)) }
        }.sortedWith(compareByDescending<ComponentRecord> { it.exported }.thenBy { it.type.ordinal })
    }

    private fun ActivityInfo.toRecord(pkg: String, filters: List<IntentFilterModel>) = ComponentRecord(
        packageName = pkg,
        className = name,
        type = ComponentType.ACTIVITY,
        exported = exported,
        enabled = isEnabled,
        permission = permission,
        processName = processName,
        intentFilters = filters,
    )

    private fun ActivityInfo.toRecord(pkg: String, type: ComponentType) = ComponentRecord(
        packageName = pkg,
        className = name,
        type = type,
        exported = exported,
        enabled = isEnabled,
        permission = permission,
        processName = processName,
    )

    private fun ServiceInfo.toRecord(pkg: String) = ComponentRecord(
        packageName = pkg,
        className = name,
        type = ComponentType.SERVICE,
        exported = exported,
        enabled = isEnabled,
        permission = permission,
        processName = processName,
    )

    private fun ProviderInfo.toRecord(pkg: String) = ComponentRecord(
        packageName = pkg,
        className = name,
        type = ComponentType.PROVIDER,
        exported = exported,
        enabled = isEnabled,
        permission = null,
        readPermission = readPermission,
        writePermission = writePermission,
        authority = authority,
        grantUriPermissions = grantUriPermissions,
        processName = processName,
    )

    // packages with http/https VIEW+BROWSABLE handlers (app links)
    private fun appLinkHandlerPackages(): Set<String> {
        val result = HashSet<String>()
        for (scheme in listOf("https", "http")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$scheme://example.com/"))
                .addCategory(Intent.CATEGORY_BROWSABLE)
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).forEach {
                it.activityInfo?.packageName?.let(result::add)
            }
        }
        return result
    }

    // app-link filters keyed by activity class
    private fun appLinkFiltersFor(packageName: String): Map<String, List<IntentFilterModel>> {
        val out = HashMap<String, MutableList<IntentFilterModel>>()
        for (scheme in listOf("https", "http")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("$scheme://example.com/"))
                .addCategory(Intent.CATEGORY_BROWSABLE)
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL or PackageManager.GET_RESOLVED_FILTER)
                .forEach { ri ->
                    val ai = ri.activityInfo ?: return@forEach
                    if (ai.packageName != packageName) return@forEach
                    val f = ri.filter ?: return@forEach
                    out.getOrPut(ai.name) { mutableListOf() }.add(f.toModel())
                }
        }
        return out
    }

    private fun android.content.IntentFilter.toModel(): IntentFilterModel {
        fun <T> iter(count: Int, get: (Int) -> T) = (0 until count).map(get)
        return IntentFilterModel(
            actions = iter(countActions()) { getAction(it) },
            categories = iter(countCategories()) { getCategory(it) },
            schemes = iter(countDataSchemes()) { getDataScheme(it) },
            hosts = iter(countDataAuthorities()) { getDataAuthority(it).host },
            paths = iter(countDataPaths()) { getDataPath(it).path },
            mimeTypes = iter(countDataTypes()) { getDataType(it) },
            priority = priority,
            // autoVerify has no stable public getter
            autoVerify = false,
        )
    }
}
