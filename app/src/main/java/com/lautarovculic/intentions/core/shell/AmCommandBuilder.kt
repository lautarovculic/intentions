package com.lautarovculic.intentions.core.shell

import com.lautarovculic.intentions.core.model.DispatchType
import com.lautarovculic.intentions.core.model.ExtraType
import com.lautarovculic.intentions.core.model.IntentSpec
import com.lautarovculic.intentions.core.model.TargetingMode

// Builds am command lines from an IntentSpec (shared with the adb/su exporters).
object AmCommandBuilder {

    data class Build(
        val command: String,
        // notes for anything am can't express
        val warnings: List<String>,
    )

    private fun subCommand(type: DispatchType): String = when (type) {
        DispatchType.START_ACTIVITY -> "am start"
        DispatchType.START_ACTIVITY_WAIT -> "am start -W"
        DispatchType.START_ACTIVITY_FOR_RESULT -> "am start" // am cannot startForResult
        DispatchType.START_SERVICE -> "am startservice"
        DispatchType.START_FOREGROUND_SERVICE -> "am start-foreground-service"
        DispatchType.SEND_BROADCAST -> "am broadcast"
        DispatchType.SEND_ORDERED_BROADCAST -> "am broadcast" // am broadcast is ordered
        else -> "am start"
    }

    // Escaped am argument tokens (action, data, type, categories, component, flags, extras).
    fun intentArgs(spec: IntentSpec, flagsMask: Int): Pair<List<String>, List<String>> {
        val args = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        spec.action?.takeIf { it.isNotBlank() }?.let { args += listOf("-a", ShellArgs.quote(it)) }
        spec.dataUri?.takeIf { it.isNotBlank() }?.let { args += listOf("-d", ShellArgs.quote(it)) }
        spec.mimeType?.takeIf { it.isNotBlank() }?.let { args += listOf("-t", ShellArgs.quote(it)) }
        spec.identifier?.takeIf { it.isNotBlank() }?.let { args += listOf("-i", ShellArgs.quote(it)) }

        spec.categories.filter { it.isNotBlank() }.forEach { args += listOf("-c", ShellArgs.quote(it)) }

        when (spec.targeting) {
            TargetingMode.EXPLICIT_COMPONENT -> {
                val comp = spec.componentShortString()
                if (comp != null) args += listOf("-n", ShellArgs.quote(comp))
                else warnings += "Explicit component selected but package/class missing."
            }
            TargetingMode.PACKAGE_ONLY -> {
                // am has no setPackage; closest is an explicit component
                warnings += "Package-only targeting is not expressible with am; sending implicitly. " +
                    "Use the in-app backend for true setPackage()."
            }
            TargetingMode.IMPLICIT -> { /* nothing */ }
        }

        if (flagsMask != 0) {
            args += listOf("-f", "0x" + Integer.toHexString(flagsMask))
        }

        spec.extras.forEach { extra ->
            val sw = extra.type.amSwitch
            if (sw == null) {
                warnings += "Extra '${extra.key}' (${extra.type.label}) cannot be expressed via am; " +
                    "use the in-app backend or Kotlin export."
                return@forEach
            }
            when (extra.type) {
                ExtraType.NULL -> args += listOf(sw, ShellArgs.quote(extra.key))
                else -> args += listOf(sw, ShellArgs.quote(extra.key), ShellArgs.quote(extra.value))
            }
        }
        return args to warnings
    }

    fun build(spec: IntentSpec, flagsMask: Int): Build {
        val (args, warnings) = intentArgs(spec, flagsMask)
        val command = (subCommand(spec.dispatchType).split(" ") + args).joinToString(" ")
        return Build(command, warnings)
    }
}
