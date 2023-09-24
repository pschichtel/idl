package tel.schich.idl.generator.kotlin.generate

import tel.schich.idl.core.Model
import tel.schich.idl.generator.kotlin.KotlinGeneratorContext

fun KotlinGeneratorContext<Model.Constant>.generateConstant() {
    val type = kotlinTypeFromDataType(definition.dataType)
    docs(definition.metadata)
    deprecatedAnnotation(definition.metadata)
    line {
        value(name, type, const = true)
        append(" = ${primitiveValue(definition.value)}")
    }
}