package ai.vier.cvg.shared.tokens

import kotlinx.serialization.Serializable

/**
 * The id of a dialog.
 */
@Serializable
@JvmInline
value class DialogId(val value: String)
