package uz.hijriy.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AsmaulHusnaTest {
    @Test
    fun ninetyNineNames() {
        val n = AsmaulHusna.names
        assertEquals(99, n.size)
        assertEquals("Ar-Rohman", n.first().uz)
        assertEquals("As-Sobur", n.last().uz)
        assertEquals(99, n.map { it.ar }.toSet().size)
        n.forEach { assertTrue(it.ar.isNotBlank() && it.meaning.isNotBlank()) }
    }
}
