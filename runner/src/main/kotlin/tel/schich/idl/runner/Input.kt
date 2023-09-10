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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import tel.schich.idl.core.Module
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension

interface Loader {
    fun load(data: ByteArray): Module
}

private val json = Json

object JsonLoader : Loader {
    @OptIn(ExperimentalSerializationApi::class)
    override fun load(data: ByteArray): Module {
        return json.decodeFromStream(ByteArrayInputStream(data))
    }
}


@OptIn(ExperimentalSerializationApi::class)
private fun convertJacksonTreeToJsonElement(jackson: JsonNode): JsonElement = when (jackson) {
    is ObjectNode ->
        JsonObject(jackson.fields().asSequence().map { (key, value) -> Pair(key, convertJacksonTreeToJsonElement(value)) }.toMap())
    is ArrayNode ->
        JsonArray(jackson.elements().asSequence().map { convertJacksonTreeToJsonElement(it) }.toList())
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
    else -> error("Unsupported jackson node type: ${jackson::class.qualifiedName}")
}

object YamlLoader : Loader {
    private val yaml = YAMLMapper()

    override fun load(data: ByteArray): Module {
        val tree = convertJacksonTreeToJsonElement(yaml.readTree(data))
        return json.decodeFromJsonElement(tree)
    }
}

val Loaders = mapOf(
    "json" to JsonLoader,
    "yml" to YamlLoader,
    "yaml" to YamlLoader,
)

fun loadModule(path: Path): Module {
    val loader = path.extension.ifEmpty { null }?.let { Loaders[it] } ?: JsonLoader
    val data = Files.readAllBytes(path)
    try {
        return loader.load(data)
    } catch (e: Exception) {
        throw Exception("Failed to load module from $path!", e)
    }
}