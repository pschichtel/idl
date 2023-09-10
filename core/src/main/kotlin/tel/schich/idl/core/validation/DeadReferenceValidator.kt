package tel.schich.idl.core.validation

import tel.schich.idl.core.Alias
import tel.schich.idl.core.Definition
import tel.schich.idl.core.Model
import tel.schich.idl.core.ModelReference
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference

data class UndefinedModuleReferencedError(
    override val module: ModuleReference,
    val definitionName: String,
    val referencedModule: ModuleReference,
) : ValidationError

data class UndefinedDefinitionReferencedError(
    override val module: ModuleReference,
    val definitionName: String,
    val referencedModule: ModuleReference,
    val referencedDefinition: String,
) : ValidationError

object DeadReferenceValidator : ModuleValidator {
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
                    UndefinedModuleReferencedError(
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
                    UndefinedDefinitionReferencedError(
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
                is Model.Record -> definition.properties.flatMap { validateReference(definition, it.model) }
                is Model.Sum -> definition.constructors.flatMap { validateReference(definition, it) }
                is Model.TaggedSum -> definition.constructors.flatMap { validateReference(definition, it.model) }
                is Model.Adt -> definition.commonProperties.flatMap { validateReference(definition, it.model) } + definition.constructors.flatMap { constructor -> constructor.properties.flatMap { validateReference(definition, it.model) } }
                is Model.Unknown -> emptyList()
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors.toSet())
    }
}