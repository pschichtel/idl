package ai.vier.cvg.shared.time

import kotlinx.serialization.Serializable

/**
 * A timezone name or time offset to UTC.
 */
@Serializable
@JvmInline
value class Timezone(val value: String)
