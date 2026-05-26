package com.michael.clipvault.core.domain

import org.json.JSONObject
import java.net.URLEncoder

sealed class Transformer {
    abstract val label: String
    abstract val key: String
    abstract fun apply(input: String): String

    object UpperCase : Transformer() {
        override val label = "UPPERCASE"
        override val key = "UPPERCASE"
        override fun apply(input: String) = input.uppercase()
    }

    object LowerCase : Transformer() {
        override val label = "lowercase"
        override val key = "LOWERCASE"
        override fun apply(input: String) = input.lowercase()
    }

    object CamelCase : Transformer() {
        override val label = "camelCase"
        override val key = "CAMELCASE"
        override fun apply(input: String): String {
            val words = input.split(Regex("[\\s_\\-]+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) return input
            return words.mapIndexed { idx, s ->
                if (idx == 0) s.lowercase() else s.lowercase().replaceFirstChar { it.uppercase() }
            }.joinToString("")
        }
    }

    object UrlEncode : Transformer() {
        override val label = "URL Encode"
        override val key = "URL_ENCODE"
        override fun apply(input: String): String = runCatching {
            URLEncoder.encode(input, "UTF-8")
        }.getOrDefault(input)
    }

    object MinifyJson : Transformer() {
        override val label = "Minify JSON"
        override val key = "MINIFY_JSON"
        override fun apply(input: String): String = runCatching {
            JSONObject(input).toString()
        }.getOrDefault(input)
    }

    object FormatJson : Transformer() {
        override val label = "Format JSON"
        override val key = "FORMAT_JSON"
        override fun apply(input: String): String = runCatching {
            JSONObject(input).toString(4)
        }.getOrDefault(input)
    }

    object TrimWhitespace : Transformer() {
        override val label = "Trim Spacing"
        override val key = "TRIM_WHITESPACE"
        override fun apply(input: String): String = input.trim().replace(Regex("\\s+"), " ")
    }

    data class RegexReplace(val pattern: String, val replacement: String) : Transformer() {
        override val label = "Regex Replace"
        override val key = "REGEX_REPLACE"
        override fun apply(input: String): String = runCatching {
            input.replace(Regex(pattern), replacement)
        }.getOrDefault(input)
    }

    companion object {
        val ALL: List<Transformer>
            get() = listOf(UpperCase, LowerCase, CamelCase, UrlEncode, MinifyJson, FormatJson, TrimWhitespace)

        fun fromKey(key: String): Transformer {
            return when (key) {
                "UPPERCASE" -> UpperCase
                "LOWERCASE" -> LowerCase
                "CAMELCASE" -> CamelCase
                "URL_ENCODE" -> UrlEncode
                "MINIFY_JSON" -> MinifyJson
                "FORMAT_JSON" -> FormatJson
                "TRIM_WHITESPACE" -> TrimWhitespace
                else -> TrimWhitespace
            }
        }
    }
}
