package tel.schich.idl.runner.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import tel.schich.idl.core.ModelReference
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.validation.CommonPropertyDuplicatesTypePropertyInAdtError
import tel.schich.idl.core.validation.ConstructorPropertyDuplicatesCommonPropertyInAdtError
import tel.schich.idl.core.validation.ConstructorPropertyDuplicatesTypePropertyInAdtError
import tel.schich.idl.core.validation.CyclicReferenceError
import tel.schich.idl.core.validation.DuplicateCommonPropertyInAdtError
import tel.schich.idl.core.validation.DuplicateConstructorInAdtError
import tel.schich.idl.core.validation.DuplicateConstructorInSumError
import tel.schich.idl.core.validation.DuplicateConstructorInTaggedSumError
import tel.schich.idl.core.validation.DuplicateConstructorPropertyInAdtError
import tel.schich.idl.core.validation.DuplicateRecordPropertyError
import tel.schich.idl.core.validation.DuplicateTagInTaggedSumError
import tel.schich.idl.core.validation.DuplicatedDefinitionError
import tel.schich.idl.core.validation.DuplicatedEnumerationEntryError
import tel.schich.idl.core.validation.DuplicatedModuleError
import tel.schich.idl.core.validation.EmptyRecordError
import tel.schich.idl.core.validation.InvalidModuleNameError
import tel.schich.idl.core.validation.NonRecordModelReferenceAsRecordPropertySourceError
import tel.schich.idl.core.validation.NullDefaultInNonNullableRecordPropertyError
import tel.schich.idl.core.validation.RecordPropertyOverlapsWithPropertySourcesError
import tel.schich.idl.core.validation.RecordPropertySourcesOverlappingError
import tel.schich.idl.core.validation.UndefinedDefinitionReferencedError
import tel.schich.idl.core.validation.UndefinedModuleReferencedError
import tel.schich.idl.runner.loadModule
import tel.schich.idl.core.validation.ValidationError
import tel.schich.idl.core.validation.ValidationResult
import tel.schich.idl.core.validation.validateModule
import tel.schich.idl.runner.generate.resolveToFiles
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence
import kotlin.system.exitProcess

internal fun CliktCommand.error(string: String) {
    echo(string, err = true)
}

internal fun CliktCommand.resolveModulePaths(sources: List<Path>): List<Path> {
    val resolvedPaths = resolveToFiles(sources)

    if (resolvedPaths.isEmpty()) {
        error("Non of the given paths were viable:")
        for (inputPath in sources) {
            error("  - $inputPath")
        }
        exitProcess(1)
    }

    return resolvedPaths
}

internal fun CliktCommand.printValidationErrors(validationErrors: Set<ValidationError>) {
    fun modelRef(module: ModuleReference, ref: ModelReference): String {
        return "${ref.module ?: module}#${ref.name}"
    }

    error("Validation errors:")
    for ((module, errors) in validationErrors.groupBy { it.module }) {
        error("    Module: $module")
        for (error in errors) {
            when (error) {
                is DuplicatedModuleError -> {
                    error("        Module has been defined in multiple files:")
                    for (sourceFile in error.sourceFiles) {
                        error("            - $sourceFile")
                    }
                }
                is DuplicatedDefinitionError -> {
                    error("        There are multiple definitions named ${error.name}: ${error.indices.joinToString(", ")}")
                }
                is InvalidModuleNameError -> {
                    error("        The module name is invalid: ${error.reason}")
                }
                is UndefinedModuleReferencedError -> {
                    error("        An undefined module is referenced from definition ${error.definitionName}: ${error.referencedModule}")
                }
                is UndefinedDefinitionReferencedError -> {
                    error("        An undefined definition is referenced from definition ${error.definitionName}: ${error.referencedDefinition} from ${error.referencedModule}")
                }
                is CyclicReferenceError -> {
                    error("        A cyclic reference has been detected, path:")
                    for (ref in error.path) {
                        error("          - ${ref.module} -> ${ref.name}")
                    }
                }
                is NullDefaultInNonNullableRecordPropertyError -> {
                    error("        Property ${error.property} of record ${error.definition} is not nullable but has null as its default value")
                }
                is DuplicateRecordPropertyError -> {
                    error("        Property ${error.property} of record ${error.definition} is defined multiple times at indices: ${error.indices.joinToString(", ")}")
                }
                is DuplicateConstructorInSumError -> {
                    error("        Constructor ${error.constructor} has been defined multiple times in sum ${error.definition}: ${error.indices.joinToString(", ")}")
                }
                is DuplicateConstructorInTaggedSumError -> {
                    error("        Constructor ${error.constructor} has been defined multiple times in tagged sum ${error.definition}: ${error.indices.joinToString(", ")}")
                }
                is DuplicateTagInTaggedSumError -> {
                    error("        Tagged-Sum ${error.definition} has the tag value ${error.tagValue} for multiple constructors: ${error.constructors.joinToString(", ")}")
                }
                is CommonPropertyDuplicatesTypePropertyInAdtError -> {
                    error("        ADT ${error.definition} has the type property ${error.typeProperty} additionally defined as common property")
                }
                is ConstructorPropertyDuplicatesCommonPropertyInAdtError -> {
                    error("        Constructor ${error.constructor} of ADT ${error.definition} also defines the common property ${error.property}")
                }
                is ConstructorPropertyDuplicatesTypePropertyInAdtError -> {
                    error("        Constructor ${error.constructor} of ADT ${error.definition} also defines the type property ${error.typeProperty}")
                }
                is DuplicateCommonPropertyInAdtError -> {
                    error("        ADT ${error.definition} defines the common property ${error.property} multiple times: ${error.indices.joinToString(", ")}")
                }
                is DuplicateConstructorInAdtError -> {
                    error("        ADT ${error.definition} defines the constructor ${error.constructor} multiple times: ${error.indices.joinToString(", ")}")
                }
                is DuplicateConstructorPropertyInAdtError -> {
                    error("        Constructor ${error.constructor} of ADT ${error.definition} defines the property ${error.property} multiple times: ${error.indices.joinToString(", ")}")
                }
                is EmptyRecordError -> {
                    error("        Record ${error.definition} does not have any properties")
                }
                is NonRecordModelReferenceAsRecordPropertySourceError -> {
                    error("        Record ${error.definition} tries to copy properties from the non-record definition ${modelRef(error.module, error.reference)}")
                }
                is RecordPropertyOverlapsWithPropertySourcesError -> {
                    error("        Record ${error.definition} has properties that overlap with its property source ${modelRef(error.module, error.source)}: ${error.properties.joinToString(", ")}")
                }
                is RecordPropertySourcesOverlappingError -> {
                    error("        Record ${error.definition} has overlapping property sources ${modelRef(error.module, error.sourceA)} and ${modelRef(error.module, error.sourceB)}, both define these properties: ${error.properties.joinToString(", ")}")
                }
                is DuplicatedEnumerationEntryError -> {
                    error("        Enumeration ${error.definition} has multiple entries named ${error.entry}: ${error.indices.joinToString(", ")}")
                }
            }
        }
    }
}

internal fun CliktCommand.validate(modules: List<Module>) {
    val validationResult = modules
        .map { validateModule(it, modules) }
        .reduce(ValidationResult::plus)

    when (validationResult) {
        is ValidationResult.Valid -> {
            echo("Modules valid!")
        }

        is ValidationResult.Invalid -> {
            error("Module validation failed!")
            printValidationErrors(validationResult.errors)
            exitProcess(1)
        }
    }
}

internal fun ParameterHolder.moduleSourcesOption(): OptionWithValues<List<Path>, Path, Path> {
    return option("--source")
        .path(mustExist = true, mustBeReadable = true)
        .multiple(required = true)
}

internal fun exit(code: Int): Nothing {
    throw CliktError(statusCode = code)
}