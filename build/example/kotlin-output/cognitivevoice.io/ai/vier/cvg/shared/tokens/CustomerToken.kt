package ai.vier.cvg.shared.tokens

import kotlinx.serialization.Serializable

/**
 * The unique token of a customer. You can find your token on your customer configuration page.
 */
@Serializable
@JvmInline
value class CustomerToken(val value: String)
