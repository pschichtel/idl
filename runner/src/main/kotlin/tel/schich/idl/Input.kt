package tel.schich.idl

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension

interface Loader {
    fun load(data: ByteArray): Module
}

object JsonLoader : Loader {
    private val json = Json

    @OptIn(ExperimentalSerializationApi::class)
    override fun load(data: ByteArray): Module {
        return json.decodeFromStream(ByteArrayInputStream(data))
    }
}

val Loaders = mapOf(
    "json" to JsonLoader,
)

fun loadModule(path: Path): Module {
    val loader = path.extension.ifEmpty { null }?.let { Loaders[it] } ?: JsonLoader
    val data = Files.readAllBytes(path)
    return loader.load(data)
}