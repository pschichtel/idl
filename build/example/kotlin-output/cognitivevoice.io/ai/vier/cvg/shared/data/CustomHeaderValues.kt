package ai.vier.cvg.shared.data

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class CustomHeaderValues(val value: List<CustomHeaderValue>)
