package tel.schich.idl.generator.kotlin.generate

import tel.schich.idl.core.Model
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.generator.kotlin.KotlinGeneratorContext
import tel.schich.idl.generator.kotlin.NewTypeAnnotation
import tel.schich.idl.generator.kotlin.RepresentAsAnnotation
import tel.schich.idl.generator.kotlin.generate.library.serializableAnnotation

private fun KotlinGeneratorContext<*>.collectionDefinition(defaultCollectionType: String, types: List<String>) {
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

fun KotlinGeneratorContext<Model.HomogenousMap>.generateHomogenousMap() {
    val keyType = definitionType(definition.keyModel)
    val valueType = definitionType(definition.valueModel)
    collectionDefinition(defaultCollectionType = "kotlin.collections.Map", listOf(keyType, valueType))
}

fun KotlinGeneratorContext<Model.HomogenousSet>.generateHomogenousSet() {
    val type = definitionType(definition.itemModel)
    collectionDefinition(defaultCollectionType = "kotlin.collections.Set", listOf(type))
}

fun KotlinGeneratorContext<Model.HomogenousList>.generateHomogenousList() {
    val type = definitionType(definition.itemModel)
    collectionDefinition(defaultCollectionType = "kotlin.collections.List", listOf(type))
}