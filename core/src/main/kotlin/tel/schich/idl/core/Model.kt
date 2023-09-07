package tel.schich.idl.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.core.constraint.HomogenousListConstraint
import tel.schich.idl.core.constraint.HomogenousMapConstraint
import tel.schich.idl.core.constraint.HomogenousSetConstraint
import tel.schich.idl.core.constraint.PrimitiveConstraint

@Serializable
@JvmInline
value class PrimitiveDataType(
    val name: String,
)

@Serializable
data class Example(
    val metadata: BasicMetadata,
    val example: JsonElement,
)

@Serializable
data class ModelMetadata(
    override val name: String,
    override val summary: String? = null,
    override val description: String? = null,
    override val annotations: Map<String, String> = emptyMap(),
    val examples: Set<Example> = emptySet(),
    val deprecated: Boolean = false,
) : Metadata

@Serializable
data class ModelReference(
    val module: ModuleReference? = null,
    val name: String,
)

@Serializable
@JvmInline
value class Tag(
    val tag: String,
)

@Serializable
data class TaggedConstructor(
    val metadata: ModelMetadata,
    val tag: Tag,
    val model: ModelReference,
)

@Serializable
data class EnumerationEntry(
    val metadata: BasicMetadata,
    val value: JsonElement = JsonPrimitive(metadata.name),
)

@Serializable
sealed interface Definition {
    val metadata: ModelMetadata
}

@Serializable
@SerialName("alias")
data class Alias(
    override val metadata: ModelMetadata,
    val aliasedModel: ModelReference,
) : Definition

@Serializable
data class DefaultValue(val value: JsonElement)

@Serializable
data class RecordProperty(
    val metadata: BasicMetadata,
    val model: ModelReference,
    // TODO can this inferred from nullable and default ?
    val nullable: Boolean = false,
    val default: DefaultValue? = null,
    val deprecated: Boolean = false,
)

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed interface Model : Definition {
    @Serializable
    @SerialName("unknown")
    data class Unknown(
        override val metadata: ModelMetadata,
    ) : Model

    @Serializable
    @SerialName("constant")
    data class Constant(
        override val metadata: ModelMetadata,
        val dataType: PrimitiveDataType,
        val value: JsonElement,
    ) : Model

    @Serializable
    @SerialName("primitive")
    data class Primitive(
        override val metadata: ModelMetadata,
        val dataType: PrimitiveDataType,
        val constraints: Set<PrimitiveConstraint> = emptySet(),
    ) : Model

    @Serializable
    @SerialName("record")
    data class Record(
        override val metadata: ModelMetadata,
        val properties: List<RecordProperty>,
    ) : Model

    @Serializable
    @SerialName("list")
    data class HomogenousList(
        override val metadata: ModelMetadata,
        val itemModel: ModelReference,
        val constraints: Set<HomogenousListConstraint> = emptySet(),
    ) : Model

    @Serializable
    @SerialName("set")
    data class HomogenousSet(
        override val metadata: ModelMetadata,
        val itemModel: ModelReference,
        val constraints: Set<HomogenousSetConstraint> = emptySet(),
    ) : Model

    @Serializable
    @SerialName("map")
    data class HomogenousMap(
        override val metadata: ModelMetadata,
        val keyModel: ModelReference,
        val valueModel: ModelReference,
        val constraints: Set<HomogenousMapConstraint> = emptySet(),
    ) : Model

    @Serializable
    @SerialName("product")
    data class Product(
        override val metadata: ModelMetadata,
        val components: List<ModelReference>,
    ) : Model

    @Serializable
    @SerialName("sum")
    data class Sum(
        override val metadata: ModelMetadata,
        val constructors: Set<ModelReference>,
    ) : Model

    @Serializable
    @SerialName("tagged-sum")
    data class TaggedSum(
        override val metadata: ModelMetadata,
        val constructors: Set<TaggedConstructor>,
    ) : Model

    @Serializable
    @SerialName("raw")
    data class RawStream(
        override val metadata: ModelMetadata,
    ) : Model

    @Serializable
    @SerialName("enum")
    data class Enumeration(
        override val metadata: ModelMetadata,
        val dataType: PrimitiveDataType,
        val entries: List<EnumerationEntry>,
    ) : Model
}