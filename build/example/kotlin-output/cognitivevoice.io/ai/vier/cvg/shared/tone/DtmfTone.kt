package ai.vier.cvg.shared.tone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DtmfTone(val value: String) {
    @SerialName("0")
    TONE_0(value = "0"),
    @SerialName("1")
    TONE_1(value = "1"),
    @SerialName("2")
    TONE_2(value = "2"),
    @SerialName("3")
    TONE_3(value = "3"),
    @SerialName("4")
    TONE_4(value = "4"),
    @SerialName("5")
    TONE_5(value = "5"),
    @SerialName("6")
    TONE_6(value = "6"),
    @SerialName("7")
    TONE_7(value = "7"),
    @SerialName("8")
    TONE_8(value = "8"),
    @SerialName("9")
    TONE_9(value = "9"),
    @SerialName("A")
    TONE_A(value = "A"),
    @SerialName("B")
    TONE_B(value = "B"),
    @SerialName("C")
    TONE_C(value = "C"),
    @SerialName("D")
    TONE_D(value = "D"),
    @SerialName("*")
    TONE_STAR(value = "*"),
    @SerialName("#")
    TONE_POUND(value = "#"),
}
