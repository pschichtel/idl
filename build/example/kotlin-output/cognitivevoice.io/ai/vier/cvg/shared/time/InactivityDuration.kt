package ai.vier.cvg.shared.time

/**
 * The duration of the inactivity period in milliseconds.
 * The timestamp of the last activity in this call can be calculated with `timestamp - duration`.
 * 
 * When an inactivity timeout triggers within a potential utterance, we can't yet know whether that utterance
 * will result in a successful transcription or if it was likely just background noise. To make sure
 * inactivity events aren't sent in the former case, the events are delayed to the end of the utterance.
 * As a result, this duration may be longer than the inactivity timeout configured in the project settings.
 */
typealias InactivityDuration = Duration
