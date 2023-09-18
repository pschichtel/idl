package ai.vier.cvg.shared.phonenumber

import kotlinx.serialization.Serializable

/**
 * The number to display to the callee. This is a best-effort feature since we cannot guarantee that all
 * gateways the traffic flows through will retain this information.
 * 
 * Setting this value to `"anonymous"` will signal a suppressed number to the callee.
 */
@Serializable
@JvmInline
value class CallerId(val value: String)
