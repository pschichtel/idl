package tel.schich.idl.core.validation

import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference

data class DuplicatedDefinitionError(
    override val module: ModuleReference,
    val name: String,
    val indices: List<Int>,
) : ValidationError

object DuplicateDefinitionValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val errors = module.definitions
            .asSequence()
            .map { it.metadata.name }
            .withIndex()
            .groupBy({ it.value }, { it.index })
            .filter { it.value.size > 1 }
            .map { (name, indices) -> DuplicatedDefinitionError(module.reference, name, indices) }
            .toSet()

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors)
    }
}