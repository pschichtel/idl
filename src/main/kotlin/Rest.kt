
@JvmInline
value class HttpMethod(val name: String) {
    companion object {
        val Get = HttpMethod("GET")
        val Post = HttpMethod("POST")
        val Put = HttpMethod("PUT")
        val Head = HttpMethod("HEAD")
        val Options = HttpMethod("OPTIONS")
        val Delete = HttpMethod("DELETE")
        val Patch = HttpMethod("PATCH")
        val Trace = HttpMethod("TRACE")
        val Query = HttpMethod("QUERY")
        val Connect = HttpMethod("CONNECT")
    }
}

data class OperationMetadata(
    override val name: String,
    override val summary: String?,
    override val description: String?,
) : Metadata

data class Content(
    val mimeType: String,
    val model: ModelReference<*>,
)

data class Operation(
    val metadata: OperationMetadata,
    val method: HttpMethod,
    val requestBody: Content?,
    val responseBody: Content?,
)

data class RestApiMetadata(
    val version: String,
    override val name: String,
    override val summary: String?,
    override val description: String?,
) : Metadata

data class ModelCollection(val models: Set<Model<*>>)

data class RestApi(
    val metadata: RestApiMetadata,
    val operations: Set<Operation>,
    val models: ModelCollection,
)

interface RestApiBuilder {
    var summary: String?
    var description: String?

    fun primitive(name: String, type: PrimitiveType, summary: String? = null, description: String? = null): ModelReference<Model.Primitive>
    fun record(name: String, builder: RecordBuilder.() -> Unit): ModelReference<Model.Record>
}

private class InternalRestApiBuilder(
    private val name: String,
    private val version: String,
) : RestApiBuilder {
    private val operations: MutableMap<String, Operation> = mutableMapOf()
    private val models: MutableMap<ModelId, Model<*>> = mutableMapOf()

    override var summary: String? = null
    override var description: String? = null

    fun build() = RestApi(
        RestApiMetadata(version, name, summary, description),
        operations.values.toSet(),
        ModelCollection(models.values.toSet()),
    )

    private inline fun <T : Model<T>> buildModel(block: (ModelId) -> Model<T>): ModelReference<T> {
        val id = ModelId.random()
        val model = block(id)
        models[id] = model
        return model.reference
    }

    override fun primitive(
        name: String,
        type: PrimitiveType,
        summary: String?,
        description: String?,
    ): ModelReference<Model.Primitive> = buildModel { id ->
        Model.Primitive(ModelMetadata(id, name, summary, description), type)
    }

    override fun record(
        name: String,
        builder: RecordBuilder.() -> Unit
    ): ModelReference<Model.Record> = buildModel { id ->
        InternalRecordBuilder(id, name).also(builder).build()
    }
}

fun buildRestApi(
    name: String,
    version: String,
    block: RestApiBuilder.() -> Unit,
): RestApi {
    return InternalRestApiBuilder(name, version).also(block).build()
}