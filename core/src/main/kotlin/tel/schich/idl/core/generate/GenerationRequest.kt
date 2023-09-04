package tel.schich.idl.core.generate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tel.schich.idl.core.Annotation
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.validation.GeneratorValidationError

@Serializable
data class GenerationRequest(
    val modules: List<Module>,
    val subjects: Set<ModuleReference>,
    val annotations: Map<String, String>,
    // TODO some form of configuration pass through?
)

fun <T : Any> GenerationRequest.getAnnotation(annotation: Annotation<T>): T? =
    annotation.getValue(annotations)

@Serializable
sealed interface GenerationResult {
    @Serializable
    @SerialName("success")
    data class Success(val generatedFiles: List<String>) : GenerationResult

    @Serializable
    @SerialName("invalid")
    data class Invalid(val errors: GeneratorValidationError) : GenerationResult

    @Serializable
    @SerialName("failure")
    data class Failure(val reason: String) : GenerationResult
}