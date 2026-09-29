package uz.hijriy.app.data

import android.content.Context
import org.json.JSONObject

/** Mushaf qatori: [type] 0 — to'liq tekislanadigan, 1 — markazda, 2 — sura sarlavhasi (text = sura raqami), 3 — bismillah. */
class MushafLine(val type: Int, val text: String)

class MushafPage(val number: Int, val lines: List<MushafLine>, val firstSura: Int, val firstAyah: Int)

class Mushaf(val pages: List<MushafPage>, private val ayahPage: List<IntArray>) {
    val pageCount get() = pages.size
    fun pageOf(sura: Int, ayah: Int): Int {
        val a = ayahPage.getOrNull(sura - 1) ?: return 1
        return a[(ayah - 1).coerceIn(0, a.size - 1)]
    }
}

object MushafRepo {
    @Volatile
    private var cache: Mushaf? = null

    fun load(context: Context): Mushaf {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val j = JSONObject(context.assets.open("mushaf.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
            val apArr = j.getJSONArray("ap")
            val ap = (0 until apArr.length()).map { i ->
                val a = apArr.getJSONArray(i); IntArray(a.length()) { a.getInt(it) }
            }
            // Har sahifadagi birinchi oyat
            val first = HashMap<Int, Pair<Int, Int>>()
            ap.forEachIndexed { si, arr ->
                arr.forEachIndexed { ai, p -> if (!first.containsKey(p)) first[p] = (si + 1) to (ai + 1) }
            }
            val pArr = j.getJSONArray("pages")
            var last = 1 to 1
            val pages = (0 until pArr.length()).map { i ->
                val ls = pArr.getJSONArray(i)
                val lines = (0 until ls.length()).map { k ->
                    val l = ls.getJSONArray(k); MushafLine(l.getInt(0), l.getString(1))
                }
                val f = first[i + 1] ?: last
                last = f
                MushafPage(i + 1, lines, f.first, f.second)
            }
            return Mushaf(pages, ap).also { cache = it }
        }
    }
}
