package uz.hijriy.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HijriTest {

    @Test
    fun userExample_21March2001() {
        val h = Hijri.fromGregorian(LocalDate.of(2001, 3, 21))
        assertEquals(HijriDate(1421, 12, 26), h)
        assertEquals("Zulhijja", h.monthName)
    }

    @Test
    fun ummAlQuraVectors() {
        val lines = javaClass.classLoader!!.getResourceAsStream("ummalqura_vectors.txt")!!
            .bufferedReader().readLines().filter { it.isNotBlank() }
        assertEquals(608, lines.size)
        for (line in lines) {
            val p = line.split(" ")
            val g = LocalDate.parse(p[0])
            val expected = HijriDate(p[1].toInt(), p[2].toInt(), p[3].toInt())
            assertEquals("G→H $g", expected, Hijri.fromGregorian(g))
            assertEquals("H→G $expected", g, Hijri.toGregorian(expected))
            assertEquals("oy uzunligi $expected", p[4].toInt(), Hijri.monthLength(expected.year, expected.month))
        }
    }

    @Test
    fun adjustmentShiftsBothWays() {
        val g = LocalDate.of(2026, 2, 18)
        assertEquals(HijriDate(1447, 9, 1), Hijri.fromGregorian(g))
        assertEquals(HijriDate(1447, 8, 29), Hijri.fromGregorian(g, -1))
        assertEquals(LocalDate.of(2026, 2, 19), Hijri.toGregorian(HijriDate(1447, 9, 1), -1))
    }

    @Test
    fun invalidDateRejected() {
        assertNull(Hijri.toGregorian(HijriDate(1447, 13, 1)))
        assertNull(Hijri.toGregorian(HijriDate(1447, 1, 31)))
    }

    @Test
    fun tabularFallbackRoundTrips() {
        var d = LocalDate.of(1800, 1, 1)
        repeat(400) {
            val h = Hijri.fromGregorian(d)
            assertEquals(d, Hijri.toGregorian(h))
            d = d.plusDays(37)
        }
        var e = LocalDate.of(2080, 1, 1)
        repeat(200) {
            assertEquals(e, Hijri.toGregorian(Hijri.fromGregorian(e)))
            e = e.plusDays(41)
        }
    }
}
