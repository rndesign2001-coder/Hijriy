package uz.hijriy.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** O'zbek kirill yozuvini lotinchaga o'giradi (rasmiy qoidalar asosida). */
object UzTranslit {
    private val map = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'ж' to "j", 'з' to "z", 'и' to "i",
        'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r",
        'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "x", 'ч' to "ch", 'ш' to "sh", 'ъ' to "'",
        'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya", 'ў' to "o'", 'қ' to "q", 'ғ' to "g'", 'ҳ' to "h",
        'ё' to "yo", 'ы' to "i", 'щ' to "sh",
    )
    private val vowels = "аеёиоуэюяўы"

    fun toLatin(s: String): String {
        val sb = StringBuilder(s.length + 16)
        for (i in s.indices) {
            val ch = s[i]
            val lower = ch.lowercaseChar()
            val upper = ch != lower
            val prev = if (i > 0) s[i - 1].lowercaseChar() else ' '
            val out: String = when (lower) {
                // е so'z boshida va unlidan keyin "ye"
                'е' -> if (!prev.isLetter() || prev in vowels || prev == 'ъ' || prev == 'ь') "ye" else "e"
                // ц: so'z boshida va undoshdan keyin "s", unlidan keyin "ts"
                'ц' -> if (prev in vowels) "ts" else "s"
                else -> map[lower] ?: ch.toString()
            }
            if (!upper || out.isEmpty()) { sb.append(out); continue }
            // Bosh harf: yonidagi harf ham katta bo'lsa — hammasi katta (QUR'ON), aks holda faqat birinchisi (Qur'on)
            val nextUpper = i + 1 < s.length && s[i + 1].isUpperCase()
            val prevUpper = i > 0 && s[i - 1].isUpperCase()
            sb.append(if (nextUpper || (prevUpper && !(i + 1 < s.length && s[i + 1].isLowerCase()))) out.uppercase() else out.replaceFirstChar { it.uppercaseChar() })
        }
        return sb.toString()
    }
}

/** Qur'on ma'nolarining o'zbekcha tarjimasi (Muhammad Sodiq Muhammad Yusuf). Bir marta yuklab olinadi. */
object TranslationRepo {
    private const val FILE = "uz_sodik.json"
    private const val URL_API = "https://api.alquran.cloud/v1/quran/uz.sodik"

    sealed interface State {
        data object Missing : State
        data class Downloading(val percent: Int) : State
        data class Ready(val suras: List<List<String>>) : State
        data class Failed(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Missing)
    val state: StateFlow<State> = _state.asStateFlow()

    fun file(ctx: Context) = File(ctx.filesDir, FILE)

    suspend fun loadLocal(ctx: Context) = withContext(Dispatchers.IO) {
        if (_state.value is State.Ready) return@withContext
        val f = file(ctx)
        if (!f.exists()) { _state.value = State.Missing; return@withContext }
        runCatching { parseCompact(f.readText()) }
            .onSuccess { _state.value = State.Ready(it) }
            .onFailure { f.delete(); _state.value = State.Missing }
    }

    suspend fun download(ctx: Context) = withContext(Dispatchers.IO) {
        _state.value = State.Downloading(0)
        try {
            val c = URL(URL_API).openConnection() as HttpURLConnection
            c.connectTimeout = 20000; c.readTimeout = 60000
            c.setRequestProperty("User-Agent", "HijriyTaqvim/1.0 (Android)")
            if (c.responseCode != 200) throw RuntimeException("HTTP ${c.responseCode}")
            val total = c.contentLengthLong.takeIf { it > 0 } ?: 4_500_000L
            val sb = StringBuilder()
            c.inputStream.bufferedReader().use { r ->
                val buf = CharArray(16384); var read = 0L
                while (true) {
                    val n = r.read(buf); if (n < 0) break
                    sb.append(buf, 0, n); read += n
                    _state.value = State.Downloading(((read * 100) / total).toInt().coerceIn(0, 99))
                }
            }
            val data = JSONObject(sb.toString()).getJSONObject("data").getJSONArray("surahs")
            val suras = (0 until data.length()).map { i ->
                val ay = data.getJSONObject(i).getJSONArray("ayahs")
                (0 until ay.length()).map { k -> ay.getJSONObject(k).getString("text") }
            }
            require(suras.size == 114 && suras.sumOf { it.size } == 6236) { "Tarjima to'liq emas" }
            file(ctx).writeText(JSONArray(suras.map { JSONArray(it) }).toString())
            _state.value = State.Ready(suras)
        } catch (e: Exception) {
            _state.value = State.Failed("Yuklab bo'lmadi: internetni tekshiring")
        }
    }

    fun delete(ctx: Context) { file(ctx).delete(); _state.value = State.Missing }

    private fun parseCompact(s: String): List<List<String>> {
        val a = JSONArray(s)
        return (0 until a.length()).map { i -> val x = a.getJSONArray(i); (0 until x.length()).map { x.getString(it) } }
    }
}
