package ai.vier.cvg.shared.project

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FeatureFlag(val value: String) {
    @SerialName("SIP_URI_IN_SESSION_REQUEST")
    SIP_URI_IN_SESSION_REQUEST(value = "SIP_URI_IN_SESSION_REQUEST"),
    @SerialName("SIP_URI_PARAMS_AS_HEADERS")
    SIP_URI_PARAMS_AS_HEADERS(value = "SIP_URI_PARAMS_AS_HEADERS"),
}
