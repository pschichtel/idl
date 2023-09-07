package tel.schich.idl.core.validation

import kotlinx.serialization.json.JsonNull
import tel.schich.idl.core.Model
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference

data class DuplicateRecordPropertyError(
    override val module: ModuleReference,
    val definition: String,
    val property: String,
    val indices: List<Int>,
) : ValidationError

object DuplicateRecordPropertyValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val errors = module.definitions
            .filterIsInstance<Model.Record>()
            .flatMap { record ->
                record.properties
                    .withIndex()
                    .groupBy({ it.value.metadata.name }, { it.index })
                    .filter { it.value.size > 1 }
                    .map { (propertyName, indices) ->
                        DuplicateRecordPropertyError(module.reference, record.metadata.name, propertyName, indices)
                    }
            }

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors.toSet())
    }
}