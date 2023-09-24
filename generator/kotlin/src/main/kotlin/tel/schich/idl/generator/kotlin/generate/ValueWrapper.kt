package tel.schich.idl.generator.kotlin.generate

import tel.schich.idl.core.Alias
import tel.schich.idl.core.Model
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.generator.kotlin.KotlinGeneratorContext
import tel.schich.idl.generator.kotlin.NewTypeAnnotation
import tel.schich.idl.generator.kotlin.RepresentAsAnnotation
import tel.schich.idl.generator.kotlin.generate.library.serializableAnnotation

private fun KotlinGeneratorContext<*>.typeWrappingDefinition(type: String) {
    val representationType = definition.metadata.getAnnotation(RepresentAsAnnotation) ?: type
    val newType = definition.metadata.getAnnotation(NewTypeAnnotation) ?: true

    docs(definition.metadata)
    deprecatedAnnotation(definition.metadata)
    if (newType) {
        serializableAnnotation(serializationLibrary)
        valueClass(name, valueFieldName(definition.metadata), representationType)
    } else {
        typeAlias(name, representationType)
    }
}

fun KotlinGeneratorContext<Model.Unknown>.generateUnknown() {
    typeWrappingDefinition(type = "kotlin.Any")
}

fun KotlinGeneratorContext<Alias>.generateAlias() {
    typeWrappingDefinition(definitionType(definition.aliasedModel))
}

fun KotlinGeneratorContext<Model.Primitive>.generatePrimitive() {
    typeWrappingDefinition(kotlinTypeFromDataType(definition.dataType))
}