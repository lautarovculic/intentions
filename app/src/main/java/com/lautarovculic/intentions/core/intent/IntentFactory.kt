package com.lautarovculic.intentions.core.intent

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.lautarovculic.intentions.core.model.ExtraType
import com.lautarovculic.intentions.core.model.ExtraSpec
import com.lautarovculic.intentions.core.model.IntentSpec
import com.lautarovculic.intentions.core.model.TargetingMode

// Builds a real Android Intent from an IntentSpec (in-app backend).
object IntentFactory {

    fun create(spec: IntentSpec): Intent {
        val intent = Intent()
        spec.action?.takeIf { it.isNotBlank() }?.let { intent.action = it }

        val uri = spec.dataUri?.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        val type = spec.mimeType?.takeIf { it.isNotBlank() }
        when {
            uri != null && type != null -> intent.setDataAndType(uri, type)
            uri != null -> intent.data = uri
            type != null -> intent.type = type
        }

        spec.categories.filter { it.isNotBlank() }.forEach { intent.addCategory(it) }

        when (spec.targeting) {
            TargetingMode.EXPLICIT_COMPONENT ->
                if (spec.hasExplicitComponent) {
                    intent.component = ComponentName(spec.componentPackage!!, spec.componentClass!!)
                }
            TargetingMode.PACKAGE_ONLY -> spec.packageName?.let { intent.setPackage(it) }
            TargetingMode.IMPLICIT -> { /* nothing */ }
        }

        val mask = IntentFlag.maskOf(spec.flags)
        if (mask != 0) intent.addFlags(mask)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            spec.identifier?.takeIf { it.isNotBlank() }?.let { intent.identifier = it }
        }

        spec.extras.forEach { applyExtra(intent, it) }
        return intent
    }

    private fun applyExtra(intent: Intent, extra: ExtraSpec) {
        val k = extra.key
        when (extra.type) {
            ExtraType.STRING -> intent.putExtra(k, extra.value)
            ExtraType.NULL -> intent.putExtra(k, null as String?)
            ExtraType.BOOLEAN -> intent.putExtra(k, extra.value.toBooleanStrictOrNull() ?: false)
            ExtraType.INT -> intent.putExtra(k, extra.value.toIntOrNull() ?: 0)
            ExtraType.LONG -> intent.putExtra(k, extra.value.toLongOrNull() ?: 0L)
            ExtraType.FLOAT -> intent.putExtra(k, extra.value.toFloatOrNull() ?: 0f)
            ExtraType.DOUBLE -> intent.putExtra(k, extra.value.toDoubleOrNull() ?: 0.0)
            ExtraType.URI -> intent.putExtra(k, Uri.parse(extra.value))
            ExtraType.STRING_ARRAY -> intent.putExtra(k, extra.arrayValues().toTypedArray())
            ExtraType.INT_ARRAY -> intent.putExtra(k, extra.arrayValues().map { it.toIntOrNull() ?: 0 }.toIntArray())
            ExtraType.LONG_ARRAY -> intent.putExtra(k, extra.arrayValues().map { it.toLongOrNull() ?: 0L }.toLongArray())
            ExtraType.FLOAT_ARRAY -> intent.putExtra(k, extra.arrayValues().map { it.toFloatOrNull() ?: 0f }.toFloatArray())
            ExtraType.BOOLEAN_ARRAY ->
                intent.putExtra(k, extra.arrayValues().map { it.toBooleanStrictOrNull() ?: false }.toBooleanArray())
        }
    }
}
