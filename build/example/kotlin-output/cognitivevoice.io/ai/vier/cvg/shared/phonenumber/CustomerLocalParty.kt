package ai.vier.cvg.shared.phonenumber

import kotlinx.serialization.Serializable

/**
 * A phone number (+E.164 format) or a SIP URI (RFC 3261) of the callee (inbound) or caller (outbound).
 */
@Serializable
@JvmInline
value class CustomerLocalParty(val value: String)
