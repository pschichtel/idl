package ai.vier.cvg.assist

import ai.vier.cvg.shared.speaker.SpeakerSelection
import ai.vier.cvg.shared.tokens.DialogId
import ai.vier.cvg.shared.tokens.ResellerToken
import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionStopParameters(
    val resellerToken: ResellerToken,
    val dialogId: DialogId,
    val speakers: SpeakerSelection,
)
