package tel.schich.idl.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

typealias Annotations = Map<String, JsonElement>
typealias AnnotationParser<T> = (JsonElement) -> T

const val ANNOTATION_NAMESPACE_SEPARATOR = '/'

fun valueAsString(value: JsonElement): String = (value as? JsonPrimitive)?.content ?: value.toString()
fun valueAsBoolean(value: JsonElement): Boolean = when (value) {
    is JsonArray -> value.isNotEmpty()
    is JsonObject -> value.values.isNotEmpty()
    is JsonPrimitive -> value.content.toBoolean()
    is JsonNull -> false
}
fun <T> valueFromString(stringParser: (String) -> T): AnnotationParser<T> {
    return { stringParser(valueAsString(it)) }
}
inline fun <reified T> valueFromJson(json: Json = Json): AnnotationParser<T> {
    return { json.decodeFromJsonElement<T>(it) }
}

open class Annotation<T : Any>(val namespace: String, val name: String, val parser: AnnotationParser<T>) {
    val fullName = "$namespace$ANNOTATION_NAMESPACE_SEPARATOR$name"

    fun getValue(annotations: Annotations): T? = annotations[fullName]?.let(parser)
    fun getValue(metadata: Metadata): T? = getValue(metadata.annotations)
}

fun <T : Any> Metadata.getAnnotation(annotation: Annotation<T>): T? = annotation.getValue(annotations)
