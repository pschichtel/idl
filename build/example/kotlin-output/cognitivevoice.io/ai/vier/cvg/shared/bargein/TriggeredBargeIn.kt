package ai.vier.cvg.shared.bargein

import kotlinx.serialization.Serializable

/**
 * This flag is set to true if this message triggered barge-in and the bot was interrupted.
 */
@Serializable
@JvmInline
value class TriggeredBargeIn(val value: Boolean)
