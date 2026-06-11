package com.lautarovculic.intentions.core.model

import kotlinx.serialization.Serializable

// Package metadata for the Apps browser.
@Serializable
data class InstalledAppRecord(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val uid: Int,
    val minSdk: Int,
    val targetSdk: Int,
    val isSystem: Boolean,
    val isDebuggable: Boolean,
    val enabled: Boolean,
    val sourceDir: String?,
    val dataDir: String?,
    val sharedUserId: String?,
    val installerPackage: String?,
    val requestedPermissions: List<String> = emptyList(),
    val exportedActivityCount: Int = 0,
    val exportedServiceCount: Int = 0,
    val exportedReceiverCount: Int = 0,
    val exportedProviderCount: Int = 0,
    val deepLinkCount: Int = 0,
    val hasAppLinks: Boolean = false,
) {
    val totalExportedComponents: Int
        get() = exportedActivityCount + exportedServiceCount +
            exportedReceiverCount + exportedProviderCount
}
