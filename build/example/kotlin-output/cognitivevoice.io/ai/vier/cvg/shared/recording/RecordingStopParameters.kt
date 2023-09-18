package ai.vier.cvg.shared.recording

import ai.vier.cvg.shared.tokens.DialogId
import kotlinx.serialization.Serializable

@Serializable
data class RecordingStopParameters(
    val dialogId: DialogId,
    val recordingId: RecordingId? = null,
    val terminate: TerminateRecordingOnStop = false,
)
