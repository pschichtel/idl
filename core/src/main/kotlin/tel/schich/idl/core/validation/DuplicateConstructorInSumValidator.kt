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

data class DuplicateConstructorInSumError(
    override val module: ModuleReference,
    val definition: String,
    val constructor: String,
    val indices: Set<Int>,
) : ValidationError

object DuplicateConstructorInSumValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val taggedSumErrors = module.definitions
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

        val sumErrors = module.definitions
            .filterIsInstance<Model.Sum>()
            .flatMap { sum ->
                sum.constructors
                    .withIndex()
                    .groupBy({ it.value.metadata.name }, { it.index })
                    .filter { it.value.size > 1 }
                    .map { (constructorName, indices) ->
                        DuplicateConstructorInSumError(module.reference, sum.metadata.name, constructorName, indices.toSet())
                    }
            }

        val errors = taggedSumErrors + sumErrors
        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors.toSet())
    }
}