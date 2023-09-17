package tel.schich.idl.generator.kotlin

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.core.Alias
import tel.schich.idl.core.Annotation
import tel.schich.idl.core.AnnotationParser
import tel.schich.idl.core.Definition
import tel.schich.idl.core.Metadata
import tel.schich.idl.core.Model
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.generate.GenerationRequest
import tel.schich.idl.core.generate.GenerationResult
import tel.schich.idl.core.getAnnotation
import tel.schich.idl.core.resolveForeignProperties
import tel.schich.idl.core.resolveModelReference
import tel.schich.idl.core.valueAsIs
import tel.schich.idl.runner.command.JvmInProcessGenerator
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createDirectories

class KotlinAnnotation<T : Any>(name: String, parser: AnnotationParser<T>) :
    Annotation<T>(namespace = "tel.schich.idl.generator.kotlin", name, parser)

val PackageAnnotation = KotlinAnnotation("package", ::valueAsIs)
val FileNameAnnotation = KotlinAnnotation("file-name", ::valueAsIs)
val SymbolNameAnnotation = KotlinAnnotation("symbol-name", ::valueAsIs)
val ValueFieldNameAnnotation = KotlinAnnotation("value-field-name", ::valueAsIs)

class KotlinGenerator : JvmInProcessGenerator {

    private fun derivePackageName(moduleRef: ModuleReference): String {
        return idiomaticPackageName(moduleRef.name)
    }

    private fun getPackage(module: Module): String {
        return module.metadata.getAnnotation(PackageAnnotation)
            ?: derivePackageName(module.reference)
    }

    private fun definitionName(metadata: Metadata): String {
        return metadata.getAnnotation(SymbolNameAnnotation) ?: idiomaticClassName(metadata.name)
    }

    private fun valueFieldName(metadata: Metadata): String {
        return metadata.getAnnotation(ValueFieldNameAnnotation) ?: "value"
    }

    private fun FileBuilder.docs(metadata: Metadata) {
        metadata.description?.let {
            docBlock(it)
        }
    }

    private fun constructInstance(subject: Module, module: Module, definition: Definition, value: JsonElement): String {
        // TODO attempt to construct a valid instance given the JSON value and definition or error out
        return (value as? JsonPrimitive)?.let(::primitiveValue) ?: "null"
    }

    override fun generate(request: GenerationRequest): GenerationResult {
        val modules = request.modules
        if (modules.isEmpty()) {
            error("No modules have been requested!")
        }
        val subjectModules = modules.filter { it.reference in request.subjects }

        val generatedFiles = subjectModules.flatMap { subjectModule ->
            val packageName = getPackage(subjectModule)
            val modulePath = request.outputPath.resolve(packageName.replace('.', File.separatorChar))
            modulePath.createDirectories()

            subjectModule.definitions.map { definition ->
                val name = definitionName(definition.metadata)
                val fileName = definition.metadata.getAnnotation(FileNameAnnotation) ?: name
                val filePath = modulePath.resolve("$fileName.kt")

                val code = buildFile(packageName) {
                    when (definition) {
                        is Model.Primitive -> {
                            val type = kotlinTypeFromDataType(definition.dataType)

                            docs(definition.metadata)
                            valueClass(name, valueFieldName(definition.metadata), type)
                        }
                        is Model.HomogenousList -> {
                            val (referencedModule, referencedDefinition) = resolveModelReference(subjectModule, modules, definition.itemModel)!!
                            val type = "${useImported("kotlin.collections.List")}<" + useImported(getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata)) + ">"

                            docs(definition.metadata)
                            valueClass(name, valueFieldName(definition.metadata), type)
                        }
                        is Model.HomogenousSet -> {
                            val (referencedModule, referencedDefinition) = resolveModelReference(subjectModule, modules, definition.itemModel)!!
                            val type = "${useImported("kotlin.collections.Set")}<" + useImported(getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata)) + ">"

                            docs(definition.metadata)
                            valueClass(name, valueFieldName(definition.metadata), type)
                        }
                        is Model.HomogenousMap -> {
                            val (keyModule, keyDef) = resolveModelReference(subjectModule, modules, definition.keyModel)!!
                            val (valueModule, valueDef) = resolveModelReference(subjectModule, modules, definition.valueModel)!!
                            val type = "${useImported("kotlin.collections.Map")}<${useImported(getPackage(keyModule) + "." + definitionName(keyDef.metadata))}, ${useImported(getPackage(valueModule) + "." + definitionName(valueDef.metadata))}>"

                            docs(definition.metadata)
                            valueClass(name, valueFieldName(definition.metadata), type)
                        }
                        is Model.Constant -> {
                            val type = kotlinTypeFromDataType(definition.dataType)
                            docs(definition.metadata)
                            line {
                                value(name, type, const = true)
                                append(" = ${primitiveValue(definition.value)}")
                            }
                        }
                        is Alias -> {
                            val (referencedModule, referencedDefinition) = resolveModelReference(subjectModule, modules, definition.aliasedModel)!!
                            docs(definition.metadata)
                            typeAlias(name, getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata))
                        }
                        is Model.Unknown -> {
                            docs(definition.metadata)
                            typeAlias(name, "kotlin.Any")
                        }
                        is Model.Enumeration -> {
                            val valueType = kotlinTypeFromDataType(definition.dataType)
                            val valueFieldName = valueFieldName(definition.metadata)

                            docs(definition.metadata)
                            indent()
                            append("enum class ${symbolName(name)}(val $valueFieldName: ${useImported(valueType)})")
                            block {
                                for (entry in definition.entries) {
                                    val value = entry.value
                                    val entryName = entry.metadata.getAnnotation(SymbolNameAnnotation) ?: entry.metadata.name
                                    line {
                                        append("${symbolName(entryName)}(${valueFieldName} = ${primitiveValue(value)}),")
                                    }
                                }
                            }
                        }
                        is Model.Product -> {
                            docs(definition.metadata)
                            line {
                                append("data class ${symbolName(name)}(")
                            }
                            indented {
                                for ((i, component) in definition.components.withIndex()) {
                                    val (referencedModule, referencedDefinition) = resolveModelReference(
                                        subjectModule,
                                        modules,
                                        component
                                    )!!
                                    line {
                                        value(
                                            tupleFieldName(i + 1),
                                            getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata)
                                        )
                                        append(",")
                                    }
                                }
                            }
                            line {
                                append(")")
                            }
                        }
                        is Model.Adt -> {
                            docs(definition.metadata)
                            indent()
                            append("sealed interface ${symbolName(name)}")
                            block {
                                if (definition.commonProperties.isNotEmpty()) {
                                    for (property in definition.commonProperties) {
                                        val propertyName = property.metadata.getAnnotation(SymbolNameAnnotation)
                                            ?: idiomaticName(property.metadata.name)
                                        val (referencedModule, referencedDefinition) = resolveModelReference(
                                            subjectModule,
                                            modules,
                                            property.model
                                        )!!
                                        docs(property.metadata)
                                        line {
                                            value(
                                                propertyName,
                                                getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata)
                                            )
                                        }
                                    }
                                    append("\n")
                                }
                                var firstConstructor = true
                                for (constructor in definition.constructors) {
                                    if (!firstConstructor) {
                                        append("\n")
                                    }
                                    firstConstructor = false
                                    val constructorName = constructor.metadata.getAnnotation(SymbolNameAnnotation)
                                        ?: idiomaticClassName(constructor.metadata.name)
                                    docs(constructor.metadata)
                                    line {
                                        append("data class ${symbolName(constructorName)}(")
                                    }
                                    indented {
                                        for (property in definition.commonProperties) {
                                            val propertyName = property.metadata.getAnnotation(SymbolNameAnnotation)
                                                ?: idiomaticName(property.metadata.name)
                                            val (referencedModule, referencedDefinition) = resolveModelReference(
                                                subjectModule,
                                                modules,
                                                property.model
                                            )!!
                                            docs(property.metadata)
                                            line {
                                                value(
                                                    propertyName,
                                                    getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata),
                                                    override = true,
                                                )
                                                append(",")
                                            }
                                        }
                                        for (property in constructor.properties) {
                                            val propertyName = property.metadata.getAnnotation(SymbolNameAnnotation)
                                                ?: idiomaticName(property.metadata.name)
                                            val (referencedModule, referencedDefinition) = resolveModelReference(
                                                subjectModule,
                                                modules,
                                                property.model
                                            )!!
                                            docs(property.metadata)
                                            line {
                                                value(
                                                    propertyName,
                                                    getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata),
                                                )
                                                append(",")
                                            }
                                        }
                                    }
                                    line {
                                        append(") : ${symbolName(name)}")
                                    }
                                }
                            }
                        }
                        is Model.Record -> {
                            docs(definition.metadata)
                            line {
                                append("data class $name(")
                            }
                            val foreignProperties = resolveForeignProperties(subjectModule, definition, modules)
                            val properties = foreignProperties + definition.properties.map { Pair(subjectModule, it) }
                            indented {
                                for ((module, property) in properties) {
                                    val propertyName = property.metadata.getAnnotation(SymbolNameAnnotation)
                                        ?: idiomaticName(property.metadata.name)
                                    val (referencedModule, referencedDefinition) = resolveModelReference(
                                        module,
                                        modules,
                                        property.model
                                    )!!
                                    docs(definition.metadata)
                                    line {
                                        value(
                                            propertyName,
                                            getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata)
                                        )
                                        if (property.nullable) {
                                            append("?")
                                        }
                                        val default = property.default
                                        if (default != null) {
                                            append(" = ")
                                            append(constructInstance(subjectModule, referencedModule, referencedDefinition, default.value))
                                        }
                                        append(",")
                                    }
                                }
                            }
                            line {
                                append(")")
                            }
                        }
                        is Model.Sum -> {
                            docs(definition.metadata)
                            indent()
                            append("sealed interface ${symbolName(name)}")
                            block {
                                var firstConstructor = true
                                for (constructor in definition.constructors) {
                                    if (!firstConstructor) {
                                        append("\n")
                                    }
                                    firstConstructor = false
                                    val constructorName = constructor.metadata.getAnnotation(SymbolNameAnnotation)
                                        ?: idiomaticClassName(constructor.metadata.name)
                                    val (referencedModule, referencedDefinition) = resolveModelReference(
                                        subjectModule,
                                        modules,
                                        constructor.model
                                    )!!
                                    docs(constructor.metadata)
                                    line {
                                        append("data class ${symbolName(constructorName)}(")
                                        value(
                                            valueFieldName(constructor.metadata),
                                            getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata),
                                        )
                                        append(") : ${symbolName(name)}")
                                    }
                                }
                            }
                        }
                        is Model.TaggedSum -> {
                            // TODO this is so far identical to the untagged some and has no concept of tag encodings
                            docs(definition.metadata)
                            indent()
                            append("sealed interface ${symbolName(name)}")
                            block {
                                var firstConstructor = true
                                for (constructor in definition.constructors) {
                                    if (!firstConstructor) {
                                        append("\n")
                                    }
                                    firstConstructor = false
                                    val constructorName = constructor.metadata.getAnnotation(SymbolNameAnnotation)
                                        ?: idiomaticClassName(constructor.metadata.name)
                                    docs(constructor.metadata)
                                    line {
                                        append("data class ${symbolName(constructorName)}(")
                                    }
                                    indented {
                                        val (referencedModule, referencedDefinition) = resolveModelReference(
                                            subjectModule,
                                            modules,
                                            constructor.model
                                        )!!
                                        value(
                                            valueFieldName(constructor.metadata),
                                            getPackage(referencedModule) + "." + definitionName(referencedDefinition.metadata),
                                        )
                                    }
                                    line {
                                        append(") : ${symbolName(name)}")
                                    }
                                }
                            }
                        }
                    }
                }
                Files.write(filePath, code.toByteArray())

                filePath
            }
        }

        return GenerationResult.Success(generatedFiles)
    }
}