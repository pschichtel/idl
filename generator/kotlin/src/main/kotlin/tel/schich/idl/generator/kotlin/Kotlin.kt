package tel.schich.idl.generator.kotlin

import com.ibm.icu.text.RuleBasedNumberFormat
import com.ibm.icu.util.ULocale
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.core.PrimitiveDataType
import tel.schich.idl.core.splitIntoWords

val preImportedPackage = setOf("kotlin", "kotlin.collections", "kotlin.jvm")
val hardKeywords = setOf("class", "interface", "val", "var")

fun idiomaticPackageName(name: String): String {
    return name.replace("-", "")
}

fun idiomaticClassName(name: String): String {
    return splitIntoWords(name) { it.lowercase().replaceFirstChar { c -> c.uppercase() } }.joinToString("")
}

fun idiomaticName(name: String): String {
    val words = splitIntoWords(name, String::uppercase).asSequence()
    return words.first() + words.drop(1).joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
}

fun idiomaticEnumEntryName(name: String): String {
    return splitIntoWords(name, String::uppercase).joinToString("_")
}

fun symbolName(s: String): String {
    if (s in hardKeywords) {
        return "`$s`"
    }
    return s
}

fun primitiveValue(json: JsonPrimitive): String {
    if (json is JsonNull) {
        return "null"
    }
    if (json.isString) {
        return "\"" + json.content.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    }
    return json.content
}

private val spellOutFormatter = RuleBasedNumberFormat(ULocale.ENGLISH, RuleBasedNumberFormat.SPELLOUT)
fun tupleFieldName(n: Int): String {
    val parts = spellOutFormatter.format(n).split('-')
    val lastPart = when (val lastPart = parts.last()) {
        "one" -> "first"
        "two" -> "second"
        "three" -> "third"
        else -> when {
            lastPart.endsWith('y') -> "${lastPart.dropLast(1)}ieth"
            lastPart.endsWith("ve") -> "${lastPart.dropLast(2)}fth"
            lastPart.endsWith('e') -> "${lastPart.dropLast(1)}th"
            lastPart.endsWith('t') -> "${lastPart}h"
            else -> "${lastPart}th"
        }
    }
    val firstParts = parts.dropLast(1)
    if (firstParts.isEmpty()) {
        return lastPart
    }
    return firstParts.first() + (firstParts.asSequence().drop(1) + sequenceOf(lastPart)).joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
}

class FileBuilder(
    private val packageName: String,
    private val imports: MutableSet<String>,
    private val topLevelSymbols: MutableSet<String>,
    private val builder: StringBuilder,
    val indentionLevel: UInt,
) {
    private val indention = "    "

    fun topLevelSymbolName(name: String): String {
        if (name in topLevelSymbols) {
            error("Global symbol $name was defined repeatedly!")
        }
        topLevelSymbols.add(name)
        return symbolName(name)
    }

    fun useImported(qualifiedName: String): String {
        val lastDotPos = qualifiedName.lastIndexOf('.')
        if (lastDotPos == -1) {
            // nothing to import
            return symbolName(qualifiedName)
        }
        val referencedPackageName = qualifiedName.substring(0, lastDotPos)
        if (referencedPackageName.isBlank()) {
            error("Symbol has illegal package name: $qualifiedName")
        }
        val symbolName = qualifiedName.substring(lastDotPos + 1)
        if (symbolName.isBlank()) {
            error("Symbol has illegal name: $qualifiedName")
        }

        if (symbolName in topLevelSymbols) {
            return symbolName(qualifiedName)
        }
        topLevelSymbols += symbolName

        if (referencedPackageName != this.packageName && referencedPackageName !in preImportedPackage) {
            imports.add(qualifiedName)
        }

        return symbolName
    }

    fun indent() {
        repeat(this.indentionLevel.toInt()) {
            builder.append(indention)
        }
    }

    fun append(s: String) {
        builder.append(s)
    }

    fun indented(block: FileBuilder.() -> Unit) {
        FileBuilder(packageName, imports, topLevelSymbols, builder, indentionLevel + 1u).also(block)
    }

    fun block(block: FileBuilder.() -> Unit) {
        append(" {\n")
        indented(block)
        line {
            append("}")
        }
    }

    fun line(block: FileBuilder.() -> Unit) {
        indent()
        block()
        append("\n")
    }

    fun value(name: String, type: String, const: Boolean = false, private: Boolean = false, override: Boolean = false) {
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

    fun annotation(type: String) {
        append("@${useImported(type)}")
    }

    fun valueClass(name: String, valueFieldName: String, type: String) {
        line {
            annotation(type = JvmInline::class.qualifiedName!!)
        }
        line {
            append("value class ${topLevelSymbolName(name)}(")
            value(valueFieldName, type)
            append(")")
        }
    }

    fun typeAlias(name: String, referencedType: String) {
        line {
            append("typealias ${topLevelSymbolName(name)} = ${useImported(referencedType)}")
        }
    }

    fun docBlock(text: String) {
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

    private fun quoteName(name: String): String {
        return name.split('.').joinToString(".") {
            if (it in hardKeywords) {
                "`$it`"
            } else {
                it
            }
        }
    }

    override fun toString(): String {
        val output = StringBuilder()
        output.append("package ${quoteName(packageName)}\n\n")
        val sortedImports = imports.toList().sorted()
        if (sortedImports.isNotEmpty()) {
            for (import in sortedImports) {
                output.append("import ${quoteName(import)}\n")
            }
            output.append("\n")
        }
        output.append(builder)
        return output.toString()
    }
}

fun buildFile(packageName: String, block: FileBuilder.() -> Unit): String {
    return FileBuilder(packageName, mutableSetOf(), mutableSetOf(), StringBuilder(), indentionLevel = 0u).also(block).toString()
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