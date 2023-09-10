package tel.schich.idl.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.core.constraint.CollectionSizeRange
import tel.schich.idl.core.constraint.FloatValueRange
import tel.schich.idl.core.constraint.IntegerValueRange
import tel.schich.idl.core.constraint.StringLengthRange

@Serializable
sealed interface PrimitiveDataType {
    @Serializable
    @SerialName("string")
    data class String(
        val lengthRange: StringLengthRange? = null,
        @Serializable(with = RegexSerializer::class)
        val regex: Regex? = null,
        val startsWith: String? = null,
        val endsWith: String? = null,
    ) : PrimitiveDataType

    @Serializable
    @SerialName("integer")
    data class Integer(
        val size: UInt? = null,
        val signed: Boolean = true,
        val range: IntegerValueRange? = null,
    ) : PrimitiveDataType

    @Serializable
    @SerialName("float")
    data class Float(
        val size: UInt? = null,
        val signed: Boolean = true,
        val range: FloatValueRange? = null,
    ) : PrimitiveDataType

    @Serializable
    @SerialName("boolean")
    data object Bool : PrimitiveDataType

    @Serializable
    @SerialName("custom")
    data class Custom(val configuration: JsonElement = JsonNull) : PrimitiveDataType
}

@Serializable
data class Example(
    val metadata: BasicMetadata,
    val example: JsonElement,
)

@Serializable
data class ModelMetadata(
    override val name: String,
    override val description: String? = null,
    override val deprecated: Boolean = false,
    override val annotations: Map<String, String> = emptyMap(),
    val examples: Set<Example> = emptySet(),
) : Metadata

@Serializable
data class ModelReference(
    val module: ModuleReference? = null,
    val name: String,
)

@Serializable
@JvmInline
value class Tag(
    val tag: JsonPrimitive,
)

@Serializable
data class TaggedConstructor(
    val metadata: BasicMetadata,
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
    val nullable: Boolean = false,
    val default: DefaultValue? = null,
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
        val sizeRange: CollectionSizeRange? = null,
        val uniqueValues: Boolean = false,
    ) : Model

    @Serializable
    @SerialName("set")
    data class HomogenousSet(
        override val metadata: ModelMetadata,
        val itemModel: ModelReference,
        val sizeRange: CollectionSizeRange? = null,
    ) : Model

    @Serializable
    @SerialName("map")
    data class HomogenousMap(
        override val metadata: ModelMetadata,
        val keyModel: ModelReference,
        val valueModel: ModelReference,
        val sizeRange: CollectionSizeRange? = null,
        val uniqueValues: Boolean = false,
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
        val tagDataType: PrimitiveDataType,
        val constructors: List<TaggedConstructor>,
    ) : Model

    // TODO revamp naming of sum, tagged-sum and adt... ?
    @Serializable
    @SerialName("adt")
    data class Adt(
        override val metadata: ModelMetadata,
        val typeProperty: String = "type",
        val commonProperties: List<RecordProperty> = emptyList(),
        val constructors: List<Record>,
    ) : Model

    @Serializable
    @SerialName("enum")
    data class Enumeration(
        override val metadata: ModelMetadata,
        val dataType: PrimitiveDataType,
        val entries: List<EnumerationEntry>,
    ) : Model
}