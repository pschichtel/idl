package tel.schich.idl.generator.kotlin.generate

import java.io.Writer

interface FileBuilder {
    fun topLevelSymbolName(name: String): String
    fun useImported(qualifiedName: String): String
    fun indent()
    fun append(s: String)
    fun indented(block: FileBuilder.() -> Unit)
    fun codeBlock(block: FileBuilder.() -> Unit)
    fun line(block: FileBuilder.() -> Unit)
}

class SimpleFileBuilder private constructor(
    private val packageName: String,
    private val imports: MutableSet<String>,
    private val topLevelSymbols: MutableSet<String>,
    private val builder: StringBuilder,
    private val indentionLevel: UInt
) : FileBuilder {
    private val indention = "    "

    constructor(packageName: String) : this(packageName, mutableSetOf(), mutableSetOf(), StringBuilder(), 0u)

    override fun topLevelSymbolName(name: String): String {
        if (name in topLevelSymbols) {
            error("Global symbol $name was defined repeatedly!")
        }
        topLevelSymbols.add(name)
        return symbolName(name)
    }

    override fun useImported(qualifiedName: String): String {
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

        if (qualifiedName in imports) {
            return symbolName(symbolName)
        }
        if (symbolName in topLevelSymbols) {
            return symbolName(qualifiedName)
        }
        topLevelSymbols += symbolName

        if (referencedPackageName != this.packageName && referencedPackageName !in PreImportedPackage) {
            imports.add(qualifiedName)
        }

        return symbolName
    }

    override fun indent() {
        repeat(this.indentionLevel.toInt()) {
            builder.append(indention)
        }
    }

    override fun append(s: String) {
        builder.append(s)
    }

    override fun indented(block: FileBuilder.() -> Unit) {
        SimpleFileBuilder(packageName, imports, topLevelSymbols, builder, indentionLevel + 1u).also(block)
    }

    override fun codeBlock(block: FileBuilder.() -> Unit) {
        append(" {\n")
        indented(block)
        line {
            append("}")
        }
    }

    override fun line(block: FileBuilder.() -> Unit) {
        indent()
        block()
        append("\n")
    }

    private fun quoteName(name: String): String {
        return name.split(PACKAGE_SEPARATOR).joinToString(PACKAGE_SEPARATOR) {
            if (it in HardKeywords) {
                "`$it`"
            } else {
                it
            }
        }
    }

    fun write(writer: Writer) {
        writer.write("package ${quoteName(packageName)}\n\n")
        val sortedImports = imports.toList().sorted()
        if (sortedImports.isNotEmpty()) {
            for (import in sortedImports) {
                writer.write("import ${quoteName(import)}\n")
            }
            writer.write("\n")
        }
        writer.write(builder.toString())
    }
}