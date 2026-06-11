package com.lautarovculic.intentions.domain.fuzz

import com.lautarovculic.intentions.core.model.ExtraSpec
import com.lautarovculic.intentions.core.model.ExtraType
import com.lautarovculic.intentions.core.model.IntentSpec

// Generates mutated IntentSpecs for the fuzzer (deep-link + extra modes).
object FuzzEngine {

    // common deep-link param keys
    val PARAM_KEYS = listOf(
        "url", "uri", "redirect", "redirect_uri", "return_url", "next", "continue",
        "callback", "callback_url", "web_url", "webview_url", "link", "target", "path",
        "route", "screen", "ref", "reference", "code", "token", "jwt", "access_token",
        "id_token", "deeplink", "deep_link",
    )

    // value payloads
    val VALUE_PAYLOADS = listOf(
        "https://attacker.example",
        "https://attacker.example/callback",
        "//attacker.example",
        "https://victim.example.attacker.example",
        "javascript:alert(document.domain)",
        "file:///data/data/PACKAGE/databases/",
        "content://com.android.externalstorage/",
        "intent://scan/#Intent;scheme=zxing;end",
        "../../../../etc/hosts",
        "..%2f..%2f..%2fetc%2fpasswd",
        "%252e%252e%252f",
        "⁄attacker.example",
        "",
        " ",
        "A".repeat(4096),
    )

    // privilege/state extras
    val EXTRA_PAYLOADS = listOf(
        ExtraSpec("debug", ExtraType.BOOLEAN, "true"),
        ExtraSpec("is_admin", ExtraType.BOOLEAN, "true"),
        ExtraSpec("admin", ExtraType.BOOLEAN, "true"),
        ExtraSpec("authenticated", ExtraType.BOOLEAN, "true"),
        ExtraSpec("isVerified", ExtraType.BOOLEAN, "true"),
        ExtraSpec("user_id", ExtraType.INT, "0"),
        ExtraSpec("user_id", ExtraType.INT, "-1"),
        ExtraSpec("url", ExtraType.STRING, "https://attacker.example"),
        ExtraSpec("redirect_uri", ExtraType.STRING, "https://attacker.example/callback"),
        ExtraSpec("token", ExtraType.STRING, "test"),
    )

    data class FuzzCase(val label: String, val spec: IntentSpec)

    // Deep-link fuzzing: set each param key to each payload in the URI query.
    fun deepLinkCases(
        base: IntentSpec,
        paramKeys: List<String> = PARAM_KEYS,
        payloads: List<String> = VALUE_PAYLOADS,
    ): List<FuzzCase> {
        val baseUri = base.dataUri ?: return emptyList()
        val cases = mutableListOf<FuzzCase>()
        for (key in paramKeys) {
            for (payload in payloads) {
                val mutated = setQueryParam(baseUri, key, payload)
                cases += FuzzCase(
                    label = "$key=${payload.take(24)}${if (payload.length > 24) "…" else ""}",
                    spec = base.copy(dataUri = mutated),
                )
            }
        }
        return cases
    }

    // extra fuzzing: add each payload extra to the base spec
    fun extraCases(base: IntentSpec, payloads: List<ExtraSpec> = EXTRA_PAYLOADS): List<FuzzCase> =
        payloads.map { payload ->
            val merged = base.extras.filterNot { it.key == payload.key } + payload
            FuzzCase("${payload.key}=${payload.value} (${payload.type.label})", base.copy(extras = merged))
        }

    private fun setQueryParam(uri: String, key: String, value: String): String {
        val encodedValue = value.replace(" ", "%20")
        val hashIndex = uri.indexOf('#')
        val fragment = if (hashIndex >= 0) uri.substring(hashIndex) else ""
        val withoutFragment = if (hashIndex >= 0) uri.substring(0, hashIndex) else uri
        val qIndex = withoutFragment.indexOf('?')
        return if (qIndex < 0) {
            "$withoutFragment?$key=$encodedValue$fragment"
        } else {
            val basePart = withoutFragment.substring(0, qIndex)
            val query = withoutFragment.substring(qIndex + 1)
            val params = query.split("&").filter { it.isNotEmpty() && !it.startsWith("$key=") }
            val newQuery = (params + "$key=$encodedValue").joinToString("&")
            "$basePart?$newQuery$fragment"
        }
    }
}
