package tel.schich.idl.generator.kotlin.generate.library

import tel.schich.idl.core.Definition
import tel.schich.idl.core.Metadata
import tel.schich.idl.core.Model
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.generator.kotlin.DiscriminatorValueAnnotation
import tel.schich.idl.generator.kotlin.generate.FileBuilder
import tel.schich.idl.generator.kotlin.SerializationLibrary
import tel.schich.idl.generator.kotlin.generate.annotation

fun FileBuilder.serializableAnnotation(serializationLibrary: SerializationLibrary?) {
    if (serializationLibrary == SerializationLibrary.KOTLINX_SERIALIZATION) {
        line {
            annotation("kotlinx.serialization.Serializable")
        }
    }
}

fun FileBuilder.jsonClassDiscriminatorAnnotation(serializationLibrary: SerializationLibrary?, discriminatorFieldName: String) {
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

fun FileBuilder.serialNameAnnotation(serializationLibrary: SerializationLibrary?, metadata: Metadata, value: String? = null) {
    if (serializationLibrary == SerializationLibrary.KOTLINX_SERIALIZATION) {
        val serialName = metadata.getAnnotation(DiscriminatorValueAnnotation) ?: value ?: metadata.name
        line {
            annotation("kotlinx.serialization.SerialName")
            append("(\"${serialName}\")")
        }
    }
}

fun FileBuilder.contextualAnnotation(serializationLibrary: SerializationLibrary?, referencedDefinition: Definition) {
    if (serializationLibrary == SerializationLibrary.KOTLINX_SERIALIZATION && (referencedDefinition is Model.Sum || referencedDefinition is Model.TaggedSum)) {
        line {
            annotation("kotlinx.serialization.Contextual")
        }
    }
}