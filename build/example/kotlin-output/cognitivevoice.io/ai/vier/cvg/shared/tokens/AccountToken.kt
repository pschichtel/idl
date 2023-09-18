package ai.vier.cvg.shared.tokens

import kotlinx.serialization.Serializable

/**
 * The unique token of an account. You can find your token on your account configuration page.
 */
@Serializable
@JvmInline
value class AccountToken(val value: String)
