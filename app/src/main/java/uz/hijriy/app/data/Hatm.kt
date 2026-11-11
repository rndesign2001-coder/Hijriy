package uz.hijriy.app.data

import org.json.JSONObject
import java.time.LocalDate
import kotlin.math.ceil

/** Hatm (Qur'onni to'liq o'qib chiqish) rejasi: oxirgi ochilgan mushaf sahifasiga qarab hisoblanadi. */
data class HatmPlan(
    val fromPage: Int,
    val currentPage: Int,
    val daysLeft: Int,
    val todayPages: Int,
    val progress: Float,
    val end: LocalDate,
) {
    companion object {
        const val PAGES = 604

        fun of(s: Settings, today: LocalDate): HatmPlan? {
            if (s.hatmStart <= 0 || s.hatmEnd <= 0) return null
            val end = LocalDate.ofEpochDay(s.hatmEnd)
            val cur = s.mushafPage.coerceIn(s.hatmFromPage, PAGES)
            val total = (PAGES - s.hatmFromPage + 1).coerceAtLeast(1)
            val done = (cur - s.hatmFromPage).coerceAtLeast(0)
            val daysLeft = (s.hatmEnd - today.toEpochDay() + 1).toInt().coerceAtLeast(1)
            val tp = ceil((PAGES - cur + 1).toDouble() / daysLeft).toInt().coerceAtLeast(0)
            return HatmPlan(s.hatmFromPage, cur, daysLeft, tp, (done.toFloat() / total).coerceIn(0f, 1f), end)
        }
    }
}

/** Namoz kundaligi: har kun uchun 5 farz namozining bit-niqobi (bomdod=1, peshin=2, asr=4, shom=8, xufton=16). */
class JournalStore(private val store: SettingsStore) {
    private val map: MutableMap<Long, Int> = runCatching {
        val j = JSONObject(store.getString("journal") ?: "{}")
        j.keys().asSequence().associate { it.toLong() to j.getInt(it) }.toMutableMap()
    }.getOrDefault(mutableMapOf())

    val state = kotlinx.coroutines.flow.MutableStateFlow(map.toMap())

    fun get(day: LocalDate): Int = map[day.toEpochDay()] ?: 0

    fun toggle(day: LocalDate, bit: Int) {
        val k = day.toEpochDay()
        val v = (map[k] ?: 0) xor bit
        if (v == 0) map.remove(k) else map[k] = v
        state.value = map.toMap()
        store.putString("journal", JSONObject(map.mapKeys { it.key.toString() }).toString())
    }

    /** Ketma-ket to'liq (5/5) kunlar soni — bugundan (yoki kechadan) orqaga. */
    fun streak(today: LocalDate): Int {
        var d = if (get(today) == 31) today else today.minusDays(1)
        var n = 0
        while (get(d) == 31) { n++; d = d.minusDays(1) }
        return n
    }
}
