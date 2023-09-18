package ai.vier.cvg.shared.speechservice

import kotlinx.serialization.Serializable

/**
 * This parameter is optional. If specified, it overrides the synthesizer list from the project settings.
 * 
 * A list of synthesizer vendor names like `"GOOGLE"`. When using custom synthesizer profiles,
 * the profile name is attached as a suffix separated by a dash, e.g. `"GOOGLE-profile"`.
 * Alternatively, the profile token can be used directly (without the vendor name).
 * 
 * The first synthesizer in the list has the highest priority. Additional synthesizers
 * are used as a fallback (in order) in case a service is currently unreachable.
 * 
 * The following synthesizer vendors are currently available:
 * 
 *   - `GOOGLE`
 *   - `MICROSOFT`
 *   - `IBM`
 *   - `AMAZON`
 *   - `NUANCE`
 */
@Serializable
@JvmInline
value class SynthesizerSelection(val value: List<SynthesizerName>)
