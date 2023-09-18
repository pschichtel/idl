package ai.vier.cvg.shared.tokens

import kotlinx.serialization.Serializable

/**
 * This object encapsulates the tokens of the organizational units relevant for most API calls.
 */
@Serializable
data class ProjectContext(
    /**
     * This object encapsulates the tokens of the organizational units relevant for most API calls.
     */
    val projectToken: ProjectToken,
    /**
     * This object encapsulates the tokens of the organizational units relevant for most API calls.
     */
    val resellerToken: ResellerToken,
)
