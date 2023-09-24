package tel.schich.idl.generator.kotlin.generate

interface FileBuilder {
    fun topLevelSymbolName(name: String): String
    fun useImported(qualifiedName: String): String
    fun indent()
    fun append(s: String)
    fun indented(block: FileBuilder.() -> Unit)
    fun block(block: FileBuilder.() -> Unit)
    fun line(block: FileBuilder.() -> Unit)
}

fun buildFile(packageName: String, block: FileBuilder.() -> Unit): String {
    return SimpleFileBuilder(
        packageName,
        mutableSetOf(),
        mutableSetOf(),
        StringBuilder(),
        indentionLevel = 0u,
    ).also(block).toString()
}

class SimpleFileBuilder(
    private val packageName: String,
    private val imports: MutableSet<String>,
    private val topLevelSymbols: MutableSet<String>,
    private val builder: StringBuilder,
    val indentionLevel: UInt,
) : FileBuilder {
    private val indention = "    "

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

    override fun block(block: FileBuilder.() -> Unit) {
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