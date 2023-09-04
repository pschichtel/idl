package tel.schich.idl.core.validation

import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import java.nio.file.Path

data class DuplicatedModuleError(
    override val module: ModuleReference,
    val sourceFiles: Set<Path>,
) : ValidationError

object DuplicateModuleValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val duplicatedModules = allModules.filter { it.reference == module.reference }
        if (duplicatedModules.size <= 1) {
            return ValidationResult.Valid
        }
        val error = DuplicatedModuleError(
            module.reference,
            duplicatedModules.mapNotNull { it.metadata.sourcePath }.toSet()
        )
        return ValidationResult.Invalid(setOf(error))
    }
}