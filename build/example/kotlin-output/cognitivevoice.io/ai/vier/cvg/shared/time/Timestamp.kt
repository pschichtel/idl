package ai.vier.cvg.shared.time

import kotlinx.serialization.Serializable

/**
 * An instant of time, measured in milliseconds since the epoch (1970-01-01) in UTC.
 */
@Serializable
@JvmInline
value class Timestamp(val value: Long)
