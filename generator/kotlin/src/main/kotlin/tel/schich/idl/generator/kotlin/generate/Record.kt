package tel.schich.idl.generator.kotlin.generate

import tel.schich.idl.core.Model
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.core.resolveForeignProperties
import tel.schich.idl.core.resolveModelReference
import tel.schich.idl.generator.kotlin.KotlinGeneratorContext
import tel.schich.idl.generator.kotlin.SymbolNameAnnotation
import tel.schich.idl.generator.kotlin.generate.library.contextualAnnotation
import tel.schich.idl.generator.kotlin.generate.library.serializableAnnotation

fun KotlinGeneratorContext<Model.Record>.generateRecord() {
    docs(definition.metadata)
    serializableAnnotation(serializationLibrary)
    deprecatedAnnotation(definition.metadata)
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
            deprecatedAnnotation(definition.metadata)
            line {
                value(propertyName, type)
                if (property.nullable) {
                    append("?")
                }
                val default = property.default
                if (default != null) {
                    append(" = ")
                    instantiateDefinition(request, subjectModule, referencedModule, referencedDefinition, property.nullable, default)
                }
                append(",")
            }
        }
    }
    line {
        append(")")
    }
}