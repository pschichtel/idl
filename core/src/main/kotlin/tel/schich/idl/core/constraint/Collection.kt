package tel.schich.idl.core.constraint

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

sealed interface CollectionConstraint

@Serializable
sealed interface HomogenousListConstraint : CollectionConstraint
@Serializable
sealed interface HomogenousSetConstraint : CollectionConstraint
@Serializable
sealed interface HomogenousMapConstraint : CollectionConstraint

@Serializable
@SerialName("size-range")
data class CollectionSizeRange(
    val minimum: Int = 0,
    val maximum: Int? = null,
) : HomogenousListConstraint, HomogenousSetConstraint, HomogenousMapConstraint

@Serializable
@SerialName("values-unique")
data object CollectionValuesUnique : HomogenousListConstraint, HomogenousMapConstraint

@Serializable
enum class PrimitiveConstraintSubject {
    VALUE,
    LENGTH,
}