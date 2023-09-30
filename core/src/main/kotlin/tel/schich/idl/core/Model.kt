package tel.schich.idl.core

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import tel.schich.idl.core.constraint.CollectionSizeRange
import tel.schich.idl.core.constraint.FloatValueRange
import tel.schich.idl.core.constraint.IntegerValueRange
import tel.schich.idl.core.constraint.StringLengthRange

@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
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
    override val annotations: Annotations = emptyMap(),
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
data class TaggedSumConstructor(
    val metadata: BasicMetadata,
    val tag: Tag,
    val model: ModelReference,
)

@Serializable
data class SumConstructor(
    val metadata: BasicMetadata,
    val model: ModelReference,
)

@Serializable
data class EnumerationEntry(
    val metadata: BasicMetadata,
    val value: JsonPrimitive = JsonPrimitive(metadata.name),
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

@Serializable(RecordPropertySerializer::class)
data class RecordProperty(
    val metadata: BasicMetadata,
    val model: ModelReference,
    val nullable: Boolean = false,
    val default: JsonElement? = null,
)

private object RecordPropertySerializer : KSerializer<RecordProperty> {
    override val descriptor = buildClassSerialDescriptor(RecordProperty::class.qualifiedName!!) {
        element("metadata", BasicMetadata.serializer().descriptor)
        element("model", ModelReference.serializer().descriptor)
        element("nullable", Boolean.serializer().descriptor, isOptional = true)
        element("default", JsonElement.serializer().descriptor, isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: RecordProperty) {
        encoder.beginStructure(descriptor).apply {
            encodeSerializableElement(descriptor, index = 0, BasicMetadata.serializer(), value.metadata)
            encodeSerializableElement(descriptor, index = 1, ModelReference.serializer(), value.model)
            if (value.nullable) {
                encodeBooleanElement(descriptor, index = 2, value = true)
            }
            value.default?.let {
                encodeSerializableElement(descriptor, index = 3, JsonElement.serializer(), it)
            }
        }.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        var metadata: BasicMetadata? = null
        var model: ModelReference? = null
        var nullable = false
        var default: JsonElement? = null

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE ->
                    break
                CompositeDecoder.UNKNOWN_NAME ->
                    throw SerializationException("unknown field!")
                0 ->
                    metadata = decodeSerializableElement(descriptor, index, BasicMetadata.serializer())
                1 ->
                    model = decodeSerializableElement(descriptor, index, ModelReference.serializer())
                2 ->
                    nullable = decodeBooleanElement(descriptor, index)
                3 ->
                    default = decodeSerializableElement(descriptor, index, JsonElement.serializer())
            }
        }
        if (metadata == null) {
            throw SerializationException("metadata is required in RecordProperty!")
        }
        if (model == null) {
            throw SerializationException("model is required in RecordProperty!")
        }
        RecordProperty(metadata, model, nullable, default)
    }
}

@Serializable
data class AdtConstructor(
    val metadata: BasicMetadata,
    val properties: List<RecordProperty>,
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
    @SerialName("primitive")
    data class Primitive(
        override val metadata: ModelMetadata,
        val dataType: PrimitiveDataType,
    ) : Model

    @Serializable
    @SerialName("record")
    data class Record(
        override val metadata: ModelMetadata,
        val propertiesFrom: List<ModelReference> = emptyList(),
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
        val constructors: List<SumConstructor>,
    ) : Model

    @Serializable
    @SerialName("tagged-sum")
    data class TaggedSum(
        override val metadata: ModelMetadata,
        val tagDataType: PrimitiveDataType,
        val constructors: List<TaggedSumConstructor>,
    ) : Model

    // TODO revamp naming of sum, tagged-sum and adt... ?
    @Serializable
    @SerialName("adt")
    data class Adt(
        override val metadata: ModelMetadata,
        val typeProperty: String = "type",
        val commonProperties: List<RecordProperty> = emptyList(),
        val constructors: List<AdtConstructor>,
    ) : Model

    @Serializable
    @SerialName("enum")
    data class Enumeration(
        override val metadata: ModelMetadata,
        val dataType: PrimitiveDataType,
        val entries: List<EnumerationEntry>,
    ) : Model
}