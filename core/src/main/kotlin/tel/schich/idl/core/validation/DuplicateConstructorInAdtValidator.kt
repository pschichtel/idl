package tel.schich.idl.core.validation

import tel.schich.idl.core.Model
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference

data class DuplicateConstructorInAdtError(
    override val module: ModuleReference,
    val definition: String,
    val constructor: String,
    val indices: Set<Int>,
) : ValidationError

object DuplicateConstructorInAdtValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val errors = module.definitions
            .filterIsInstance<Model.Adt>()
            .flatMap { adt ->
                adt.constructors
                    .withIndex()
                    .groupBy({ it.value.metadata.name }, { it.index })
                    .filter { it.value.size > 1 }
                    .map { (constructor, indices) ->
                        DuplicateConstructorInAdtError(module.reference, adt.metadata.name, constructor, indices.toSet())
                    }
            }

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors.toSet())
    }
}