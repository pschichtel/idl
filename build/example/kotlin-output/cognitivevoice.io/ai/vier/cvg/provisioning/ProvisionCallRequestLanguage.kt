package ai.vier.cvg.provisioning

import ai.vier.cvg.shared.language.Language

sealed interface ProvisionCallRequestLanguage {
    data class Selection(val value: ProvisioningLanguageSelection) : ProvisionCallRequestLanguage

    data class Single(val value: Language) : ProvisionCallRequestLanguage
}
