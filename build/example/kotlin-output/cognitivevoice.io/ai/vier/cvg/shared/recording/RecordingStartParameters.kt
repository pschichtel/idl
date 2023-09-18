package ai.vier.cvg.shared.recording

import ai.vier.cvg.shared.speaker.SpeakerSelection
import ai.vier.cvg.shared.time.Duration
import ai.vier.cvg.shared.tokens.DialogId
import kotlinx.serialization.Serializable

@Serializable
data class RecordingStartParameters(
    val dialogId: DialogId,
    val maxDuration: Duration? = null,
    val recordingId: RecordingId? = null,
    val speakers: SpeakerSelection? = null,
)
