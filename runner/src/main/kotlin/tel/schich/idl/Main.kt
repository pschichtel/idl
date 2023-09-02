package tel.schich.idl

import tel.schich.idl.validation.ValidationError
import tel.schich.idl.validation.ValidationResult
import tel.schich.idl.validation.validateModule
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.asSequence
import kotlin.system.exitProcess

private fun errorln(string: String) {
    System.err.println(string)
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        errorln("Usage: <subject module> <source file or dir>...")
        exitProcess(1)
    }

    val subjectModuleReference = ModuleReference.parse(args[0])
    if (subjectModuleReference == null) {
        errorln("<subject module> must be of format module-name:version")
        exitProcess(1)
    }

    val inputPaths = args.drop(1)
    val inputFiles = inputPaths
        .map { Paths.get(it) }
        .filter { Files.exists(it) }
        .map { it.toRealPath() }
        .flatMap {
            when {
                Files.isDirectory(it) -> Files.walk(it).asSequence().filterNot(Files::isDirectory)
                else -> sequenceOf(it)
            }
        }

    if (inputFiles.isEmpty()) {
        errorln("Non of the given paths were viable:")
        for (inputPath in inputPaths) {
            errorln("  - $inputPath")
        }
        exitProcess(1)
    }

    val modules = inputFiles.map { inputFile ->
        val module = loadModule(inputFile)
        module.copy(metadata = module.metadata.copy(sourcePath = inputFile))
    }

    val subjectModule = modules.find { it.reference == subjectModuleReference }
    if (subjectModule == null) {
        errorln("Module $subjectModuleReference not found in loaded modules:")
        for (module in modules) {
            errorln(" - ${module.reference}")
        }
        exitProcess(1)
    }

    val validationResult = modules.map { validateModule(it, modules) }.reduce(ValidationResult::plus)
    when (validationResult) {
        is ValidationResult.Valid -> {
            println("Modules valid!")
        }
        is ValidationResult.Invalid -> {
            errorln("Module validation failed!")
            errorln("Validation errors:")
            for ((module, errors) in validationResult.errors.groupBy { it.module }) {
                errorln("    Module: $module")
                for (error in errors) {
                    when (error) {
                        is ValidationError.DuplicatedModule -> {
                            errorln("        Module has been defined in multiple files:")
                            for (sourceFile in error.sourceFiles) {
                                errorln("            - $sourceFile")
                            }
                        }

                        is ValidationError.DuplicatedDefinition -> {
                            errorln("        There are multiple definitions named ${error.name}: ${error.indices.joinToString(", ")}")
                        }

                        is ValidationError.InvalidModuleName -> {
                            errorln("        The module name is invalid: ${error.reason}")
                        }

                        is ValidationError.UndefinedModuleReferenced -> {
                            errorln("        An undefined module is referenced from definition ${error.definitionName}: ${error.referencedModule}")
                        }
                        is ValidationError.UndefinedDefinitionReferenced -> {
                            errorln("        An undefined definition is referenced from definition ${error.definitionName}: ${error.referencedDefinition} from ${error.referencedModule}")
                        }
                    }
                }
            }
            exitProcess(1)
        }
    }

    for ((name, module) in modules) {
        println("Module $name:")
        println("    $module")
        println()
    }

    println("Subject: ${subjectModule.metadata.name}")
}