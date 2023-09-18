package ai.vier.cvg.provisioning

import ai.vier.cvg.shared.project.BotConfiguration
import ai.vier.cvg.shared.project.BotToken
import ai.vier.cvg.shared.project.BotUrl
import ai.vier.cvg.shared.project.InactivityTimeout
import ai.vier.cvg.shared.project.MinimumNoiseLevel
import ai.vier.cvg.shared.project.UtteranceTimeout
import ai.vier.cvg.shared.speechservice.SynthesizerSelection
import ai.vier.cvg.shared.speechservice.TranscriberSelection
import ai.vier.cvg.shared.tokens.ExternalCallId
import ai.vier.cvg.shared.tokens.ProjectToken
import ai.vier.cvg.shared.tokens.ResellerToken
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ProvisionCallRequest(
    val resellerToken: ResellerToken,
    val projectToken: ProjectToken,
    val callId: ExternalCallId,
    val botUrl: BotUrl? = null,
    val botAccessToken: BotToken? = null,
    val botConfiguration: BotConfiguration? = null,
    @Contextual
    val language: ProvisionCallRequestLanguage? = null,
    val transcriberVendors: TranscriberSelection? = null,
    val synthesizerVendors: SynthesizerSelection? = null,
    val writeDialogData: EnabledDialogData = true,
    val inactivityTimeout: InactivityTimeout? = null,
    val minimumNoiseLevel: MinimumNoiseLevel? = null,
    val utteranceTimeout: UtteranceTimeout? = null,
)
