package ai.vier.cvg.shared.project

import kotlinx.serialization.Serializable

/**
 * Duration after which the inactivity detection timeout occurs.
 * The value is in milliseconds.
 */
@Serializable
@JvmInline
value class InactivityTimeout(val value: UInt)
