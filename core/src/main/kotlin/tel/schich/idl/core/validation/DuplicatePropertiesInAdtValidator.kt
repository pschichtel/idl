package tel.schich.idl.core.validation

import tel.schich.idl.core.Model
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference

data class ConstructorPropertyDuplicatesTypePropertyInAdtError(
    override val module: ModuleReference,
    val definition: String,
    val typeProperty: String,
    val constructor: String,
) : ValidationError

data class CommonPropertyDuplicatesTypePropertyInAdtError(
    override val module: ModuleReference,
    val definition: String,
    val typeProperty: String,
) : ValidationError

data class DuplicateCommonPropertyInAdtError(
    override val module: ModuleReference,
    val definition: String,
    val property: String,
    val indices: Set<Int>,
) : ValidationError

data class DuplicateConstructorPropertyInAdtError(
    override val module: ModuleReference,
    val definition: String,
    val constructor: String,
    val property: String,
    val indices: Set<Int>,
) : ValidationError

data class ConstructorPropertyDuplicatesCommonPropertyInAdtError(
    override val module: ModuleReference,
    val definition: String,
    val constructor: String,
    val property: String,
) : ValidationError

object DuplicatePropertiesInAdtValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {
        val adts = module.definitions
            .filterIsInstance<Model.Adt>()

        val errors = adts
            .flatMap { adt ->
                buildList {
                    val commonPropertyNames = adt.commonProperties.map { it.metadata.name }.toSet()
                    if (adt.commonProperties.any { property -> property.metadata.name == adt.typeProperty }) {
                        add(CommonPropertyDuplicatesTypePropertyInAdtError(module.reference, adt.metadata.name, adt.typeProperty))
                    }
                    adt.commonProperties
                        .withIndex()
                        .groupBy({ it.value.metadata.name }, { it.index })
                        .filter { it.value.size > 1 }
                        .forEach { (property, indices) ->
                            add(DuplicateCommonPropertyInAdtError(module.reference, adt.metadata.name, property, indices.toSet()))
                        }

                    for (constructor in adt.constructors) {
                        if (constructor.properties.any { property -> property.metadata.name == adt.typeProperty }) {
                            add(ConstructorPropertyDuplicatesTypePropertyInAdtError(module.reference, adt.metadata.name, adt.typeProperty, constructor.metadata.name))
                        }
                        constructor.properties
                            .filter { it.metadata.name in commonPropertyNames }
                            .forEach {
                                add(ConstructorPropertyDuplicatesCommonPropertyInAdtError(module.reference, adt.metadata.name, constructor.metadata.name, it.metadata.name))
                            }
                        constructor.properties
                            .withIndex()
                            .groupBy({ it.value.metadata.name }, { it.index })
                            .filter { it.value.size > 1 }
                            .forEach { (property, indices) ->
                                add(DuplicateConstructorPropertyInAdtError(module.reference, adt.metadata.name, constructor.metadata.name, property, indices.toSet()))
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