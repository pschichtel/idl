package ai.vier.cvg.shared.speaker

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The speaker describes a participant of the dialog.
 * 
 * - `CUSTOMER` is the human caller/callee
 * - `AGENT` can be a virtual agent (bot), or a human agent during the assist use case
 */
@Serializable
enum class Speaker(val value: String) {
    @SerialName("CUSTOMER")
    CUSTOMER(value = "CUSTOMER"),
    @SerialName("AGENT")
    AGENT(value = "AGENT"),
}
