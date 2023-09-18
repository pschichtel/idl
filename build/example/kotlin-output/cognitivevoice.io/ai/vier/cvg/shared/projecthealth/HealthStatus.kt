package ai.vier.cvg.shared.projecthealth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Overall health status for a project, determined by the health event with the worst status.
 * `UNKNOWN` if no calls happened within the checked time period.
 */
@Serializable
enum class HealthStatus(val value: String) {
    @SerialName("UNKNOWN")
    UNKNOWN(value = "UNKNOWN"),
    @SerialName("OK")
    OK(value = "OK"),
    @SerialName("WARNING")
    WARNING(value = "WARNING"),
    @SerialName("CRITICAL")
    CRITICAL(value = "CRITICAL"),
}
