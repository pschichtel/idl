package tel.schich.idl.core.constraint

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement


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