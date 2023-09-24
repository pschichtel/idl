package tel.schich.idl.generator.kotlin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tel.schich.idl.core.Alias
import tel.schich.idl.core.Annotation
import tel.schich.idl.core.AnnotationParser
import tel.schich.idl.core.Definition
import tel.schich.idl.core.Model
import tel.schich.idl.core.Module
import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.GenerationResult
import tel.schich.idl.core.generate.getAnnotation
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.core.valueAsBoolean
import tel.schich.idl.core.valueAsString
import tel.schich.idl.core.valueFromJson
import tel.schich.idl.generator.kotlin.generate.FileBuilder
import tel.schich.idl.generator.kotlin.generate.SimpleFileBuilder
import tel.schich.idl.generator.kotlin.generate.definitionName
import tel.schich.idl.generator.kotlin.generate.generateAdt
import tel.schich.idl.generator.kotlin.generate.generateAlias
import tel.schich.idl.generator.kotlin.generate.generateConstant
import tel.schich.idl.generator.kotlin.generate.generateEnumeration
import tel.schich.idl.generator.kotlin.generate.generateHomogenousList
import tel.schich.idl.generator.kotlin.generate.generateHomogenousMap
import tel.schich.idl.generator.kotlin.generate.generateHomogenousSet
import tel.schich.idl.generator.kotlin.generate.generatePrimitive
import tel.schich.idl.generator.kotlin.generate.generateProduct
import tel.schich.idl.generator.kotlin.generate.generateRecord
import tel.schich.idl.generator.kotlin.generate.generateSum
import tel.schich.idl.generator.kotlin.generate.generateTaggedSum
import tel.schich.idl.generator.kotlin.generate.generateUnknown
import tel.schich.idl.generator.kotlin.generate.getPackage
import tel.schich.idl.generator.kotlin.generate.typeAlias
import tel.schich.idl.runner.command.JvmInProcessGenerator
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

class KotlinAnnotation<T : Any>(name: String, parser: AnnotationParser<T>) :
    Annotation<T>(namespace = "tel.schich.idl.generator.kotlin", name, parser)

@Serializable
enum class TaggedSumEncoding {
    RECORD_PROPERTY,
    WRAPPER_RECORD,
}

@Serializable
enum class SerializationLibrary {
    @SerialName("kotlinx.serialization")
    KOTLINX_SERIALIZATION,

    @SerialName("jackson")
    JACKSON,
}

val SerializationLibraryAnnotation = KotlinAnnotation("serialization-library", valueFromJson<SerializationLibrary>())
val PackageAnnotation = KotlinAnnotation("package", ::valueAsString)
val FileNameAnnotation = KotlinAnnotation("file-name", ::valueAsString)
val SymbolNameAnnotation = KotlinAnnotation("symbol-name", ::valueAsString)
val ValueFieldNameAnnotation = KotlinAnnotation("value-field-name", ::valueAsString)
val DiscriminatorFieldNameAnnotation = KotlinAnnotation(name = "discriminator-field", ::valueAsString)
val DiscriminatorValueAnnotation = KotlinAnnotation(name = "discriminator-value", ::valueAsString)
val TaggedSumEncodingAnnotation = KotlinAnnotation(name = "tagged-sum-encoding", valueFromJson<TaggedSumEncoding>())
val RepresentAsAnnotation = KotlinAnnotation(name = "represent-as", ::valueAsString)
val NewTypeAnnotation = KotlinAnnotation(name = "new-type", ::valueAsBoolean)
val ModelNameFormatAnnotation = KotlinAnnotation(name = "model-name-format", ::valueAsString)

data class KotlinGeneratorContext<T : Definition>(
    val request: GenerationRequest,
    val subjectModule: Module,
    val modules: List<Module>,
    val serializationLibrary: SerializationLibrary?,
    val fileBuilder: FileBuilder,
    val name: String,
    val definition: T,
) : FileBuilder by fileBuilder

class KotlinGenerator : JvmInProcessGenerator {
    private fun generateModule(
        request: GenerationRequest,
        subjectModule: Module,
        modules: List<Module>,
        serializationLibrary: SerializationLibrary?
    ): List<Path> {
        val packageName = getPackage(subjectModule)

        return subjectModule.definitions.map { definition ->
            val name = definitionName(request, subjectModule, definition.metadata)
            val fileName = definition.metadata.getAnnotation(FileNameAnnotation) ?: name
            val filePath = request.outputPath
                .resolve(packageName.replace('.', File.separatorChar))
                .resolve("$fileName.kt")

            val builder = SimpleFileBuilder(packageName)
            fun <T : Definition> ctx(def: T): KotlinGeneratorContext<T> = KotlinGeneratorContext(
                request,
                subjectModule,
                modules,
                serializationLibrary,
                builder,
                name,
                def,
            )

            fun unlessOtherwiseRepresented(block: () -> Unit) {
                val representationType = definition.metadata.getAnnotation(RepresentAsAnnotation)
                if (representationType != null) {
                    builder.typeAlias(name, representationType)
                } else {
                    block()
                }
            }

            when (definition) {
                is Model.Primitive -> {
                    ctx(definition).generatePrimitive()
                }
                is Model.HomogenousList -> {
                    ctx(definition).generateHomogenousList()
                }
                is Model.HomogenousSet -> {
                    ctx(definition).generateHomogenousSet()
                }
                is Model.HomogenousMap -> {
                    ctx(definition).generateHomogenousMap()
                }
                is Model.Constant -> {
                    ctx(definition).generateConstant()
                }
                is Alias -> {
                    ctx(definition).generateAlias()
                }
                is Model.Unknown -> {
                    ctx(definition).generateUnknown()
                }
                is Model.Enumeration -> unlessOtherwiseRepresented {
                    ctx(definition).generateEnumeration()
                }
                is Model.Product -> unlessOtherwiseRepresented {
                    ctx(definition).generateProduct()
                }
                is Model.Adt -> unlessOtherwiseRepresented {
                    ctx(definition).generateAdt()
                }
                is Model.Record -> unlessOtherwiseRepresented {
                    ctx(definition).generateRecord()
                }
                is Model.Sum -> unlessOtherwiseRepresented {
                    ctx(definition).generateSum()
                }
                is Model.TaggedSum -> unlessOtherwiseRepresented {
                    ctx(definition).generateTaggedSum()
                }
            }


            filePath.also {
                it.parent.createDirectories()
                Files.newBufferedWriter(it).use(builder::write)
            }
        }
    }

    override fun generate(request: GenerationRequest): GenerationResult {
        val serializationLibrary = request.getAnnotation(SerializationLibraryAnnotation)
        val modules = request.modules
        if (modules.isEmpty()) {
            error("No modules have been requested!")
        }
        val subjectModules = modules.filter { it.reference in request.subjects }

        val generatedFiles = subjectModules.flatMap { subjectModule ->
            generateModule(request, subjectModule, modules, serializationLibrary)
        }

        return GenerationResult.Success(generatedFiles)
    }
}