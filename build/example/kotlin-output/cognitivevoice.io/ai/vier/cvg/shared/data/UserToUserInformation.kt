package ai.vier.cvg.shared.data

import kotlinx.serialization.Serializable

/**
 * The User-to-User Information (UUI) header is a mechanism for transmitting custom data in a SIP message.
 * 
 * Custom SIP headers and the UUI header combined must not exceed 128 bytes of data.
 */
@Serializable
@JvmInline
value class UserToUserInformation(val value: List<UserToUserInformationValue>)
