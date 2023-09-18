package ai.vier.cvg.shared.tokens

import kotlinx.serialization.Serializable

/**
 * The unique token of a speech service profile. You can find the token on the configuration page of the profile.
 */
@Serializable
@JvmInline
value class ProfileToken(val value: String)
