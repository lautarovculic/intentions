package com.lautarovculic.intentions.core.intent

import com.lautarovculic.intentions.core.model.DispatchType
import com.lautarovculic.intentions.core.model.ExtraSpec
import com.lautarovculic.intentions.core.model.ExtraType
import com.lautarovculic.intentions.core.model.IntentSpec
import com.lautarovculic.intentions.core.model.TargetingMode
import com.lautarovculic.intentions.core.shell.AmCommandBuilder
import com.lautarovculic.intentions.core.shell.ContentCommandBuilder
import kotlinx.serialization.json.Json

// Renders an IntentSpec as adb / su / Kotlin / JSON.
object IntentExporter {

    val json = Json {
        prettyPrint = true
        encodeDefaults = false
        ignoreUnknownKeys = true
    }

    enum class Format(val label: String) { ADB("adb"), SU("su"), KOTLIN("Kotlin"), JSON("JSON") }

    fun export(spec: IntentSpec, format: Format): String = when (format) {
        Format.ADB -> adb(spec)
        Format.SU -> su(spec)
        Format.KOTLIN -> kotlin(spec)
        Format.JSON -> json.encodeToString(IntentSpec.serializer(), spec)
    }

    fun all(spec: IntentSpec): Map<Format, String> =
        Format.entries.associateWith { export(spec, it) }

    private fun baseCommand(spec: IntentSpec): String =
        if (spec.dispatchType.isContent)
            ContentCommandBuilder.build(ContentCommandBuilder.fromSpec(spec))
        else
            AmCommandBuilder.build(spec, IntentFlag.maskOf(spec.flags)).command

    fun adb(spec: IntentSpec): String = "adb shell " + baseCommand(spec)

    fun su(spec: IntentSpec): String {
        val cmd = baseCommand(spec)
        // wrap in su -c '...', escaping embedded quotes
        return "su -c '" + cmd.replace("'", "'\\''") + "'"
    }

    fun kotlin(spec: IntentSpec): String = buildString {
        if (spec.dispatchType.isContent) {
            appendLine(kotlinContent(spec))
            return@buildString
        }
        appendLine("val intent = Intent().apply {")
        spec.action?.takeIf { it.isNotBlank() }?.let { appendLine("    action = \"$it\"") }
        when {
            spec.dataUri != null && spec.mimeType != null ->
                appendLine("    setDataAndType(Uri.parse(\"${spec.dataUri}\"), \"${spec.mimeType}\")")
            spec.dataUri != null -> appendLine("    data = Uri.parse(\"${spec.dataUri}\")")
            spec.mimeType != null -> appendLine("    type = \"${spec.mimeType}\"")
        }
        if (spec.targeting == TargetingMode.EXPLICIT_COMPONENT && spec.hasExplicitComponent) {
            appendLine("    component = ComponentName(\"${spec.componentPackage}\", \"${spec.componentClass}\")")
        } else if (spec.targeting == TargetingMode.PACKAGE_ONLY && spec.packageName != null) {
            appendLine("    setPackage(\"${spec.packageName}\")")
        }
        spec.categories.filter { it.isNotBlank() }.forEach { appendLine("    addCategory(\"$it\")") }
        IntentFlag.maskOf(spec.flags).takeIf { it != 0 }?.let {
            val names = IntentFlag.decode(it).joinToString(" or ") { f -> "Intent.FLAG_$f" }
            appendLine("    addFlags($names)")
        }
        spec.extras.forEach { appendLine("    " + kotlinExtra(it)) }
        appendLine("}")
        appendLine()
        appendLine(kotlinDispatchCall(spec.dispatchType))
    }

    private fun kotlinDispatchCall(type: DispatchType): String = when (type) {
        DispatchType.SEND_BROADCAST -> "context.sendBroadcast(intent)"
        DispatchType.SEND_ORDERED_BROADCAST -> "context.sendOrderedBroadcast(intent, null)"
        DispatchType.START_SERVICE -> "context.startService(intent)"
        DispatchType.START_FOREGROUND_SERVICE -> "context.startForegroundService(intent)"
        DispatchType.START_ACTIVITY_FOR_RESULT -> "startActivityForResult(intent, 1001)"
        else -> "context.startActivity(intent)"
    }

    private fun kotlinExtra(e: ExtraSpec): String {
        val k = "\"${e.key}\""
        return when (e.type) {
            ExtraType.STRING -> "putExtra($k, \"${e.value}\")"
            ExtraType.NULL -> "putExtra($k, null as String?)"
            ExtraType.BOOLEAN -> "putExtra($k, ${e.value.toBooleanStrictOrNull() ?: false})"
            ExtraType.INT -> "putExtra($k, ${e.value.toIntOrNull() ?: 0})"
            ExtraType.LONG -> "putExtra($k, ${e.value.toLongOrNull() ?: 0L}L)"
            ExtraType.FLOAT -> "putExtra($k, ${e.value.toFloatOrNull() ?: 0f}f)"
            ExtraType.DOUBLE -> "putExtra($k, ${e.value.toDoubleOrNull() ?: 0.0})"
            ExtraType.URI -> "putExtra($k, Uri.parse(\"${e.value}\"))"
            ExtraType.STRING_ARRAY -> "putExtra($k, arrayOf(${e.arrayValues().joinToString { "\"$it\"" }}))"
            ExtraType.INT_ARRAY -> "putExtra($k, intArrayOf(${e.arrayValues().joinToString()}))"
            ExtraType.LONG_ARRAY -> "putExtra($k, longArrayOf(${e.arrayValues().joinToString { "${it}L" }}))"
            ExtraType.FLOAT_ARRAY -> "putExtra($k, floatArrayOf(${e.arrayValues().joinToString { "${it}f" }}))"
            ExtraType.BOOLEAN_ARRAY -> "putExtra($k, booleanArrayOf(${e.arrayValues().joinToString()}))"
        }
    }

    private fun kotlinContent(spec: IntentSpec): String {
        val uri = spec.dataUri ?: "content://authority/path"
        return when (spec.dispatchType) {
            DispatchType.CONTENT_QUERY ->
                "context.contentResolver.query(Uri.parse(\"$uri\"), null, null, null, null)?.use { /* read */ }"
            DispatchType.CONTENT_DELETE ->
                "context.contentResolver.delete(Uri.parse(\"$uri\"), null, null)"
            DispatchType.CONTENT_OPEN_FILE ->
                "context.contentResolver.openInputStream(Uri.parse(\"$uri\"))?.use { it.readBytes() }"
            DispatchType.CONTENT_INSERT, DispatchType.CONTENT_UPDATE -> buildString {
                appendLine("val values = ContentValues().apply {")
                spec.extras.forEach { appendLine("    put(\"${it.key}\", \"${it.value}\")") }
                appendLine("}")
                if (spec.dispatchType == DispatchType.CONTENT_INSERT)
                    append("context.contentResolver.insert(Uri.parse(\"$uri\"), values)")
                else
                    append("context.contentResolver.update(Uri.parse(\"$uri\"), values, null, null)")
            }
            else -> "// unsupported"
        }
    }
}
