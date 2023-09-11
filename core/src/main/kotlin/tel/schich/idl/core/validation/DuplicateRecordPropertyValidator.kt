package tel.schich.idl.core.validation

import tel.schich.idl.core.Model
import tel.schich.idl.core.ModelReference
import tel.schich.idl.core.Module
import tel.schich.idl.core.ModuleReference
import tel.schich.idl.core.resolveModelReferenceToDefinition

data class DuplicateRecordPropertyError(
    override val module: ModuleReference,
    val definition: String,
    val property: String,
    val indices: List<Int>,
) : ValidationError

data class RecordPropertySourcesOverlappingError(
    override val module: ModuleReference,
    val definition: String,
    val properties: Set<String>,
    val sourceA: ModelReference,
    val sourceB: ModelReference,
) : ValidationError

data class RecordPropertyOverlapsWithPropertySourcesError(
    override val module: ModuleReference,
    val definition: String,
    val properties: Set<String>,
    val source: ModelReference,
) : ValidationError

object DuplicateRecordPropertyValidator : ModuleValidator {
    override fun validate(module: Module, allModules: List<Module>): ValidationResult {

        fun resolvePropertySources(record: Model.Record): List<Pair<ModelReference, Model.Record>> {
            return record.propertiesFrom.mapNotNull { ref ->
                resolveModelReferenceToDefinition<Model.Record>(module, allModules, ref)?.let { Pair(ref, it) }
            }
        }
        fun resolvePropertySet(start: Model.Record): Set<String> {
            val pending = ArrayDeque<Model.Record>()
            val seen = mutableSetOf<Model.Record>()
            val properties = mutableSetOf<String>()

            pending.addLast(start)
            while (pending.isNotEmpty()) {
                val record = pending.removeFirst()
                seen.add(record)
                properties.addAll(record.properties.map { it.metadata.name })

                pending.addAll(resolvePropertySources(record).map { it.second }.filterNot { it in seen })
            }

            return properties
        }

        val duplicatePropertyErrors = module.definitions
            .filterIsInstance<Model.Record>()
            .flatMap { record ->
                buildList {
                    record.properties
                        .withIndex()
                        .groupBy({ it.value.metadata.name }, { it.index })
                        .filter { it.value.size > 1 }
                        .forEach { (propertyName, indices) ->
                            add(DuplicateRecordPropertyError(module.reference, record.metadata.name, propertyName, indices))
                        }

                    if (record.propertiesFrom.isNotEmpty()) {
                        val ownProperties = record.properties.map { it.metadata.name }.toSet()
                        val sources = resolvePropertySources(record)
                            .map { (ref, record) -> Triple(ref, record, resolvePropertySet(record)) }
                        for ((refA, a, propertiesA) in sources) {
                            val ownOverlap = propertiesA.intersect(ownProperties)
                            if (ownOverlap.isNotEmpty()) {
                                add(RecordPropertyOverlapsWithPropertySourcesError(module.reference, record.metadata.name, ownOverlap, refA))
                            }

                            for ((refB, b, propertiesB) in sources) {
                                if (a == b) {
                                    continue
                                }
                                val overlap = propertiesA.intersect(propertiesB)
                                if (overlap.isNotEmpty()) {
                                    add(RecordPropertySourcesOverlappingError(module.reference, record.metadata.name, overlap, refA, refB))
                                }
                            }
                        }
                    }
                }
            }

        if (duplicatePropertyErrors.isEmpty()) {
            return ValidationResult.Valid
        }

        return ValidationResult.Invalid(duplicatePropertyErrors.toSet())
    }
}