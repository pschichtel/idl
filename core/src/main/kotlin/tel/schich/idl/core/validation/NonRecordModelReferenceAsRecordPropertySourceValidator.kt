package tel.schich.idl.core.validation

import tel.schich.idl.core.Model
import tel.schich.idl.core.ModelReference
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.resolveModelReference
import tel.schich.idl.core.resolveModelReferenceToDefinition

data class NonRecordModelReferenceAsRecordPropertySourceError(
    override val module: ModuleReference,
    val definition: String,
    val reference: ModelReference,
) : ValidationError

object NonRecordModelReferenceAsRecordPropertySourceValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val errors = module.definitions
            .filterIsInstance<Model.Record>()
            .filter { it.propertiesFrom.isNotEmpty() }
            .flatMap { record ->
                record.propertiesFrom.mapNotNull { modelRef ->
                    resolveModelReferenceToDefinition<Model>(module, allModules, modelRef)?.let { definition ->
                        if (definition is Model.Record) {
                            null
                        } else {
                            NonRecordModelReferenceAsRecordPropertySourceError(module.reference, record.metadata.name, modelRef)
                        }
                    }
                }
            }

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors.toSet())
    }
}