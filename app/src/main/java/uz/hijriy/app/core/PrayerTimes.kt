package uz.hijriy.app.core

import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class Prayer(val uz: String, val notifiable: Boolean) {
    FAJR("Bomdod", true),
    SUNRISE("Quyosh", false),
    DHUHR("Peshin", true),
    ASR("Asr", true),
    MAGHRIB("Shom", true),
    ISHA("Xufton", true);
}

/**
 * Hisoblash usullari. [dhuhrPlus], [maghribPlus] — usulga xos ehtiyot daqiqalari.
 * O'zbekiston usuli muslim.uz (O'zbekiston musulmonlari idorasi) rasmiy vaqtlariga kalibrlangan:
 * bomdod/xufton 15.5°, asr — Hanafiy, shom = quyosh botishi + 4 daqiqa.
 */
enum class CalcMethod(
    val title: String,
    val fajr: Double,
    val isha: Double,
    val ishaMinutes: Int = 0,
    val dhuhrPlus: Int = 0,
    val maghribPlus: Int = 0,
) {
    UZBEKISTAN("O'zbekiston (O'MI)", 15.5, 15.5, maghribPlus = 4),
    MWL("Musulmon olami ligasi", 18.0, 17.0),
    ISNA("Shimoliy Amerika (ISNA)", 15.0, 15.0),
    EGYPT("Misr", 19.5, 17.5),
    KARACHI("Karachi", 18.0, 18.0),
    UMM_AL_QURA("Ummul-Quro (Makka)", 18.5, 0.0, ishaMinutes = 90),
    TURKEY("Turkiya (Diyanet)", 18.0, 17.0),
    CUSTOM("Qo'lda burchak", 15.5, 15.5);
}

data class CalcSettings(
    val method: CalcMethod = CalcMethod.UZBEKISTAN,
    val hanafi: Boolean = true,
    val customFajr: Double = 15.5,
    val customIsha: Double = 15.5,
) {
    val fajrAngle get() = if (method == CalcMethod.CUSTOM) customFajr else method.fajr
    val ishaAngle get() = if (method == CalcMethod.CUSTOM) customIsha else method.isha
}

/** Foydalanuvchining har bir vaqt uchun tuzatishi. */
data class PrayerAdjust(
    /** Hisoblangan vaqtga qo'shiladigan daqiqa (+/-). */
    val offset: Int = 0,
    /** >= 0 bo'lsa — har kuni shu doimiy vaqt (kun boshidan daqiqa). */
    val fixed: Int = -1,
) {
    val isDefault get() = offset == 0 && fixed < 0
}

/** Bir kunlik natija, daqiqalarda (kun boshidan, mahalliy vaqt). */
data class DayTimes(
    val date: LocalDate,
    /** Faqat astronomik hisob (usul ehtiyotlari bilan), foydalanuvchi tuzatishisiz. */
    val calculated: Map<Prayer, Int>,
    /** Foydalanuvchi tuzatishlari qo'llangan yakuniy vaqt. */
    val final: Map<Prayer, Int>,
    val sunriseExact: Double,
    val sunsetExact: Double,
) {
    operator fun get(p: Prayer): Int = final.getValue(p)
    fun isAdjusted(p: Prayer) = final[p] != calculated[p]
    val dayLengthMinutes: Int get() = Math.round((sunsetExact - sunriseExact) * 60).toInt()
}

object PrayerCalc {

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)

    private fun julian(y0: Int, m0: Int, d: Int): Double {
        var y = y0
        var m = m0
        if (m <= 2) {
            y -= 1; m += 12
        }
        val a = y / 100
        val b = 2 - a + a / 4
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5
    }

    /** Quyosh og'ishi (declination) va vaqt tenglamasi (soat). */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val dd = jd - 2451545.0
        val g = (357.529 + 0.98560028 * dd).mod(360.0)
        val q = (280.459 + 0.98564736 * dd).mod(360.0)
        val l = (q + 1.915 * sin(rad(g)) + 0.020 * sin(rad(2 * g))).mod(360.0)
        val e = 23.439 - 0.00000036 * dd
        val ra = deg(atan2(cos(rad(e)) * sin(rad(l)), cos(rad(l)))) / 15.0
        val decl = deg(asin(sin(rad(e)) * sin(rad(l))))
        var eqt = q / 15.0 - ra.mod(24.0)
        eqt = (eqt + 12).mod(24.0) - 12
        return decl to eqt
    }

    /**
     * Soatlarda (mahalliy vaqt) hisoblaydi. [tzHours] — UTC farqi (O'zbekiston: 5).
     * Qaytaradi: fajr, sunrise, dhuhr, asr, sunset, isha (soat, kasr bilan).
     */
    fun rawHours(
        date: LocalDate, lat: Double, lon: Double, tzHours: Double,
        fajrAngle: Double, ishaAngle: Double, ishaMinutes: Int, hanafi: Boolean, elevation: Double = 0.0,
    ): DoubleArray {
        val jDate = julian(date.year, date.monthValue, date.dayOfMonth) - lon / 360.0
        fun noon(t: Double): Double = 12 - sunPosition(jDate + t / 24).second
        fun angleTime(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sunPosition(jDate + t / 24).first
            val n = noon(t)
            val x = (-sin(rad(angle)) - sin(rad(decl)) * sin(rad(lat))) / (cos(rad(decl)) * cos(rad(lat)))
            val h = deg(acos(x.coerceIn(-1.0, 1.0))) / 15.0
            return if (ccw) n - h else n + h
        }
        fun asr(factor: Double, t: Double): Double {
            val decl = sunPosition(jDate + t / 24).first
            val a = -deg(atan(1 / (factor + tan(rad(abs(lat - decl))))))
            return angleTime(a, t, false)
        }
        val riseAngle = 0.833 + 0.0347 * sqrt(elevation.coerceAtLeast(0.0))
        // Boshlang'ich taxminlar
        var f = 5.0; var sr = 6.0; var dh = 12.0; var asr = 13.0; var ss = 18.0; var isha = 18.0
        repeat(2) {
            val nf = angleTime(fajrAngle, f, true)
            val nsr = angleTime(riseAngle, sr, true)
            val ndh = noon(dh)
            val nasr = asr(if (hanafi) 2.0 else 1.0, asr)
            val nss = angleTime(riseAngle, ss, false)
            val nisha = angleTime(ishaAngle, isha, false)
            f = nf; sr = nsr; dh = ndh; asr = nasr; ss = nss; isha = nisha
        }
        val adj = tzHours - lon / 15.0
        val sunset = ss + adj
        val ishaH = if (ishaMinutes > 0) sunset + ishaMinutes / 60.0 else isha + adj
        return doubleArrayOf(f + adj, sr + adj, dh + adj, asr + adj, sunset, ishaH)
    }

    fun compute(
        date: LocalDate,
        lat: Double,
        lon: Double,
        tzHours: Double,
        settings: CalcSettings,
        adjust: Map<Prayer, PrayerAdjust> = emptyMap(),
    ): DayTimes {
        val m = settings.method
        val h = rawHours(date, lat, lon, tzHours, settings.fajrAngle, settings.ishaAngle, m.ishaMinutes, settings.hanafi)
        fun toMin(x: Double) = Math.round(x * 60.0).toInt()
        val calc = linkedMapOf(
            Prayer.FAJR to toMin(h[0]),
            Prayer.SUNRISE to toMin(h[1]),
            Prayer.DHUHR to toMin(h[2]) + m.dhuhrPlus,
            Prayer.ASR to toMin(h[3]),
            Prayer.MAGHRIB to toMin(h[4]) + m.maghribPlus,
            Prayer.ISHA to toMin(h[5]),
        )
        val fin = LinkedHashMap<Prayer, Int>()
        for ((p, v) in calc) {
            val a = adjust[p]
            fin[p] = when {
                a == null -> v
                a.fixed >= 0 -> a.fixed
                else -> v + a.offset
            }
        }
        return DayTimes(date, calc, fin, h[1], h[4])
    }
}

object Qibla {
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206

    /** Shimoldan soat yo'nalishi bo'yicha qibla burchagi (haqiqiy shimolga nisbatan). */
    fun bearing(lat: Double, lon: Double): Double {
        val p1 = Math.toRadians(lat)
        val p2 = Math.toRadians(KAABA_LAT)
        val dl = Math.toRadians(KAABA_LON - lon)
        val y = sin(dl)
        val x = cos(p1) * tan(p2) - sin(p1) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    /** Ka'bagacha masofa, km. */
    fun distanceKm(lat: Double, lon: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - lat)
        val dLon = Math.toRadians(KAABA_LON - lon)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat)) * cos(Math.toRadians(KAABA_LAT)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * r * atan2(sqrt(a), sqrt(1 - a))
    }
}

fun fmtMin(m: Int): String {
    val x = ((m % 1440) + 1440) % 1440
    return "%02d:%02d".format(x / 60, x % 60)
}
