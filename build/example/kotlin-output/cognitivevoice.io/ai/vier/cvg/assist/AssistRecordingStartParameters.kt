package ai.vier.cvg.assist

import ai.vier.cvg.shared.recording.RecordingId
import ai.vier.cvg.shared.speaker.SpeakerSelection
import ai.vier.cvg.shared.time.Duration
import ai.vier.cvg.shared.tokens.DialogId
import ai.vier.cvg.shared.tokens.ResellerToken
import kotlinx.serialization.Serializable

@Serializable
data class AssistRecordingStartParameters(
    val dialogId: DialogId,
    val maxDuration: Duration? = null,
    val recordingId: RecordingId? = null,
    val speakers: SpeakerSelection? = null,
    val resellerToken: ResellerToken,
)
