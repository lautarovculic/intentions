package com.lautarovculic.intentions.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ComponentType(val label: String) {
    ACTIVITY("Activity"),
    SERVICE("Service"),
    RECEIVER("Receiver"),
    PROVIDER("Provider"),
}

// An IPC-exposed component from the PackageManager.
@Serializable
data class ComponentRecord(
    val packageName: String,
    val className: String,
    val type: ComponentType,
    val exported: Boolean,
    val enabled: Boolean,
    val permission: String? = null,
    val readPermission: String? = null,   // providers
    val writePermission: String? = null,  // providers
    val authority: String? = null,        // providers
    val grantUriPermissions: Boolean = false,
    val processName: String? = null,
    val intentFilters: List<IntentFilterModel> = emptyList(),
) {
    val shortClassName: String
        get() = if (className.startsWith(packageName))
            className.removePrefix(packageName) else className

    val shortString: String get() = "$packageName/$shortClassName"

    // exported with no permission gate = reachable by any app
    val isUnprotectedExported: Boolean
        get() = exported && permission.isNullOrBlank() &&
            readPermission.isNullOrBlank() && writePermission.isNullOrBlank()

    val deepLinks: List<DeepLinkSurface>
        get() = intentFilters.flatMap { it.deepLinkSurfaces() }

    val hasDeepLinks: Boolean get() = deepLinks.isNotEmpty()
}

@Serializable
data class IntentFilterModel(
    val actions: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val schemes: List<String> = emptyList(),
    val hosts: List<String> = emptyList(),
    val ports: List<String> = emptyList(),
    val paths: List<String> = emptyList(),
    val mimeTypes: List<String> = emptyList(),
    val priority: Int = 0,
    val autoVerify: Boolean = false,
) {
    val isViewBrowsable: Boolean
        get() = actions.any { it == "android.intent.action.VIEW" } &&
            categories.any { it == "android.intent.category.BROWSABLE" }

    // app link = http/https VIEW+BROWSABLE
    val isAppLink: Boolean
        get() = isViewBrowsable && schemes.any { it == "http" || it == "https" }

    fun deepLinkSurfaces(): List<DeepLinkSurface> {
        if (!isViewBrowsable && schemes.isEmpty()) return emptyList()
        val result = mutableListOf<DeepLinkSurface>()
        val effectiveSchemes = if (schemes.isEmpty()) listOf("") else schemes
        val effectiveHosts = if (hosts.isEmpty()) listOf("") else hosts
        for (scheme in effectiveSchemes) {
            for (host in effectiveHosts) {
                result += DeepLinkSurface(
                    scheme = scheme,
                    host = host,
                    paths = paths,
                    isAppLink = isAppLink,
                    autoVerify = autoVerify,
                )
            }
        }
        return result
    }
}

@Serializable
data class DeepLinkSurface(
    val scheme: String,
    val host: String,
    val paths: List<String> = emptyList(),
    val isAppLink: Boolean = false,
    val autoVerify: Boolean = false,
) {
    // example URI for the builder/fuzzer
    fun exampleUri(): String {
        val s = scheme.ifBlank { "scheme" }
        val h = host.ifBlank { "host" }
        val p = paths.firstOrNull()?.takeIf { it.isNotBlank() } ?: "/path"
        val path = if (p.startsWith("/")) p else "/$p"
        return "$s://$h$path"
    }
}
