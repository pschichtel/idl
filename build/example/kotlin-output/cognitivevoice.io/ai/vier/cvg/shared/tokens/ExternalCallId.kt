package ai.vier.cvg.shared.tokens

import kotlinx.serialization.Serializable

/**
 * This ID can be an arbitrary string provided by the client. The ID should be chosen in a way that
 * guarantees uniqueness across a project for as long as the dialog data is retained.
 */
@Serializable
@JvmInline
value class ExternalCallId(val value: String)
