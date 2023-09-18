package ai.vier.cvg.shared.tokens

import kotlinx.serialization.Serializable

/**
 * The unique token of a reseller. You can find your token on your project configuration page.
 */
@Serializable
@JvmInline
value class ResellerToken(val value: String)
