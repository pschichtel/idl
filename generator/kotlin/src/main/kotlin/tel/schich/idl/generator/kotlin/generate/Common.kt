package tel.schich.idl.generator.kotlin.generate

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.core.Definition
import tel.schich.idl.core.Metadata
import tel.schich.idl.core.ModelReference
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.PrimitiveDataType
import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.getAnnotation
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.core.resolveModelReference
import tel.schich.idl.core.splitIntoWords
import tel.schich.idl.core.valueAsString
import tel.schich.idl.generator.kotlin.DiscriminatorValueAnnotation
import tel.schich.idl.generator.kotlin.KotlinAnnotation
import tel.schich.idl.generator.kotlin.KotlinGeneratorContext
import tel.schich.idl.generator.kotlin.ModelNameFormatAnnotation
import tel.schich.idl.generator.kotlin.PackageAnnotation
import tel.schich.idl.generator.kotlin.SymbolNameAnnotation
import tel.schich.idl.generator.kotlin.ValueFieldNameAnnotation

const val PACKAGE_SEPARATOR = "."
val PreImportedPackage = setOf("kotlin", "kotlin.collections", "kotlin.jvm")
val HardKeywords = setOf("class", "interface", "val", "var")

private fun derivePackageName(moduleRef: ModuleReference): String {
    return idiomaticPackageName(moduleRef.name)
}

fun getPackage(module: Module): String {
    return module.metadata.getAnnotation(PackageAnnotation)
        ?: derivePackageName(module.reference)
}

fun definitionName(request: GenerationRequest, module: Module, metadata: Metadata): String {
    val format = metadata.getAnnotation(ModelNameFormatAnnotation)
        ?: module.metadata.getAnnotation(ModelNameFormatAnnotation)
        ?: request.getAnnotation(ModelNameFormatAnnotation)
        ?: "%s"
    return metadata.getAnnotation(SymbolNameAnnotation) ?: idiomaticClassName(format.format(metadata.name))
}

fun KotlinGeneratorContext<*>.definitionName(module: Module, metadata: Metadata): String {
    return definitionName(request, module, metadata)
}

fun KotlinGeneratorContext<*>.definitionType(module: Module, definition: Definition): String {
    return getPackage(module) + PACKAGE_SEPARATOR + definitionName(module, definition.metadata)
}

fun KotlinGeneratorContext<*>.definitionType(modelReference: ModelReference): String {
    val (referencedModule, referencedDefinition) = resolveModelReference(subjectModule, modules, modelReference)!!
    return definitionType(referencedModule, referencedDefinition)
}

fun valueFieldName(metadata: Metadata): String {
    return metadata.getAnnotation(ValueFieldNameAnnotation) ?: "value"
}

fun FileBuilder.docs(metadata: Metadata) {
    metadata.description?.let {
        docBlock(it)
    }
}

fun kotlinTypeFromDataType(dataType: PrimitiveDataType): String {
    return when (dataType) {
        is PrimitiveDataType.Bool -> "kotlin.Boolean"
        is PrimitiveDataType.Float -> {
            val size = dataType.size
            when {
                size == null || size > 64u -> "java.lang.BigDecimal"
                size > 32u -> "kotlin.Double"
                else -> "kotlin.Float"
            }
        }
        is PrimitiveDataType.Integer -> {
            val size = dataType.size
            if (dataType.signed) {
                when {
                    size == null || size > 64u -> "java.lang.BigInteger"
                    size > 32u -> "kotlin.Long"
                    size > 16u -> "kotlin.Int"
                    size > 8u -> "kotlin.Short"
                    else -> "kotlin.Byte"
                }
            } else {
                when {
                    size == null || size > 64u -> "java.lang.BigInteger"
                    size > 32u -> "kotlin.ULong"
                    size > 16u -> "kotlin.UInt"
                    size > 8u -> "kotlin.UShort"
                    else -> "kotlin.UByte"
                }
            }
        }
        is PrimitiveDataType.String -> "kotlin.String"
    }
}

fun literalString(string: String): String {
    if (string.isEmpty()) {
        return "\"\""
    }
    val kotlinString = string.asSequence().flatMap {
        when (it) {
            '\n' -> sequenceOf('\\', 'n')
            '\r' -> sequenceOf('\\', 'r')
            '\t' -> sequenceOf('\\', 't')
            '\b' -> sequenceOf('\\', 'b')
            '\\' -> sequenceOf('\\', '\\')
            '"' -> sequenceOf('\\', '"')
            '$' -> sequenceOf('\\', '$')
            else -> sequenceOf(it)
        }
    }.joinToString("")
    return "\"${kotlinString}\""
}

fun primitiveValue(json: JsonPrimitive): String {
    if (json is JsonNull) {
        return "null"
    }
    if (json.isString) {
        return literalString(json.content)
    }
    return json.content
}

fun idiomaticPackageName(name: String): String {
    return name.replace("-", "")
}

fun idiomaticClassName(name: String): String {
    return splitIntoWords(name) { it.lowercase().replaceFirstChar { c -> c.uppercase() } }.joinToString("")
}

fun idiomaticName(name: String): String {
    val words = splitIntoWords(name, String::lowercase).asSequence()
    return words.first() + words.drop(1).joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
}

fun idiomaticEnumEntryName(name: String): String {
    return splitIntoWords(name, String::uppercase).joinToString("_")
}

fun symbolName(s: String): String {
    if (s in HardKeywords) {
        return "`$s`"
    }
    return s
}

fun quoteName(name: String): String {
    return name.split(PACKAGE_SEPARATOR).joinToString(PACKAGE_SEPARATOR, transform = ::symbolName)
}

fun FileBuilder.value(name: String, type: String, const: Boolean = false, private: Boolean = false, override: Boolean = false) {
    if (override) {
        append("override ")
    }
    if (private) {
        append("private ")
    }
    if (const) {
        append("const ")
    }
    append("val ${symbolName(name)}: ${useImported(type)}")
}

fun FileBuilder.annotation(type: String) {
    append("@${useImported(type)}")
}

fun FileBuilder.valueClass(name: String, valueFieldName: String, type: String) {
    line {
        annotation(type = JvmInline::class.qualifiedName!!)
    }
    line {
        append("value class ${topLevelSymbolName(name)}(")
        value(valueFieldName, type)
        append(")")
    }
}

fun FileBuilder.typeAlias(name: String, referencedType: String) {
    line {
        append("typealias ${topLevelSymbolName(name)} = ${useImported(referencedType)}")
    }
}

fun FileBuilder.docBlock(text: String) {
    line {
        append("/**")
    }
    for (line in text.split('\n')) {
        line {
            append(" * $line")
        }
    }
    line {
        append(" */")
    }
}

private val DeprecationMessageAnnotation = KotlinAnnotation("deprecation-message", ::valueAsString)

fun FileBuilder.deprecatedAnnotation(metadata: Metadata) {
    if (!metadata.deprecated) {
        return
    }

    val message = metadata.getAnnotation(DeprecationMessageAnnotation)
    line {
        annotation("kotlin.Deprecated")
        if (message != null) {
            append("(message = ")
            append(literalString(message))
            append(")")
        }
    }
}

fun FileBuilder.lineComment(comment: String) {
    line {
        append("// $comment")
    }
}

fun discriminatorStringValue(metadata: Metadata, explicitValue: JsonPrimitive? = null): JsonPrimitive {
    return metadata.getAnnotation(DiscriminatorValueAnnotation)?.let(::JsonPrimitive)
        ?: explicitValue?.content?.let(::JsonPrimitive)
        ?: JsonPrimitive(metadata.name)
}
