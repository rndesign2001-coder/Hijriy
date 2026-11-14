package uz.hijriy.app.data

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
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
    /** Har oyatni necha marta takrorlash (yodlash rejimi). */
    var repeat: Int = 1
    private var played = 0
    private var appCtx: Context? = null
    private var focusReq: AudioFocusRequest? = null
    private var resumeOnFocus = false

    /** Hozir tanlangan qori nomi (bildirishnoma uchun). */
    val reciterName: String get() = reciter.name

    fun init(context: Context) { appCtx = context.applicationContext }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> { resumeOnFocus = false; pause() }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (_state.value.playing) { resumeOnFocus = true; pause() }
            }
            AudioManager.AUDIOFOCUS_GAIN -> if (resumeOnFocus) { resumeOnFocus = false; resume() }
        }
    }

    private fun requestFocus() {
        val ctx = appCtx ?: return
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val req = focusReq ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setOnAudioFocusChangeListener(focusListener).build().also { focusReq = it }
        runCatching { am.requestAudioFocus(req) }
    }

    private fun abandonFocus() {
        val ctx = appCtx ?: return
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        focusReq?.let { runCatching { am.abandonAudioFocusRequest(it) } }
        resumeOnFocus = false
    }

    fun play(sura: Int, fromAyah: Int, ayahCount: Int, r: Reciter) {
        reciter = r; suraLen = ayahCount
        requestFocus()
        appCtx?.let { uz.hijriy.app.notify.QuranAudioService.start(it) }
        // Fotiha va Tavbadan tashqari suralar boshida bismillah
        val start = if (fromAyah <= 1 && sura != 1 && sura != 9) 0 else fromAyah.coerceIn(1, ayahCount)
        load(sura, start)
    }

    private fun load(sura: Int, ayah: Int, again: Boolean = false) {
        if (!again) played = 0
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
            appCtx?.let { mp.setWakeMode(it, PowerManager.PARTIAL_WAKE_LOCK) }
            mp.setDataSource(url)
            mp.setOnPreparedListener {
                if (player === it) {
                    it.start(); _state.value = _state.value.copy(loading = false)
                }
            }
            mp.setOnCompletionListener {
                if (player !== it) return@setOnCompletionListener
                played++
                if (ayah > 0 && played < repeat) { load(sura, ayah, again = true); return@setOnCompletionListener }
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

    fun pause() {
        val p = player ?: return
        val st = _state.value
        if (st.loading || !st.playing) return
        runCatching { p.pause() }
        _state.value = st.copy(playing = false)
    }

    fun resume() {
        val p = player ?: return
        val st = _state.value
        if (st.loading || st.playing) return
        requestFocus()
        runCatching { p.start() }
        _state.value = st.copy(playing = true)
    }

    fun togglePause() { if (_state.value.playing) pause() else resume() }

    /** Tilovat davomida qorini almashtiradi: shu oyatdan yangi qori bilan davom etadi. */
    fun changeReciter(r: Reciter) {
        if (r.folder == reciter.folder) return
        reciter = r
        val s = _state.value
        if (s.active) load(s.sura, s.ayah, again = true)
    }

    fun next() { val s = _state.value; if (s.active && s.ayah < suraLen) load(s.sura, s.ayah + 1) }
    fun prev() { val s = _state.value; if (s.active && s.ayah > 1) load(s.sura, s.ayah - 1) }

    fun stop() {
        release()
        abandonFocus()
        _state.value = PlayState()
    }

    private fun release() {
        player?.let { runCatching { it.reset(); it.release() } }
        player = null
    }
}
