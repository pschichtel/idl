package ai.vier.cvg.shared.recording

import kotlinx.serialization.Serializable

/**
 * Whether the recording should be terminated, rather than just paused. If terminated, the recording will be
 * processed as soon as possible instead of deferring processing until the dialog has ended.
 * 
 * A terminated recording can't be resumed again with `/recording/start`.
 */
@Serializable
@JvmInline
value class TerminateRecordingOnStop(val value: Boolean)
