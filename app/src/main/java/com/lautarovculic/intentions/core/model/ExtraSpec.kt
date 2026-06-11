package com.lautarovculic.intentions.core.model

import kotlinx.serialization.Serializable

// A single typed Intent extra; value kept as a raw string, arrays comma-separated.
@Serializable
data class ExtraSpec(
    val key: String,
    val type: ExtraType,
    val value: String = "",
) {
    fun arrayValues(): List<String> =
        if (value.isBlank()) emptyList() else value.split(",").map { it.trim() }
}

@Serializable
enum class ExtraType(
    val label: String,
    // am/content --bind switch, or null if not expressible in shell
    val amSwitch: String?,
    val isArray: Boolean = false,
) {
    STRING("String", "--es"),
    BOOLEAN("Boolean", "--ez"),
    INT("Int", "--ei"),
    LONG("Long", "--el"),
    FLOAT("Float", "--ef"),
    DOUBLE("Double", null),                 // am has no double switch
    URI("Uri", "--eu"),
    NULL("null (String)", "--esn"),
    STRING_ARRAY("String[]", "--esa", isArray = true),
    INT_ARRAY("Int[]", "--eia", isArray = true),
    LONG_ARRAY("Long[]", "--ela", isArray = true),
    FLOAT_ARRAY("Float[]", "--efa", isArray = true),
    BOOLEAN_ARRAY("Boolean[]", null, isArray = true); // am has no boolean-array switch

    val shellSupported: Boolean get() = amSwitch != null

    companion object {
        val selectable: List<ExtraType> = entries
    }
}
