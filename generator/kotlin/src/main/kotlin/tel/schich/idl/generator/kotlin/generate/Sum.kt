package tel.schich.idl.generator.kotlin.generate

import tel.schich.idl.core.Model
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.generator.kotlin.KotlinGeneratorContext
import tel.schich.idl.generator.kotlin.SymbolNameAnnotation

fun KotlinGeneratorContext<Model.Sum>.generateSum() {
    docs(definition.metadata)
    indent()
    append("sealed interface ${topLevelSymbolName(name)}")
    codeBlock {
        var firstConstructor = true
        for (constructor in definition.constructors) {
            if (!firstConstructor) {
                append("\n")
            }
            firstConstructor = false
            val constructorName = constructor.metadata.getAnnotation(SymbolNameAnnotation)
                ?: idiomaticClassName(constructor.metadata.name)
            val type = definitionType(constructor.model)
            docs(constructor.metadata)
            line {
                append("data class ${symbolName(constructorName)}(")
                value(valueFieldName(constructor.metadata), type)
                append(") : ${symbolName(name)}")
            }
        }
    }
}