@file:OptIn(ExperimentalSerializationApi::class)

package tel.schich.idl.runner

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.BigIntegerNode
import com.fasterxml.jackson.databind.node.BooleanNode
import com.fasterxml.jackson.databind.node.DecimalNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.FloatNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.LongNode
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ShortNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import tel.schich.idl.core.Annotations
import tel.schich.idl.core.Module
import java.io.ByteArrayInputStream
import java.io.IOException
import java.lang.RuntimeException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension

interface Loader {
    fun <T> load(data: ByteArray, deserializationStrategy: DeserializationStrategy<T>): T
}

class LoaderException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

internal val json = Json

internal inline fun <reified T> Loader.load(data: ByteArray): T {
    return load(data, json.serializersModule.serializer())
}

private inline fun <T> handleJsonException(deserializationStrategy: DeserializationStrategy<T>, block: (DeserializationStrategy<T>) -> T): T {
    return try {
        block(deserializationStrategy)
    } catch (e: SerializationException) {
        throw LoaderException("Failed to parse JSON!", e)
    } catch (e: IllegalArgumentException) {
        throw LoaderException("Failed to map JSON to ${deserializationStrategy.descriptor.serialName}!", e)
    }
}

object JsonLoader : Loader {
    override fun <T> load(data: ByteArray, deserializationStrategy: DeserializationStrategy<T>): T {
        return handleJsonException(deserializationStrategy) {
            json.decodeFromStream(it, ByteArrayInputStream(data))
        }
    }
}


private fun convertJacksonTreeToJsonElement(jackson: JsonNode): JsonElement = when (jackson) {
    is ObjectNode -> {
        val obj = buildMap(jackson.size()) {
            for ((key, value) in jackson.fields()) {
                put(key, convertJacksonTreeToJsonElement(value))
            }
        }
        JsonObject(obj)
    }
    is ArrayNode -> {
        val array = buildList(jackson.size()) {
            for (element in jackson.elements()) {
                add(convertJacksonTreeToJsonElement(element))
            }
        }
        JsonArray(array)
    }
    is NullNode ->
        JsonNull
    is BooleanNode ->
        JsonPrimitive(jackson.booleanValue())
    is TextNode ->
        JsonPrimitive(jackson.textValue())
    is DoubleNode ->
        JsonPrimitive(jackson.doubleValue())
    is FloatNode ->
        JsonPrimitive(jackson.floatValue())
    is ShortNode ->
        JsonPrimitive(jackson.shortValue())
    is IntNode ->
        JsonPrimitive(jackson.intValue())
    is LongNode ->
        JsonPrimitive(jackson.longValue())
    is BigIntegerNode ->
        JsonUnquotedLiteral(jackson.bigIntegerValue().toString())
    is DecimalNode ->
        JsonUnquotedLiteral(jackson.decimalValue().toString())
    else -> throw LoaderException("Unsupported jackson node type: ${jackson::class.qualifiedName}")
}

object YamlLoader : Loader {
    private val yaml = YAMLMapper()

    override fun <T> load(data: ByteArray, deserializationStrategy: DeserializationStrategy<T>): T {
        val yamlTree = try {
            yaml.readTree(data)
        } catch (e: IOException) {
            throw LoaderException("Failed to read yaml tree!", e)
        }
        val tree = convertJacksonTreeToJsonElement(yamlTree)
        return handleJsonException(deserializationStrategy) {
            json.decodeFromJsonElement(it, tree)
        }
    }
}

val Loaders = mapOf(
    "json" to JsonLoader,
    "yml" to YamlLoader,
    "yaml" to YamlLoader,
)

private inline fun <reified T> load(path: Path): T {
    val loader = path.extension.ifEmpty { null }?.let { Loaders[it] } ?: JsonLoader
    val data = try {
        Files.readAllBytes(path)
    } catch (e: IOException) {
        throw LoaderException("Failed to read data from $path!", e)
    }
    try {
        return loader.load(data)
    } catch (e: LoaderException) {
        throw e
    } catch (e: Exception) {
        throw LoaderException("Failed to load module from $path!", e)
    }
}

fun loadModule(path: Path): Module = load(path)
fun loadAnnotations(path: Path): Annotations = load(path)