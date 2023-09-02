package tel.schich.idl.validation

import tel.schich.idl.*
import java.nio.file.Path

sealed interface ValidationError {
    val module: ModuleReference

    data class DuplicatedModule(override val module: ModuleReference, val sourceFiles: Set<Path>) : ValidationError
    data class InvalidModuleName(override val module: ModuleReference, val reason: String) : ValidationError
    data class DuplicatedDefinition(override val module: ModuleReference, val name: String, val indices: List<Int>) : ValidationError
    data class UndefinedModuleReferenced(override val module: ModuleReference, val definitionName: String, val referencedModule: ModuleReference) : ValidationError
    data class UndefinedDefinitionReferenced(override val module: ModuleReference, val definitionName: String, val referencedModule: ModuleReference, val referencedDefinition: String) : ValidationError
}

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errors: Set<ValidationError>) : ValidationResult

    operator fun plus(other: ValidationResult): ValidationResult {
        val thisErrors = when (this) {
            is Valid -> emptySet()
            is Invalid -> errors
        }
        val otherErrors = when (other) {
            is Valid -> emptySet()
            is Invalid -> other.errors
        }

        if (thisErrors.isEmpty() && otherErrors.isEmpty()) {
            return Valid
        }

        return Invalid(thisErrors + otherErrors)
    }
}

interface ModuleValidator {
    fun validate(module: Module, allModules: List<Module>): ValidationResult
}

object DuplicateModuleValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val duplicatedModules = allModules.filter { it.reference == module.reference }
        if (duplicatedModules.size <= 1) {
            return ValidationResult.Valid
        }
        val error = ValidationError.DuplicatedModule(
            module.reference,
            duplicatedModules.mapNotNull { it.metadata.sourcePath }.toSet()
        )
        return ValidationResult.Invalid(setOf(error))
    }
}

object ModuleNameValidator : ModuleValidator {
    private val segmentPattern = "^[a-z][a-z0-9]*(-[a-z0-9]+)*$".toRegex()

    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val nameSegments = module.metadata.name.split('.')
        val errors = buildSet<ValidationError> {
            if (nameSegments.size == 1) {
                add(
                    ValidationError.InvalidModuleName(
                        module.reference,
                        reason = "Module names must be have at least 2 dot-separated segments. A reversed domain name (schich.tel -> tel.schich) is recommended.",
                    ),
                )
            }
            if (nameSegments.any { !it.matches(segmentPattern) }) {
                add(
                    ValidationError.InvalidModuleName(
                        module.reference,
                        reason = "Module name segments must match $segmentPattern.",
                    ),
                )
            }
            if (nameSegments.any { it.length > 63 }) {
                add(
                    ValidationError.InvalidModuleName(
                        module.reference,
                        reason = "Module name segments must be at most 63 characters.",
                    ),
                )
            }
            if (module.metadata.name.length > 255) {
                add(
                    ValidationError.InvalidModuleName(
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

object DuplicateDefinitionValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val errors = module.definitions
            .asSequence()
            .map { it.metadata.name }
            .withIndex()
            .groupBy({ it.value }, { it.index })
            .filter { it.value.size > 1 }
            .map { (name, indices) -> ValidationError.DuplicatedDefinition(module.reference, name, indices) }
            .toSet()

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors)
    }
}

object ReferenceValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val moduleLookup = allModules.associate { m ->
            Pair(m.reference, m.definitions.map { it.metadata.name }.toSet())
        }

        fun validateReference(definition: Definition, ref: ModelReference): List<ValidationError> = buildList {
            val referencedModule = ref.module ?: module.reference
            val definitionLookup = moduleLookup[referencedModule]
            val referencingDefinitionName = definition.metadata.name
            if (definitionLookup == null) {
                add(
                    ValidationError.UndefinedModuleReferenced(
                        module.reference,
                        referencingDefinitionName,
                        referencedModule
                    )
                )
                return@buildList
            }

            val referencedDefinitionName = ref.name
            if (referencedDefinitionName !in definitionLookup) {
                add(
                    ValidationError.UndefinedDefinitionReferenced(
                        module.reference,
                        referencingDefinitionName,
                        referencedModule,
                        referencedDefinitionName
                    )
                )
                return@buildList
            }
        }

        val errors = module.definitions.flatMap { definition ->
            when (definition) {
                is Alias -> validateReference(definition, definition.aliasedModel)
                is Model.Constant -> emptyList()
                is Model.Enumeration -> emptyList()
                is Model.HomogenousList -> validateReference(definition, definition.itemModel)
                is Model.HomogenousMap -> validateReference(definition, definition.keyModel) + validateReference(
                    definition,
                    definition.valueModel
                )

                is Model.HomogenousSet -> validateReference(definition, definition.itemModel)
                is Model.Primitive -> emptyList()
                is Model.Product -> definition.components.flatMap { validateReference(definition, it) }
                is Model.RawStream -> emptyList()
                is Model.Record -> definition.properties.flatMap { validateReference(definition, it.model) }
                is Model.Sum -> definition.constructors.flatMap { validateReference(definition, it) }
                is Model.TaggedSum -> definition.constructors.flatMap { validateReference(definition, it.model) }
                is Model.Unknown -> emptyList()
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors.toSet())
    }
}

private val validations: List<ModuleValidator> = listOf(
    DuplicateModuleValidator,
    ModuleNameValidator,
    DuplicateDefinitionValidator,
    ReferenceValidator,
)

fun validateModule(module: Module, allModules: List<Module>): ValidationResult {
    return validations.fold<_, ValidationResult>(ValidationResult.Valid) { result, validator ->
        result + validator.validate(module, allModules)
    }
}