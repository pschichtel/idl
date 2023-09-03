package tel.schich.idl.core.generate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference

@Serializable
data class GenerationRequest(
    val modules: List<Module>,
    val subjects: Set<ModuleReference>,
    // TODO some form of configuration pass through?
)

@Serializable
sealed interface GenerationResult {
    @Serializable
    @SerialName("success")
    data class Success(val generatedFiles: List<String>) : GenerationResult

    @Serializable
    @SerialName("failure")
    data class Failure(val reason: String) : GenerationResult
}
