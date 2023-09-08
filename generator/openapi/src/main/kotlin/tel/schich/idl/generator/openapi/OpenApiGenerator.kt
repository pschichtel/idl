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

        val generatedFiles = subjectModules.map { module ->
            fun referenceToModel(ref: ModelReference, overrideMetadata: Metadata? = null): ReferenceSchema {
                val uri = URI(ref.module?.let { relativeOutputFilePath(it) } ?: "")
                val reference = Reference(uri, JsonPointer.fromString("/components/schemas/${ref.name}"))
                return ReferenceSchema(reference, overrideMetadata?.description)
            }

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
                            type = type?.let(::setOf),
                            format = format,
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                        )
                    }
                    is Model.Record -> {
                        SimpleSchema(
                            type = setOf(SchemaType.OBJECT),
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                            required = definition.properties.filter { it.default == null }.map {
                                PropertyName(it.metadata.name)
                            },
                            properties = definition.properties.associate {
                                // TODO the schema needs to be cloned when nullable and possible in other situations
                                Pair(PropertyName(it.metadata.name), referenceToModel(it.model, it.metadata))
                            }
                        )
                    }
                    is Model.HomogenousList -> {
                        SimpleSchema(
                            type = setOf(SchemaType.ARRAY),
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                            items = referenceToModel(definition.itemModel)
                        )
                    }
                    is Model.HomogenousSet -> {
                        SimpleSchema(
                            type = setOf(SchemaType.ARRAY),
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                            uniqueItems = true,
                            items = referenceToModel(definition.itemModel)
                        )
                    }
                    is Model.HomogenousMap -> {
                        SimpleSchema(
                            type = setOf(SchemaType.OBJECT),
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                            additionalProperties = referenceToModel(definition.valueModel)
                        )
                    }
                    is Model.Sum -> {
                        SimpleSchema(
                            oneOf = definition.constructors.map {
                                referenceToModel(it)
                            }
                        )
                    }
                    is Model.TaggedSum -> {
                        TODO()
                    }
                    is Model.Enumeration -> {
                        val (type, format) = primitiveType(definition.dataType, definition.metadata)
                        SimpleSchema(
                            type = type?.let(::setOf),
                            format = format,
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                            enum = definition.entries.map { it.value },
                        )
                    }
                    is Alias -> {
                        referenceToModel(definition.aliasedModel)
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
                                referenceToModel(it)
                            },
                            minItems = size,
                            maxItems = size,
                        )
                    }
                    is Model.Constant -> {
                        val (type, format) = primitiveType(definition.dataType, definition.metadata)
                        SimpleSchema(
                            type = type?.let(::setOf),
                            format = format,
                            description = definition.metadata.description,
                            deprecated = definition.metadata.deprecated,
                            const = definition.value,
                        )
                    }
                    is Model.RawStream -> TODO()
                }
                Pair(SchemaName(schemaName), schema)
            }
            val components = Components(
                schemas = schemas,
            )
            val spec = OpenApiSpec(
                openapi = "3.1.0",
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

            outputPath
        }

        return GenerationResult.Success(generatedFiles)
    }
}