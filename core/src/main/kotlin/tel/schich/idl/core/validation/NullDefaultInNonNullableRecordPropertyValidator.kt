package tel.schich.idl.core.validation

import kotlinx.serialization.json.JsonNull
import tel.schich.idl.core.Model
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference

data class NullDefaultInNonNullableRecordPropertyError(
    override val module: ModuleReference,
    val definition: String,
    val property: String,
) : ValidationError

object NullDefaultInNonNullableRecordPropertyValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val errors = module.definitions
            .filterIsInstance<Model.Record>()
            .flatMap { record ->
                record.properties
                    .filterNot { it.nullable }
                    .filter { it.default != null && it.default.value is JsonNull }
                    .map { property ->
                        NullDefaultInNonNullableRecordPropertyError(
                            module.reference,
                            record.metadata.name,
                            property.metadata.name,
                        )
                    }
            }

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors.toSet())
    }
}