package ai.vier.cvg.shared.confidence

import kotlinx.serialization.Serializable

/**
 * The confidence of the transcription result as a percentage from 0 to 100,
 * or 100 in case of DTMF.
 */
@Serializable
@JvmInline
value class Confidence(val value: UInt)
