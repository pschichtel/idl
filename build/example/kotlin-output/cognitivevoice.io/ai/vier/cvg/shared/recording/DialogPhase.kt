package ai.vier.cvg.shared.recording

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Overall health status for a project, determined by the health event with the worst status.
 * `UNKNOWN` if no calls happened within the checked time period.
 */
@Serializable
enum class DialogPhase(val value: String) {
    @SerialName("BOT")
    BOT(value = "BOT"),
    @SerialName("ASSIST")
    ASSIST(value = "ASSIST"),
}
