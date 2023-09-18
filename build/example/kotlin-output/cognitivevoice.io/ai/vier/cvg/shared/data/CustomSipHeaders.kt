package ai.vier.cvg.shared.data

import kotlinx.serialization.Serializable

/**
 * This object has a property for each custom SIP header. A header is considered a custom header if its name
 * starts with `X-`, no other header names are allowed. Header names are case-insensitive and thus unique in
 * this object. Each header has a list of header values as its value. The order of the header values per header
 * is consistent with the order in which the headers appeared in the SIP message.
 * 
 * **Notes**:
 *  * All header names will have their letter casing converted to lowercase.
 *  * The amount of data that can be transferred via SIP headers is *very* limited. Only 128 bytes of data will be
 *    accepted. The User-to-User Information (UUI) header is also included in this limit.
 *  * Custom SIP headers are a best-effort API, any SIP proxy on the path to the system, that is supposed to read
 *    the information, can manipulate or drop headers.
 */
@Serializable
@JvmInline
value class CustomSipHeaders(val value: Map<CustomHeaderName, CustomHeaderValues>)
