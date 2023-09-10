package tel.schich.idl.core.constraint

import kotlinx.serialization.Serializable
import tel.schich.idl.core.BigDecimalSerializer
import tel.schich.idl.core.BigIntegerSerializer
import java.math.BigDecimal
import java.math.BigInteger


@Serializable
data class StringLengthRange(
    val minimum: ULong? = null,
    val maximum: ULong? = null,
)

@Serializable
data class IntegerValueRange(
    @Serializable(with = BigIntegerSerializer::class)
    val minimum: BigInteger? = null,
    @Serializable(with = BigIntegerSerializer::class)
    val maximum: BigInteger? = null,
)

@Serializable
data class FloatValueRange(
    @Serializable(with = BigDecimalSerializer::class)
    val minimum: BigDecimal? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val maximum: BigDecimal? = null,
)