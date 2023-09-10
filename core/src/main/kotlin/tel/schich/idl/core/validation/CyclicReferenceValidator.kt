package tel.schich.idl.core.validation

import tel.schich.idl.core.Alias
import tel.schich.idl.core.Definition
import tel.schich.idl.core.Model
import tel.schich.idl.core.ModelReference
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.constraint.CollectionConstraint
import tel.schich.idl.core.constraint.CollectionSizeRange
import tel.schich.idl.core.constraint.CollectionValuesUnique

data class CyclicReferenceError(
    override val module: ModuleReference,
    val path: List<ModelReference>,
) : ValidationError

object CyclicReferenceValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val moduleLookup = allModules.associate { m ->
            Pair(m.reference, m.definitions.associateBy { it.metadata.name })
        }

        fun lookupDefinition(currentModule: ModuleReference, ref: ModelReference): Pair<ModuleReference, Definition>? {
            val moduleRef = ref.module ?: currentModule
            val definitionLookup = moduleLookup[moduleRef]
                ?: return null
            return definitionLookup[ref.name]?.let { Pair(moduleRef, it) }
        }

        fun resolveMinimumSize(constraints: Set<CollectionConstraint>): Int {
            var minimum = 0
            for (constraint in constraints) {
                when (constraint) {
                    is CollectionSizeRange -> {
                        if (constraint.minimum > minimum) {
                           minimum = constraint.minimum
                        }
                    }
                    is CollectionValuesUnique -> {}
                }
            }
            return minimum
        }

        fun refWithModule(moduleRef: ModuleReference, ref: ModelReference): ModelReference {
            if (ref.module != null) {
                return ref
            }
            return ref.copy(module = moduleRef)
        }

        fun detectCycle(currentModule: ModuleReference, ref: ModelReference, path: List<ModelReference>): List<ModelReference>? {
            if (ref in path) {
                return path + ref
            }
            val (referencedModuleRef, definition) = lookupDefinition(currentModule, ref) ?: return null

            return when (definition) {
                is Alias -> detectCycle(referencedModuleRef, refWithModule(currentModule, definition.aliasedModel), path + ref)
                is Model.Constant -> null
                is Model.Enumeration -> null
                is Model.HomogenousList -> {
                    if (resolveMinimumSize(definition.constraints) > 0) {
                        detectCycle(referencedModuleRef, definition.itemModel, path + ref)
                    } else {
                        null
                    }
                }
                is Model.HomogenousSet -> {
                    if (resolveMinimumSize(definition.constraints) > 0) {
                        detectCycle(referencedModuleRef, definition.itemModel, path + ref)
                    } else {
                        null
                    }
                }
                is Model.HomogenousMap -> {
                    if (resolveMinimumSize(definition.constraints) > 0) {
                        detectCycle(referencedModuleRef, definition.keyModel, path + ref)
                            ?: detectCycle(referencedModuleRef, definition.valueModel, path + ref)
                    } else {
                        null
                    }
                }
                is Model.Primitive -> null
                is Model.Product -> {
                    definition.components.asSequence()
                        .mapNotNull {
                            detectCycle(referencedModuleRef, it, path + ref)
                        }
                        .firstOrNull()
                }
                is Model.Record -> {
                    definition.properties.asSequence()
                        .filter { it.default == null }
                        .mapNotNull {
                            detectCycle(referencedModuleRef, it.model, path + ref)
                        }
                        .firstOrNull()
                }
                is Model.Sum -> {
                    val constructors = definition.constructors
                    val paths = constructors.map {
                        detectCycle(referencedModuleRef, it, path + ref)
                    }
                    // if any constructor exists that can be satisfied, then the other cycles are fine.
                    if (paths.any { it == null }) {
                        null
                    } else {
                        paths.firstOrNull()
                    }
                }
                is Model.TaggedSum -> {
                    val constructors = definition.constructors
                    val paths = constructors.map {
                        detectCycle(referencedModuleRef, it.model, path + ref)
                    }
                    // if any constructor exists that can be satisfied, then the other cycles are fine.
                    if (paths.any { it == null }) {
                        null
                    } else {
                        paths.firstOrNull()
                    }
                }
                is Model.Adt -> {
                    val commonPropertyCycle = definition.commonProperties.asSequence()
                        .filter { it.default == null }
                        .mapNotNull {
                            detectCycle(referencedModuleRef, it.model, path + ref)
                        }
                        .firstOrNull()
                    if (commonPropertyCycle != null) {
                        commonPropertyCycle
                    } else {
                        val paths = definition.constructors.map { record ->
                            record.properties.asSequence()
                                .filter { it.default == null }
                                .mapNotNull {
                                    detectCycle(referencedModuleRef, it.model, path + ref)
                                }
                                .firstOrNull()
                        }
                        // if any constructor exists that can be satisfied, then the other cycles are fine.
                        if (paths.any { it == null }) {
                            null
                        } else {
                            paths.firstOrNull()
                        }
                    }
                }
                is Model.Unknown -> null
            }
        }


        val errors = module.definitions.mapNotNull { definition ->
            detectCycle(module.reference, ModelReference(module.reference, definition.metadata.name), emptyList())?.let {
                CyclicReferenceError(module.reference, it)
            }
        }

        if (errors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(errors.toSet())
    }
}