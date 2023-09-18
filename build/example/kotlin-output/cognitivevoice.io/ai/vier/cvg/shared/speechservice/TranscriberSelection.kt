package ai.vier.cvg.shared.speechservice

import kotlinx.serialization.Serializable

/**
 * This parameter is optional. If specified, it overrides the transcriber list from the project settings.
 * 
 * A list of transcriber vendor names like `"GOOGLE"`. When using custom transcriber profiles,
 * the profile name is attached as a suffix separated by a dash, e.g. `"GOOGLE-profile"`.
 * Alternatively, the profile token can be used directly (without the vendor name).
 * 
 * The first transcriber in the list has the highest priority. Additional transcribers
 * are used as a fallback (in order) in case a service is currently unreachable.
 * 
 * The following transcriber vendors are currently available:
 * 
 *   - `GOOGLE`
 *   - `MICROSOFT`
 *   - `IBM`
 *   - `EML`
 */
@Serializable
@JvmInline
value class TranscriberSelection(val value: List<TranscriberName>)
