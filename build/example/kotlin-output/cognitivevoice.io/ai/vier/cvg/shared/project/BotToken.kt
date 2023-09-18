package ai.vier.cvg.shared.project

import kotlinx.serialization.Serializable

/**
 * A bearer token that is used for calls to the Bot API.
 */
@Serializable
@JvmInline
value class BotToken(val value: String)
