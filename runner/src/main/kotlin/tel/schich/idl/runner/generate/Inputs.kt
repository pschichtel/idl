package tel.schich.idl.runner.generate

import tel.schich.idl.core.Annotations
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.runner.loadAnnotations
import tel.schich.idl.runner.loadModule
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

data class GenerationInputs(val modules: List<Module>, val annotations: Annotations)

fun resolveToFiles(sources: List<Path>): List<Path> {
    return sources
        .asSequence()
        .filter { Files.exists(it) }
        .map { it.toRealPath() }
        .flatMap {
            when {
                Files.isDirectory(it) -> Files.walk(it).asSequence().filterNot(Files::isDirectory)
                else -> sequenceOf(it)
            }
        }
        .toList()
}

fun loadInputs(specInputs: List<Path>, annotationInputs: List<Path>): GenerationInputs {
    val modules = specInputs.map { inputFile ->
        val module = loadModule(inputFile)
        module.copy(metadata = module.metadata.copy(sourcePath = inputFile))
    }
    val annotations = annotationInputs.fold<_, Annotations>(emptyMap()) { merged, inputFile ->
        merged + loadAnnotations(inputFile)
    }

    return GenerationInputs(modules, annotations)
}

class UnknownSubjectsException(val unknownSubjects: Set<ModuleReference>) :
    IllegalArgumentException("Unknown subjects: ${unknownSubjects.joinToString(", ")}")

fun resolveSubjects(modules: List<Module>, requestedSubjects: List<ModuleReference>?): Set<ModuleReference> {
    val loadedModuleReferences = modules.map { it.reference }.toSet()
    return if (requestedSubjects.isNullOrEmpty()) {
        loadedModuleReferences
    } else {
        requestedSubjects.toSet().also { selectedSubjects ->
            val unknownSubjects = selectedSubjects - loadedModuleReferences
            if (unknownSubjects.isNotEmpty()) {
                throw UnknownSubjectsException(unknownSubjects)
            }
        }
    }
}