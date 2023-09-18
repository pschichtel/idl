package ai.vier.cvg.shared.data

import kotlinx.serialization.Serializable

/**
 * Contains custom data that was attached to the dialog via the Dialog API before the session request.
 * This is only possible when using the Provisioning API, otherwise the map is always empty.
 * 
 */
@Serializable
@JvmInline
value class CustomDialogData(val value: Map<CustomDialogDataKey, CustomDialogDataValues>)
