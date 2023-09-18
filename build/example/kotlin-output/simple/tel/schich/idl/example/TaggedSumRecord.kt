package tel.schich.idl.example

sealed interface TaggedSumRecord {
    data class Ctor1(val value: Record) : TaggedSumRecord

    data class Ctor2(val value: Record) : TaggedSumRecord
}
