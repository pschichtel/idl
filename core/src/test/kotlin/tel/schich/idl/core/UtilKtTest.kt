package tel.schich.idl.core

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class UtilKtTest {

    @Test
    fun nameWordSplitting() {
        assertEquals(listOf("a", "b"), splitIntoWords("a b"))
        assertEquals(listOf("a", "b"), splitIntoWords("a\nb"))
        assertEquals(listOf("a", "b"), splitIntoWords("a\tb"))
        assertEquals(listOf("a", "b"), splitIntoWords("a-b"))
        assertEquals(listOf("a", "b"), splitIntoWords("a--b"))
        assertEquals(listOf("A", "Ab"), splitIntoWords("AAb"))
        assertEquals(listOf("A"), splitIntoWords("A_"))
        assertEquals(listOf("A"), splitIntoWords("_A"))
        assertEquals(listOf("Ab", "Cd"), splitIntoWords("AbCd"))
        assertEquals(listOf("ab", "Cd"), splitIntoWords("abCd"))
        assertEquals(listOf("on", "Dtmf"), splitIntoWords("onDtmf"))
    }
}