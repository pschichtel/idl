package ai.vier.cvg.shared.data

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class CustomDialogDataValues(val value: List<CustomHeaderValue>)
