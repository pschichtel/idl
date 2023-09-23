package tel.schich.idl.core

import kotlinx.serialization.Serializable

sealed interface Metadata {
    val name: String
    val description: String?
    val deprecated: Boolean
    val annotations: Annotations
}

@Serializable
data class BasicMetadata(
    override val name: String,
    override val description: String? = null,
    override val deprecated: Boolean = false,
    override val annotations: Annotations = emptyMap(),
) : Metadata