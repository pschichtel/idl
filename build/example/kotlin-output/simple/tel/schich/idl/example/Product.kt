package tel.schich.idl.example

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * product
 */
@Serializable
data class Product(
    @Contextual
    val first: UntaggedSum,
    val second: Test,
)
