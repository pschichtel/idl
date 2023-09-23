package tel.schich.idl.generator.kotlin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.core.Alias
import tel.schich.idl.core.Annotation
import tel.schich.idl.core.AnnotationParser
import tel.schich.idl.core.Definition
import tel.schich.idl.core.Metadata
import tel.schich.idl.core.Model
import tel.schich.idl.core.ModelReference
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.GenerationResult
import tel.schich.idl.core.generate.getAnnotation
import tel.schich.idl.core.generate.invalidModule
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.core.resolveForeignProperties
import tel.schich.idl.core.resolveModelReference
import tel.schich.idl.core.valueAsBoolean
import tel.schich.idl.core.valueAsString
import tel.schich.idl.core.valueFromJson
import tel.schich.idl.runner.command.JvmInProcessGenerator
import java.io.File
import java.nio.file.Files
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

class KotlinGenerator : JvmInProcessGenerator {

    private fun derivePackageName(moduleRef: ModuleReference): String {
        return idiomaticPackageName(moduleRef.name)
    }

    private fun getPackage(module: Module): String {
        return module.metadata.getAnnotation(PackageAnnotation)
            ?: derivePackageName(module.reference)
    }

    private fun valueFieldName(metadata: Metadata): String {
        return metadata.getAnnotation(ValueFieldNameAnnotation) ?: "value"
    }

    private fun discriminatorFieldName(metadata: Metadata): String {
        return metadata.getAnnotation(DiscriminatorFieldNameAnnotation) ?: "type"
    }

    private fun FileBuilder.docs(metadata: Metadata) {
        metadata.description?.let {
            docBlock(it)
        }
    }

    private fun constructInstance(subject: Module, module: Module, definition: Definition, value: JsonElement): String {
        // TODO attempt to construct a valid instance given the JSON value and definition or error out
        return (value as? JsonPrimitive)?.let(::primitiveValue) ?: "null"
    }

    private fun FileBuilder.serializableAnnotation(serializationLibrary: SerializationLibrary?) {
        if (serializationLibrary == SerializationLibrary.KOTLINX_SERIALIZATION) {
            line {
                annotation("kotlinx.serialization.Serializable")
            }
        }
    }

    private fun FileBuilder.jsonClassDiscriminatorAnnotation(serializationLibrary: SerializationLibrary?, discriminatorFieldName: String) {
        if (serializationLibrary == SerializationLibrary.KOTLINX_SERIALIZATION && discriminatorFieldName != "type") {
            line {
                annotation("kotlin.OptIn")
                append("(${useImported("kotlinx.serialization.ExperimentalSerializationApi")}::class)")
            }
            line {
                annotation("kotlinx.serialization.json.JsonClassDiscriminator")
                append("(\"${discriminatorFieldName}\")")
            }
        }
    }

    private fun FileBuilder.serialNameAnnotation(serializationLibrary: SerializationLibrary?, metadata: Metadata, value: String? = null) {
        if (serializationLibrary == SerializationLibrary.KOTLINX_SERIALIZATION) {
            val serialName = metadata.getAnnotation(DiscriminatorValueAnnotation) ?: value ?: metadata.name
            line {
                annotation("kotlinx.serialization.SerialName")
                append("(\"${serialName}\")")
            }
        }
    }

    private fun FileBuilder.contextualAnnotation(serializationLibrary: SerializationLibrary?, referencedDefinition: Definition) {
        if (serializationLibrary == SerializationLibrary.KOTLINX_SERIALIZATION && (referencedDefinition is Model.Sum || referencedDefinition is Model.TaggedSum)) {
            line {
                annotation("kotlinx.serialization.Contextual")
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

        fun definitionName(module: Module, metadata: Metadata): String {
            val format = metadata.getAnnotation(ModelNameFormatAnnotation)
                ?: module.metadata.getAnnotation(ModelNameFormatAnnotation)
                ?: request.getAnnotation(ModelNameFormatAnnotation)
                ?: "%s"
            return metadata.getAnnotation(SymbolNameAnnotation) ?: idiomaticClassName(format.format(metadata.name))
        }

        fun definitionType(module: Module, definition: Definition): String {
            return getPackage(module) + "." + definitionName(module, definition.metadata)
        }

        val generatedFiles = subjectModules.flatMap { subjectModule ->
            val packageName = getPackage(subjectModule)
            val modulePath = request.outputPath.resolve(packageName.replace('.', File.separatorChar))
            modulePath.createDirectories()

            fun definitionType(modelReference: ModelReference): String {
                val (referencedModule, referencedDefinition) = resolveModelReference(subjectModule, modules, modelReference)!!
                return definitionType(referencedModule, referencedDefinition)
            }

            subjectModule.definitions.map { definition ->
                val name = definitionName(subjectModule, definition.metadata)
                val fileName = definition.metadata.getAnnotation(FileNameAnnotation) ?: name
                val filePath = modulePath.resolve("$fileName.kt")

                fun FileBuilder.typeWrappingDefinition(type: String) {
                    val representationType = definition.metadata.getAnnotation(RepresentAsAnnotation) ?: type
                    val newType = definition.metadata.getAnnotation(NewTypeAnnotation) ?: true

                    docs(definition.metadata)
                    if (newType) {
                        serializableAnnotation(serializationLibrary)
                        valueClass(name, valueFieldName(definition.metadata), representationType)
                    } else {
                        typeAlias(name, representationType)
                    }
                }

                fun FileBuilder.collectionDefinition(defaultCollectionType: String, types: List<String>) {
                    val collectionType = definition.metadata.getAnnotation(RepresentAsAnnotation) ?: defaultCollectionType
                    val typeSignature = types.joinToString(", ", transform = ::useImported)
                    val type = "${useImported(collectionType)}<$typeSignature>"
                    val newType = definition.metadata.getAnnotation(NewTypeAnnotation) ?: true

                    docs(definition.metadata)
                    if (newType) {
                        serializableAnnotation(serializationLibrary)
                        valueClass(name, valueFieldName(definition.metadata), type)
                    } else {
                        typeAlias(name, type)
                    }
                }

                val code = buildFile(packageName) {

                    fun unlessOtherwiseRepresented(block: () -> Unit) {
                        val representationType = definition.metadata.getAnnotation(RepresentAsAnnotation)
                        if (representationType != null) {
                            typeAlias(name, representationType)
                        } else {
                            block()
                        }
                    }

                    when (definition) {
                        is Model.Primitive -> {
                            typeWrappingDefinition(kotlinTypeFromDataType(definition.dataType))
                        }
                        is Model.HomogenousList -> {
                            val type = definitionType(definition.itemModel)
                            collectionDefinition(defaultCollectionType = "kotlin.collections.List", listOf(type))
                        }
                        is Model.HomogenousSet -> {
                            val type = definitionType(definition.itemModel)
                            collectionDefinition(defaultCollectionType = "kotlin.collections.Set", listOf(type))
                        }
                        is Model.HomogenousMap -> {
                            val keyType = definitionType(definition.keyModel)
                            val valueType = definitionType(definition.valueModel)
                            collectionDefinition(defaultCollectionType = "kotlin.collections.Map", listOf(keyType, valueType))
                        }
                        is Model.Constant -> {
                            val type = kotlinTypeFromDataType(definition.dataType)
                            docs(definition.metadata)
                            line {
                                value(name, type, const = true)
                                append(" = ${primitiveValue(definition.value)}")
                            }
                        }
                        is Alias -> {
                            typeWrappingDefinition(definitionType(definition.aliasedModel))
                        }
                        is Model.Unknown -> {
                            typeWrappingDefinition(type = "kotlin.Any")
                        }
                        is Model.Enumeration -> unlessOtherwiseRepresented {
                            val valueType = kotlinTypeFromDataType(definition.dataType)
                            val valueFieldName = valueFieldName(definition.metadata)

                            docs(definition.metadata)
                            serializableAnnotation(serializationLibrary)
                            indent()
                            append("enum class ${topLevelSymbolName(name)}(")
                            value(valueFieldName, valueType)
                            append(")")
                            block {
                                for (entry in definition.entries) {
                                    val value = entry.value
                                    val entryName = entry.metadata.getAnnotation(SymbolNameAnnotation)
                                        ?: idiomaticEnumEntryName(entry.metadata.name)
                                    docs(entry.metadata)
                                    val stringValue = if (value.isString) value.content else null
                                    serialNameAnnotation(serializationLibrary, entry.metadata, stringValue)
                                    line {
                                        append("${symbolName(entryName)}(${valueFieldName} = ${primitiveValue(value)}),")
                                    }
                                }
                            }
                        }
                        is Model.Product -> unlessOtherwiseRepresented {
                            docs(definition.metadata)
                            serializableAnnotation(serializationLibrary)
                            line {
                                append("data class ${topLevelSymbolName(name)}(")
                            }
                            indented {
                                for ((i, component) in definition.components.withIndex()) {
                                    val (referencedModule, referencedDefinition) = resolveModelReference(
                                        subjectModule,
                                        modules,
                                        component
                                    )!!
                                    val type = definitionType(referencedModule, referencedDefinition)
                                    contextualAnnotation(serializationLibrary, referencedDefinition)
                                    line {
                                        value(tupleFieldName(i + 1), type)
                                        append(",")
                                    }
                                }
                            }
                            line {
                                append(")")
                            }
                        }
                        is Model.Adt -> unlessOtherwiseRepresented {
                            docs(definition.metadata)
                            jsonClassDiscriminatorAnnotation(serializationLibrary, discriminatorFieldName(definition.metadata))
                            serializableAnnotation(serializationLibrary)
                            indent()
                            append("sealed interface ${topLevelSymbolName(name)}")
                            block {
                                if (definition.commonProperties.isNotEmpty()) {
                                    for (property in definition.commonProperties) {
                                        val propertyName = property.metadata.getAnnotation(SymbolNameAnnotation)
                                            ?: idiomaticName(property.metadata.name)
                                        val type = definitionType(property.model)
                                        docs(property.metadata)
                                        line {
                                            value(propertyName, type)
                                        }
                                    }
                                    append("\n")
                                }
                                var firstConstructor = true
                                for (constructor in definition.constructors) {
                                    if (!firstConstructor) {
                                        append("\n")
                                    }
                                    firstConstructor = false
                                    val constructorName = constructor.metadata.getAnnotation(SymbolNameAnnotation)
                                        ?: idiomaticClassName(constructor.metadata.name)
                                    docs(constructor.metadata)
                                    serializableAnnotation(serializationLibrary)
                                    serialNameAnnotation(serializationLibrary, constructor.metadata)
                                    line {
                                        append("data class ${topLevelSymbolName(constructorName)}(")
                                    }
                                    indented {
                                        for (property in definition.commonProperties) {
                                            val propertyName = property.metadata.getAnnotation(SymbolNameAnnotation)
                                                ?: idiomaticName(property.metadata.name)
                                            val (referencedModule, referencedDefinition) = resolveModelReference(
                                                subjectModule,
                                                modules,
                                                property.model
                                            )!!
                                            val type = definitionType(referencedModule, referencedDefinition)
                                            docs(property.metadata)
                                            contextualAnnotation(serializationLibrary, referencedDefinition)
                                            line {
                                                value(propertyName, type, override = true)
                                                append(",")
                                            }
                                        }
                                        for (property in constructor.properties) {
                                            val propertyName = property.metadata.getAnnotation(SymbolNameAnnotation)
                                                ?: idiomaticName(property.metadata.name)
                                            val (referencedModule, referencedDefinition) = resolveModelReference(
                                                subjectModule,
                                                modules,
                                                property.model
                                            )!!
                                            val type = definitionType(referencedModule, referencedDefinition)
                                            docs(property.metadata)
                                            contextualAnnotation(serializationLibrary, referencedDefinition)
                                            line {
                                                value(propertyName, type)
                                                append(",")
                                            }
                                        }
                                    }
                                    line {
                                        append(") : ${symbolName(name)}")
                                    }
                                }
                            }
                        }
                        is Model.Record -> unlessOtherwiseRepresented {
                            docs(definition.metadata)
                            serializableAnnotation(serializationLibrary)
                            // TODO add @SerialName if used in tagged sum and implement interfaces accordingly
                            line {
                                append("data class ${topLevelSymbolName(name)}(")
                            }
                            val foreignProperties = resolveForeignProperties(subjectModule, definition, modules)
                            val properties = foreignProperties + definition.properties.map { Pair(subjectModule, it) }
                            indented {
                                for ((module, property) in properties) {
                                    val propertyName = property.metadata.getAnnotation(SymbolNameAnnotation)
                                        ?: idiomaticName(property.metadata.name)
                                    val (referencedModule, referencedDefinition) = resolveModelReference(
                                        module,
                                        modules,
                                        property.model
                                    )!!
                                    val type = definitionType(referencedModule, referencedDefinition)
                                    docs(definition.metadata)
                                    contextualAnnotation(serializationLibrary, referencedDefinition)
                                    line {
                                        value(
                                            propertyName,
                                            type
                                        )
                                        if (property.nullable) {
                                            append("?")
                                        }
                                        val default = property.default
                                        if (default != null) {
                                            append(" = ")
                                            append(constructInstance(subjectModule, referencedModule, referencedDefinition, default.value))
                                        }
                                        append(",")
                                    }
                                }
                            }
                            line {
                                append(")")
                            }
                        }
                        is Model.Sum -> unlessOtherwiseRepresented {
                            docs(definition.metadata)
                            indent()
                            append("sealed interface ${topLevelSymbolName(name)}")
                            block {
                                var firstConstructor = true
                                for (constructor in definition.constructors) {
                                    if (!firstConstructor) {
                                        append("\n")
                                    }
                                    firstConstructor = false
                                    val constructorName = constructor.metadata.getAnnotation(SymbolNameAnnotation)
                                        ?: idiomaticClassName(constructor.metadata.name)
                                    val type = definitionType(constructor.model)
                                    docs(constructor.metadata)
                                    line {
                                        append("data class ${symbolName(constructorName)}(")
                                        value(valueFieldName(constructor.metadata), type)
                                        append(") : ${symbolName(name)}")
                                    }
                                }
                            }
                        }
                        is Model.TaggedSum -> unlessOtherwiseRepresented {
                            val encoding = definition.metadata.getAnnotation(TaggedSumEncodingAnnotation) ?: TaggedSumEncoding.WRAPPER_RECORD
                            val discriminatorFieldName = discriminatorFieldName(definition.metadata)

                            when (encoding) {
                                TaggedSumEncoding.RECORD_PROPERTY -> {
                                    docs(definition.metadata)
                                    serializableAnnotation(serializationLibrary)
                                    jsonClassDiscriminatorAnnotation(serializationLibrary, discriminatorFieldName)
                                    line {
                                        append("sealed interface ${topLevelSymbolName(name)}")
                                    }
                                    // just validate here, constructor records will add the interface impl and annotation
                                    for (constructor in definition.constructors) {
                                        val (referencedModule, referencedDefinition) = resolveModelReference(subjectModule, modules, constructor.model)!!
                                        if (referencedModule != subjectModule) {
                                            invalidModule(subjectModule.reference, "The ${TaggedSumEncoding.RECORD_PROPERTY} encoding requires all constructors to be defined in the same module!")
                                        }
                                        if (referencedDefinition !is Model.Record) {
                                            invalidModule(subjectModule.reference, "The ${TaggedSumEncoding.RECORD_PROPERTY} encoding requires all constructors to be records!")
                                        }
                                        if (referencedDefinition.properties.any { property -> property.metadata.name == discriminatorFieldName }) {
                                            invalidModule(subjectModule.reference, "The ${TaggedSumEncoding.RECORD_PROPERTY} requires a discriminator field name that does not exist in any of its constructors, $discriminatorFieldName already exists in ${constructor.metadata.name}!")
                                        }
                                    }
                                    append("\n")
                                }
                                TaggedSumEncoding.WRAPPER_RECORD -> {
                                    docs(definition.metadata)
                                    indent()
                                    append("sealed interface ${topLevelSymbolName(name)}")
                                    block {
                                        var firstConstructor = true
                                        for (constructor in definition.constructors) {
                                            if (!firstConstructor) {
                                                append("\n")
                                            }
                                            firstConstructor = false
                                            val constructorName = constructor.metadata.getAnnotation(SymbolNameAnnotation)
                                                ?: idiomaticClassName(constructor.metadata.name)
                                            docs(constructor.metadata)
                                            line {
                                                append("data class ${symbolName(constructorName)}(")
                                                val type = definitionType(constructor.model)
                                                value(valueFieldName(definition.metadata), type)
                                                append(") : ${symbolName(name)}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Files.write(filePath, code.toByteArray())

                filePath
            }
        }

        return GenerationResult.Success(generatedFiles)
    }
}