package tel.schich.idl.constraint

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement
import tel.schich.idl.ModelReference
import tel.schich.idl.Module


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

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed interface PrimitiveConstraint {
    @Serializable
    @SerialName("gt")
    data class GreaterThan(
        val subject: PrimitiveConstraintSubject = PrimitiveConstraintSubject.VALUE,
        val value: JsonElement,
    ) : PrimitiveConstraint

    @Serializable
    @SerialName("gte")
    data class GreaterThanOrEqualTo(
        val subject: PrimitiveConstraintSubject = PrimitiveConstraintSubject.VALUE,
        val value: JsonElement,
    ) : PrimitiveConstraint

    @Serializable
    @SerialName("lt")
    data class LessThan(
        val subject: PrimitiveConstraintSubject = PrimitiveConstraintSubject.VALUE,
        val value: JsonElement,
    ) : PrimitiveConstraint

    @Serializable
    @SerialName("lte")
    data class LessThanOrEqualTo(
        val subject: PrimitiveConstraintSubject = PrimitiveConstraintSubject.VALUE,
        val value: JsonElement,
    ) : PrimitiveConstraint

    @Serializable
    @SerialName("eq")
    data class EqualTo(
        val subject: PrimitiveConstraintSubject = PrimitiveConstraintSubject.VALUE,
        val value: JsonElement,
    ) : PrimitiveConstraint

    @Serializable
    @SerialName("ne")
    data class NotEqualTo(
        val subject: PrimitiveConstraintSubject = PrimitiveConstraintSubject.VALUE,
        val value: JsonElement,
    ) : PrimitiveConstraint

    @Serializable
    @SerialName("regex")
    data class MatchesRegex(
        val regex: String,
    ) : PrimitiveConstraint

    @Serializable
    @SerialName("starts-with")
    data class StartsWith(
        val prefix: String,
    ) : PrimitiveConstraint

    @Serializable
    @SerialName("ends-with")
    data class EndsWith(
        val suffix: String,
    ) : PrimitiveConstraint
}