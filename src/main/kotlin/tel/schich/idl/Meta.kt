package tel.schich.idl

sealed interface Metadata {
    val name: String
    val summary: String?
    val description: String?
    val annotations: Map<String, String>
}