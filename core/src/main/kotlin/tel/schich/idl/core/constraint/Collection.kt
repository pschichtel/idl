package tel.schich.idl.core.constraint

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@SerialName("size-range")
data class CollectionSizeRange(
    val minimum: ULong = 0u,
    val maximum: ULong? = null,
)
