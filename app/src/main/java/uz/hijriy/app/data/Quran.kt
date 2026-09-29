package uz.hijriy.app.data

import android.content.Context
import org.json.JSONObject

class Ayah(val number: Int, val text: String, val ann: IntArray)

class Sura(
    val number: Int,
    val arName: String,
    val translit: String,
    val uzName: String,
    val meccan: Boolean,
    val ayahs: List<Ayah>,
) {
    val count get() = ayahs.size
    val hasBismillah get() = number != 1 && number != 9
}

class Quran(
    val suras: List<Sura>,
    val bismillah: Ayah,
    /** Juz boshlanishi: (sura, oyat), 30 ta. */
    val juz: List<Pair<Int, Int>>,
) {
    fun juzOf(sura: Int, ayah: Int): Int {
        var j = 1
        for ((i, p) in juz.withIndex()) {
            if (sura > p.first || (sura == p.first && ayah >= p.second)) j = i + 1
        }
        return j
    }
}

object QuranRepo {
    @Volatile
    private var cache: Quran? = null

    fun load(context: Context): Quran {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val txt = context.assets.open("quran.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val j = JSONObject(txt)
            fun ints(a: org.json.JSONArray): IntArray = IntArray(a.length()) { a.getInt(it) }
            val b = j.getJSONObject("bism")
            val bism = Ayah(0, b.getString("t"), ints(b.getJSONArray("a")))
            val sArr = j.getJSONArray("suras")
            val suras = (0 until sArr.length()).map { i ->
                val s = sArr.getJSONObject(i)
                val aArr = s.getJSONArray("a")
                Sura(
                    number = s.getInt("n"),
                    arName = s.getString("ar"),
                    translit = s.getString("tr"),
                    uzName = s.getString("uz"),
                    meccan = s.getString("t") == "K",   // K — Makkiy, D — Madaniy
                    ayahs = (0 until aArr.length()).map { k ->
                        val a = aArr.getJSONArray(k)
                        Ayah(k + 1, a.getString(0), ints(a.getJSONArray(1)))
                    }
                )
            }
            val jz = j.getJSONArray("juz")
            val juz = (0 until jz.length()).map { val p = jz.getJSONArray(it); p.getInt(0) to p.getInt(1) }
            return Quran(suras, bism, juz).also { cache = it }
        }
    }
}

/** Tajvid qoidalari: indeks quran.json dagi "rules" tartibiga mos. */
enum class TajweedRule(val uz: String, val light: Long, val dark: Long) {
    HAMZAT_WASL("Hamzai vasl (o'qilmaydi)", 0xFF9E9E9E, 0xFF8A8F8C),
    LAM_SHAMSIYYAH("Lomi shamsiya (o'qilmaydi)", 0xFF9E9E9E, 0xFF8A8F8C),
    SILENT("O'qilmaydigan harf", 0xFF9E9E9E, 0xFF8A8F8C),
    MADD_2("Madd tabiiy (2 harakat)", 0xFF537FFF, 0xFF7FA0FF),
    MADD_246("Madd oriz / lin (2-4-6)", 0xFF4050FF, 0xFF8C95FF),
    MADD_6("Madd lozim (6 harakat)", 0xFF000EBC, 0xFF6F7BFF),
    MADD_MUTTASIL("Madd muttasil (4-5)", 0xFF2144C1, 0xFF6E8CF0),
    MADD_MUNFASIL("Madd munfasil (4-5)", 0xFF5B6FC8, 0xFF9BA9EA),
    GHUNNAH("G'unna", 0xFFFF7E1E, 0xFFFF9D52),
    QALQALAH("Qalqala", 0xFFDD0008, 0xFFFF5C63),
    IKHFA("Ixfo", 0xFF9400A8, 0xFFD36BE4),
    IKHFA_SHAFAWI("Ixfoi shafaviy", 0xFFD500B7, 0xFFF06ADB),
    IQLAB("Iqlob", 0xFF26BFFD, 0xFF5ED3FF),
    IDGHAAM_GHUNNAH("Idg'om g'unnali", 0xFF169777, 0xFF3FD1A8),
    IDGHAAM_NO_GHUNNAH("Idg'om g'unnasiz", 0xFF169200, 0xFF5CCB45),
    IDGHAAM_SHAFAWI("Idg'omi shafaviy", 0xFF58B800, 0xFF8BDB3C),
    IDGHAAM_MUTAJANISAYN("Idg'omi mutajonisayn", 0xFFA1A1A1, 0xFFB5B5B5),
    IDGHAAM_MUTAQARIBAYN("Idg'omi mutaqoribayn", 0xFFA1A1A1, 0xFFB5B5B5);

    companion object {
        val legend: List<TajweedRule> = entries.filter { it != LAM_SHAMSIYYAH && it != HAMZAT_WASL && it != IDGHAAM_MUTAQARIBAYN }
    }
}
