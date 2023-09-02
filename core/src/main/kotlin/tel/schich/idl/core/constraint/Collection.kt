package tel.schich.idl.core.constraint

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
sealed interface HomogenousListConstraint
@Serializable
sealed interface HomogenousSetConstraint
@Serializable
sealed interface HomogenousMapConstraint

@Serializable
@SerialName("size-gt")
data class CollectionSizeGreaterThan(
    val value: Int,
) : HomogenousListConstraint, HomogenousSetConstraint, HomogenousMapConstraint

@Serializable
@SerialName("size-gte")
data class CollectionSizeGreaterThanOrEqualTo(
    val value: Int,
) : HomogenousListConstraint, HomogenousSetConstraint, HomogenousMapConstraint

@Serializable
@SerialName("size-lt")
data class CollectionSizeLessThan(
    val value: Int,
) : HomogenousListConstraint, HomogenousSetConstraint, HomogenousMapConstraint

@Serializable
@SerialName("size-lte")
data class CollectionSizeLessThanOrEqualTo(
    val value: Int,
) : HomogenousListConstraint, HomogenousSetConstraint, HomogenousMapConstraint

@Serializable
@SerialName("size-eq")
data class CollectionSizeEqualTo(
    val value: Int,
) : HomogenousListConstraint, HomogenousSetConstraint, HomogenousMapConstraint

@Serializable
@SerialName("size-ne")
data class CollectionSizeNotEqualTo(
    val value: Int,
) : HomogenousListConstraint, HomogenousSetConstraint, HomogenousMapConstraint

@Serializable
@SerialName("values-unique")
data object CollectionValuesUnique : HomogenousListConstraint, HomogenousMapConstraint

@Serializable
enum class PrimitiveConstraintSubject {
    VALUE,
    LENGTH,
}