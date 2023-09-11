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