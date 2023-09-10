package tel.schich.idl.runner.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.convert
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import tel.schich.idl.core.ANNOTATION_NAMESPACE_SEPARATOR
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.GenerationResult
import java.nio.file.Path

internal abstract class GenerateCommand(name: String) : CliktCommand(name = name) {
    private val moduleSources: List<Path> by moduleSourcesOption()
    private val annotations: List<Pair<String, String>> by option("--annotation", "-a").convert {
        val equalsPosition = it.indexOf(char = '=')
        if (equalsPosition == -1) {
            Pair(it, "")
        } else {
            Pair(it.substring(0, equalsPosition), it.substring(equalsPosition + 1))
        }
    }.multiple()
    private val subjectReferences by option("--subject").convert { arg ->
        ModuleReference.parse(arg)
            ?: fail("the subject must be a valid module reference of the format module-name:version")
    }.multiple(required = false)
    private val outputPath by option("--output", "-o").path().required()

    protected inline fun withRequest(block: (GenerationRequest) -> GenerationResult): GenerationResult {
        val modules = loadModules(moduleSources)
        validate(modules)

        echo("Modules loaded:")
        for (module in modules) {
            echo("  - ${module.reference}")
        }

        val loadedModuleReferences = modules.map { it.reference }.toSet()
        val subjects = if (subjectReferences.isEmpty()) {
            loadedModuleReferences
        } else {
            subjectReferences.toSet().also { selectedSubjects ->
                val unknownSubjects = selectedSubjects - loadedModuleReferences
                if (unknownSubjects.isNotEmpty()) {
                    error("Some subjects have not been loaded:")
                    for (unknownSubject in unknownSubjects) {
                        error(" - $unknownSubject")
                    }
                    exit(1)
                }
            }
        }

        val request = GenerationRequest(
            modules,
            subjects,
            annotations.toMap(),
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