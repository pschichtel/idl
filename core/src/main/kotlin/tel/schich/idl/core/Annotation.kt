package tel.schich.idl.core

typealias Annotations = Map<String, String>
typealias AnnotationParser<T> = (String) -> T

const val ANNOTATION_NAMESPACE_SEPARATOR = '/'

fun <T> valueAsIs(value: T): T = value
fun valueAsBoolean(value: String): Boolean = value.toBoolean()

open class Annotation<T : Any>(val namespace: String, val name: String, val parser: AnnotationParser<T>) {
    val fullName = "$namespace$ANNOTATION_NAMESPACE_SEPARATOR$name"

    fun getValue(annotations: Annotations): T? = annotations[fullName]?.let(parser)
    fun getValue(metadata: Metadata): T? = getValue(metadata.annotations)
}

fun <T : Any> Metadata.getAnnotation(annotation: Annotation<T>): T? = annotation.getValue(annotations)
