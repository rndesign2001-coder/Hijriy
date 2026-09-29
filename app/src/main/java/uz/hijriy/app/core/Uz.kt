package uz.hijriy.app.core

import java.time.DayOfWeek
import java.time.LocalDate

object Uz {
    val MONTHS = listOf(
        "Yanvar", "Fevral", "Mart", "Aprel", "May", "Iyun",
        "Iyul", "Avgust", "Sentabr", "Oktabr", "Noyabr", "Dekabr"
    )
    val WEEKDAYS = listOf("Dushanba", "Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba", "Yakshanba")
    val WEEKDAYS_SHORT = listOf("Du", "Se", "Ch", "Pa", "Ju", "Sh", "Ya")

    fun weekday(d: DayOfWeek) = WEEKDAYS[d.value - 1]
    fun gregorian(d: LocalDate) = "${d.dayOfMonth} ${MONTHS[d.monthValue - 1]} ${d.year}"
    fun gregorianFull(d: LocalDate) = "${weekday(d.dayOfWeek)}, ${gregorian(d)}"

    private const val AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"
    fun arabicNumber(n: Int): String =
        n.toString().map { c -> if (c in '0'..'9') AR_DIGITS[c - '0'] else c }.joinToString("")

    fun duration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "$h soat $m daqiqa" else "$m daqiqa"
    }

    fun countdown(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    }
}
