package tel.schich.idl.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * test-adt
 */
@Serializable
sealed interface Adt {
    val timestamp: Test

    @Serializable
    @SerialName("a")
    data class A(
        override val timestamp: Test,
        val counter: Test,
    ) : Adt

    @Serializable
    @SerialName("b")
    data class B(
        override val timestamp: Test,
        val hits: Test,
    ) : Adt
}
