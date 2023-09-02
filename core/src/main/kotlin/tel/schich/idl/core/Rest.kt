package tel.schich.idl.core

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class HttpMethod(val name: String) {
    companion object {
        val Get = HttpMethod("GET")
        val Post = HttpMethod("POST")
        val Put = HttpMethod("PUT")
        val Head = HttpMethod("HEAD")
        val Options = HttpMethod("OPTIONS")
        val Delete = HttpMethod("DELETE")
        val Patch = HttpMethod("PATCH")
        val Trace = HttpMethod("TRACE")
        val Query = HttpMethod("QUERY")
        val Connect = HttpMethod("CONNECT")
    }
}

@Serializable
data class Content(
    val mimeType: String,
    val model: ModelReference,
)

@Serializable
data class Operation(
    val metadata: BasicMetadata,
    val method: HttpMethod,
    val requestBody: Content?,
    val responseBody: Content?,
)
