package tel.schich.idl.core

import kotlinx.serialization.Serializable

sealed interface Metadata {
    val name: String
    val summary: String?
    val description: String?
    val annotations: Map<String, String>
}

@Serializable
data class BasicMetadata(
    override val name: String,
    override val summary: String? = null,
    override val description: String? = null,
    override val annotations: Map<String, String> = emptyMap(),
) : Metadata