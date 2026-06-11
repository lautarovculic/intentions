package com.lautarovculic.intentions.core.shell

// Shell argument escaping so dynamic values can't break out of their argument.
object ShellArgs {

    // POSIX single-quote escaping
    fun quote(value: String): String {
        if (value.isEmpty()) return "''"
        // fast path: no quoting needed
        if (value.all { it.isLetterOrDigit() || it in SAFE }) return value
        return "'" + value.replace("'", "'\\''") + "'"
    }

    // quote each value as a single shell token
    fun quoteAll(values: List<String>): String = values.joinToString(" ") { quote(it) }

    private val SAFE = setOf('_', '-', '.', '/', '@', '%', '+', ':', '=', ',')
}
