package tel.schich.idl.example

sealed interface UntaggedSum {
    data class Name(val value: Test) : UntaggedSum
}
