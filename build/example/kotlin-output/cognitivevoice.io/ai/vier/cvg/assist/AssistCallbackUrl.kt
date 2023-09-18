package ai.vier.cvg.assist

import kotlinx.serialization.Serializable

/**
 * The base URL for sending transcription events.
 */
@Serializable
@JvmInline
value class AssistCallbackUrl(val value: String)
