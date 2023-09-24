package tel.schich.idl.generator.kotlin.generate.library

import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.generator.kotlin.SerializationLibrary
import tel.schich.idl.generator.kotlin.generate.FileBuilder
import tel.schich.idl.generator.kotlin.generate.annotation
import tel.schich.idl.generator.kotlin.generate.lineComment
import tel.schich.idl.generator.kotlin.generate.literalString
import tel.schich.idl.generator.kotlin.generate.primitiveValue
import tel.schich.idl.generator.kotlin.generate.symbolName

fun FileBuilder.jsonTypeInfoAnnotation(serializationLibrary: SerializationLibrary?, discriminatorFieldName: String, subTypes: Map<String, JsonPrimitive>) {
    if (serializationLibrary != SerializationLibrary.JACKSON) {
        return
    }

    val jsonTypeInfo = "com.fasterxml.jackson.annotation.JsonTypeInfo"
    val jsonSubTypes = "com.fasterxml.jackson.annotation.JsonSubTypes"
    line {
        annotation(jsonTypeInfo)
        append("(")
    }
    indented {
        line {
            append("use = ${useImported(jsonTypeInfo)}.${symbolName("Id")}.${symbolName("NAME")},")
        }
        line {
            append("include = ${useImported(jsonTypeInfo)}.${symbolName("As")}.${symbolName("PROPERTY")},")
        }
        line {
            append("property = ")
            append(literalString(discriminatorFieldName))
            append(",")
        }
    }
    line {
        append(")")
    }
    if (subTypes.isNotEmpty()) {
        line {
            annotation(jsonSubTypes)
            append("(")
        }
        indented {
            line {
                append("value = [")
            }
            indented {
                for ((type, value) in subTypes) {
                    line {
                        append(
                            "${useImported(jsonSubTypes)}.${symbolName("Type")}(value = ${useImported(type)}::class, name = ${primitiveValue(value)}),"
                        )
                    }
                }
            }
            line {
                append("]")
            }
        }
        line {
            append(")")
        }
    }
}