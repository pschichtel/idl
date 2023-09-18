package ai.vier.cvg.shared.project

import kotlinx.serialization.Serializable

/**
 * Optional transcription parameter. A level in dB that a signal must
 * surpass to be considered at all.
 */
@Serializable
@JvmInline
value class MinimumNoiseLevel(val value: Int)
