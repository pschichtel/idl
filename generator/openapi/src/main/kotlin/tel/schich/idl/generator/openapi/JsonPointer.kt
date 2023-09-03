package tel.schich.idl.generator.openapi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.generator.openapi.DereferenceErrorReason.AFTER_END_OF_ARRAY
import tel.schich.idl.generator.openapi.DereferenceErrorReason.ARRAY_INDEX_OUT_OF_BOUNDS
import tel.schich.idl.generator.openapi.DereferenceErrorReason.NULL_DEREFERENCE
import tel.schich.idl.generator.openapi.DereferenceErrorReason.OBJECT_PROPERTY_UNDEFINED
import tel.schich.idl.generator.openapi.DereferenceErrorReason.PRIMITIVE_DEREFERENCE
import tel.schich.idl.generator.openapi.DereferenceErrorReason.PROPERTY_DEREFERENCE_ON_ARRAY

enum class DereferenceErrorReason {
    NULL_DEREFERENCE,
    PRIMITIVE_DEREFERENCE,
    PROPERTY_DEREFERENCE_ON_ARRAY,
    OBJECT_PROPERTY_UNDEFINED,
    ARRAY_INDEX_OUT_OF_BOUNDS,
    AFTER_END_OF_ARRAY,
}

class JsonPointerDeferenceException(val pointer: JsonPointer, val errorLocation: List<String>, val reason: DereferenceErrorReason) :
    RuntimeException("Failed to dereference pointer $pointer at $errorLocation: $reason")

// https://datatracker.ietf.org/doc/html/rfc6901
data class JsonPointer(val raw: String, val segments: List<String>) {
    override fun toString() = raw

    fun dereference(element: JsonElement): JsonElement {
        fun error(index: Int, reason: DereferenceErrorReason): Nothing {
            throw JsonPointerDeferenceException(this, segments.subList(0, index + 1), reason)
        }

        return segments.withIndex().fold(element) { e, (index, segment)  ->
            when (e) {
                is JsonObject ->
                    e[segment] ?: error(index, OBJECT_PROPERTY_UNDEFINED)
                is JsonArray -> when {
                    segment.matches(arrayIndexRegex) ->
                        e.getOrNull(segment.toInt()) ?: error(index, ARRAY_INDEX_OUT_OF_BOUNDS)
                    segment == "-" ->
                        error(index, AFTER_END_OF_ARRAY)
                    else ->
                        error(index, PROPERTY_DEREFERENCE_ON_ARRAY)
                }
                is JsonNull -> error(index, NULL_DEREFERENCE)
                is JsonPrimitive -> error(index, PRIMITIVE_DEREFERENCE)
            }
        }
    }

    operator fun plus(segment: String): JsonPointer =
        JsonPointer("$raw$SEPARATOR${escape(segment)}", segments + segment)

    operator fun plus(segment: UInt): JsonPointer {
        val string = segment.toString()
        return JsonPointer("$raw$SEPARATOR$string", segments + string)
    }

    fun parent(): JsonPointer {
        if (segments.size <= 1) {
            return Empty
        }

        return fromSegments(segments.subList(0, segments.size - 1))
    }

    companion object {
        val Empty = JsonPointer("", emptyList())

        private const val ESCAPE = "~"
        private const val SEPARATOR = "/"

        private const val ESCAPED_ESCAPE = "~0"
        private const val ESCAPED_SEPARATOR = "~1"
        private val escapeSequenceRegex = """$ESCAPED_ESCAPE|$ESCAPED_SEPARATOR""".toRegex()
        private val specialCharRegex = """[$ESCAPE$SEPARATOR]""".toRegex()
        private val arrayIndexRegex = """0|[1-9]\d*""".toRegex()

        private fun escape(sequence: CharSequence): String {
            return sequence.replace(specialCharRegex) {
                when (val value = it.value) {
                    ESCAPE -> ESCAPED_ESCAPE
                    SEPARATOR -> ESCAPED_SEPARATOR
                    else -> value
                }
            }
        }

        private fun unescape(sequence: CharSequence): String {
            return sequence.replace(escapeSequenceRegex) {
                when (val value = it.value) {
                    ESCAPED_ESCAPE -> ESCAPE
                    ESCAPED_SEPARATOR -> SEPARATOR
                    else -> value
                }
            }
        }

        fun fromSegments(segments: List<String>): JsonPointer {
            val raw = segments.joinToString(separator = SEPARATOR, prefix = SEPARATOR) { escape(it) }
            return JsonPointer(raw, segments)
        }

        fun fromString(value: String): JsonPointer {
            if (value.isEmpty()) {
                return Empty
            }

            var index = 0
            if (value[index] != '/') {
                error("JSON pointers must start with a /")
            }

            val length = value.length
            val segments = buildList {
                while (index < length) {
                    val newIndex = value.indexOf('/', startIndex = index + 1)
                    index = if (newIndex == -1) {
                        add(unescape(value.subSequence(index + 1, length)))
                        length
                    } else {
                        add(unescape(value.substring(index + 1, newIndex)))
                        newIndex
                    }
                }
            }

            return JsonPointer(value, segments)
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = JsonPointer::class)
object JsonPointerSerializer : KSerializer<JsonPointer> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("JsonPointer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: JsonPointer) {
        encoder.encodeString(value.raw)
    }

    override fun deserialize(decoder: Decoder): JsonPointer {
        return JsonPointer.fromString(decoder.decodeString())
    }
}