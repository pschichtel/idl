package ai.vier.cvg.shared.project

import kotlinx.serialization.Serializable

/**
 * URL to the server that implements the Bot API.
 */
@Serializable
@JvmInline
value class BotUrl(val value: String)
