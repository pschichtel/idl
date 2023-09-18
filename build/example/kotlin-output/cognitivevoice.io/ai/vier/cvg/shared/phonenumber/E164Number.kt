package ai.vier.cvg.shared.phonenumber

import kotlinx.serialization.Serializable

/**
 * A phone number in +E.164 format.
 */
@Serializable
@JvmInline
value class E164Number(val value: String)
