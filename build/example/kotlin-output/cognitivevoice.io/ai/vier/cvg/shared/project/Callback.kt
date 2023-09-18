package ai.vier.cvg.shared.project

import kotlinx.serialization.Serializable

/**
 * The base URL against which API calls should be made, e.g. https://cognitivevoice.io/v1 for the
 * production environment and https://stage.cognitivevoice.io/v1 for the staging environment.
 */
@Serializable
@JvmInline
value class Callback(val value: String)
