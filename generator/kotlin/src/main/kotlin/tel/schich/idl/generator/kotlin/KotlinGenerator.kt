package tel.schich.idl.generator.kotlin

import tel.schich.idl.core.Annotation
import tel.schich.idl.core.AnnotationParser
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.GenerationResult
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.core.valueAsIs
import tel.schich.idl.runner.command.JvmInProcessGenerator
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createDirectories

class KotlinAnnotation<T : Any>(name: String, parser: AnnotationParser<T>) :
    Annotation<T>(namespace = "tel.schich.idl.generator.kotlin", name, parser)

val PackageAnnotation = KotlinAnnotation("package", ::valueAsIs)
val FileNameAnnotation = KotlinAnnotation("file-name", ::valueAsIs)

class KotlinGenerator : JvmInProcessGenerator {

    private fun derivePackageName(moduleRef: ModuleReference): String {
        return moduleRef.name.replace("-", "")
    }

    private fun getPackage(module: Module): String {
        return module.metadata.getAnnotation(PackageAnnotation)
            ?: derivePackageName(module.reference)
    }

    override fun generate(request: GenerationRequest): GenerationResult {

        val modules = request.modules
        if (modules.isEmpty()) {
            error("No modules have been requested!")
        }
        val subjectModules = modules.filter { it.reference in request.subjects }

        val generatedFiles = subjectModules.flatMap { subjectModule ->
            val packageName = getPackage(subjectModule)
            val modulePath = request.outputPath.resolve(packageName.replace('.', File.separatorChar))
            modulePath.createDirectories()

            subjectModule.definitions.map { definition ->
                val name = definition.metadata.name
                val fileName = definition.metadata.getAnnotation(FileNameAnnotation) ?: name
                val filePath = modulePath.resolve("$fileName.kt")

                Files.write(filePath, "package $packageName\n\nclass $name() {\n}\n".toByteArray())
                filePath
            }
        }

        return GenerationResult.Success(generatedFiles)
    }
}