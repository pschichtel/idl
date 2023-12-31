package tel.schich.idl.generator.kotlin.generate

import kotlinx.serialization.Serializable
import tel.schich.idl.core.Metadata
import tel.schich.idl.core.Model
import tel.schich.idl.core.generate.invalidModule
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.core.resolveModelReference
import tel.schich.idl.core.valueFromJson
import tel.schich.idl.generator.kotlin.DiscriminatorFieldNameAnnotation
import tel.schich.idl.generator.kotlin.KotlinAnnotation
import tel.schich.idl.generator.kotlin.KotlinGeneratorContext
import tel.schich.idl.generator.kotlin.SymbolNameAnnotation
import tel.schich.idl.generator.kotlin.generate.library.SubTypeInfo
import tel.schich.idl.generator.kotlin.generate.library.contextualAnnotation
import tel.schich.idl.generator.kotlin.generate.library.jsonClassDiscriminatorAnnotation
import tel.schich.idl.generator.kotlin.generate.library.jsonTypeInfoAnnotation
import tel.schich.idl.generator.kotlin.generate.library.serialNameAnnotation
import tel.schich.idl.generator.kotlin.generate.library.serializableAnnotation

@Serializable
enum class TaggedSumEncoding {
    RECORD_PROPERTY,
    WRAPPER_RECORD,
}

private val TaggedSumEncodingAnnotation = KotlinAnnotation(name = "tagged-sum-encoding", valueFromJson<TaggedSumEncoding>())

private fun discriminatorFieldName(metadata: Metadata): String {
    return metadata.getAnnotation(DiscriminatorFieldNameAnnotation) ?: "type"
}

fun KotlinGeneratorContext<Model.TaggedSum>.generateTaggedSum() {
    val encoding = definition.metadata.getAnnotation(TaggedSumEncodingAnnotation) ?: TaggedSumEncoding.WRAPPER_RECORD
    val discriminatorFieldName = discriminatorFieldName(definition.metadata)

    when (encoding) {
        TaggedSumEncoding.RECORD_PROPERTY -> {
            val subTypes = definition.constructors.map { constructor ->
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

                SubTypeInfo(definitionType(referencedModule, referencedDefinition), null, discriminatorStringValue(constructor.metadata, constructor.tag.tag))
            }
            docs(definition.metadata)
            serializableAnnotation(serializationLibrary)
            jsonClassDiscriminatorAnnotation(serializationLibrary, discriminatorFieldName)
            jsonTypeInfoAnnotation(serializationLibrary, discriminatorFieldName, subTypes)
            deprecatedAnnotation(definition.metadata)
            line {
                append("sealed interface ${topLevelSymbolName(name)}")
            }
            append("\n")
        }
        TaggedSumEncoding.WRAPPER_RECORD -> {
            docs(definition.metadata)
            indent()
            append("sealed interface ${topLevelSymbolName(name)}")
            codeBlock {
                var firstConstructor = true
                for (constructor in definition.constructors) {
                    if (!firstConstructor) {
                        append("\n")
                    }
                    firstConstructor = false
                    val constructorName = constructor.metadata.getAnnotation(SymbolNameAnnotation)
                        ?: idiomaticClassName(constructor.metadata.name)
                    docs(constructor.metadata)
                    deprecatedAnnotation(definition.metadata)
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

private fun constructorName(metadata: Metadata): String {
    return metadata.getAnnotation(SymbolNameAnnotation)
        ?: idiomaticClassName(metadata.name)
}

fun KotlinGeneratorContext<Model.Adt>.generateAdt() {
    val discriminatorFieldName = discriminatorFieldName(definition.metadata)

    docs(definition.metadata)
    jsonClassDiscriminatorAnnotation(serializationLibrary, discriminatorFieldName)
    serializableAnnotation(serializationLibrary)
    val subTypes = definition.constructors.map {
        SubTypeInfo(name, constructorName(it.metadata), discriminatorStringValue(it.metadata))
    }
    jsonTypeInfoAnnotation(serializationLibrary, discriminatorFieldName, subTypes)
    deprecatedAnnotation(definition.metadata)
    indent()
    append("sealed interface ${topLevelSymbolName(name)}")
    codeBlock {
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
            serialNameAnnotation(serializationLibrary, discriminatorStringValue(constructor.metadata))
            deprecatedAnnotation(definition.metadata)
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