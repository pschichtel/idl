package ai.vier.cvg.shared.speechservice

import kotlinx.serialization.Serializable

/**
 * If a phrase list is given, transcription results only trigger barge-in if they match one of the phrases.
 * A phrase must not contain more than 80 characters, and there must not be more than 100 phrases.
 * 
 * A phrase "matches" if the transcription result fully contains it. Some notes about matching:
 *   - matching is case insensitive
 *   - matching ignores delimiters
 *   - only whole words are matched
 *   - phrases can be single words
 */
@Serializable
@JvmInline
value class PhraseList(val value: List<Phrase>)
