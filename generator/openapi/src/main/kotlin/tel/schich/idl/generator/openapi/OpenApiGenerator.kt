package tel.schich.idl.generator.openapi

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
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
import tel.schich.idl.core.TaggedSumConstructor
import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.GenerationResult
import tel.schich.idl.core.generate.InvalidModuleException
import tel.schich.idl.core.generate.getAnnotation
import tel.schich.idl.core.generate.invalidModule
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.core.resolveForeignProperties
import tel.schich.idl.core.resolveModelReference
import tel.schich.idl.core.validation.GeneratorValidationError
import tel.schich.idl.core.valueAsString
import tel.schich.idl.core.valueFromJson
import tel.schich.idl.runner.command.JvmInProcessGenerator
import java.io.File
import java.math.BigDecimal
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories

class OpenApiAnnotation<T : Any>(name: String, parser: AnnotationParser<T>) :
    Annotation<T>(namespace = "tel.schich.idl.generator.openapi", name, parser)

@Serializable
enum class TaggedSumEncoding {
    RECORD_PROPERTY,
    WRAPPER_RECORD,
    WRAPPER_TUPLE,
}

val SpecVersionAnnotation = OpenApiAnnotation(name = "spec-version", ::valueAsString)
val SpecLicenseAnnotation = OpenApiAnnotation(name = "license", valueFromJson<License>())
val SpecServersAnnotation = OpenApiAnnotation(name = "servers", valueFromJson<List<Server>>())
val SpecContactAnnotation = OpenApiAnnotation(name = "contact", valueFromJson<Contact>())

val SchemaNameAnnotation = OpenApiAnnotation(name = "schema-name", ::valueAsString)
val PrimitiveFormatAnnotation = OpenApiAnnotation(name = "primitive-format", ::valueAsString)
val TaggedSumEncodingAnnotation = OpenApiAnnotation(name = "tagged-sum-encoding", valueFromJson<TaggedSumEncoding>())
val TaggedSumTagFieldNameAnnotation = OpenApiAnnotation(name = "tagged-sum-tag-field", valueFromJson<PropertyName>())
val TaggedSumValueFieldNameAnnotation = OpenApiAnnotation(name = "tagged-sum-value-field", valueFromJson<PropertyName>())

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
        val (schemaType, defaultFormat) = when (dataType) {
            is PrimitiveDataType.Integer -> {
                val size = dataType.size
                when {
                    size != null && size <= 32u -> Pair(SchemaType.INTEGER, "int32")
                    size != null && size <= 64u -> Pair(SchemaType.INTEGER, "int64")
                    else -> Pair(SchemaType.INTEGER, null)
                }
            }
            is PrimitiveDataType.Float -> {
                val size = dataType.size
                when {
                    size != null && size <= 32u -> Pair(SchemaType.NUMBER, "float")
                    size != null && size <= 64u -> Pair(SchemaType.NUMBER, "double")
                    else -> Pair(SchemaType.NUMBER, null)
                }
            }
            is PrimitiveDataType.String -> Pair(SchemaType.STRING, null)
            is PrimitiveDataType.Bool -> Pair(SchemaType.BOOLEAN, null)
            is PrimitiveDataType.Custom -> Pair(null, null)
        }
        val format = (metadata.getAnnotation(PrimitiveFormatAnnotation) ?: defaultFormat)?.let(::TypeFormat)
        return Pair(schemaType, format)
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
                    oneOf = listOf(schema, nullSchema()),
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
                val outputFilePath = relativeOutputFilePath(moduleRef, commonNamePrefix)
                val parentPath = subjectPath.parent
                val path = if (parentPath == null) {
                    outputFilePath
                } else {
                    parentPath.relativize(outputFilePath)
                }
                URI(path.toString())
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
            // if validation has passed then this is safe (no dead refs, no duplicates).
            return resolveModelReference(module, modules, ref)!!
        }

        fun generateRequiredProperties(properties: List<RecordProperty>): List<PropertyName> {
            return properties.filter { it.default == null }.map {
                PropertyName(it.metadata.name)
            }
        }

        fun generateProperties(properties: List<Pair<Module, RecordProperty>>): Map<PropertyName, Schema> {
            return properties.associate { (module, property) ->
                val (referencedModule, referencedDefinition) = resolveModelReference(module, modules, property.model)!!

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
                when (val dataType = definition.dataType) {
                    is PrimitiveDataType.Integer -> {
                        val minimum = dataType.range?.minimum?.toBigDecimal() ?: (if (dataType.signed) null else BigDecimal.ZERO)
                        val maximum = dataType.range?.maximum?.toBigDecimal()
                        SimpleSchema(
                            type = type?.let(::typeWithNull),
                            format = format,
                            description = description,
                            deprecated = deprecated,
                            example = examples.firstOrNull(),
                            examples = examples,
                            default = withDefault,
                            minimum = minimum,
                            maximum = maximum,
                        )
                    }
                    is PrimitiveDataType.Float -> {
                        val minimum = dataType.range?.minimum ?: (if (dataType.signed) null else BigDecimal.ZERO)
                        val maximum = dataType.range?.maximum
                        SimpleSchema(
                            type = type?.let(::typeWithNull),
                            format = format,
                            description = description,
                            deprecated = deprecated,
                            example = examples.firstOrNull(),
                            examples = examples,
                            default = withDefault,
                            minimum = minimum,
                            maximum = maximum,
                        )
                    }
                    is PrimitiveDataType.String -> {
                        val minLength = dataType.lengthRange?.minimum
                        val maxLength = dataType.lengthRange?.maximum
                        val pattern = dataType.regex?.toString()?.let(::RegularExpression)
                        SimpleSchema(
                            type = type?.let(::typeWithNull),
                            format = format,
                            description = description,
                            deprecated = deprecated,
                            example = examples.firstOrNull(),
                            examples = examples,
                            default = withDefault,
                            minLength = minLength,
                            maxLength = maxLength,
                            pattern = pattern,
                        )
                    }
                    is PrimitiveDataType.Bool -> {
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
                    is PrimitiveDataType.Custom -> {
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
                }
            }
            is Model.Record -> {
                val foreignProperties = resolveForeignProperties(module, definition, modules)
                val properties = foreignProperties + definition.properties.map { Pair(module, it) }
                SimpleSchema(
                    type = typeWithNull(SchemaType.OBJECT),
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
                    default = withDefault,
                    required = generateRequiredProperties(properties.map { it.second }),
                    properties = generateProperties(properties)
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
                    uniqueItems = definition.uniqueValues,
                    minItems = definition.sizeRange?.minimum,
                    maxItems = definition.sizeRange?.maximum,
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
                    minItems = definition.sizeRange?.minimum,
                    maxItems = definition.sizeRange?.maximum,
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
                    minProperties = definition.sizeRange?.minimum,
                    maxProperties = definition.sizeRange?.maximum,
                    additionalProperties = referenceToModel(module, definition.valueModel)
                )
            }
            is Model.Sum -> {
                val nullSchema = if (asNullable) {
                    listOf(nullSchema())
                } else {
                    emptyList()
                }
                SimpleSchema(
                    description = description,
                    deprecated = deprecated,
                    example = examples.firstOrNull(),
                    examples = examples,
                    oneOf = definition.constructors.map {
                        referenceToModel(module, it.model)
                    } + nullSchema,
                )
            }
            is Model.TaggedSum -> {
                val encoding = definition.metadata.getAnnotation(TaggedSumEncodingAnnotation) ?: TaggedSumEncoding.WRAPPER_TUPLE
                val tagFieldName = definition.metadata.getAnnotation(TaggedSumTagFieldNameAnnotation) ?: PropertyName("type")
                val valueFieldName = definition.metadata.getAnnotation(TaggedSumValueFieldNameAnnotation) ?: PropertyName("value")

                val nullSchema = if (asNullable) {
                    listOf(nullSchema())
                } else {
                    emptyList()
                }

                fun createTagConstant(constructor: TaggedSumConstructor): Schema {
                    val (type, format) = primitiveType(definition.tagDataType, definition.metadata)
                    return SimpleSchema(type = type?.let(::setOf), format = format, const = constructor.tag.tag)
                }

                val schemas = when (encoding) {
                    TaggedSumEncoding.RECORD_PROPERTY -> {
                        definition.constructors.map {
                            val (_, referencedDefinition) = lookupDefinition(it.model)
                            if (referencedDefinition !is Model.Record) {
                                invalidModule(subject.reference, "The ${TaggedSumEncoding.RECORD_PROPERTY} encoding requires all constructors to be records!")
                            }
                            if (referencedDefinition.properties.any { property -> property.metadata.name == tagFieldName.name }) {
                                invalidModule(subject.reference, "The ${TaggedSumEncoding.RECORD_PROPERTY} requires a tag field name that does not exist in any of its constructors, $tagFieldName already exists in ${it.metadata.name}!")
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
                    listOf(nullSchema())
                } else {
                    emptyList()
                }

                val schemas = definition.constructors.map { record ->
                    val typePropertyName = PropertyName(definition.typeProperty)
                    val typeSchema = SimpleSchema(type = setOf(SchemaType.STRING), const = JsonPrimitive(record.metadata.name))
                    val properties = (definition.commonProperties + record.properties).map { Pair(module, it) }
                    SimpleSchema(
                        type = setOf(SchemaType.OBJECT),
                        description = description,
                        deprecated = deprecated,
                        required = listOf(typePropertyName) + generateRequiredProperties(properties.map { it.second }),
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

    private fun generateModel(request: GenerationRequest): List<Path> {
        val defaultSpecVersion = request.getAnnotation(SpecVersionAnnotation)

        val modules = request.modules
        if (modules.isEmpty()) {
            error("No modules have been requested!")
        }

        val subjectModules = modules.filter { it.reference in request.subjects }

        val commonNamePrefix = determineCommonPrefix(modules)
        println(commonNamePrefix)

        val generatedFiles = subjectModules.map { module ->
            val contact = module.metadata.getAnnotation(SpecContactAnnotation)
                ?: request.getAnnotation(SpecContactAnnotation)
            val info = Info(
                title = module.metadata.name,
                version = module.metadata.getAnnotation(SpecVersionAnnotation) ?: defaultSpecVersion,
                summary = module.metadata.summary,
                description = module.metadata.description,
                license = module.metadata.getAnnotation(SpecLicenseAnnotation),
                contact = contact,
            )

            val schemas: Map<SchemaName, Schema> = module.definitions.associate { definition ->
                val schemaName = definition.metadata.getAnnotation(SchemaNameAnnotation) ?: definition.metadata.name
                val schema = try {
                    generateSchema(
                        module,
                        definition,
                        module,
                        modules,
                        commonNamePrefix,
                        asNullable = false,
                        withDefault = null,
                        overrideMetadata = null,
                    )
                } catch (e: Exception) {
                    throw Exception("Failed to generate schema for module ${module.reference}", e)
                }
                Pair(SchemaName(schemaName), schema)
            }
            val components = Components(
                schemas = schemas,
            )
            val servers = module.metadata.getAnnotation(SpecServersAnnotation)
                ?: request.getAnnotation(SpecServersAnnotation)
                ?: emptyList()
            val spec = OpenApiSpec(
                openapi = "3.1.0",
                info = info,
                servers = servers,
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

        return generatedFiles
    }

    override fun generate(request: GenerationRequest): GenerationResult {
        return try {
            GenerationResult.Success(generateModel(request))
        } catch (e: InvalidModuleException) {
            GenerationResult.Invalid(listOf(GeneratorValidationError(e.module, e.reason)))
        } catch (e: Exception) {
            e.printStackTrace(System.err)
            GenerationResult.Failure(e.message ?: "unknown reason")
        }
    }
}