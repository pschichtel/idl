package tel.schich.idl.runner.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import tel.schich.idl.core.Annotations
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.GenerationResult
import tel.schich.idl.runner.generate.JvmInProcessGenerator
import tel.schich.idl.runner.generate.UnknownSubjectsException
import tel.schich.idl.runner.generate.loadInputs
import tel.schich.idl.runner.generate.resolveSubjects
import java.nio.file.Path

private fun mergeAnnotations(all: List<Annotations>): Annotations {
    return all.fold(emptyMap()) { out, current -> out + current }
}

internal abstract class GenerateCommand(name: String) : CliktCommand(name = name) {
    private val moduleSources: List<Path> by moduleSourcesOption()
    private val annotations: List<Path> by option("--annotations", "-a").path(mustExist = true).multiple()
    private val subjectReferences by option("--subject").convert { arg ->
        ModuleReference.parse(arg)
            ?: fail("the subject must be a valid module reference of the format module-name:version")
    }.multiple(required = false)
    private val outputPath by option("--output", "-o").path().required()

    protected inline fun withRequest(block: (GenerationRequest) -> GenerationResult): GenerationResult {
        val resolvedModulePaths = resolveModulePaths(moduleSources)
        val inputs = loadInputs(resolvedModulePaths, annotations)
        if (inputs.modules.isEmpty()) {
            error("No modules have been loaded!")
            return GenerationResult.Failure("No modules have been loaded!")
        }

        validate(inputs.modules)

        echo("Modules loaded:")
        for (module in inputs.modules) {
            echo("  - ${module.reference}")
        }

        val subjects = try {
            resolveSubjects(inputs.modules, subjectReferences)
        } catch (e: UnknownSubjectsException) {
            error("Some subjects have not been loaded:")
            for (unknownSubject in e.unknownSubjects) {
                error(" - $unknownSubject")
            }
            exit(1)
        }

        val request = GenerationRequest(
            inputs.modules,
            subjects,
            inputs.annotations,
            outputPath,
        )
        return block(request)
    }
}

internal class InProcessGenerate : GenerateCommand(name = "generate-in-process") {
    private val generator by argument("class").convert { className ->
        Class.forName(className).getConstructor().newInstance() as JvmInProcessGenerator
    }

    override fun run() {
        val result = withRequest { generationRequest ->
            generator.generate(generationRequest)
        }

        when (result) {
            is GenerationResult.Failure -> {
                error("Generation failed: ${result.reason}")
            }
            is GenerationResult.Invalid -> {
                error("The generator rejected the model for these reasons:")
                for (error in result.errors) {
                    error(" - in module ${error.module}: ${error.description}")
                }
            }
            is GenerationResult.Success -> {
                echo("Successfully generated the following files:")
                for (generatedFile in result.generatedFiles) {
                    echo(" - $generatedFile")
                }
            }
        }
    }
}