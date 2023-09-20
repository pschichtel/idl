package tel.schich.idl.generator.kotlin

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.core.Alias
import tel.schich.idl.core.Annotation
import tel.schich.idl.core.AnnotationParser
import tel.schich.idl.core.Definition
import tel.schich.idl.core.Metadata
import tel.schich.idl.core.Model
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
import tel.schich.idl.core.valueAsIs
import tel.schich.idl.runner.command.JvmInProcessGenerator
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createDirectories

class KotlinAnnotation<T : Any>(name: String, parser: AnnotationParser<T>) :
    Annotation<T>(namespace = "tel.schich.idl.generator.kotlin", name, parser)

enum class TaggedSumEncoding {
    RECORD_PROPERTY,
    WRAPPER_RECORD,
}

enum class SerializationLibary {
    KOTLINX_SERIALIZATION,
}

val SerializationLibraryAnnotation = KotlinAnnotation("serialization-library") {
    when (it.lowercase()) {
        "kotlinx.serialization" -> SerializationLibary.KOTLINX_SERIALIZATION
        else -> error("Unknown serialization-library value $it")
    }
}
val PackageAnnotation = KotlinAnnotation("package", ::valueAsIs)
val FileNameAnnotation = KotlinAnnotation("file-name", ::valueAsIs)
val SymbolNameAnnotation = KotlinAnnotation("symbol-name", ::valueAsIs)
val ValueFieldNameAnnotation = KotlinAnnotation("value-field-name", ::valueAsIs)
val DiscriminatorFieldNameAnnotation = KotlinAnnotation(name = "discriminator-field", ::valueAsIs)
val DiscriminatorValueAnnotation = KotlinAnnotation(name = "discriminator-value", ::valueAsIs)
val TaggedSumEncodingAnnotation = KotlinAnnotation(name = "tagged-sum-encoding", TaggedSumEncoding::valueOf)
val RepresentAsAnnotation = KotlinAnnotation(name = "represent-as", ::valueAsIs)
val NewTypeAnnotation = KotlinAnnotation(name = "new-type", ::valueAsBoolean)

class KotlinGenerator : JvmInProcessGenerator {

    private fun derivePackageName(moduleRef: ModuleReference): String {
        return idiomaticPackageName(moduleRef.name)
    }

    private fun getPackage(module: Module): String {
        return module.metadata.getAnnotation(PackageAnnotation)
            ?: derivePackageName(module.reference)
    }

    private fun definitionName(metadata: Metadata): String {
        return metadata.getAnnotation(SymbolNameAnnotation) ?: idiomaticClassName(metadata.name)
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

    private fun FileBuilder.serializableAnnotation(serializationLibrary: SerializationLibary?) {
        if (serializationLibrary == SerializationLibary.KOTLINX_SERIALIZATION) {
            line {
                annotation("kotlinx.serialization.Serializable")
            }
        }
    }

    private fun FileBuilder.jsonClassDiscriminatorAnnotation(serializationLibrary: SerializationLibary?, discriminatorFieldName: String) {
        if (serializationLibrary == SerializationLibary.KOTLINX_SERIALIZATION && discriminatorFieldName != "type") {
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

    private fun FileBuilder.serialNameAnnotation(serializationLibrary: SerializationLibary?, metadata: Metadata, value: String? = null) {
        if (serializationLibrary == SerializationLibary.KOTLINX_SERIALIZATION) {
            val serialName = metadata.getAnnotation(DiscriminatorValueAnnotation) ?: value ?: metadata.name
            line {
                annotation("kotlinx.serialization.SerialName")
                append("(\"${serialName}\")")
            }
        }
    }

    private fun FileBuilder.contextualAnnotation(serializationLibrary: SerializationLibary?, referencedDefinition: Definition) {
        if (serializationLibrary == SerializationLibary.KOTLINX_SERIALIZATION && (referencedDefinition is Model.Sum || referencedDefinition is Model.TaggedSum)) {
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

        val generatedFiles = subjectModules.flatMap { subjectModule ->
            val packageName = getPackage(subjectModule)
            val modulePath = request.outputPath.resolve(packageName.replace('.', File.separatorChar))
            modulePath.createDirectories()

            subjectModule.definitions.map { definition ->
                val name = definitionName(definition.metadata)
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
                    when (definition) {
                        is Model.Primitive -> {
                            typeWrappingDefinition(kotlinTypeFromDataType(definition.dataType))
                        }
                        is Model.HomogenousList -> {
                            val (referencedModule, referencedDefinition) = resolveModelReference(subjectModule, modules, definition.itemModel)!!
                            val type = getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata)

                            collectionDefinition(defaultCollectionType = "kotlin.collections.List", listOf(type))
                        }
                        is Model.HomogenousSet -> {
                            val (referencedModule, referencedDefinition) = resolveModelReference(subjectModule, modules, definition.itemModel)!!
                            val type = getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata)

                            collectionDefinition(defaultCollectionType = "kotlin.collections.Set", listOf(type))
                        }
                        is Model.HomogenousMap -> {
                            val (keyModule, keyDef) = resolveModelReference(subjectModule, modules, definition.keyModel)!!
                            val (valueModule, valueDef) = resolveModelReference(subjectModule, modules, definition.valueModel)!!
                            val keyType = getPackage(keyModule) + "." + definitionName(keyDef.metadata)
                            val valueType = getPackage(valueModule) + "." + definitionName(valueDef.metadata)

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
                            val (referencedModule, referencedDefinition) = resolveModelReference(subjectModule, modules, definition.aliasedModel)!!
                            val type = getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata)
                            typeWrappingDefinition(type)
                        }
                        is Model.Unknown -> {
                            typeWrappingDefinition(type = "kotlin.Any")
                        }
                        is Model.Enumeration -> {
                            val valueType = kotlinTypeFromDataType(definition.dataType)
                            val valueFieldName = valueFieldName(definition.metadata)

                            docs(definition.metadata)
                            serializableAnnotation(serializationLibrary)
                            indent()
                            append("enum class ${symbolName(name)}(")
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
                        is Model.Product -> {
                            docs(definition.metadata)
                            serializableAnnotation(serializationLibrary)
                            line {
                                append("data class ${symbolName(name)}(")
                            }
                            indented {
                                for ((i, component) in definition.components.withIndex()) {
                                    val (referencedModule, referencedDefinition) = resolveModelReference(
                                        subjectModule,
                                        modules,
                                        component
                                    )!!
                                    contextualAnnotation(serializationLibrary, referencedDefinition)
                                    line {
                                        value(
                                            tupleFieldName(i + 1),
                                            getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata)
                                        )
                                        append(",")
                                    }
                                }
                            }
                            line {
                                append(")")
                            }
                        }
                        is Model.Adt -> {
                            docs(definition.metadata)
                            jsonClassDiscriminatorAnnotation(serializationLibrary, discriminatorFieldName(definition.metadata))
                            serializableAnnotation(serializationLibrary)
                            indent()
                            append("sealed interface ${symbolName(name)}")
                            block {
                                if (definition.commonProperties.isNotEmpty()) {
                                    for (property in definition.commonProperties) {
                                        val propertyName = property.metadata.getAnnotation(SymbolNameAnnotation)
                                            ?: idiomaticName(property.metadata.name)
                                        val (referencedModule, referencedDefinition) = resolveModelReference(
                                            subjectModule,
                                            modules,
                                            property.model
                                        )!!
                                        docs(property.metadata)
                                        line {
                                            value(
                                                propertyName,
                                                getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata)
                                            )
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
                                        append("data class ${symbolName(constructorName)}(")
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
                                            docs(property.metadata)
                                            contextualAnnotation(serializationLibrary, referencedDefinition)
                                            line {
                                                value(
                                                    propertyName,
                                                    getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata),
                                                    override = true,
                                                )
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
                                            docs(property.metadata)
                                            contextualAnnotation(serializationLibrary, referencedDefinition)
                                            line {
                                                value(
                                                    propertyName,
                                                    getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata),
                                                )
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
                        is Model.Record -> {
                            docs(definition.metadata)
                            serializableAnnotation(serializationLibrary)
                            // TODO add @SerialName if used in tagged sum and implement interfaces accordingly
                            line {
                                append("data class ${symbolName(name)}(")
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
                                    docs(definition.metadata)
                                    contextualAnnotation(serializationLibrary, referencedDefinition)
                                    line {
                                        value(
                                            propertyName,
                                            getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata)
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
                        is Model.Sum -> {
                            docs(definition.metadata)
                            indent()
                            append("sealed interface ${symbolName(name)}")
                            block {
                                var firstConstructor = true
                                for (constructor in definition.constructors) {
                                    if (!firstConstructor) {
                                        append("\n")
                                    }
                                    firstConstructor = false
                                    val constructorName = constructor.metadata.getAnnotation(SymbolNameAnnotation)
                                        ?: idiomaticClassName(constructor.metadata.name)
                                    val (referencedModule, referencedDefinition) = resolveModelReference(
                                        subjectModule,
                                        modules,
                                        constructor.model
                                    )!!
                                    docs(constructor.metadata)
                                    line {
                                        append("data class ${symbolName(constructorName)}(")
                                        value(
                                            valueFieldName(constructor.metadata),
                                            getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata),
                                        )
                                        append(") : ${symbolName(name)}")
                                    }
                                }
                            }
                        }
                        is Model.TaggedSum -> {
                            val encoding = definition.metadata.getAnnotation(TaggedSumEncodingAnnotation) ?: TaggedSumEncoding.WRAPPER_RECORD
                            val discriminatorFieldName = discriminatorFieldName(definition.metadata)

                            when (encoding) {
                                TaggedSumEncoding.RECORD_PROPERTY -> {
                                    docs(definition.metadata)
                                    serializableAnnotation(serializationLibrary)
                                    jsonClassDiscriminatorAnnotation(serializationLibrary, discriminatorFieldName)
                                    line {
                                        append("sealed interface ${symbolName(name)}")
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
                                    append("sealed interface ${symbolName(name)}")
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
                                                val (referencedModule, referencedDefinition) = resolveModelReference(
                                                    subjectModule,
                                                    modules,
                                                    constructor.model
                                                )!!
                                                value(
                                                    valueFieldName(definition.metadata),
                                                    getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata),
                                                )
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