package ai.vier.cvg.shared.project

import kotlinx.serialization.json.JsonElement

/**
 * Advanced configuration for the bot. Can contain arbitrary JSON data, including `null`.
 * Please consult the documentation of the bot you are using for the required format.
 */
typealias BotConfiguration = JsonElement
