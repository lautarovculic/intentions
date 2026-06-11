package com.lautarovculic.intentions.core.model

import kotlinx.serialization.Serializable

// Serializable model for an IPC dispatch; Intents and shell commands are built from this.
@Serializable
data class IntentSpec(
    val dispatchType: DispatchType = DispatchType.START_ACTIVITY,
    val targeting: TargetingMode = TargetingMode.IMPLICIT,
    val action: String? = null,
    val dataUri: String? = null,
    val mimeType: String? = null,
    // setPackage target for implicit intents
    val packageName: String? = null,
    // explicit component: package + FQ class
    val componentPackage: String? = null,
    val componentClass: String? = null,
    val categories: List<String> = emptyList(),
    // IntentFlag names, resolved to an int mask at dispatch
    val flags: List<String> = emptyList(),
    val extras: List<ExtraSpec> = emptyList(),
    val identifier: String? = null,
) {
    val hasExplicitComponent: Boolean
        get() = !componentPackage.isNullOrBlank() && !componentClass.isNullOrBlank()

    // pkg/class short form
    fun componentShortString(): String? {
        val pkg = componentPackage ?: return null
        val cls = componentClass ?: return null
        val shortCls = if (cls.startsWith(pkg)) cls.removePrefix(pkg) else cls
        return "$pkg/$shortCls"
    }

    companion object {
        fun forComponent(pkg: String, cls: String, dispatchType: DispatchType) = IntentSpec(
            dispatchType = dispatchType,
            targeting = TargetingMode.EXPLICIT_COMPONENT,
            componentPackage = pkg,
            componentClass = cls,
        )

        fun forDeepLink(uri: String, pkg: String? = null) = IntentSpec(
            dispatchType = DispatchType.START_ACTIVITY,
            targeting = if (pkg != null) TargetingMode.PACKAGE_ONLY else TargetingMode.IMPLICIT,
            action = "android.intent.action.VIEW",
            dataUri = uri,
            packageName = pkg,
            categories = listOf(
                "android.intent.category.BROWSABLE",
                "android.intent.category.DEFAULT",
            ),
        )
    }
}

// how the dispatch is delivered
@Serializable
enum class DispatchType(val label: String, val isContent: Boolean = false) {
    START_ACTIVITY("startActivity"),
    START_ACTIVITY_FOR_RESULT("startActivityForResult"),
    START_ACTIVITY_WAIT("am start -W"),
    START_SERVICE("startService"),
    START_FOREGROUND_SERVICE("startForegroundService"),
    SEND_BROADCAST("sendBroadcast"),
    SEND_ORDERED_BROADCAST("sendOrderedBroadcast"),
    CONTENT_QUERY("content query", isContent = true),
    CONTENT_INSERT("content insert", isContent = true),
    CONTENT_UPDATE("content update", isContent = true),
    CONTENT_DELETE("content delete", isContent = true),
    CONTENT_OPEN_FILE("content read", isContent = true);
}

@Serializable
enum class TargetingMode(val label: String) {
    EXPLICIT_COMPONENT("Explicit component"),
    PACKAGE_ONLY("Package-only"),
    IMPLICIT("Implicit"),
}
