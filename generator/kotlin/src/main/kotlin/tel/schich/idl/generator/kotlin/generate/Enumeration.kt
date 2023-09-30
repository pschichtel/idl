package tel.schich.idl.generator.kotlin.generate

import tel.schich.idl.core.Model
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.generator.kotlin.KotlinGeneratorContext
import tel.schich.idl.generator.kotlin.SymbolNameAnnotation
import tel.schich.idl.generator.kotlin.generate.library.serialNameAnnotation
import tel.schich.idl.generator.kotlin.generate.library.serializableAnnotation

fun KotlinGeneratorContext<Model.Enumeration>.generateEnumeration() {
    val valueType = kotlinTypeFromDataType(definition.dataType)
    val valueFieldName = valueFieldName(definition.metadata)

    docs(definition.metadata)
    serializableAnnotation(serializationLibrary)
    deprecatedAnnotation(definition.metadata)
    indent()
    append("enum class ${topLevelSymbolName(name)}(")
    value(valueFieldName, valueType)
    append(")")
    codeBlock {
        for (entry in definition.entries) {
            val value = entry.value
            val entryName = entry.metadata.getAnnotation(SymbolNameAnnotation)
                ?: idiomaticEnumEntryName(entry.metadata.name)
            docs(entry.metadata)
            serialNameAnnotation(serializationLibrary, discriminatorStringValue(entry.metadata, value))
            deprecatedAnnotation(definition.metadata)
            line {
                append("${symbolName(entryName)}(${valueFieldName} = ${primitiveValue(value)}),")
            }
        }
    }
}