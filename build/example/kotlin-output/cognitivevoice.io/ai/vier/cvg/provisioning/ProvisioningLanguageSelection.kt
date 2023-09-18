package ai.vier.cvg.provisioning

import ai.vier.cvg.shared.language.Language
import kotlinx.serialization.Serializable

@Serializable
data class ProvisioningLanguageSelection(
    val transcriber: Language,
    val synthesizer: Language,
)
