package tel.schich.idl.core.validation

import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.core.Model
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference

data class DuplicateTagInTaggedSumError(
    override val module: ModuleReference,
    val definition: String,
    val tagValue: JsonPrimitive,
    val constructors: Set<String>,
) : ValidationError

object DuplicateTagInTaggedSumValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val errors = module.definitions
            .filterIsInstance<Model.TaggedSum>()
            .flatMap { sum ->
                sum.constructors
                    .groupBy({ it.tag.tag }, { it.metadata.name })
                    .filter { it.value.size > 1 }
                    .map { (tagValue, constructorNames) ->
                        DuplicateTagInTaggedSumError(module.reference, sum.metadata.name, tagValue, constructorNames.toSet())
                    }
            }

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors.toSet())
    }
}