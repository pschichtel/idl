package tel.schich.idl.generator.openapi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tel.schich.idl.core.Alias
import tel.schich.idl.core.MODULE_NAME_SEPARATOR
import tel.schich.idl.core.Module
import tel.schich.idl.core.Annotation
import tel.schich.idl.core.AnnotationParser
import tel.schich.idl.core.Metadata
import tel.schich.idl.core.Model
import tel.schich.idl.core.ModelReference
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.PrimitiveDataType
import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.GenerationResult
import tel.schich.idl.core.generate.getAnnotation
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.core.valueAsIs
import tel.schich.idl.runner.command.JvmInProcessGenerator
import java.io.File
import java.net.URI
import java.nio.file.Files
import kotlin.io.path.createDirectories

class OpenApiAnnotation<T : Any>(name: String, parser: AnnotationParser<T>) :
    Annotation<T>(namespace = "tel.schich.idl.generator.openapi", name, parser)

val OpenApiVersionAnnotation = OpenApiAnnotation(name = "openapi-version", ::valueAsIs)
val SpecVersionAnnotation = OpenApiAnnotation(name = "spec-version", ::valueAsIs)
val SchemaNameAnnotation = OpenApiAnnotation(name = "schema-name", ::valueAsIs)
val PrimitiveFormatAnnotation = OpenApiAnnotation(name = "primitive-format", ::valueAsIs)

class OpenApiGenerator : JvmInProcessGenerator {
    @OptIn(ExperimentalSerializationApi::class)
    private val encoder = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
    }

    private fun determineCommonPrefix(modules: List<Module>): String {
        val splitNames = modules.map { it.metadata.name.split(MODULE_NAME_SEPARATOR).dropLast(1) }

        fun commonPrefixLength(a: List<String>, b: List<String>): List<String> {
            val zipped = a.zip(b)
            val length = zipped.withIndex().firstOrNull { it.value.first != it.value.second }?.index ?: zipped.size
            if (a.size == length) {
                return a
            }
            return a.subList(0, length)
        }

        return splitNames.reduce(::commonPrefixLength)
            .joinToString(separator = "$MODULE_NAME_SEPARATOR", postfix = "$MODULE_NAME_SEPARATOR")
    }

    override fun generate(request: GenerationRequest): GenerationResult {
        val defaultOpenApiVersion = request.getAnnotation(OpenApiVersionAnnotation) ?: "3.0.1"
        val defaultSpecVersion = request.getAnnotation(SpecVersionAnnotation)

        val modules = request.modules
        if (modules.isEmpty()) {
            return GenerationResult.Failure(reason = "No modules have been requested!")
        }

        val subjectModules = modules.filter { it.reference in request.subjects }

        val commonNamePrefix = determineCommonPrefix(modules)
        println(commonNamePrefix)

        fun relativeOutputFilePath(module: ModuleReference): String =
            "${module.name.removePrefix(commonNamePrefix).replace(MODULE_NAME_SEPARATOR, File.separatorChar)}.json"

        for (module in subjectModules) {

            fun referenceToModel(ref: ModelReference): Reference {
                val uri = URI(ref.module?.let { relativeOutputFilePath(it) } ?: "")
                return Reference(uri, JsonPointer.fromString("/components/schemas/${ref.name}"))
            }

            val version = module.metadata.getAnnotation(OpenApiVersionAnnotation) ?: defaultOpenApiVersion

            val info = Info(
                title = module.metadata.name,
                version = module.metadata.getAnnotation(SpecVersionAnnotation) ?: defaultSpecVersion,
                summary = module.metadata.summary,
                description = module.metadata.description,
            )

            fun primitiveType(dataType: PrimitiveDataType, metadata: Metadata): Pair<SchemaType?, TypeFormat?> {
                val type = when (dataType.name) {
                    "int32",
                    "int64" -> SchemaType.INTEGER
                    "float32",
                    "float64" -> SchemaType.NUMBER
                    "boolean" -> SchemaType.BOOLEAN
                    "string" -> SchemaType.STRING
                    else -> null
                }
                val format = metadata.getAnnotation(PrimitiveFormatAnnotation)?.let(::TypeFormat)
                return Pair(type, format)
            }

            val schemas: Map<SchemaName, Schema> = module.definitions.associate { definition ->
                val schemaName = definition.metadata.getAnnotation(SchemaNameAnnotation) ?: definition.metadata.name
                val schema: Schema = when (definition) {
                    is Model.Primitive -> {
                        val (type, format) = primitiveType(definition.dataType, definition.metadata)
                        SimpleSchema(
                            type = type,
                            format = format,
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                        )
                    }
                    is Model.Record -> {
                        SimpleSchema(
                            type = SchemaType.OBJECT,
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                            properties = definition.properties.associate {
                                Pair(PropertyName(it.metadata.name), SimpleSchema(ref = referenceToModel(it.model)))
                            }
                        )
                    }
                    is Model.HomogenousList -> {
                        SimpleSchema(
                            type = SchemaType.ARRAY,
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                            items = SimpleSchema(ref = referenceToModel(definition.itemModel))
                        )
                    }
                    is Model.HomogenousSet -> {
                        SimpleSchema(
                            type = SchemaType.ARRAY,
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                            uniqueItems = true,
                            items = SimpleSchema(ref = referenceToModel(definition.itemModel))
                        )
                    }
                    is Model.HomogenousMap -> {
                        SimpleSchema(
                            type = SchemaType.OBJECT,
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                            additionalProperties = SimpleSchema(ref = referenceToModel(definition.valueModel))
                        )
                    }
                    is Model.Sum -> {
                        SimpleSchema(
                            oneOf = definition.constructors.map {
                                SimpleSchema(ref = referenceToModel(it))
                            }
                        )
                    }
                    is Model.Enumeration -> {
                        val (type, format) = primitiveType(definition.dataType, definition.metadata)
                        SimpleSchema(
                            type = type,
                            format = format,
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                            enum = definition.entries.map { it.value },
                        )
                    }
                    is Alias -> {
                        SimpleSchema(ref = referenceToModel(definition.aliasedModel))
                    }
                    is Model.Unknown -> {
                        SimpleSchema(
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                        )
                    }
                    is Model.Product -> {
                        val size = definition.components.size.toBigInteger()
                        TupleSchema(
                            prefixItems = definition.components.map {
                                SimpleSchema(ref = referenceToModel(it))
                            },
                            minItems = size,
                            maxItems = size,
                        )
                    }
                    else -> SimpleSchema.Empty
                }
                Pair(SchemaName(schemaName), schema)
            }
            val components = Components(
                schemas = schemas,
            )
            val spec = OpenApiSpec(
                openapi = version,
                info = info,
                components = components,
            )
            val json = encoder.encodeToString(spec)

            val relativePath = relativeOutputFilePath(module.reference)
            val outputPath = request.outputPath.resolve(relativePath)
            outputPath.parent.createDirectories()
            Files.write(outputPath, json.toByteArray())
            println(relativePath)
            println(outputPath)
            println(json)
        }

        return GenerationResult.Success(emptyList())
    }
}