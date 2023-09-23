package tel.schich.idl.core.validation

import tel.schich.idl.core.CanonicalName
import tel.schich.idl.core.Model
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.canonicalName

data class DuplicatedEnumerationEntryError(
    override val module: ModuleReference,
    val definition: String,
    val entry: CanonicalName,
    val indices: List<Int>,
) : ValidationError

object DuplicateEnumerationEntryValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val errors = module.definitions
            .filterIsInstance<Model.Enumeration>()
            .asSequence()
            .flatMap { enum ->
                enum.entries
                    .asSequence()
                    .withIndex()
                    .groupBy({ canonicalName(it.value.metadata.name) }, { it.index })
                    .filter { it.value.size > 1 }
                    .map { (name, indices) -> DuplicatedEnumerationEntryError(module.reference, enum.metadata.name, name, indices) }
            }
            .toSet()

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors)
    }
}