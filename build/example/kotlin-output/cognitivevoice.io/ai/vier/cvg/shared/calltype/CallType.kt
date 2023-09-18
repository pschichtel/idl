package ai.vier.cvg.shared.calltype

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CallType(val value: String) {
    @SerialName("INBOUND")
    INBOUND(value = "INBOUND"),
    @SerialName("INBOUND_PROVISIONED")
    INBOUND_PROVISIONED(value = "INBOUND_PROVISIONED"),
    @SerialName("OUTBOUND")
    OUTBOUND(value = "OUTBOUND"),
}
