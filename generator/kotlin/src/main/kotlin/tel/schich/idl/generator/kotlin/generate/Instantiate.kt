package tel.schich.idl.generator.kotlin.generate

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.core.Alias
import tel.schich.idl.core.Definition
import tel.schich.idl.core.Model
import tel.schich.idl.core.Module
import tel.schich.idl.core.PrimitiveDataType
import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.invalidModule
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.core.resolveModelReference
import tel.schich.idl.generator.kotlin.NewTypeAnnotation
import tel.schich.idl.generator.kotlin.RepresentAsAnnotation


fun FileBuilder.instantiateDefinition(request: GenerationRequest, subject: Module, definingModule: Module, definition: Definition, nullableContext: Boolean, value: JsonElement) {
    if (value is JsonNull) {
        if (!nullableContext) {
            invalidModule(
                subject.reference,
                "Definition ${definition.metadata.name} cannot be instantiated with null in non-nullable context"
            )
        } else {
            append("null")
            return
        }
    }
    when (definition) {
        is Alias -> instantiateAlias(request, subject, definingModule, definition, nullableContext, value)
        is Model.Adt -> todo()
        is Model.Enumeration -> todo()
        is Model.HomogenousList -> todo()
        is Model.HomogenousMap -> todo()
        is Model.HomogenousSet -> todo()
        is Model.Primitive -> instantiatePrimitive(request, definingModule, definition, value)
        is Model.Product -> todo()
        is Model.Record -> todo()
        is Model.Sum -> todo()
        is Model.TaggedSum -> todo()
        is Model.Unknown -> todo()
    }
}

fun FileBuilder.instantiateAlias(request: GenerationRequest, subject: Module, definingModule: Module, definition: Alias, nullableContext: Boolean, value: JsonElement) {
    val (referencedModule, referencedDefinition) = resolveModelReference(definingModule, request.modules, definition.aliasedModel)!!
    instantiateDefinition(request, subject, referencedModule, referencedDefinition, nullableContext, value)
}

fun FileBuilder.instantiatePrimitive(request: GenerationRequest, definingModule: Module, definition: Model.Primitive, value: JsonElement) {
    if (definition.metadata.getAnnotation(RepresentAsAnnotation) != null) {
        todo()
        return
    }
    if (value !is JsonPrimitive) {
        invalidModule(definingModule.reference, "Primitive ${definition.metadata.name} cannot be instantiated with non-primitive: $value")
    }

    val newType = definition.metadata.getAnnotation(NewTypeAnnotation) != false

    if (newType) {
        val name = definitionName(request, definingModule, definition.metadata)
        val fieldName = valueFieldName(definition.metadata)
        append("$name(${symbolName(fieldName)} = ")
    }
    when (val dataType = definition.dataType) {
        is PrimitiveDataType.Bool -> {
            append(value.content)
        }
        is PrimitiveDataType.Float -> {
            val size = dataType.size
            when {
                size == null || size > 64u -> {
                    append("${useImported("java.math.BigDecimal")}(${literalString(value.content)})")
                }
                size > 32u -> {
                    append(value.content)
                }
                else -> append("${value.content}f")
            }
        }
        is PrimitiveDataType.Integer -> {
            val size = dataType.size
            val signSuffix = if (dataType.signed) "" else "u"
            when {
                size == null || size > 64u -> {
                    append("${useImported("java.math.BigInteger")}(${literalString(value.content)})")
                }
                size > 32u -> {
                    append("${value.content}${signSuffix}L")
                }
                else -> append("${value.content}$signSuffix")
            }
        }
        is PrimitiveDataType.String -> append(literalString(value.content))
    }
    if (newType) {
        append(")")
    }
}

