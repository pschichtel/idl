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