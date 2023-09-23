package tel.schich.idl.core.generate

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tel.schich.idl.core.Annotation
import tel.schich.idl.core.Annotations
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.validation.GeneratorValidationError
import java.nio.file.Path

@Serializable
data class GenerationRequest(
    val modules: List<Module>,
    val subjects: Set<ModuleReference>,
    val annotations: Annotations,
    val outputPath: Path,
)

fun <T : Any> GenerationRequest.getAnnotation(annotation: Annotation<T>): T? =
    annotation.getValue(annotations)

class InvalidModuleException(val module: ModuleReference, val reason: String) : Exception(reason)

fun invalidModule(module: ModuleReference, reason: String): Nothing {
    throw InvalidModuleException(module, reason)
}

@Serializable
sealed interface GenerationResult {
    @Serializable
    @SerialName("success")
    data class Success(val generatedFiles: List<Path>) : GenerationResult

    @Serializable
    @SerialName("invalid")
    data class Invalid(val errors: List<GeneratorValidationError>) : GenerationResult

    @Serializable
    @SerialName("failure")
    data class Failure(val reason: String) : GenerationResult
}
