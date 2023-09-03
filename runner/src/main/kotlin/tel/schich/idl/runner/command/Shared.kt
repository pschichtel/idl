package tel.schich.idl.runner.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import tel.schich.idl.core.Module
import tel.schich.idl.runner.loadModule
import tel.schich.idl.runner.validation.ValidationError
import tel.schich.idl.runner.validation.ValidationResult
import tel.schich.idl.runner.validation.validateModule
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence
import kotlin.system.exitProcess

internal fun CliktCommand.error(string: String) {
    echo(string, err = true)
}

internal fun CliktCommand.loadModules(sources: List<Path>): List<Module> {
    val inputFiles = sources
        .filter { Files.exists(it) }
        .map { it.toRealPath() }
        .flatMap {
            when {
                Files.isDirectory(it) -> Files.walk(it).asSequence().filterNot(Files::isDirectory)
                else -> sequenceOf(it)
            }
        }

    if (inputFiles.isEmpty()) {
        error("Non of the given paths were viable:")
        for (inputPath in sources) {
            error("  - $inputPath")
        }
        exitProcess(1)
    }

    return inputFiles.map { inputFile ->
        val module = loadModule(inputFile)
        module.copy(metadata = module.metadata.copy(sourcePath = inputFile))
    }
}

internal fun CliktCommand.printValidationErrors(validationErrors: Set<ValidationError>) {
    error("Validation errors:")
    for ((module, errors) in validationErrors.groupBy { it.module }) {
        error("    Module: $module")
        for (error in errors) {
            when (error) {
                is ValidationError.DuplicatedModule -> {
                    error("        Module has been defined in multiple files:")
                    for (sourceFile in error.sourceFiles) {
                        error("            - $sourceFile")
                    }
                }

                is ValidationError.DuplicatedDefinition -> {
                    error("        There are multiple definitions named ${error.name}: ${error.indices.joinToString(", ")}")
                }

                is ValidationError.InvalidModuleName -> {
                    error("        The module name is invalid: ${error.reason}")
                }

                is ValidationError.UndefinedModuleReferenced -> {
                    error("        An undefined module is referenced from definition ${error.definitionName}: ${error.referencedModule}")
                }
                is ValidationError.UndefinedDefinitionReferenced -> {
                    error("        An undefined definition is referenced from definition ${error.definitionName}: ${error.referencedDefinition} from ${error.referencedModule}")
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

internal fun CliktCommand.exit(code: Int): Nothing {
    throw CliktError(statusCode = code)
}