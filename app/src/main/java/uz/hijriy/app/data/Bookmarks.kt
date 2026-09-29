package uz.hijriy.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Xatcho'p: oyat ([page]=0) yoki mushaf sahifasi. */
data class Bookmark(val sura: Int, val ayah: Int, val page: Int, val time: Long) {
    val isPage get() = page > 0
}

class Bookmarks(private val store: SettingsStore) {
    private val _list = MutableStateFlow(load())
    val list: StateFlow<List<Bookmark>> = _list.asStateFlow()

    private fun load(): List<Bookmark> = runCatching {
        val a = JSONArray(store.getString("bookmarks") ?: "[]")
        (0 until a.length()).map { a.getJSONObject(it).let { o -> Bookmark(o.getInt("s"), o.getInt("a"), o.optInt("p", 0), o.optLong("t")) } }
    }.getOrDefault(emptyList())

    private fun save(l: List<Bookmark>) {
        _list.value = l
        store.putString("bookmarks", JSONArray(l.map { JSONObject().put("s", it.sura).put("a", it.ayah).put("p", it.page).put("t", it.time) }).toString())
    }

    fun hasAyah(sura: Int, ayah: Int) = _list.value.any { !it.isPage && it.sura == sura && it.ayah == ayah }
    fun hasPage(page: Int) = _list.value.any { it.page == page }

    fun toggleAyah(sura: Int, ayah: Int) {
        val l = _list.value
        save(if (hasAyah(sura, ayah)) l.filterNot { !it.isPage && it.sura == sura && it.ayah == ayah }
        else listOf(Bookmark(sura, ayah, 0, System.currentTimeMillis())) + l)
    }

    fun togglePage(page: Int, sura: Int, ayah: Int) {
        val l = _list.value
        save(if (hasPage(page)) l.filterNot { it.page == page } else listOf(Bookmark(sura, ayah, page, System.currentTimeMillis())) + l)
    }

    fun remove(b: Bookmark) = save(_list.value - b)
}

/** Qazo namozlar hisoblagichi. */
class QazoStore(private val store: SettingsStore) {
    val names = listOf("Bomdod", "Peshin", "Asr", "Shom", "Xufton", "Vitr")
    private val _counts = MutableStateFlow(load())
    val counts: StateFlow<List<Int>> = _counts.asStateFlow()

    private fun load(): List<Int> = runCatching {
        val a = JSONArray(store.getString("qazo") ?: "[]")
        List(names.size) { if (it < a.length()) a.getInt(it) else 0 }
    }.getOrDefault(List(names.size) { 0 })

    fun set(i: Int, v: Int) {
        val l = _counts.value.toMutableList(); l[i] = v.coerceIn(0, 999_999)
        _counts.value = l; store.putString("qazo", JSONArray(l).toString())
    }

    fun addAll(n: Int) { for (i in names.indices) set(i, _counts.value[i] + n) }
    fun reset() { for (i in names.indices) set(i, 0) }
}
