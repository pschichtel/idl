package ai.vier.cvg.shared.recording

import kotlinx.serialization.Serializable

/**
 * Recording IDs are arbitrary strings which can by supplied to start a dedicated recording in addition to
 * the main recording. The ID must be unique within each dialog phase.
 * 
 * If the recording ID is omitted, `"default"` is used.
 */
@Serializable
@JvmInline
value class RecordingId(val value: String)
