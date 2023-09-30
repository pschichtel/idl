package tel.schich.idl.generator.kotlin.generate.library

import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.core.Definition
import tel.schich.idl.core.Model
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.core.valueAsBoolean
import tel.schich.idl.generator.kotlin.KotlinAnnotation
import tel.schich.idl.generator.kotlin.generate.FileBuilder
import tel.schich.idl.generator.kotlin.SerializationLibrary
import tel.schich.idl.generator.kotlin.generate.annotation
import tel.schich.idl.generator.kotlin.generate.primitiveValue

fun FileBuilder.serializableAnnotation(serializationLibrary: SerializationLibrary?) {
    if (serializationLibrary == SerializationLibrary.KOTLINX_SERIALIZATION) {
        line {
            annotation("kotlinx.serialization.Serializable")
        }
    }
}

fun FileBuilder.jsonClassDiscriminatorAnnotation(serializationLibrary: SerializationLibrary?, discriminatorFieldName: String) {
    if (serializationLibrary != SerializationLibrary.KOTLINX_SERIALIZATION || discriminatorFieldName == "type") {
        return
    }

    line {
        annotation("kotlin.OptIn")
        append("(${useImported("kotlinx.serialization.ExperimentalSerializationApi")}::class)")
    }
    line {
        annotation("kotlinx.serialization.json.JsonClassDiscriminator")
        append("(\"${discriminatorFieldName}\")")
    }
}

fun FileBuilder.serialNameAnnotation(serializationLibrary: SerializationLibrary?, discriminatorValue: JsonPrimitive) {
    if (serializationLibrary == SerializationLibrary.KOTLINX_SERIALIZATION) {
        line {
            annotation("kotlinx.serialization.SerialName")
            append("(${primitiveValue(discriminatorValue)})")
        }
    }
}

val KotlinxSerializationContextualAnnotation = KotlinAnnotation("kotlinx.serialization-contextual", ::valueAsBoolean)

fun FileBuilder.contextualAnnotation(serializationLibrary: SerializationLibrary?, referencedDefinition: Definition) {
    // TODO @Contextual needs to appear in many more places (e.g. collection parameters, value wrappers, ...)
    if (serializationLibrary == SerializationLibrary.KOTLINX_SERIALIZATION) {
        val isComplex = referencedDefinition is Model.Sum || referencedDefinition is Model.TaggedSum
        val isAnnotated = referencedDefinition.metadata.getAnnotation(KotlinxSerializationContextualAnnotation) == true
        if (isAnnotated || isComplex) {
            line {
                annotation("kotlinx.serialization.Contextual")
            }
        }
    }
}