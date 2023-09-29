package tel.schich.idl.generator.kotlin.generate.library

import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.generator.kotlin.SerializationLibrary
import tel.schich.idl.generator.kotlin.generate.FileBuilder
import tel.schich.idl.generator.kotlin.generate.annotation
import tel.schich.idl.generator.kotlin.generate.literalString
import tel.schich.idl.generator.kotlin.generate.primitiveValue
import tel.schich.idl.generator.kotlin.generate.quoteName
import tel.schich.idl.generator.kotlin.generate.symbolName

data class SubTypeInfo(
    val import: String,
    val nestedClass: String?,
    val tag: JsonPrimitive,
)

fun FileBuilder.jsonTypeInfoAnnotation(serializationLibrary: SerializationLibrary?, discriminatorFieldName: String, subTypes: List<SubTypeInfo>) {
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
                for ((import, nestedClass, value) in subTypes) {
                    val stringTag = JsonPrimitive(value.content)
                    line {
                        append("${useImported(jsonSubTypes)}.${symbolName("Type")}(value = ")
                        val imported = useImported(import)
                        append(imported)
                        if (nestedClass != null) {
                            append(".${quoteName(nestedClass)}")
                        }
                        append("::class, name = ${primitiveValue(stringTag)}),")
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