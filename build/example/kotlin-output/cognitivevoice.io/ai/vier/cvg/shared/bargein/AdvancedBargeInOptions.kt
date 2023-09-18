package ai.vier.cvg.shared.bargein

import ai.vier.cvg.shared.confidence.Confidence
import ai.vier.cvg.shared.speechservice.PhraseList
import kotlinx.serialization.Serializable

/**
 * By default, transcriptions made while the bot is 'talking' (synthesis or audio file playback) are ignored.
 * However, if barge-in is enabled, the audio output is interrupted and the transcription result is sent to the bot.
 * 
 * Note that this feature is currently fairly sensitive to background noise, in which case audio output may be
 * interrupted erroneously.
 */
@Serializable
data class AdvancedBargeInOptions(
    /**
     * By default, transcriptions made while the bot is 'talking' (synthesis or audio file playback) are ignored.
     * However, if barge-in is enabled, the audio output is interrupted and the transcription result is sent to the bot.
     * 
     * Note that this feature is currently fairly sensitive to background noise, in which case audio output may be
     * interrupted erroneously.
     */
    val onSpeech: EnableBargeInOnSpeech,
    /**
     * By default, transcriptions made while the bot is 'talking' (synthesis or audio file playback) are ignored.
     * However, if barge-in is enabled, the audio output is interrupted and the transcription result is sent to the bot.
     * 
     * Note that this feature is currently fairly sensitive to background noise, in which case audio output may be
     * interrupted erroneously.
     */
    val onDtmf: EnableBargeInOnDtmf,
    /**
     * By default, transcriptions made while the bot is 'talking' (synthesis or audio file playback) are ignored.
     * However, if barge-in is enabled, the audio output is interrupted and the transcription result is sent to the bot.
     * 
     * Note that this feature is currently fairly sensitive to background noise, in which case audio output may be
     * interrupted erroneously.
     */
    val confidence: Confidence? = null,
    /**
     * By default, transcriptions made while the bot is 'talking' (synthesis or audio file playback) are ignored.
     * However, if barge-in is enabled, the audio output is interrupted and the transcription result is sent to the bot.
     * 
     * Note that this feature is currently fairly sensitive to background noise, in which case audio output may be
     * interrupted erroneously.
     */
    val phraseList: PhraseList? = null,
)
