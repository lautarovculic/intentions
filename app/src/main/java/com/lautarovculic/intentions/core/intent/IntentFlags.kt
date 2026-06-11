package com.lautarovculic.intentions.core.intent

import android.content.Intent

// Selectable Intent flags; maps names <-> int masks.
enum class IntentFlag(val mask: Int) {
    ACTIVITY_NEW_TASK(Intent.FLAG_ACTIVITY_NEW_TASK),
    ACTIVITY_CLEAR_TOP(Intent.FLAG_ACTIVITY_CLEAR_TOP),
    ACTIVITY_CLEAR_TASK(Intent.FLAG_ACTIVITY_CLEAR_TASK),
    ACTIVITY_SINGLE_TOP(Intent.FLAG_ACTIVITY_SINGLE_TOP),
    ACTIVITY_NO_HISTORY(Intent.FLAG_ACTIVITY_NO_HISTORY),
    ACTIVITY_MULTIPLE_TASK(Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
    ACTIVITY_EXCLUDE_FROM_RECENTS(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS),
    ACTIVITY_REORDER_TO_FRONT(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
    ACTIVITY_NO_ANIMATION(Intent.FLAG_ACTIVITY_NO_ANIMATION),
    ACTIVITY_LAUNCH_ADJACENT(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT),
    GRANT_READ_URI_PERMISSION(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    GRANT_WRITE_URI_PERMISSION(Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
    GRANT_PERSISTABLE_URI_PERMISSION(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),
    GRANT_PREFIX_URI_PERMISSION(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION),
    RECEIVER_FOREGROUND(Intent.FLAG_RECEIVER_FOREGROUND),
    FROM_BACKGROUND(Intent.FLAG_FROM_BACKGROUND),
    DEBUG_LOG_RESOLUTION(Intent.FLAG_DEBUG_LOG_RESOLUTION);

    companion object {
        fun maskOf(names: Collection<String>): Int {
            var mask = 0
            for (n in names) {
                runCatching { valueOf(n) }.getOrNull()?.let { mask = mask or it.mask }
            }
            return mask
        }

        fun decode(mask: Int): List<String> =
            entries.filter { (mask and it.mask) == it.mask && it.mask != 0 }.map { it.name }
    }
}
