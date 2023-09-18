package ai.vier.cvg.assist

import kotlinx.serialization.Serializable

/**
 * A token that is used to authenticate requests to the callback URL.
 */
@Serializable
@JvmInline
value class AssistAuthToken(val value: String)
