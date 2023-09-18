package ai.vier.cvg.provisioning

import kotlinx.serialization.Serializable

/**
 * The message describing the error
 */
@Serializable
@JvmInline
value class ProvisionCallErrorMessage(val value: String)
