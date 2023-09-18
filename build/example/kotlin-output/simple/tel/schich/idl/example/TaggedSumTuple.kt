package tel.schich.idl.example

sealed interface TaggedSumTuple {
    data class Ctor1(val value: Record) : TaggedSumTuple

    data class Ctor2(val value: Record) : TaggedSumTuple
}
