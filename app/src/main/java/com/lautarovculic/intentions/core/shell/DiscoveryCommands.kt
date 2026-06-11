package com.lautarovculic.intentions.core.shell

// Read-only discovery commands: pm / dumpsys / logcat.
object PmCommands {
    fun listPackages(includePaths: Boolean = true, includeUid: Boolean = true, userApps: Boolean? = null): String {
        val sb = StringBuilder("pm list packages")
        if (includePaths) sb.append(" -f")
        if (includeUid) sb.append(" -U")
        when (userApps) {
            true -> sb.append(" -3")
            false -> sb.append(" -s")
            null -> {}
        }
        return sb.toString()
    }

    fun path(pkg: String) = "pm path ${ShellArgs.quote(pkg)}"
    fun dump(pkg: String) = "pm dump ${ShellArgs.quote(pkg)}"
    fun clear(pkg: String) = "pm clear ${ShellArgs.quote(pkg)}"
}

object DumpsysCommands {
    fun packageInfo(pkg: String) = "dumpsys package ${ShellArgs.quote(pkg)}"
    fun activities() = "dumpsys activity activities"
    fun broadcasts() = "dumpsys activity broadcasts"
    fun services() = "dumpsys activity services"
    fun providers() = "dumpsys activity providers"
    fun packageQueries() = "dumpsys package queries"
    fun topActivity() = "dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'"
}

object LogcatCommands {
    // threadtime stream for live capture
    fun stream() = "logcat -v threadtime"

    // one-shot dump of the last N lines
    fun dump(lines: Int = 500) = "logcat -d -v threadtime -t $lines"

    fun clear() = "logcat -c"

    // Tail from now (-T 1) instead of replaying the whole buffer.
    // -s (not a trailing *:S) avoids the su shell glob-expanding '*'.
    fun streamFiltered(): String {
        val tags = listOf(
            "ActivityTaskManager", "ActivityManager", "BroadcastQueue",
            "AndroidRuntime", "PackageManager", "WindowManager",
        )
        return "logcat -v threadtime -T 1 -s " + tags.joinToString(" ") { "$it:V" }
    }
}
