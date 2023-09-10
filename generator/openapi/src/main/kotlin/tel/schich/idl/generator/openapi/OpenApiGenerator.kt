package tel.schich.idl.generator.openapi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.core.Alias
import tel.schich.idl.core.MODULE_NAME_SEPARATOR
import tel.schich.idl.core.Module
import tel.schich.idl.core.Annotation
import tel.schich.idl.core.AnnotationParser
import tel.schich.idl.core.Definition
import tel.schich.idl.core.Metadata
import tel.schich.idl.core.Model
import tel.schich.idl.core.ModelMetadata
import tel.schich.idl.core.ModelReference
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.PrimitiveDataType
import tel.schich.idl.core.RecordProperty
import tel.schich.idl.core.TaggedConstructor
import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.GenerationResult
import tel.schich.idl.core.generate.getAnnotation
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.core.valueAsIs
import tel.schich.idl.runner.command.JvmInProcessGenerator
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories

class OpenApiAnnotation<T : Any>(name: String, parser: AnnotationParser<T>) :
    Annotation<T>(namespace = "tel.schich.idl.generator.openapi", name, parser)

enum class TaggedSumEncoding {
    RECORD_PROPERTY,
    WRAPPER_RECORD,
    WRAPPER_TUPLE,
}

val SpecVersionAnnotation = OpenApiAnnotation(name = "spec-version", ::valueAsIs)
val SchemaNameAnnotation = OpenApiAnnotation(name = "schema-name", ::valueAsIs)
val PrimitiveFormatAnnotation = OpenApiAnnotation(name = "primitive-format", ::valueAsIs)
val TaggedSumEncodingAnnotation = OpenApiAnnotation(name = "tagged-sum-encoding", TaggedSumEncoding::valueOf)
val TaggedSumTagFieldNameAnnotation = OpenApiAnnotation(name = "tagged-sum-tag-field", ::PropertyName)
val TaggedSumValueFieldNameAnnotation = OpenApiAnnotation(name = "tagged-sum-value-field", ::PropertyName)

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
        val subjectPath = relativeOutputFilePath(subject.reference, commonNamePrefix)

        fun typeWithNull(type: SchemaType): Set<SchemaType> {
            return if (asNullable) {
                setOf(type, SchemaType.NULL)
            } else {
                setOf(type)
            }
        }

        fun oneOfWithNull(schema: Schema): Schema {
            return if (asNullable) {
                SimpleSchema(
                    description = schema.description,
                    deprecated = schema.deprecated,
                    oneOf = listOf(schema, NullSchema),
                )
            } else {
                schema
            }
        }

        fun referenceToModel(definingModule: Module, ref: ModelReference, overrideMetadata: Metadata? = null): ReferenceSchema {
            val moduleRef = ref.module ?: definingModule.reference
            val uri = if (subject.reference == moduleRef) {
                URI("")
            } else {
                URI(subjectPath.parent.relativize(relativeOutputFilePath(moduleRef, commonNamePrefix)).toString())
            }
            val reference = Reference(uri, JsonPointer.fromString("/components/schemas/${ref.name}"))
            val examples = (overrideMetadata as? ModelMetadata)?.examples?.map { it.example }
            return ReferenceSchema(
                reference,
                overrideMetadata?.description,
                overrideMetadata?.deprecated,
                examples?.firstOrNull(),
                examples,
            )
        }

        fun lookupDefinition(ref: ModelReference): Pair<Module, Definition> {
            val referencedModule = ref.module?.let { moduleRef ->
                // if validation has passed then this is safe (no dead refs, no duplicates).
                modules.first { it.reference == moduleRef }
            } ?: module
            // if validation has passed then this is safe (no dead refs, no duplicates).
            val referencedDefinition = referencedModule.definitions.first { it.metadata.name == ref.name }

            return Pair(referencedModule, referencedDefinition)
        }

        fun generateRequiredProperties(properties: List<RecordProperty>): List<PropertyName> {
            return properties.filter { it.default == null }.map {
                PropertyName(it.metadata.name)
            }
        }

        fun generateProperties(properties: List<RecordProperty>): Map<PropertyName, Schema> {
            return properties.associate { property ->
                val (referencedModule, referencedDefinition) = lookupDefinition(property.model)

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
        }

        val description = overrideMetadata?.description ?: definition.metadata.description
        val deprecated = if (overrideMetadata?.deprecated == true || definition.metadata.deprecated) true else null
        val examples = ((overrideMetadata as? ModelMetadata)?.examples ?: definition.metadata.examples)
            .map { it.example }

        return when (definition) {
            is Model.Primitive -> {
                val (type, format) = primitiveType(definition.dataType, definition.metadata)
                SimpleSchema(
                    type = type?.let(::typeWithNull),
                    format = format,
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
                    default = withDefault,
                )
            }
            is Model.Record -> {
                SimpleSchema(
                    type = typeWithNull(SchemaType.OBJECT),
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
                    default = withDefault,
                    required = generateRequiredProperties(definition.properties),
                    properties = generateProperties(definition.properties)
                )
            }
            is Model.HomogenousList -> {
                SimpleSchema(
                    type = typeWithNull(SchemaType.ARRAY),
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
                    default = withDefault,
                    items = referenceToModel(module, definition.itemModel)
                )
            }
            is Model.HomogenousSet -> {
                SimpleSchema(
                    type = typeWithNull(SchemaType.ARRAY),
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
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
                    example = examples.firstOrNull(),
                    examples = examples,
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
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
                    oneOf = definition.constructors.map {
                        referenceToModel(module, it)
                    } + nullSchema,
                )
            }
            is Model.TaggedSum -> {
                val encoding = definition.metadata.getAnnotation(TaggedSumEncodingAnnotation) ?: TaggedSumEncoding.WRAPPER_TUPLE
                val tagFieldName = definition.metadata.getAnnotation(TaggedSumTagFieldNameAnnotation) ?: PropertyName("type")
                val valueFieldName = definition.metadata.getAnnotation(TaggedSumValueFieldNameAnnotation) ?: PropertyName("value")

                val nullSchema = if (asNullable) {
                    listOf(NullSchema)
                } else {
                    emptyList()
                }

                fun createTagConstant(constructor: TaggedConstructor): Schema {
                    val (type, format) = primitiveType(definition.tagDataType, definition.metadata)
                    return SimpleSchema(type = type?.let(::setOf), format = format, const = constructor.tag.tag)
                }

                val schemas = when (encoding) {
                    TaggedSumEncoding.RECORD_PROPERTY -> {
                        definition.constructors.map {
                            val (_, referencedDefinition) = lookupDefinition(it.model)
                            if (referencedDefinition !is Model.Record) {
                                error("The ${TaggedSumEncoding.RECORD_PROPERTY} encoding requires all constructors to be records!")
                            }
                            if (referencedDefinition.properties.any { property -> property.metadata.name == tagFieldName.name }) {
                                error("The ${TaggedSumEncoding.RECORD_PROPERTY} requires a tag field name that does not exist in any of its constructors, $tagFieldName already exists in ${it.metadata.name}!")
                            }
                            SimpleSchema(
                                allOf = listOf(
                                    SimpleSchema(
                                        type = setOf(SchemaType.OBJECT),
                                        description = referencedDefinition.metadata.description,
                                        deprecated = referencedDefinition.metadata.deprecated,
                                        required = listOf(tagFieldName),
                                        properties = mapOf(
                                            tagFieldName to createTagConstant(it),
                                        )
                                    ),
                                    referenceToModel(module, it.model),
                                )
                            )
                        }
                    }
                    TaggedSumEncoding.WRAPPER_RECORD -> {
                        definition.constructors.map {
                            SimpleSchema(
                                type = setOf(SchemaType.OBJECT),
                                required = listOf(tagFieldName, valueFieldName),
                                properties = mapOf(
                                    tagFieldName to createTagConstant(it),
                                    valueFieldName to referenceToModel(module, it.model),
                                ),
                            )
                        }
                    }
                    TaggedSumEncoding.WRAPPER_TUPLE -> {
                        definition.constructors.map {
                            TupleSchema(prefixItems = listOf(createTagConstant(it), referenceToModel(module, it.model)))
                        }
                    }
                }

                SimpleSchema(
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
                    oneOf = schemas + nullSchema,
                )
            }
            is Model.Enumeration -> {
                val (type, format) = primitiveType(definition.dataType, definition.metadata)
                SimpleSchema(
                    type = type?.let(::typeWithNull),
                    format = format,
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
                    default = withDefault,
                    enum = definition.entries.map { it.value },
                )
            }
            is Alias -> oneOfWithNull(
                referenceToModel(module, definition.aliasedModel, definition.metadata)
            )
            is Model.Unknown -> oneOfWithNull(
                SimpleSchema(
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
                    default = withDefault,
                )
            )
            is Model.Product -> {
                TupleSchema(
                    nullable = asNullable,
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
                    default = withDefault,
                    prefixItems = definition.components.map {
                        referenceToModel(module, it)
                    },
                )
            }
            is Model.Constant -> {
                val (type, format) = primitiveType(definition.dataType, definition.metadata)
                SimpleSchema(
                    type = type?.let(::typeWithNull),
                    format = format,
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
                    default = withDefault,
                    const = definition.value,
                )
            }

            is Model.Adt -> {
                val nullSchema = if (asNullable) {
                    listOf(NullSchema)
                } else {
                    emptyList()
                }

                val schemas = definition.constructors.map { record ->
                    val typePropertyName = PropertyName(definition.typeProperty)
                    val typeSchema = SimpleSchema(type = setOf(SchemaType.STRING), const = JsonPrimitive(record.metadata.name))
                    val properties = definition.commonProperties + record.properties
                    SimpleSchema(
                        type = setOf(SchemaType.OBJECT),
                        description = description,
                        deprecated = deprecated,
                        required = listOf(typePropertyName) + generateRequiredProperties(properties),
                        properties = mapOf(typePropertyName to typeSchema) + generateProperties(properties)
                    )
                }
                SimpleSchema(
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
                    oneOf = schemas + nullSchema
                )
            }
        }
    }

    private fun relativeOutputFilePath(module: ModuleReference, commonNamePrefix: String): Path {
        return Paths.get("${module.name.removePrefix(commonNamePrefix).replace(MODULE_NAME_SEPARATOR, File.separatorChar)}.json")
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