package ai.vier.cvg.assist

import ai.vier.cvg.shared.phonenumber.E164Number
import ai.vier.cvg.shared.tokens.ResellerToken
import kotlinx.serialization.Serializable

@Serializable
data class AcceptAssistParameters(
    val resellerToken: ResellerToken,
    val phoneNumber: E164Number,
    val callbackUrl: AssistCallbackUrl,
    val authToken: AssistAuthToken? = null,
)
