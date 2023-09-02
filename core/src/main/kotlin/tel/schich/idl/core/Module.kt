package tel.schich.idl.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.nio.file.Path

@Serializable
@JvmInline
value class ModuleVersion(val version: String)

@Serializable
data class ModuleMetadata(
    override val name: String,
    val version: ModuleVersion,
    override val summary: String? = null,
    override val description: String? = null,
    override val annotations: Map<String, String> = emptyMap(),
    val sourcePath: Path? = null
) : Metadata

@Serializable
data class ModuleReference(
    val name: String,
    val version: ModuleVersion,
) {
    override fun toString(): String = "$name:${version.version}"

    companion object {
        fun parse(reference: String): ModuleReference? {
            val colonPosition = reference.lastIndexOf(':')
            if (colonPosition == -1) {
                return null
            }
            return ModuleReference(reference.substring(0, colonPosition), ModuleVersion(reference.substring(colonPosition + 1)))
        }
    }
}

@Serializable
data class Module(
    val metadata: ModuleMetadata,
    val definitions: List<Definition> = emptyList(),
    val operations: List<Operation> = emptyList(),
) {
    @Transient
    val reference = ModuleReference(metadata.name, metadata.version)
}