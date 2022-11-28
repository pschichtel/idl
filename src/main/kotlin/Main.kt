

fun main() {
    val api = buildRestApi(name = "test", version = "1.0") {

        summary = "Some fancy API!"

        val someString = primitive("SomeString", PrimitiveType.STRING)

        val linkedStringList = record("LinkedStringList") {
            property("value", someString)
            optionalProperty("next", self)
        }
    }

    println(api)
}