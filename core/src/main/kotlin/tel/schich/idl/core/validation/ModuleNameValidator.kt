package tel.schich.idl.core.validation

import tel.schich.idl.core.MODULE_NAME_SEPARATOR
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference

data class InvalidModuleNameError(
    override val module: ModuleReference,
    val reason: String,
) : ValidationError

object ModuleNameValidator : ModuleValidator {
    private val segmentPattern = "^[a-z][a-z0-9]*(-[a-z0-9]+)*$".toRegex()

    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val nameSegments = module.metadata.name.split(MODULE_NAME_SEPARATOR)
        val errors = buildSet<ValidationError> {
            if (nameSegments.size == 1) {
                add(
                    InvalidModuleNameError(
                        module.reference,
                        reason = "Module names must be have at least 2 dot-separated segments. A reversed domain name (schich.tel -> tel.schich) is recommended.",
                    ),
                )
            }
            if (nameSegments.any { !it.matches(segmentPattern) }) {
                add(
                    InvalidModuleNameError(
                        module.reference,
                        reason = "Module name segments must match $segmentPattern.",
                    ),
                )
            }
            if (nameSegments.any { it.length > 63 }) {
                add(
                    InvalidModuleNameError(
                        module.reference,
                        reason = "Module name segments must be at most 63 characters.",
                    ),
                )
            }
            if (module.metadata.name.length > 255) {
                add(
                    InvalidModuleNameError(
                        module.reference,
                        reason = "Module name must be at most 255 characters.",
                    ),
                )
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }
        return ValidationResult.Invalid(errors)
    }
}