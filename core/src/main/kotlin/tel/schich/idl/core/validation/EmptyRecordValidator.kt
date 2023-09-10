package tel.schich.idl.core.validation

import tel.schich.idl.core.Model
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference

data class EmptyRecordError(
    override val module: ModuleReference,
    val definition: String,
) : ValidationError

object EmptyRecordValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val errors = module.definitions
            .filterIsInstance<Model.Record>()
            .filter { it.properties.isEmpty() }
            .map { record ->
                EmptyRecordError(module.reference, record.metadata.name)
            }

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors.toSet())
    }
}