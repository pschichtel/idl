package ai.vier.cvg.shared.speaker

import kotlinx.serialization.Serializable

/**
 * Which speaker(s) to select, by default both.
 */
@Serializable
@JvmInline
value class SpeakerSelection(val value: Set<Speaker>)
