package ai.vier.cvg.provisioning

import ai.vier.cvg.shared.phonenumber.E164Number
import ai.vier.cvg.shared.tokens.DialogId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ProvisionCallResponse {
    @Serializable
    @SerialName("Success")
    data class Success(
        val target: E164Number,
        val dialogId: DialogId,
    ) : ProvisionCallResponse

    @Serializable
    @SerialName("Error")
    data class Error(
        val message: ProvisionCallErrorMessage,
    ) : ProvisionCallResponse
}
