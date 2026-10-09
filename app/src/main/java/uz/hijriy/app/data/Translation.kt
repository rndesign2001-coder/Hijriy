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

    // ---------------- Lotin → kirill ----------------
    private const val APOS = "'’ʻʼ‘`"
    private val lat = mapOf(
        'a' to 'а', 'b' to 'б', 'd' to 'д', 'f' to 'ф', 'g' to 'г', 'h' to 'ҳ', 'i' to 'и', 'j' to 'ж', 'k' to 'к',
        'l' to 'л', 'm' to 'м', 'n' to 'н', 'o' to 'о', 'p' to 'п', 'q' to 'қ', 'r' to 'р', 's' to 'с', 't' to 'т',
        'u' to 'у', 'v' to 'в', 'x' to 'х', 'y' to 'й', 'z' to 'з', 'c' to 'ц', 'w' to 'в',
    )
    private const val LAT_VOWELS = "aeiou"
    /** Kirillda boshqacha yoziladigan so'zlar (asosan rus tilidan o'zlashganlar). */
    private val words = mapOf(
        "yanvar" to "январь", "fevral" to "февраль", "aprel" to "апрель", "iyun" to "июнь", "iyul" to "июль",
        "sentabr" to "сентябрь", "oktabr" to "октябрь", "noyabr" to "ноябрь", "dekabr" to "декабрь",
        "sentyabr" to "сентябрь", "oktyabr" to "октябрь", "mushaf" to "мусҳаф",
    )
    private val keep = setOf("GPS", "UV", "hPa", "MWL", "ISNA", "APK", "AAB", "PNG", "JPG", "MP3", "km", "Wi-Fi")
    private val WORD = Regex("[A-Za-z]+(?:[$APOS][A-Za-z]+)*(?<=[oOgG])[$APOS]|[A-Za-z]+(?:[$APOS][A-Za-z]+)*")

    /** O'zbek lotin yozuvini kirillga o'giradi (rasmiy qoidalar: o'→ў, g'→ғ, sh, ch, yo/yu/ya/ye, e/э, ' → ъ). */
    fun toCyrillic(s: String): String {
        if (s.isEmpty() || s.none { it in 'A'..'Z' || it in 'a'..'z' } || "://" in s) return s
        return WORD.replace(s) { m -> word(m.value) }
    }

    private fun word(w: String): String {
        if (w in keep) return w
        words[w.lowercase()]?.let { r ->
            return when {
                w.length > 1 && w.all { !it.isLetter() || it.isUpperCase() } -> r.uppercase()
                w[0].isUpperCase() -> r.replaceFirstChar { it.uppercaseChar() }
                else -> r
            }
        }
        val allUpper = w.length > 1 && w.filter { it.isLetter() }.all { it.isUpperCase() }
        val sb = StringBuilder(w.length)
        var i = 0
        while (i < w.length) {
            val ch = w[i]; val lo = ch.lowercaseChar()
            val n1 = if (i + 1 < w.length) w[i + 1] else ' '
            val n1l = n1.lowercaseChar()
            val prev = if (i > 0) w[i - 1].lowercaseChar() else ' '
            var take = 1
            val out: Char = when {
                ch in APOS && prev == 's' && n1l == 'h' -> { i++; continue } // Is'hoq → Исҳоқ
                ch in APOS -> 'ъ'
                lo == 'o' && n1 in APOS -> { take = 2; 'ў' }
                lo == 'g' && n1 in APOS -> { take = 2; 'ғ' }
                lo == 's' && n1l == 'h' -> { take = 2; 'ш' }
                lo == 'c' && n1l == 'h' -> { take = 2; 'ч' }
                lo == 'y' && n1l == 'o' && (i + 2 >= w.length || w[i + 2] !in APOS) -> { take = 2; 'ё' }
                lo == 'y' && n1l == 'u' -> { take = 2; 'ю' }
                lo == 'y' && n1l == 'a' -> { take = 2; 'я' }
                lo == 'y' && n1l == 'e' -> { take = 2; 'е' }
                lo == 'e' -> if (i == 0 || prev in LAT_VOWELS) 'э' else 'е'
                else -> lat[lo] ?: ch
            }
            sb.append(if (ch.isUpperCase() || allUpper) out.uppercaseChar() else out)
            i += take
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
