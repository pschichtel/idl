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

data class Metadata(val id: ModelId, val name: String, val summary: String, val description: String)

data class ModelReference<T : Model<T>>(val id: ModelId)
data class ModelReference3<T : Model3<T>>(val id: ModelId)
data class ModelReference2(val id: ModelId)

sealed interface Model<T : Model<T>> {
    val metadata: Metadata
    val name: String
        get() = metadata.name

    val reference: ModelReference<T>
        get() = ModelReference<T>(metadata.id)

    data class Alias(override val metadata: Metadata, val aliasedModel: ModelReference<*>) : Model<Alias>
    data class Primitive(override val metadata: Metadata, val type: PrimitiveType) : Model<Primitive>
    data class Record(override val metadata: Metadata, val properties: Map<String, Model<*>>) : Model<Record>
    data class HomogenousList<I : Model<I>>(override val metadata: Metadata, val itemType: Model<I>) : Model<HomogenousList<I>>
    data class HomogenousMap<K : Model<K>, V : Model<V>>(override val metadata: Metadata, val keyType: Model<K>, val valueType: Model<V>) : Model<HomogenousMap<K, V>>
    data class TaggedSum(override val metadata: Metadata, val constructors: Map<String, Model<*>>) : Model<TaggedSum>
    data class UntaggedSum(override val metadata: Metadata, val constructors: Set<Model<*>>) : Model<UntaggedSum>
}

sealed interface Model3<T : Model3<T>> {
    val metadata: Metadata
    val name: String
        get() = metadata.name

    val reference: ModelReference3<T>
        get() = ModelReference3<T>(metadata.id)

    data class Alias(override val metadata: Metadata, val aliasedModel: ModelReference<*>) : Model3<Alias>
    data class Primitive(override val metadata: Metadata, val type: PrimitiveType) : Model3<Primitive>
    data class Record(override val metadata: Metadata, val properties: Map<String, ModelReference<*>>) : Model3<Record>
    data class HomogenousList<I : Model<I>>(override val metadata: Metadata, val itemType: ModelReference<I>) : Model3<HomogenousList<I>>
    data class HomogenousMap<K : Model<K>, V : Model<V>>(override val metadata: Metadata, val keyType: ModelReference<K>, val valueType: ModelReference<V>) : Model3<HomogenousMap<K, V>>
    data class TaggedSum(override val metadata: Metadata, val constructors: Map<String, ModelReference<*>>) : Model3<TaggedSum>
    data class UntaggedSum(override val metadata: Metadata, val constructors: Set<ModelReference<*>>) : Model3<UntaggedSum>
}

sealed interface Model2 {
    val metadata: Metadata
    val name: String
        get() = metadata.name

    val reference: ModelReference2
        get() = ModelReference2(metadata.id)

    data class Alias(override val metadata: Metadata, val aliasedModel: ModelReference<*>) : Model2
    data class Primitive(override val metadata: Metadata, val type: PrimitiveType) : Model2
    data class Record(override val metadata: Metadata, val properties: Map<String, Model2>) : Model2
    data class HomogenousList(override val metadata: Metadata, val itemType: Model2) : Model2
    data class HomogenousMap(override val metadata: Metadata, val keyType: Model2, val valueType: Model2) : Model2
    data class TaggedSum(override val metadata: Metadata, val constructors: Map<String, Model2>) : Model2
    data class UntaggedSum(override val metadata: Metadata, val constructors: Set<Model2>) : Model2
}

fun main() {
    println("Hello world! ${Model.Primitive(Metadata(ModelId.random(), "a", "b", "c"), PrimitiveType.BOOLEAN)}")
    println("Hello world! ${Model2.Primitive(Metadata(ModelId.random(), "a", "b", "c"), PrimitiveType.BOOLEAN)}")
    println("Hello world! ${Model3.Primitive(Metadata(ModelId.random(), "a", "b", "c"), PrimitiveType.BOOLEAN)}")
}