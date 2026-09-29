package uz.hijriy.app.core

import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor

data class HijriDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String get() = Hijri.MONTHS_UZ[month - 1]
    val monthNameAr: String get() = Hijri.MONTHS_AR[month - 1]
    override fun toString(): String = "$day $monthName $year"
}

/**
 * Hijriy (qamariy) taqvim konvertori.
 * Asosiy usul — Ummul-Quro rasmiy jadvali (1343–1500 h. / 1924–2077 m.),
 * jadvaldan tashqaridagi sanalar uchun arifmetik (jadvaliy) hisob.
 * [adjust] — foydalanuvchi tuzatishi (kun): +1 bo'lsa hijriy sana bir kun oldinga suriladi.
 */
object Hijri {
    val MONTHS_UZ = listOf(
        "Muharram", "Safar", "Rabiul avval", "Rabiul oxir", "Jumodul avval", "Jumodul oxir",
        "Rajab", "Sha'bon", "Ramazon", "Shavvol", "Zulqa'da", "Zulhijja"
    )
    val MONTHS_AR = listOf(
        "مُحَرَّم", "صَفَر", "رَبِيع الأَوَّل", "رَبِيع الآخِر", "جُمَادَى الأُولَى", "جُمَادَى الآخِرَة",
        "رَجَب", "شَعْبَان", "رَمَضَان", "شَوَّال", "ذُو القَعْدَة", "ذُو الحِجَّة"
    )

    private val starts get() = UmmAlQuraData.MONTH_STARTS
    private const val FIRST = UmmAlQuraData.FIRST_YEAR

    fun fromGregorian(date: LocalDate, adjust: Int = 0): HijriDate = fromEpochDay(date.toEpochDay() + adjust)

    fun toGregorian(h: HijriDate, adjust: Int = 0): LocalDate? {
        if (h.month !in 1..12 || h.day < 1 || h.day > monthLength(h.year, h.month)) return null
        return LocalDate.ofEpochDay(toEpochDay(h) - adjust)
    }

    fun monthLength(year: Int, month: Int): Int {
        val idx = (year - FIRST) * 12 + (month - 1)
        val s = starts
        return if (idx >= 0 && idx + 1 < s.size) s[idx + 1] - s[idx]
        else (tabToEpoch(if (month == 12) year + 1 else year, if (month == 12) 1 else month + 1, 1) -
                tabToEpoch(year, month, 1)).toInt()
    }

    fun isUmmAlQuraRange(date: LocalDate): Boolean {
        val e = date.toEpochDay()
        return e >= starts.first() && e < starts.last()
    }

    fun fromEpochDay(e: Long): HijriDate {
        val s = starts
        if (e >= s.first() && e < s.last()) {
            var lo = 0
            var hi = s.size - 1
            while (hi - lo > 1) {
                val mid = (lo + hi) ushr 1
                if (s[mid] <= e) lo = mid else hi = mid
            }
            return HijriDate(FIRST + lo / 12, lo % 12 + 1, (e - s[lo] + 1).toInt())
        }
        return tabFromEpoch(e)
    }

    fun toEpochDay(h: HijriDate): Long {
        val idx = (h.year - FIRST) * 12 + (h.month - 1)
        val s = starts
        return if (idx >= 0 && idx + 1 < s.size) s[idx].toLong() + h.day - 1
        else tabToEpoch(h.year, h.month, h.day)
    }

    // ---- Arifmetik (jadvaliy) hijriy taqvim, epoch 16-iyul 622 (Julian) ----
    private const val EPOCH_JD = 1948439.5
    private const val UNIX_JD = 2440587.5

    private fun tabToJd(y: Int, m: Int, d: Int): Double =
        d + ceil(29.5 * (m - 1)) + (y - 1) * 354.0 + floor((3 + 11.0 * y) / 30.0) + EPOCH_JD - 1

    private fun tabToEpoch(y: Int, m: Int, d: Int): Long = Math.round(tabToJd(y, m, d) - UNIX_JD)

    private fun tabFromEpoch(e: Long): HijriDate {
        val jd = floor(e + UNIX_JD) + 0.5
        val y = floor((30 * (jd - EPOCH_JD) + 10646) / 10631).toInt()
        val m = minOf(12, (ceil((jd - (29 + tabToJd(y, 1, 1))) / 29.5) + 1).toInt()).coerceAtLeast(1)
        val d = (jd - tabToJd(y, m, 1)).toInt() + 1
        return HijriDate(y, m, d)
    }
}

/** Muhim islomiy kunlar (hijriy oy/kun bo'yicha). */
object IslamicDays {
    data class Day(val month: Int, val day: Int, val name: String)

    val list = listOf(
        Day(1, 1, "Hijriy yangi yil"),
        Day(1, 10, "Ashuro kuni"),
        Day(3, 12, "Mavlid (Rasululloh ﷺ tavalludi)"),
        Day(7, 27, "Me'roj kechasi"),
        Day(8, 15, "Barot kechasi"),
        Day(9, 1, "Ramazon oyi boshlanishi"),
        Day(9, 27, "Qadr kechasi (taxminiy)"),
        Day(10, 1, "Ramazon hayiti"),
        Day(12, 9, "Arafa kuni"),
        Day(12, 10, "Qurbon hayiti"),
    )

    fun find(h: HijriDate): Day? = list.firstOrNull { it.month == h.month && it.day == h.day }
}
