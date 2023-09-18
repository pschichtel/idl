package ai.vier.cvg.shared.time

import kotlinx.serialization.Serializable

/**
 * An amount of time, measured in milliseconds.
 */
@Serializable
@JvmInline
value class Duration(val value: UInt)
