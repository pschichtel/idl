package tel.schich.idl.generator.kotlin.generate

import com.ibm.icu.text.RuleBasedNumberFormat
import com.ibm.icu.util.ULocale
import tel.schich.idl.core.Model
import tel.schich.idl.core.resolveModelReference
import tel.schich.idl.generator.kotlin.KotlinGeneratorContext
import tel.schich.idl.generator.kotlin.generate.library.contextualAnnotation
import tel.schich.idl.generator.kotlin.generate.library.serializableAnnotation

private val spellOutFormatter = RuleBasedNumberFormat(ULocale.ENGLISH, RuleBasedNumberFormat.SPELLOUT)
private fun tupleFieldName(n: Int): String {
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

fun KotlinGeneratorContext<Model.Product>.generateProduct() {
    docs(definition.metadata)
    serializableAnnotation(serializationLibrary)
    line {
        append("data class ${topLevelSymbolName(name)}(")
    }
    indented {
        for ((i, component) in definition.components.withIndex()) {
            val (referencedModule, referencedDefinition) = resolveModelReference(
                subjectModule,
                modules,
                component
            )!!
            val type = definitionType(referencedModule, referencedDefinition)
            contextualAnnotation(serializationLibrary, referencedDefinition)
            line {
                value(tupleFieldName(i + 1), type)
                append(",")
            }
        }
    }
    line {
        append(")")
    }
}