package com.lautarovculic.intentions.core.storage

import androidx.room.TypeConverter
import com.lautarovculic.intentions.core.model.ExtraSpec
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// compact JSON for list columns
internal val StorageJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

class Converters {
    @TypeConverter
    fun stringListToJson(value: List<String>?): String =
        StorageJson.encodeToString(ListSerializer(String.serializer()), value ?: emptyList())

    @TypeConverter
    fun jsonToStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList()
        else runCatching { StorageJson.decodeFromString(ListSerializer(String.serializer()), value) }.getOrDefault(emptyList())

    @TypeConverter
    fun extrasToJson(value: List<ExtraSpec>?): String =
        StorageJson.encodeToString(ListSerializer(ExtraSpec.serializer()), value ?: emptyList())

    @TypeConverter
    fun jsonToExtras(value: String?): List<ExtraSpec> =
        if (value.isNullOrBlank()) emptyList()
        else runCatching { StorageJson.decodeFromString(ListSerializer(ExtraSpec.serializer()), value) }.getOrDefault(emptyList())
}
