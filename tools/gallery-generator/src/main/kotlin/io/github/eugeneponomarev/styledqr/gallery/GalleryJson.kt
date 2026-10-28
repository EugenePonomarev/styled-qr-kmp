package io.github.eugeneponomarev.styledqr.gallery

import java.math.BigDecimal

internal fun galleryJson(value: Any?): String = when (value) {
    null -> "null"
    is String -> jsonString(value)
    is Boolean -> value.toString()
    is Int -> value.toString()
    is Double -> {
        require(value.isFinite()) {
            "JSON numbers must be finite"
        }
        BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
    }

    is List<*> -> value.joinToString(
        separator = ",",
        prefix = "[",
        postfix = "]",
    ) {
        galleryJson(it)
    }

    is Map<*, *> -> value.entries.joinToString(
        separator = ",",
        prefix = "{",
        postfix = "}",
    ) { (key, element) ->
        require(key is String) {
            "JSON object keys must be strings"
        }
        "${jsonString(key)}:${galleryJson(element)}"
    }

    else -> error("Unsupported gallery JSON type: ${value.javaClass.name}")
}

private fun jsonString(value: String): String = buildString {
    append('"')

    value.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> {
                if (
                    character.code < 0x20 ||
                    character in '\uD800'..'\uDFFF'
                ) {
                    append("\\u")
                    append(character.code.toString(16).padStart(4, '0'))
                } else {
                    append(character)
                }
            }
        }
    }

    append('"')
}