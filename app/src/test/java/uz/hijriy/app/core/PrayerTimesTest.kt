package uz.hijriy.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

/**
 * Toshkent uchun rasmiy vaqtlar bilan solishtirish.
 * 2026-09-29: muslim.uz (O'zbekiston musulmonlari idorasi) — 04:59 06:17 12:13 16:22 18:13 19:27.
 * Ramazon 2026 saharlik vaqtlari: 1447 h. taqvim kitobi (namozvaqti.uz).
 */
class PrayerTimesTest {
    private val lat = 41.3111
    private val lon = 69.2797
    private val s = CalcSettings()

    @Test
    fun tashkentOfficial_2026_09_29() {
        val t = PrayerCalc.compute(LocalDate.of(2026, 9, 29), lat, lon, 5.0, s)
        assertEquals("04:59", fmtMin(t[Prayer.FAJR]))
        assertEquals("06:17", fmtMin(t[Prayer.SUNRISE]))
        assertEquals("12:13", fmtMin(t[Prayer.DHUHR]))
        assertEquals("16:22", fmtMin(t[Prayer.ASR]))
        assertEquals("18:13", fmtMin(t[Prayer.MAGHRIB]))
        assertEquals("19:27", fmtMin(t[Prayer.ISHA]))
    }

    @Test
    fun tashkentRamadanFajr() {
        val refs = mapOf(
            LocalDate.of(2026, 2, 19) to "05:54",
            LocalDate.of(2026, 3, 1) to "05:40",
            LocalDate.of(2026, 3, 19) to "05:10",
            LocalDate.of(2026, 3, 20) to "05:08",
        )
        for ((d, v) in refs) {
            assertEquals(d.toString(), v, fmtMin(PrayerCalc.compute(d, lat, lon, 5.0, s)[Prayer.FAJR]))
        }
    }

    @Test
    fun adjustmentsOffsetAndFixed() {
        val d = LocalDate.of(2026, 9, 29)
        val adj = mapOf(
            Prayer.DHUHR to PrayerAdjust(offset = 17),
            Prayer.ISHA to PrayerAdjust(fixed = 20 * 60),
        )
        val t = PrayerCalc.compute(d, lat, lon, 5.0, s, adj)
        assertEquals("12:30", fmtMin(t[Prayer.DHUHR]))
        assertEquals("20:00", fmtMin(t[Prayer.ISHA]))
        assertTrue(t.isAdjusted(Prayer.DHUHR))
        // Offset keyingi kunlarda ham hisobiy vaqt bilan birga siljiydi
        val d2 = LocalDate.of(2026, 12, 21)
        val base = PrayerCalc.compute(d2, lat, lon, 5.0, s)
        val t2 = PrayerCalc.compute(d2, lat, lon, 5.0, s, adj)
        assertEquals(base[Prayer.DHUHR] + 17, t2[Prayer.DHUHR])
    }

    @Test
    fun wholeYearIsOrderedEverywhere() {
        val places = listOf(41.3111 to 69.2797, 43.768 to 59.022, 37.239 to 67.323, 40.53 to 70.927)
        for ((la, lo) in places) {
            var d = LocalDate.of(2026, 1, 1)
            repeat(365) {
                val t = PrayerCalc.compute(d, la, lo, 5.0, s)
                val v = Prayer.entries.map { t[it] }
                assertEquals("$la $d $v", v.sorted(), v)
                d = d.plusDays(1)
            }
        }
    }

    @Test
    fun qiblaTashkent() {
        val b = Qibla.bearing(41.3111, 69.2797)
        assertTrue("qibla $b", abs(b - 240.4) < 1.5)
    }
}
