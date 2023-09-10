package tel.schich.idl.core.validation

import tel.schich.idl.core.Module

interface ModuleValidator {
    fun validate(module: Module, allModules: List<Module>): ValidationResult
}

private val validations: List<ModuleValidator> = listOf(
    DuplicateModuleValidator,
    ModuleNameValidator,
    DuplicateDefinitionValidator,
    DeadReferenceValidator,
    CyclicReferenceValidator,
    NullDefaultInNonNullableRecordPropertyValidator,
    DuplicateRecordPropertyValidator,
    DuplicateConstructorInTaggedSumValidator,
    DuplicateTagInTaggedSumValidator,
    DuplicateConstructorInAdtValidator,
    DuplicatePropertiesInAdtValidator,
)

fun validateModule(module: Module, allModules: List<Module>): ValidationResult {
    return validations.fold<_, ValidationResult>(ValidationResult.Valid) { result, validator ->
        result + validator.validate(module, allModules)
    }
}