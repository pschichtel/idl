import java.util.UUID

enum class PrimitiveType {
    STRING,
    INT32,
    INT64,
    FLOAT32,
    FLOAT64,
    BOOLEAN,
}

@JvmInline
value class ModelId(val id: UUID) {
    companion object {
        fun random() = ModelId(UUID.randomUUID())
    }
}

data class ModelMetadata(
    val id: ModelId,
    override val name: String,
    override val summary: String?,
    override val description: String?,
) : Metadata

data class ModelReference<T : Model<T>>(val id: ModelId)

sealed interface Model<T : Model<T>> {
    val metadata: ModelMetadata
    val name: String
        get() = metadata.name

    val reference: ModelReference<T>
        get() = ModelReference<T>(metadata.id)

    data class Alias(
        override val metadata: ModelMetadata,
        val aliasedModel: ModelReference<*>,
    ) : Model<Alias>

    data class Primitive(
        override val metadata: ModelMetadata,
        val type: PrimitiveType,
    ) : Model<Primitive>

    data class Record(
        override val metadata: ModelMetadata,
        val properties: List<RecordProperty>,
    ) : Model<Record>

    data class HomogenousList<I : Model<I>>(
        override val metadata: ModelMetadata,
        val itemType: ModelReference<I>,
    ) : Model<HomogenousList<I>>

    data class HomogenousMap<K : Model<K>, V : Model<V>>(
        override val metadata: ModelMetadata,
        val keyType: ModelReference<K>,
        val valueType: ModelReference<V>,
    ) : Model<HomogenousMap<K, V>>

    data class TaggedSum(
        override val metadata: ModelMetadata,
        val constructors: Map<String, ModelReference<*>>,
    ) : Model<TaggedSum>

    data class UntaggedSum(
        override val metadata: ModelMetadata,
        val constructors: Set<ModelReference<*>>,
    ) : Model<UntaggedSum>

    data class RawStream(
        override val metadata: ModelMetadata,
    ) : Model<RawStream>
}

interface RecordBuilder {
    val self: ModelReference<Model.Record>

    var summary: String?
    var description: String?

    fun requiredProperty(
        name: String,
        model: ModelReference<*>,
        summary: String? = null,
        description: String? = null,
        nullable: Boolean = false,
    )

    fun optionalProperty(
        name: String,
        model: ModelReference<*>,
        summary: String? = null,
        description: String? = null,
        nullable: Boolean = false,
    )

    fun property(name: String, model: ModelReference<*>, summary: String? = null, description: String? = null) =
        requiredProperty(name, model, summary, description, nullable = false)
}

data class RecordPropertyMetadata(
    override val name: String,
    override val summary: String?,
    override val description: String?,
) : Metadata

data class RecordProperty(
    val metadata: RecordPropertyMetadata,
    val model: ModelReference<*>,
    val required: Boolean,
    val nullable: Boolean,
)

internal class InternalRecordBuilder(private val id: ModelId, private val name: String) : RecordBuilder {
    override val self = ModelReference<Model.Record>(id)
    private val existingPropertyNames = mutableSetOf<String>()
    private val properties = mutableListOf<RecordProperty>()

    override var summary: String? = null
    override var description: String? = null

    private fun addProperty(
        name: String,
        model: ModelReference<*>,
        summary: String?,
        description: String?,
        required: Boolean,
        nullable: Boolean,
    ) {
        if (name in existingPropertyNames) {
            error("Property $name already exists!")
        }
        if (required && model == self) {
            error("Required property $name self-references, the resulting model cannot be instantiated!")
        }
        properties.add(RecordProperty(RecordPropertyMetadata(name, summary, description), model, required, nullable))
    }

    override fun optionalProperty(name: String, model: ModelReference<*>, summary: String?, description: String?, nullable: Boolean) {
        addProperty(name, model, summary, description, required = false, nullable)
    }

    override fun requiredProperty(name: String, model: ModelReference<*>, summary: String?, description: String?, nullable: Boolean) {
        addProperty(name, model, summary, description, required = true, nullable)
    }

    fun build(): Model.Record {
        return Model.Record(ModelMetadata(id, name, summary, description), properties.toList())
    }
}
