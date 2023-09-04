@file:UseSerializers(
    URISerializer::class,
    LenientBigIntegerSerializer::class,
    BigDecimalSerializer::class,
    HttpStatusCodeSerializer::class,
    JsonPointerSerializer::class,
)

package tel.schich.idl.generator.openapi

import kotlinx.serialization.Contextual
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import kotlin.reflect.KClass

// https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#oasObject
@Serializable
data class OpenApiSpec(
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-9.3.1
    @SerialName("\$id")
    val id: URI? = null,
    val openapi: String,
    val info: Info,
    val servers: List<Server> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val security: List<SecurityRequirement> = emptyList(),
    val paths: Map<PathPattern, @Contextual PathItem> = emptyMap(),
    val components: Components = Components(),
)

@Serializable
data class PathItem(
    @SerialName(ReferenceObject.REF_FIELD_NAME)
    val reference: Reference? = null,
    val summary: String? = null,
    val description: String? = null,
    val get: Operation? = null,
    val put: Operation? = null,
    val post: Operation? = null,
    val delete: Operation? = null,
    val options: Operation? = null,
    val head: Operation? = null,
    val patch: Operation? = null,
    val trace: Operation? = null,
    val servers: List<Server>? = null,
    val parameters: List<@Contextual Parameter> = emptyList(),
)

@Serializable
@JvmInline
value class ExampleValue(val json: JsonElement)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("in")
sealed interface Parameter {
    val name: String
    val description: String?
    val required: Boolean
    val deprecated: Boolean
    val explode: Boolean
    val schema: StrictSchema?
    val content: Contents?
    val example: ExampleValue?
    val examples: Map<String, Example>

    @Serializable
    @SerialName("path")
    data class Path(
        override val name: String,
        override val description: String? = null,
        override val required: Boolean = false,
        override val deprecated: Boolean = false,
        val style: PathParameterStyle = PathParameterStyle.SIMPLE,
        override val explode: Boolean = style.explode,
        override val schema: StrictSchema? = null,
        override val content: Contents? = null,
        override val example: ExampleValue? = null,
        override val examples: Map<String, @Contextual Example> = emptyMap(),
    ) : Parameter

    @Serializable
    @SerialName("query")
    data class Query(
        override val name: String,
        override val description: String? = null,
        override val required: Boolean = false,
        override val deprecated: Boolean = false,
        val style: QueryParameterStyle = QueryParameterStyle.FORM,
        override val explode: Boolean = style.explode,
        val allowReserved: Boolean = false,
        override val schema: StrictSchema? = null,
        override val content: Contents? = null,
        override val example: ExampleValue? = null,
        override val examples: Map<String, @Contextual Example> = emptyMap(),
    ) : Parameter

    @Serializable
    @SerialName("header")
    data class Header(
        override val name: String,
        override val description: String? = null,
        override val required: Boolean = false,
        override val deprecated: Boolean = false,
        override val style: HeaderParameterStyle = HeaderParameterStyle.SIMPLE,
        override val explode: Boolean = style.explode,
        override val schema: StrictSchema? = null,
        override val content: Contents? = null,
        override val example: ExampleValue? = null,
        override val examples: Map<String, @Contextual Example> = emptyMap(),
    ) : Parameter, HeaderDescription

    @Serializable
    @SerialName("cookie")
    data class Cookie(
        override val name: String,
        override val description: String? = null,
        override val required: Boolean = false,
        override val deprecated: Boolean = false,
        val style: CookieParameterStyle = CookieParameterStyle.FORM,
        override val explode: Boolean = style.explode,
        override val schema: StrictSchema? = null,
        override val content: Contents? = null,
        override val example: ExampleValue? = null,
        override val examples: Map<String, @Contextual Example> = emptyMap(),
    ) : Parameter
}

sealed interface HeaderDescription {
    val description: String?
    val required: Boolean
    val deprecated: Boolean
    val style: HeaderParameterStyle
    val explode: Boolean
    val schema: StrictSchema?
    val content: Contents?
    val example: ExampleValue?
    val examples: Map<String, Example>
}

@Serializable
data class Header(
    override val description: String? = null,
    override val required: Boolean = false,
    override val deprecated: Boolean = false,
    override val style: HeaderParameterStyle = HeaderParameterStyle.SIMPLE,
    override val explode: Boolean = style.explode,
    override val schema: StrictSchema? = null,
    override val content: Contents? = null,
    override val example: ExampleValue? = null,
    override val examples: Map<String, @Contextual Example> = emptyMap(),
) : HeaderDescription

@Serializable
data class Example(
    val summary: String? = null,
    val description: String? = null,
    val value: ExampleValue,
    val externalValue: URI? = null,
)

@Serializable
enum class PathParameterStyle(@Transient val explode: Boolean = false) {
    @SerialName("matrix")
    MATRIX,
    @SerialName("label")
    LABEL,
    @SerialName("simple")
    SIMPLE,
}

@Serializable
enum class QueryParameterStyle(@Transient val explode: Boolean = false) {
    @SerialName("form")
    FORM(explode = true),
    @SerialName("spaceDelimited")
    SPACE_DELIMITED,
    @SerialName("pipeDelimited")
    PIPE_DELIMITED,
    @SerialName("deepObject")
    DEEP_OBJECT,
}

@Serializable
enum class HeaderParameterStyle(@Transient val explode: Boolean = false) {
    @SerialName("simple")
    SIMPLE,
}

@Serializable
enum class CookieParameterStyle(@Transient val explode: Boolean = false) {
    @SerialName("form")
    FORM(explode = true),
}

@Serializable
@JvmInline
value class SecurityRequirement(val requirement: Map<SchemaName, List<SecurityScope>>)

@Serializable
@JvmInline
value class SecurityScope(val name: String)

@Serializable
data class Components(
    val schemas: Map<SchemaName, Schema> = emptyMap(),
    val securitySchemes: Map<SchemaName, SecuritySchema> = emptyMap(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface SecuritySchema : ReferenceOrSecuritySchema {

    val description: String?

    @Serializable
    @SerialName("apiKey")
    data class ApiKey(
        override val description: String? = null,
        val name: String,
        @SerialName("in")
        val where: String,
    ) : SecuritySchema

    @Serializable
    @SerialName("http")
    data class Http(
        override val description: String? = null,
        val schema: String,
        val bearerFormat: String,
    ) : SecuritySchema

    @Serializable
    @SerialName("oauth2")
    data class OAuth2(
        override val description: String? = null,
        val flows: OAuth2Flows,
    ) : SecuritySchema
}

@Serializable(with = ReferenceOrSecuritySchemaSerializer::class)
sealed interface ReferenceOrSecuritySchema

@Serializable
data class OAuth2Flows(
    val implicit: OAuth2ImplicitFlow,
    val password: OAuth2PasswordFlow,
    val clientCredentials: OAuth2ClientCredentialsFlow,
    val authorizationCode: OAuth2AuthorizationCodeFlow,
)

@Serializable
@JvmInline
value class SecurityScopes(val scopes: Map<SecurityScope, String>)

@Serializable
data class OAuth2ImplicitFlow(
    val authorizationUrl: URI,
    val refreshUrl: URI,
    val scopes: SecurityScopes,
)

@Serializable
data class OAuth2PasswordFlow(
    val tokenUrl: URI,
    val refreshUrl: URI,
    val scopes: SecurityScopes,
)

@Serializable
data class OAuth2ClientCredentialsFlow(
    val tokenUrl: URI,
    val refreshUrl: URI,
    val scopes: SecurityScopes,
)

@Serializable
data class OAuth2AuthorizationCodeFlow(
    val authorizationUrl: URI,
    val tokenUrl: URI,
    val refreshUrl: URI,
    val scopes: SecurityScopes,
)

@Serializable
enum class SecuritySchemaType {
    @SerialName("apiKey")
    API_KEY,
    @SerialName("http")
    HTTP,
    @SerialName("oauth2")
    OAUTH2,
    @SerialName("openIdConnect")
    OPENID_CONNECT,
}

@Serializable
data class Server(
    val description: String? = null,
    val url: URI,
    val variable: Map<ServerVariableName, ServerVariable> = emptyMap(),
)

@Serializable
@JvmInline
value class ServerVariableName(val name: String)

@Serializable
data class ServerVariable(
    val enum: Set<String>? = null,
    val default: String,
    val description: String?,
)

@Serializable
data class Info(
    val title: String,
    val summary: String? = null,
    val description: String? = null,
    val termsOfService: String? = null,
    val contact: Contact? = null,
    val license: License? = null,
    val version: String?,
)

@Serializable
data class Contact(
    val name: String? = null,
    val url: URI? = null,
    val email: String? = null,
)

@Serializable
data class License(
    val name: String,
    val identifier: SpdxIdentifier? = null,
    val url: URI? = null,
)

@Serializable
@JvmInline
value class SpdxIdentifier(val id: String)

@Serializable
data class Tag(
    val name: TagName,
    val description: String? = null,
    val externalDocs: ExternalDocs? = null,
)

@Serializable
@JvmInline
value class TagName(val name: String)

@Serializable
@JvmInline
value class PathPattern(val pattern: String)

@Serializable
data class Operation(
    val operationId: String,
    val tags: Set<TagName> = emptySet(),
    val summary: String? = null,
    val description: String? = null,
    val externalDocs: ExternalDocs? = null,
    val parameters: List<@Contextual Parameter> = emptyList(),
    @Contextual
    val requestBody: RequestBody? = null,
    @Contextual
    val responses: Responses = Responses(),
    val deprecated: Boolean = false,
    val security: List<SecurityRequirement>? = null,
    val servers: List<Server>? = null,
)

@Serializable
@JvmInline
value class SchemaName(val name: String)

@Serializable
data class RequestBody(
    val description: String? = null,
    val content: Contents,
    val required: Boolean = false,
)

@Serializable
data class Response(
    val description: String,
    val headers: Map<String, @Contextual Header> = emptyMap(),
    val content: Contents? = null,
)

@Serializable
@JvmInline
value class Contents(val contents: Map<MimeType, MediaType>)

@Serializable
@JvmInline
value class MimeType(val type: String)

@Serializable
data class MediaType(
    val schema: StrictSchema,
    val example: ExampleValue? = null,
    val examples: Map<String, @Contextual Example> = emptyMap(),
)

@Serializable
@JvmInline
value class HttpStatusCode(val code: UShort)

data class Responses(val default: Response? = null, val statuses: Map<HttpStatusCode, Response> = emptyMap())

class ResponsesSerializer(responseKSerializer: KSerializer<Response>) : KSerializer<Responses> {
    private val mapSerializer = MapSerializer(String.serializer(), responseKSerializer)
    private val defaultFieldName = "default"

    @ExperimentalSerializationApi
    override val descriptor: SerialDescriptor = object : SerialDescriptor by mapSerializer.descriptor {
        override val serialName: String = "Responses"
    }

    override fun serialize(encoder: Encoder, value: Responses) {
        val pairs = listOfNotNull(
            value.default?.let { Pair(defaultFieldName, it) },
        ) + value.statuses.map { (key, value) -> Pair(key.code.toString(), value) }

        mapSerializer.serialize(encoder, pairs.toMap())
    }

    override fun deserialize(decoder: Decoder): Responses {
        val map = mapSerializer.deserialize(decoder).toMutableMap()
        val default = map.remove(defaultFieldName)
        val statuses = map.mapKeys { (key, _) -> HttpStatusCode(key.toUShort()) }
        return Responses(default, statuses)
    }
}

@Serializable
data class ExternalDocs(
    val url: URI,
    val description: String? = null,
)

@Serializable
@JvmInline
value class TypeFormat(val format: String)

@Serializable
@JvmInline
value class PropertyName(val name: String)

@Serializable
@JvmInline
value class RegularExpression(val raw: String) {
    fun toRegex() = raw.toRegex()
}

@Serializable
enum class SchemaType {
    @SerialName("object")
    OBJECT,
    @SerialName("array")
    ARRAY,
    @SerialName("number")
    NUMBER,
    @SerialName("integer")
    INTEGER,
    @SerialName("boolean")
    BOOLEAN,
    @SerialName("string")
    STRING,
}

@Serializable
data class Schema(
    val type: SchemaType? = null,
    @SerialName("\$ref")
    val ref: Reference? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.1
    val title: String? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.1
    val description: String? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.2
    val default: JsonElement? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.3
    val deprecated: Boolean? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.4
    val readOnly: Boolean? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.4
    val writeOnly: Boolean? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.5
    val examples: List<JsonElement>? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.1.2
    val enum: List<JsonElement>? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.1.3
    val const: JsonElement? = null,
    // https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#fixed-fields-20
    val externalDocs: ExternalDocs? = null,

    val format: TypeFormat? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.2.1
    val multipleOf: BigInteger? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.2.2
    val maximum: BigInteger? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.2.3
    val exclusiveMaximum: BigInteger? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.2.4
    val minimum: BigInteger? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.2.5
    val exclusiveMinimum: BigInteger? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.3.1
    val maxLength: Long? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.3.2
    val minLength: Long? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.3.3
    val pattern: RegularExpression? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-8.3
    val contentEncoding: String? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-8.4
    val contentMediaType: MediaType? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-8.5
    val contentSchema: Schema? = null,

    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.3.1.1
    val prefixItems: List<Schema>? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.3.1.2
    val items: Schema? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.3.1.3
    val contains: Schema? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.4.1
    val maxItems: BigInteger? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.4.2
    val minItems: BigInteger? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.4.3
    val uniqueItems: Boolean? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.4.4
    val maxContains: BigInteger? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.4.5
    val minContains: BigInteger? = null,

    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.3.2.1
    val properties: Map<PropertyName, Schema>? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.3.2.2
    val patternProperties: Map<RegularExpression, Schema>? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.3.2.3
    val additionalProperties: Schema? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.3.2.4
    val propertyNames: Schema? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.5.1
    val maxProperties: BigInteger? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.5.2
    val minProperties: BigInteger? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.5.3
    val required: List<PropertyName>? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.5.4
    val dependentRequired: Map<PropertyName, Set<PropertyName>>? = null,

    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.2.1.1
    val allOf: List<Schema>? = null,
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.2.1.3
    val oneOf: List<Schema>? = null,
    // https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#fixed-fields-20
    val discriminator: Discriminator? = null,
) {
    companion object {
        val Empty = Schema()
    }
}

@Serializable
sealed interface StrictSchema {

    sealed interface PrimitiveInstanceSchema : InstanceSchema {
        val format: TypeFormat?
    }

    sealed interface NumericInstanceSchema<T : Number> : PrimitiveInstanceSchema {
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.2.1
        val multipleOf: T?
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.2.2
        val maximum: T?
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.2.3
        val exclusiveMaximum: T?
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.2.4
        val minimum: T?
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.2.5
        val exclusiveMinimum: T?
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.1.1 (intentionally ignored array variant)
    @JsonClassDiscriminator(InstanceSchema.TYPE_FIELD_NAME)
    sealed interface InstanceSchema : StrictSchema {

        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.1
        val title: String?
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.1
        val description: String?
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.2
        val default: JsonElement?
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.3
        val deprecated: Boolean
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.4
        val readOnly: Boolean
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.4
        val writeOnly: Boolean
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-9.5
        val examples: List<JsonElement>?
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.1.2
        val enum: List<JsonElement>?
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.1.3
        val const: JsonElement?
        // https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#fixed-fields-20
        val externalDocs: ExternalDocs?

        @Serializable
        @SerialName("object")
        data class ObjectSchema(
            override val title: String? = null,
            override val description: String? = null,
            override val default: JsonElement? = null,
            override val deprecated: Boolean = false,
            override val readOnly: Boolean = false,
            override val writeOnly: Boolean = false,
            override val examples: List<JsonElement>? = null,
            override val enum: List<JsonElement>? = null,
            override val const: JsonElement? = null,
            override val externalDocs: ExternalDocs? = null,
        ) : InstanceSchema

        @Serializable
        @SerialName("array")
        data class ArraySchema(
            override val title: String? = null,
            override val description: String? = null,
            override val default: JsonElement? = null,
            override val deprecated: Boolean = false,
            override val readOnly: Boolean = false,
            override val writeOnly: Boolean = false,
            override val examples: List<JsonElement>? = null,
            override val enum: List<JsonElement>? = null,
            override val const: JsonElement? = null,
            override val externalDocs: ExternalDocs? = null,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.3.1.1
            val prefixItems: List<StrictSchema> = emptyList(),
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.3.1.2
            val items: StrictSchema? = null,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.3.1.3
            val contains: StrictSchema? = null,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.4.1
            val maxItems: BigInteger? = null,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.4.2
            val minItems: BigInteger = BigInteger.ZERO,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.4.3
            val uniqueItems: Boolean = false,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.4.4
            val maxContains: BigInteger? = null,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.4.5
            val minContains: BigInteger = BigInteger.ONE,
        ) : InstanceSchema

        @Serializable
        @SerialName("boolean")
        data class BooleanSchema(
            override val title: String? = null,
            override val description: String? = null,
            override val default: JsonElement? = null,
            override val deprecated: Boolean = false,
            override val readOnly: Boolean = false,
            override val writeOnly: Boolean = false,
            override val examples: List<JsonElement>? = null,
            override val enum: List<JsonElement>? = null,
            override val const: JsonElement? = null,
            override val externalDocs: ExternalDocs? = null,
            override val format: TypeFormat? = null,
        ) : PrimitiveInstanceSchema

        @Serializable
        @SerialName("integer")
        data class IntegerSchema(
            override val title: String? = null,
            override val description: String? = null,
            override val default: JsonElement? = null,
            override val deprecated: Boolean = false,
            override val readOnly: Boolean = false,
            override val writeOnly: Boolean = false,
            override val examples: List<JsonElement>? = null,
            override val enum: List<JsonElement>? = null,
            override val const: JsonElement? = null,
            override val externalDocs: ExternalDocs? = null,
            override val format: TypeFormat? = null,
            override val multipleOf: BigInteger? = null,
            override val maximum: BigInteger? = null,
            override val exclusiveMaximum: BigInteger? = null,
            override val minimum: BigInteger? = null,
            override val exclusiveMinimum: BigInteger? = null,
        ) : NumericInstanceSchema<BigInteger> {
            companion object {
                // from https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#data-types
                val Int32Format = TypeFormat("int32")
                val Int64Format = TypeFormat("int64")
            }
        }

        @Serializable
        @SerialName("number")
        data class NumberSchema(
            override val title: String? = null,
            override val description: String? = null,
            override val default: JsonElement? = null,
            override val deprecated: Boolean = false,
            override val readOnly: Boolean = false,
            override val writeOnly: Boolean = false,
            override val examples: List<JsonElement>? = null,
            override val enum: List<JsonElement>? = null,
            override val const: JsonElement? = null,
            override val externalDocs: ExternalDocs? = null,
            override val format: TypeFormat? = null,
            override val multipleOf: BigDecimal? = null,
            override val maximum: BigDecimal? = null,
            override val exclusiveMaximum: BigDecimal? = null,
            override val minimum: BigDecimal? = null,
            override val exclusiveMinimum: BigDecimal? = null,
        ) : NumericInstanceSchema<BigDecimal> {
            companion object {
                // from https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#data-types
                val FloatFormat = TypeFormat("float")
                val DoubleFormat = TypeFormat("double")
            }
        }

        @Serializable
        @SerialName("string")
        data class StringSchema(
            override val title: String? = null,
            override val description: String? = null,
            override val default: JsonElement? = null,
            override val deprecated: Boolean = false,
            override val readOnly: Boolean = false,
            override val writeOnly: Boolean = false,
            override val examples: List<JsonElement>? = null,
            override val enum: List<JsonElement>? = null,
            override val const: JsonElement? = null,
            override val externalDocs: ExternalDocs? = null,
            override val format: TypeFormat? = null,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.3.1
            val maxLength: Long? = null,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.3.2
            val minLength: Long? = null,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-6.3.3
            val pattern: RegularExpression? = null,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-8.3
            val contentEncoding: String? = null,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-8.4
            val contentMediaType: MediaType? = null,
            // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-8.5
            val contentSchema: StrictSchema? = null,
        ) : PrimitiveInstanceSchema {
            companion object {
                // from https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#data-types
                val PasswordFormat = TypeFormat("password")
                // from https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-7.3.1
                val DurationFormat = TypeFormat("duration")
                val TimeFormat = TypeFormat("time")
                val DateFormat = TypeFormat("date")
                val DateTimeFormat = TypeFormat("date-time")
                // from https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-7.3.2
                val EmailFormat = TypeFormat("email")
                val IdnEmailFormat = TypeFormat("idn-email")
                // from https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-7.3.3
                val HostnameFormat = TypeFormat("hostname")
                val IdnHostnameFormat = TypeFormat("idn-hostname")
                // from https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-7.3.4
                val Ipv4Format = TypeFormat("ipv4")
                val Ipv6Format = TypeFormat("ipv6")
                // from https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-7.3.5
                val UriFormat = TypeFormat("uri")
                val UriReferenceFormat = TypeFormat("uri-reference")
                val IriFormat = TypeFormat("iri")
                val IriReferenceFormat = TypeFormat("iri-reference")
                val UuidFormat = TypeFormat("uuid")
                // from https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-7.3.6
                val UriTemplateFormat = TypeFormat("uri-template")
                // from https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-7.3.7
                val JsonPointerFormat = TypeFormat("json-pointer")
                val RelativeJsonPointerFormat = TypeFormat("relative-json-pointer")
                // from https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-validation-00#section-7.3.8
                val RegexFormat = TypeFormat("regex")
            }
        }

        @Serializable
        @SerialName("null")
        data class NullSchema(
            override val title: String? = null,
            override val description: String? = null,
            override val default: JsonElement? = null,
            override val deprecated: Boolean = false,
            override val readOnly: Boolean = false,
            override val writeOnly: Boolean = false,
            override val examples: List<JsonElement>? = null,
            override val enum: List<JsonElement>? = null,
            override val const: JsonElement? = null,
            override val externalDocs: ExternalDocs? = null,
            override val format: TypeFormat? = null,
        ) : PrimitiveInstanceSchema

        companion object {
            const val TYPE_FIELD_NAME = "type"
        }
    }

    @Serializable
    data class ReferencingSchema(
        @SerialName(ReferenceObject.REF_FIELD_NAME)
        val reference: Reference
    ) : StrictSchema

    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.2.1.1
    @Serializable
    data class AllOfComposite(
        @SerialName(ALL_OF_FIELD_NAME)
        val components: List<StrictSchema>,
        // https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#fixed-fields-20
        val discriminator: Discriminator? = null,
    ) : StrictSchema {
        companion object {
            const val ALL_OF_FIELD_NAME = "allOf"
        }
    }

    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.2.1.2
    @Serializable
    data class AnyOfComposite(
        @SerialName(ANY_OF_FIELD_NAME)
        val components: List<StrictSchema>,
        // https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#fixed-fields-20
        val discriminator: Discriminator? = null,
    ) : StrictSchema {
        companion object {
            const val ANY_OF_FIELD_NAME = "anyOf"
        }
    }

    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.2.1.3
    @Serializable
    data class OneOfComposite(
        @SerialName(ONE_OF_FIELD_NAME)
        val components: List<StrictSchema>,
        // https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#fixed-fields-20
        val discriminator: Discriminator? = null,
    ) : StrictSchema {
        companion object {
            const val ONE_OF_FIELD_NAME = "oneOf"
        }
    }

    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.2.1.4
    @Serializable
    data class Not(
        @SerialName(NOT_FIELD_NAME)
        val schema: StrictSchema,
    ) : StrictSchema {
        companion object {
            const val NOT_FIELD_NAME = "not"
        }
    }

    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.2.2.1
    @Serializable
    data class If(
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.2.2.1
        @SerialName(IF_FIELD_NAME)
        val condition: StrictSchema,
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.2.2.2
        @SerialName(THEN_FIELD_NAME)
        val thenSchema: StrictSchema? = null,
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.2.2.3
        @SerialName(ELSE_FIELD_NAME)
        val elseSchema: StrictSchema? = null,
    ) : StrictSchema {
        companion object {
            const val IF_FIELD_NAME = "if"
            const val THEN_FIELD_NAME = "then"
            const val ELSE_FIELD_NAME = "else"
        }
    }

    // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.2.2.4
    @Serializable
    data class DependentSchemas(
        // https://datatracker.ietf.org/doc/html/draft-bhutton-json-schema-00#section-10.2.2.4
        @SerialName(DEPENDENT_SCHEMAS_FIELD_NAME)
        val dependentSchemas: Map<PropertyName, StrictSchema> = emptyMap(),
    ) : StrictSchema {
        companion object {
            const val DEPENDENT_SCHEMAS_FIELD_NAME = "dependentSchemas"
        }
    }

    @Serializable
    object EmptySchema : StrictSchema {
        override fun toString(): String = "EmptySchema"
    }
}

@Serializable(with = DiscriminatorMappingReferenceSerializer::class)
sealed interface DiscriminatorMappingReference

object DiscriminatorMappingReferenceSerializer : KSerializer<DiscriminatorMappingReference> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DiscriminatorMappingReference", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DiscriminatorMappingReference) = when (value) {
        is DeclaredSchemaName -> encoder.encodeString(value.name)
        is Reference -> encoder.encodeString(value.uri.toString())
    }

    override fun deserialize(decoder: Decoder): DiscriminatorMappingReference {
        val s = decoder.decodeString()
        return if (s.contains('#')) {
            ReferenceSerializer.deserializeFromString(s)
        } else {
            DeclaredSchemaName(s)
        }
    }
}

@Serializable
data class DeclaredSchemaName(val name: String) : DiscriminatorMappingReference

@Serializable
data class Discriminator(val propertyName: String, val mapping: Map<String, DiscriminatorMappingReference>? = null)

object SchemaSerializer : JsonContentPolymorphicSerializer<StrictSchema>(StrictSchema::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        element !is JsonObject -> StrictSchema.InstanceSchema.serializer()
        element.containsKey(ReferenceObject.REF_FIELD_NAME) -> StrictSchema.ReferencingSchema.serializer()
        element.containsKey(StrictSchema.InstanceSchema.TYPE_FIELD_NAME) -> StrictSchema.InstanceSchema.serializer()
        element.containsKey(StrictSchema.AllOfComposite.ALL_OF_FIELD_NAME) -> StrictSchema.AllOfComposite.serializer()
        element.containsKey(StrictSchema.AnyOfComposite.ANY_OF_FIELD_NAME) -> StrictSchema.AnyOfComposite.serializer()
        element.containsKey(StrictSchema.OneOfComposite.ONE_OF_FIELD_NAME) -> StrictSchema.OneOfComposite.serializer()
        element.containsKey(StrictSchema.Not.NOT_FIELD_NAME) -> StrictSchema.Not.serializer()
        element.containsKey(StrictSchema.If.IF_FIELD_NAME) -> StrictSchema.If.serializer()
        element.containsKey(StrictSchema.DependentSchemas.DEPENDENT_SCHEMAS_FIELD_NAME) -> StrictSchema.DependentSchemas.serializer()
        else -> StrictSchema.EmptySchema.serializer()
    }
}

@Serializable
data class ReferenceObject(
    @SerialName(REF_FIELD_NAME)
    val reference: Reference,
    val summary: String? = null,
    val description: String? = null,
) : ReferenceOrSecuritySchema {
    companion object {
        const val REF_FIELD_NAME = "\$ref"

        fun isRef(json: JsonElement) = (json as? JsonObject)?.containsKey(REF_FIELD_NAME) ?: false
    }
}

abstract class OrSerializer<A : Parent, B : Parent, Parent : Any>(
    baseClass: KClass<Parent>,
    private val conditionalDeserializer: DeserializationStrategy<A>,
    private val defaultDeserializer: DeserializationStrategy<B>,
    private val condition: (JsonElement) -> Boolean
) : JsonContentPolymorphicSerializer<Parent>(baseClass) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Parent> = when {
        condition(element) -> conditionalDeserializer
        else -> defaultDeserializer
    }
}

object ReferenceOrSecuritySchemaSerializer : OrSerializer<ReferenceObject, SecuritySchema, ReferenceOrSecuritySchema>(
    ReferenceOrSecuritySchema::class,
    ReferenceObject.serializer(),
    SecuritySchema.serializer(),
    ReferenceObject.Companion::isRef,
)

@Serializable(with = ReferenceSerializer::class)
data class Reference(val uri: URI, val pointer: JsonPointer) : DiscriminatorMappingReference

object URISerializer : KSerializer<URI> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("URI", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URI) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): URI = URI.create(decoder.decodeString())
}

object BigIntegerSerializer : KSerializer<BigInteger> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: BigInteger) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): BigInteger = BigInteger(decoder.decodeString())
}

object LenientBigIntegerSerializer : JsonTransformingSerializer<BigInteger>(BigIntegerSerializer) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonPrimitive && !element.isString) {
            return JsonPrimitive(element.content)
        }
        return super.transformDeserialize(element)
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        if (element is JsonPrimitive && element.isString) {
            return JsonPrimitive(BigInteger(element.content))
        }
        return super.transformSerialize(element)
    }
}

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toPlainString())

    override fun deserialize(decoder: Decoder): BigDecimal = BigDecimal(decoder.decodeString())
}

object LenientBigDecimalSerializer : JsonTransformingSerializer<BigDecimal>(BigDecimalSerializer) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element is JsonPrimitive && !element.isString) {
            return JsonPrimitive(element.content)
        }
        return super.transformDeserialize(element)
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        if (element is JsonPrimitive && element.isString) {
            return JsonPrimitive(BigDecimal(element.content))
        }
        return super.transformSerialize(element)
    }
}

object HttpStatusCodeSerializer : KSerializer<HttpStatusCode> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HttpStatusCode", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: HttpStatusCode) = encoder.encodeString(value.code.toString())

    override fun deserialize(decoder: Decoder) = HttpStatusCode(decoder.decodeString().toUShort())
}

object ReferenceSerializer : KSerializer<Reference> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Reference", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Reference) = encoder.encodeString(serializeToString(value))

    override fun deserialize(decoder: Decoder): Reference = deserializeFromString(decoder.decodeString())

    fun serializeToString(ref: Reference) =
        URI(ref.uri.scheme, ref.uri.userInfo, ref.uri.host, ref.uri.port, ref.uri.path, ref.uri.query, ref.pointer.toString()).toString()

    fun deserializeFromString(s: String): Reference {
        val uri = URI.create(s)
        val pointer = uri.fragment?.ifEmpty { null }?.let { JsonPointer.fromString(it) } ?: JsonPointer.Empty

        return Reference(uri, pointer)
    }
}