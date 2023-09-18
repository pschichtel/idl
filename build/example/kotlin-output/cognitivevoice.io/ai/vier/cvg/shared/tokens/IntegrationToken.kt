package ai.vier.cvg.shared.tokens

import kotlinx.serialization.Serializable

/**
 * The unique token of a external service integration.
 */
@Serializable
@JvmInline
value class IntegrationToken(val value: String)
