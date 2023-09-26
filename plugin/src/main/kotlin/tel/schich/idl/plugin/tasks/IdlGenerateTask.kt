package tel.schich.idl.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.listProperty
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.GenerationResult
import tel.schich.idl.runner.generate.JvmInProcessGenerator
import tel.schich.idl.runner.generate.loadInputs
import tel.schich.idl.runner.generate.resolveSubjects
import tel.schich.idl.runner.generate.resolveToFiles
import java.io.File
import java.lang.RuntimeException

abstract class IdlGenerateTask : DefaultTask() {
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    val moduleInputs: ListProperty<File> = project.objects.listProperty()

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    val annotationsInputs: ListProperty<File> = project.objects.listProperty()

    @get:Input
    val subjects: ListProperty<ModuleReference> = project.objects.listProperty()

    @get:OutputDirectory
    val output: Property<Directory> = project.objects.directoryProperty()

    init {
        moduleInputs.convention(listOf(project.projectDir.resolve("specs")))
        annotationsInputs.convention(listOf(project.projectDir.resolve("annotations.yaml")))
        subjects.convention(emptyList())
    }

    protected fun generate(generator: JvmInProcessGenerator) {
        val modulePaths = resolveToFiles(moduleInputs.get().map { it.toPath() })
        val annotationPaths = annotationsInputs.get().map { it.toPath() }
        val inputs = loadInputs(modulePaths, annotationPaths)
        // TODO validate inputs
        val subjects = resolveSubjects(inputs.modules, subjects.get())

        val request = GenerationRequest(inputs.modules, subjects, inputs.annotations, output.get().asFile.toPath())
        when (val result = generator.generate(request)) {
            is GenerationResult.Invalid -> {
                throw RuntimeException("Invalid: ${result.errors.joinToString("; ")}")
            }
            is GenerationResult.Failure -> {
                throw RuntimeException("Failed: ${result.reason}")
            }
            is GenerationResult.Success -> {
            }
        }
    }
}