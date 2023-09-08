package tel.schich.idl.generator.openapi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import tel.schich.idl.core.Alias
import tel.schich.idl.core.MODULE_NAME_SEPARATOR
import tel.schich.idl.core.Module
import tel.schich.idl.core.Annotation
import tel.schich.idl.core.AnnotationParser
import tel.schich.idl.core.Definition
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

    private fun primitiveType(dataType: PrimitiveDataType, metadata: Metadata): Pair<SchemaType?, TypeFormat?> {
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

    private fun generateSchema(
        subject: Module,
        definition: Definition,
        module: Module,
        modules: List<Module>,
        commonNamePrefix: String,
        asNullable: Boolean,
        withDefault: JsonElement?,
        overrideMetadata: Metadata?,
    ): Schema {
        fun typeWithNull(type: SchemaType): Set<SchemaType> {
            return if (asNullable) {
                setOf(type, SchemaType.NULL)
            } else {
                setOf(type)
            }
        }

        fun oneOfWithNull(schema: Schema): Schema {
            return if (asNullable) {
                SimpleSchema(oneOf = listOf(schema, NullSchema))
            } else {
                schema
            }
        }

        fun referenceToModel(definingModule: Module, ref: ModelReference, overrideMetadata: Metadata? = null): ReferenceSchema {
            val moduleRef = ref.module ?: definingModule.reference
            val uri = if (subject.reference == moduleRef) {
                URI("")
            } else {
                URI(relativeOutputFilePath(moduleRef, commonNamePrefix))
            }
            val reference = Reference(uri, JsonPointer.fromString("/components/schemas/${ref.name}"))
            return ReferenceSchema(reference, overrideMetadata?.description)
        }

        val description = overrideMetadata?.description ?: definition.metadata.description
        val deprecated = if (overrideMetadata?.deprecated == true || definition.metadata.deprecated) true else null

        return when (definition) {
            is Model.Primitive -> {
                val (type, format) = primitiveType(definition.dataType, definition.metadata)
                SimpleSchema(
                    type = type?.let(::typeWithNull),
                    format = format,
                    description = description,
                    deprecated = deprecated,
                    default = withDefault,
                )
            }
            is Model.Record -> {
                SimpleSchema(
                    type = typeWithNull(SchemaType.OBJECT),
                    description = description,
                    deprecated = deprecated,
                    default = withDefault,
                    required = definition.properties.filter { it.default == null }.map {
                        PropertyName(it.metadata.name)
                    },
                    properties = definition.properties.associate { property ->
                        val referencedModule = property.model.module?.let { moduleRef ->
                            // if validation has passed then this is safe (no dead refs, no duplicates).
                            modules.first { it.reference == moduleRef }
                        } ?: module
                        // if validation has passed then this is safe (no dead refs, no duplicates).
                        val referencedDefinition = referencedModule.definitions.first { it.metadata.name == property.model.name }

                        val isCloneRequired = property.nullable
                                || property.default != null
                                || property.metadata.deprecated != referencedDefinition.metadata.deprecated
                        val schema = if (isCloneRequired) {
                            generateSchema(
                                subject,
                                referencedDefinition,
                                referencedModule,
                                modules,
                                commonNamePrefix,
                                asNullable = property.nullable,
                                withDefault = property.default?.value,
                                property.metadata,
                            )
                        } else {
                            referenceToModel(
                                module,
                                property.model,
                                property.metadata,
                            )
                        }
                        Pair(PropertyName(property.metadata.name), schema)
                    }
                )
            }
            is Model.HomogenousList -> {
                SimpleSchema(
                    type = typeWithNull(SchemaType.ARRAY),
                    description = description,
                    deprecated = deprecated,
                    default = withDefault,
                    items = referenceToModel(module, definition.itemModel)
                )
            }
            is Model.HomogenousSet -> {
                SimpleSchema(
                    type = typeWithNull(SchemaType.ARRAY),
                    description = description,
                    deprecated = deprecated,
                    default = withDefault,
                    uniqueItems = true,
                    items = referenceToModel(module, definition.itemModel)
                )
            }
            is Model.HomogenousMap -> {
                SimpleSchema(
                    type = typeWithNull(SchemaType.OBJECT),
                    description = description,
                    deprecated = deprecated,
                    default = withDefault,
                    additionalProperties = referenceToModel(module, definition.valueModel)
                )
            }
            is Model.Sum -> {
                val nullSchema = if (asNullable) {
                    listOf(NullSchema)
                } else {
                    emptyList()
                }
                SimpleSchema(
                    oneOf = definition.constructors.map {
                        referenceToModel(module, it)
                    } + nullSchema,
                )
            }
            is Model.TaggedSum -> {
                TODO()
            }
            is Model.Enumeration -> {
                val (type, format) = primitiveType(definition.dataType, definition.metadata)
                SimpleSchema(
                    type = type?.let(::typeWithNull),
                    format = format,
                    description = description,
                    deprecated = deprecated,
                    default = withDefault,
                    enum = definition.entries.map { it.value },
                )
            }
            is Alias -> oneOfWithNull(
                referenceToModel(module, definition.aliasedModel)
            )
            is Model.Unknown -> oneOfWithNull(
                SimpleSchema(
                    description = description,
                    deprecated = deprecated,
                    default = withDefault,
                )
            )
            is Model.Product -> {
                val size = definition.components.size.toBigInteger()
                TupleSchema(
                    nullable = asNullable,
                    description = description,
                    deprecated = deprecated,
                    default = withDefault,
                    prefixItems = definition.components.map {
                        referenceToModel(module, it)
                    },
                    minItems = size,
                    maxItems = size,
                )
            }
            is Model.Constant -> {
                val (type, format) = primitiveType(definition.dataType, definition.metadata)
                SimpleSchema(
                    type = type?.let(::typeWithNull),
                    format = format,
                    description = description,
                    deprecated = deprecated,
                    default = withDefault,
                    const = definition.value,
                )
            }
        }
    }

    private fun relativeOutputFilePath(module: ModuleReference, commonNamePrefix: String): String =
        "${module.name.removePrefix(commonNamePrefix).replace(MODULE_NAME_SEPARATOR, File.separatorChar)}.json"

    override fun generate(request: GenerationRequest): GenerationResult {
        val defaultSpecVersion = request.getAnnotation(SpecVersionAnnotation)

        val modules = request.modules
        if (modules.isEmpty()) {
            return GenerationResult.Failure(reason = "No modules have been requested!")
        }

        val subjectModules = modules.filter { it.reference in request.subjects }

        val commonNamePrefix = determineCommonPrefix(modules)
        println(commonNamePrefix)

        val generatedFiles = subjectModules.map { module ->

            val info = Info(
                title = module.metadata.name,
                version = module.metadata.getAnnotation(SpecVersionAnnotation) ?: defaultSpecVersion,
                summary = module.metadata.summary,
                description = module.metadata.description,
            )

            val schemas: Map<SchemaName, Schema> = module.definitions.associate { definition ->
                val schemaName = definition.metadata.getAnnotation(SchemaNameAnnotation) ?: definition.metadata.name
                val schema = generateSchema(
                    module,
                    definition,
                    module,
                    modules,
                    commonNamePrefix,
                    asNullable = false,
                    withDefault = null,
                    overrideMetadata = null,
                )
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

            val relativePath = relativeOutputFilePath(module.reference, commonNamePrefix)
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