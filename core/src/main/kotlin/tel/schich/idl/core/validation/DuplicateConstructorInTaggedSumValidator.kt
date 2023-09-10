package tel.schich.idl.core.validation

import tel.schich.idl.core.Model
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference

data class DuplicateConstructorInTaggedSumError(
    override val module: ModuleReference,
    val definition: String,
    val constructor: String,
    val indices: Set<Int>,
) : ValidationError

object DuplicateConstructorInTaggedSumValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val errors = module.definitions
            .filterIsInstance<Model.TaggedSum>()
            .flatMap { sum ->
                sum.constructors
                    .withIndex()
                    .groupBy({ it.value.metadata.name }, { it.index })
                    .filter { it.value.size > 1 }
                    .map { (constructorName, indices) ->
                        DuplicateConstructorInTaggedSumError(module.reference, sum.metadata.name, constructorName, indices.toSet())
                    }
            }

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors.toSet())
    }
}