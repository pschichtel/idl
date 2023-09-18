package ai.vier.cvg.shared.language

import kotlinx.serialization.Serializable

/**
 * A language code like `de-DE` or `en-US`.
 */
@Serializable
@JvmInline
value class Language(val value: String)
