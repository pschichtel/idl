package ai.vier.cvg.assist

import ai.vier.cvg.shared.recording.RecordingId
import ai.vier.cvg.shared.recording.TerminateRecordingOnStop
import ai.vier.cvg.shared.tokens.DialogId
import ai.vier.cvg.shared.tokens.ResellerToken
import kotlinx.serialization.Serializable

@Serializable
data class AssistRecordingStopParameters(
    val dialogId: DialogId,
    val recordingId: RecordingId? = null,
    val terminate: TerminateRecordingOnStop = false,
    val resellerToken: ResellerToken,
)
