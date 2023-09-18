package ai.vier.cvg.shared.phonenumber

import kotlinx.serialization.Serializable

/**
 * A phone number (+E.164 format) or a SIP URI (RFC 3261) of the caller (inbound) or callee (outbound).
 */
@Serializable
@JvmInline
value class CustomerRemoteParty(val value: String)
