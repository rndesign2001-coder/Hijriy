package uz.hijriy.app.data

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** everyayah.com dagi qorilar (har bir oyat alohida mp3). */
data class Reciter(val name: String, val folder: String)

val Reciters = listOf(
    Reciter("Mishari Roshid Al-Afasiy", "Alafasy_128kbps"),
    Reciter("Mahmud Xalil Al-Husariy", "Husary_128kbps"),
    Reciter("Abdulbosit Abdussamad", "Abdul_Basit_Murattal_192kbps"),
    Reciter("Abdurrahmon As-Sudays", "Abdurrahmaan_As-Sudais_192kbps"),
    Reciter("Muhammad Siddiq Al-Minshoviy", "Minshawy_Murattal_128kbps"),
    Reciter("Abu Bakr Ash-Shotiriy", "Abu_Bakr_Ash-Shaatree_128kbps"),
)

fun ayahUrl(folder: String, sura: Int, ayah: Int) =
    "https://everyayah.com/data/$folder/%03d%03d.mp3".format(sura, ayah)

data class PlayState(
    val sura: Int = 0,
    /** 0 — bismillah o'qilmoqda. */
    val ayah: Int = 0,
    val playing: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
) {
    val active get() = sura > 0
}

/** Oyatlarni ketma-ket internetdan ijro etuvchi oddiy pleyer. */
object QuranAudio {
    private val _state = MutableStateFlow(PlayState())
    val state: StateFlow<PlayState> = _state.asStateFlow()
    private var player: MediaPlayer? = null
    private var reciter: Reciter = Reciters[0]
    private var suraLen = 0

    fun play(sura: Int, fromAyah: Int, ayahCount: Int, r: Reciter) {
        reciter = r; suraLen = ayahCount
        // Fotiha va Tavbadan tashqari suralar boshida bismillah
        val start = if (fromAyah <= 1 && sura != 1 && sura != 9) 0 else fromAyah.coerceIn(1, ayahCount)
        load(sura, start)
    }

    private fun load(sura: Int, ayah: Int) {
        release()
        _state.value = PlayState(sura, ayah, playing = true, loading = true)
        val url = if (ayah == 0) ayahUrl(reciter.folder, 1, 1) else ayahUrl(reciter.folder, sura, ayah)
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
            )
            mp.setDataSource(url)
            mp.setOnPreparedListener {
                if (player === it) {
                    it.start(); _state.value = _state.value.copy(loading = false)
                }
            }
            mp.setOnCompletionListener {
                if (player !== it) return@setOnCompletionListener
                val next = ayah + 1
                if (next <= suraLen) load(sura, next) else stop()
            }
            mp.setOnErrorListener { _, _, _ ->
                _state.value = _state.value.copy(playing = false, loading = false, error = "Internetga ulanib bo'lmadi")
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            _state.value = _state.value.copy(playing = false, loading = false, error = "Ijro etib bo'lmadi")
        }
    }

    fun togglePause() {
        val p = player ?: return
        val st = _state.value
        if (st.loading) return
        if (p.isPlaying) { p.pause(); _state.value = st.copy(playing = false) }
        else { p.start(); _state.value = st.copy(playing = true) }
    }

    fun next() { val s = _state.value; if (s.active && s.ayah < suraLen) load(s.sura, s.ayah + 1) }
    fun prev() { val s = _state.value; if (s.active && s.ayah > 1) load(s.sura, s.ayah - 1) }

    fun stop() {
        release()
        _state.value = PlayState()
    }

    private fun release() {
        player?.let { runCatching { it.reset(); it.release() } }
        player = null
    }
}
