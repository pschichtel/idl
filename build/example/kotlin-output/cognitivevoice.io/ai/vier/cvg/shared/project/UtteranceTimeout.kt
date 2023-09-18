package ai.vier.cvg.shared.project

import kotlinx.serialization.Serializable

/**
 * Optional transcription parameter. The time (milliseconds) of silence
 * that must occur after non-noise to complete an utterance.
 */
@Serializable
@JvmInline
value class UtteranceTimeout(val value: UInt)
