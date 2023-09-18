package ai.vier.cvg.shared.phonenumber

import kotlinx.serialization.Serializable

/**
 * A phone number (+E.164 format) of the caller (inbound) or callee (outbound).
 * If the number is unknown / suppressed, this is an empty string (`""`).
 */
@Serializable
@JvmInline
value class LegacyCustomerRemoteParty(val value: String)
