package tel.schich.idl.core.validation

import tel.schich.idl.core.Module

interface ModuleValidator {
    fun validate(module: Module, allModules: List<Module>): ValidationResult
}

// TODO detect duplicate enum entries
private val validations: List<ModuleValidator> = listOf(
    DuplicateModuleValidator,
    ModuleNameValidator,
    DuplicateDefinitionValidator,
    DeadReferenceValidator,
    CyclicReferenceValidator,
    NullDefaultInNonNullableRecordPropertyValidator,
    DuplicateRecordPropertyValidator,
    DuplicateConstructorInSumValidator,
    DuplicateTagInTaggedSumValidator,
    DuplicateConstructorInAdtValidator,
    DuplicatePropertiesInAdtValidator,
    EmptyRecordValidator,
    NonRecordModelReferenceAsRecordPropertySourceValidator,
)

fun validateModule(module: Module, allModules: List<Module>): ValidationResult {
    return validations.fold<_, ValidationResult>(ValidationResult.Valid) { result, validator ->
        result + validator.validate(module, allModules)
    }
}