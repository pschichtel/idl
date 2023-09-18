package ai.vier.cvg.shared.phonenumber

import kotlinx.serialization.Serializable

/**
 * A phone number (+E.164 format) or a SIP URI (RFC 3261) for outgoing calls.
 * 
 * Note that when using a SIP URI, the call is routed via public Internet,
 * so there are no quality of service guarantees.
 */
@Serializable
@JvmInline
value class OutboundCallDestination(val value: String)
