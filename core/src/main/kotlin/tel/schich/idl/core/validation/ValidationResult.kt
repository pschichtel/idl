package tel.schich.idl.core.validation

import kotlinx.serialization.Serializable
import tel.schich.idl.core.ModuleReference

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errors: Set<ValidationError>) : ValidationResult

    operator fun plus(other: ValidationResult): ValidationResult {
        val thisErrors = when (this) {
            is Valid -> emptySet()
            is Invalid -> errors
        }
        val otherErrors = when (other) {
            is Valid -> emptySet()
            is Invalid -> other.errors
        }

        if (thisErrors.isEmpty() && otherErrors.isEmpty()) {
            return Valid
        }

        return Invalid(thisErrors + otherErrors)
    }
}

@Serializable
data class GeneratorValidationError(val module: ModuleReference, val description: String)

sealed interface ValidationError {
    val module: ModuleReference
}