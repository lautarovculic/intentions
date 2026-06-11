package com.lautarovculic.intentions.core.shell

import com.lautarovculic.intentions.core.model.DispatchType
import com.lautarovculic.intentions.core.model.ExtraType
import com.lautarovculic.intentions.core.model.IntentSpec

// Builds content command lines for the Provider Lab.
object ContentCommandBuilder {

    // a single provider operation
    data class ProviderOp(
        val uri: String,
        val operation: DispatchType,
        val binds: List<Bind> = emptyList(),
        val projection: List<String> = emptyList(),
        val where: String? = null,
        val sort: String? = null,
    )

    data class Bind(val key: String, val type: String, val value: String)

    private fun verb(type: DispatchType): String = when (type) {
        DispatchType.CONTENT_QUERY -> "query"
        DispatchType.CONTENT_INSERT -> "insert"
        DispatchType.CONTENT_UPDATE -> "update"
        DispatchType.CONTENT_DELETE -> "delete"
        DispatchType.CONTENT_OPEN_FILE -> "read"
        else -> "query"
    }

    fun build(op: ProviderOp): String {
        val sb = StringBuilder("content ").append(verb(op.operation))
        sb.append(" --uri ").append(ShellArgs.quote(op.uri))
        if (op.operation == DispatchType.CONTENT_QUERY) {
            if (op.projection.isNotEmpty()) {
                sb.append(" --projection ").append(ShellArgs.quote(op.projection.joinToString(":")))
            }
            op.where?.takeIf { it.isNotBlank() }?.let { sb.append(" --where ").append(ShellArgs.quote(it)) }
            op.sort?.takeIf { it.isNotBlank() }?.let { sb.append(" --sort ").append(ShellArgs.quote(it)) }
        }
        op.binds.forEach { bind ->
            sb.append(" --bind ").append(ShellArgs.quote("${bind.key}:${bind.type}:${bind.value}"))
        }
        return sb.toString()
    }

    // derive a provider op from a content-* IntentSpec
    fun fromSpec(spec: IntentSpec): ProviderOp = ProviderOp(
        uri = spec.dataUri.orEmpty(),
        operation = spec.dispatchType,
        binds = spec.extras.mapNotNull { it.toBind() },
    )

    private fun com.lautarovculic.intentions.core.model.ExtraSpec.toBind(): Bind? {
        val t = when (type) {
            ExtraType.STRING, ExtraType.NULL, ExtraType.URI -> "s"
            ExtraType.INT, ExtraType.LONG -> "i"
            ExtraType.FLOAT, ExtraType.DOUBLE -> "f"
            ExtraType.BOOLEAN -> "b"
            else -> return null
        }
        return Bind(key, t, value)
    }
}
