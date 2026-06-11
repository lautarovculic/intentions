package com.lautarovculic.intentions.core.scanner

import com.lautarovculic.intentions.core.model.IntentFilterModel

// Best-effort parser for dumpsys package intent-filter blocks (ROM-dependent).
object DumpsysPackageParser {

    private val schemeRegex = Regex("""Scheme:\s*"([^"]+)"""")
    private val authorityRegex = Regex("""Authority:\s*"([^"]+)"""")
    private val actionRegex = Regex("""Action:\s*"([^"]+)"""")
    private val categoryRegex = Regex("""Category:\s*"([^"]+)"""")
    private val pathRegex = Regex("""Path(?:Pattern|Prefix|Literal)?:?\s*(?:Pattern|Prefix|Literal)?\s*"([^"]+)"""")
    private val typeRegex = Regex("""Type:\s*"([^"]+)"""")
    private val filterStart = Regex("""\bfilter\b\s+[0-9a-fA-Fx]+""")

    // parse intent-filter blocks from the dump
    fun parseFilters(dump: String): List<IntentFilterModel> {
        val blocks = mutableListOf<MutableList<String>>()
        var current: MutableList<String>? = null
        for (line in dump.lineSequence()) {
            if (filterStart.containsMatchIn(line)) {
                current = mutableListOf()
                blocks += current
            }
            current?.add(line)
        }
        return blocks.mapNotNull { block ->
            val text = block.joinToString("\n")
            val actions = actionRegex.findAll(text).map { it.groupValues[1] }.toList()
            val categories = categoryRegex.findAll(text).map { it.groupValues[1] }.toList()
            val schemes = schemeRegex.findAll(text).map { it.groupValues[1] }.distinct().toList()
            val hosts = authorityRegex.findAll(text).map { it.groupValues[1] }.distinct().toList()
            val paths = pathRegex.findAll(text).map { it.groupValues[1] }.distinct().toList()
            val types = typeRegex.findAll(text).map { it.groupValues[1] }.distinct().toList()
            if (actions.isEmpty() && schemes.isEmpty() && types.isEmpty()) return@mapNotNull null
            IntentFilterModel(
                actions = actions,
                categories = categories,
                schemes = schemes,
                hosts = hosts,
                paths = paths,
                mimeTypes = types,
            )
        }
    }
}
