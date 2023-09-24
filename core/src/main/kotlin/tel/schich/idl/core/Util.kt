package tel.schich.idl.core

fun resolveModelReference(fromModule: Module, allModules: List<Module>, modelRef: ModelReference): Pair<Module, Definition>? {
    val referencedModule = modelRef.module?.let { moduleRef ->
        allModules.firstOrNull { it.reference == moduleRef } ?: return null
    } ?: fromModule
    val referencedDefinition = referencedModule.definitions.firstOrNull { it.metadata.name == modelRef.name } ?: return null
    return Pair(referencedModule, referencedDefinition)
}

inline fun <reified T : Definition> resolveModuleReferenceOfType(fromModule: Module, allModules: List<Module>, modelRef: ModelReference): Pair<Module, T>? {
    return resolveModelReference(fromModule, allModules, modelRef)?.let { (module, def) ->
        (def as? T)?.let { Pair(module, it) }
    }
}

inline fun <reified T : Definition> resolveModelReferenceToDefinition(fromModule: Module, allModules: List<Module>, modelRef: ModelReference): T? {
    return resolveModuleReferenceOfType<T>(fromModule, allModules, modelRef)?.second
}


fun resolveForeignProperties(startModule: Module, startRecord: Model.Record, allModules: List<Module>): List<Pair<Module, RecordProperty>> {
    val pending = ArrayDeque<Pair<Module, Model.Record>>()
    val seen = mutableSetOf<Model.Record>()
    val properties = mutableListOf<Pair<Module, RecordProperty>>()

    fun next(module: Module, record: Model.Record) {
        seen.add(record)
        for (ref in record.propertiesFrom.asReversed()) {
            resolveModuleReferenceOfType<Model.Record>(module, allModules, ref)?.let {
                if (it.second !in seen) {
                    pending.addLast(it)
                }
            }
        }
    }

    next(startModule, startRecord)
    while (pending.isNotEmpty()) {
        val (currentModule, currentRecord) = pending.removeLast()

        for (property in currentRecord.properties) {
            properties.add(Pair(currentModule, property))
        }

        next(currentModule, currentRecord)
    }

    return properties
}

@JvmInline
value class CanonicalName(val words: List<String>) {
    override fun toString(): String {
        return words.joinToString(prefix = "CanonicalName(", separator = ", ", postfix = ")")
    }
}

fun canonicalName(name: String) =
    CanonicalName(splitIntoWords(name).map { it.lowercase() })

private fun isNonWordCharacter(c: Char): Boolean = when {
    c.isWhitespace() -> true
    c == '_' || c == '-' || c == '.' || c == ':' -> true
    else -> false
}

fun splitIntoWords(word: String, transform: (String) -> String = { it }): List<String> {
    if (word.isEmpty()) {
        return emptyList()
    }

    val words = mutableListOf<String>()
    val currentWord = StringBuilder(30)

    fun addWord() {
        words.add(transform(currentWord.toString()))
        currentWord.clear()
    }

    word.first().let {
        if (!isNonWordCharacter(it)) {
            currentWord.append(it)
        }
    }
    for (index in word.indices.drop(1)) {
        val current = word[index]

        when {
            isNonWordCharacter(current) -> {
                if (currentWord.isNotEmpty()) {
                    addWord()
                }
                continue
            }
            // splits can only happen on uppercase chars
            !current.isLowerCase() -> {
                val previousIsLowerCase = word[index - 1].isLowerCase()
                val nextIsLowerCase = word.getOrNull(index + 1)?.isLowerCase()
                // first of new word or end of an uppercase-run
                if (previousIsLowerCase || nextIsLowerCase == true) {
                    addWord()
                    currentWord.append(current)
                    continue
                }
            }
        }

        currentWord.append(current)
    }
    if (currentWord.isNotEmpty()) {
        addWord()
    }

    return words
}