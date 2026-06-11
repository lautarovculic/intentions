package com.lautarovculic.intentions.core.model

// Superuser provider, when detectable.
enum class RootProvider(val label: String) {
    MAGISK("Magisk"),
    KERNELSU("KernelSU"),
    APATCH("APatch"),
    SUPERSU("SuperSU"),
    GENERIC_SU("su (unknown provider)"),
    NONE("none"),
}

// Root availability snapshot for the dashboard.
data class RootStatus(
    val available: Boolean,
    val provider: RootProvider,
    val suPath: String?,
    val idOutput: String?,
    val checkedAt: Long,
) {
    companion object {
        fun unavailable(checkedAt: Long) =
            RootStatus(false, RootProvider.NONE, null, null, checkedAt)
    }
}
