package uz.hijriy.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.hijriy.app.data.Quran
import uz.hijriy.app.data.QuranRepo
import uz.hijriy.app.data.SettingsStore
import uz.hijriy.app.data.Weather
import uz.hijriy.app.data.WeatherApi
import uz.hijriy.app.notify.PrayerScheduler

sealed interface WeatherState {
    data object Idle : WeatherState
    data object Loading : WeatherState
    data class Ready(val w: Weather, val stale: Boolean = false, val error: String? = null) : WeatherState
    data class Failed(val message: String) : WeatherState
}

class HijriyApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    lateinit var settings: SettingsStore
        private set
    lateinit var bookmarks: uz.hijriy.app.data.Bookmarks
        private set
    lateinit var qazo: uz.hijriy.app.data.QazoStore
        private set

    private val _quran = MutableStateFlow<Quran?>(null)
    val quran: StateFlow<Quran?> = _quran.asStateFlow()

    private val _weather = MutableStateFlow<WeatherState>(WeatherState.Idle)
    val weather: StateFlow<WeatherState> = _weather.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsStore(this)
        bookmarks = uz.hijriy.app.data.Bookmarks(settings)
        qazo = uz.hijriy.app.data.QazoStore(settings)
        scope.launch { uz.hijriy.app.data.TranslationRepo.loadLocal(this@HijriyApp) }
        createChannel()
        loadCachedWeather()
        scope.launch(Dispatchers.Default) { _quran.value = QuranRepo.load(this@HijriyApp) }
        PrayerScheduler.reschedule(this)
    }

    fun ensureQuran() {
        if (_quran.value == null) scope.launch(Dispatchers.Default) { _quran.value = QuranRepo.load(this@HijriyApp) }
    }

    private fun loadCachedWeather() {
        val raw = settings.getString("weather_json") ?: return
        val at = settings.getString("weather_at")?.toLongOrNull() ?: 0L
        val la = settings.getString("weather_lat")?.toDoubleOrNull() ?: return
        val lo = settings.getString("weather_lon")?.toDoubleOrNull() ?: return
        runCatching { WeatherApi.parse(raw, at, la, lo) }.onSuccess {
            _weather.value = WeatherState.Ready(it, stale = true)
        }
    }

    /** Ob-havoni yangilaydi. [force]=false bo'lsa 30 daqiqadan yangi ma'lumot qayta yuklanmaydi. */
    fun refreshWeather(force: Boolean = false) {
        val s = settings.value
        val cur = _weather.value
        if (cur is WeatherState.Loading) return
        if (!force && cur is WeatherState.Ready && !cur.stale &&
            System.currentTimeMillis() - cur.w.fetchedAt < 30 * 60_000 &&
            kotlin.math.abs(cur.w.lat - s.lat) < 0.01 && kotlin.math.abs(cur.w.lon - s.lon) < 0.01
        ) return
        val prev = (cur as? WeatherState.Ready)?.w
        _weather.value = WeatherState.Loading
        scope.launch {
            try {
                val raw = WeatherApi.fetch(s.lat, s.lon)
                val now = System.currentTimeMillis()
                val w = WeatherApi.parse(raw, now, s.lat, s.lon)
                settings.putString("weather_json", raw)
                settings.putString("weather_at", now.toString())
                settings.putString("weather_lat", s.lat.toString())
                settings.putString("weather_lon", s.lon.toString())
                _weather.value = WeatherState.Ready(w)
            } catch (e: Exception) {
                val msg = "Internetga ulanib bo'lmadi"
                _weather.value = if (prev != null) WeatherState.Ready(prev, stale = true, error = msg) else WeatherState.Failed(msg)
            }
        }
    }

    private fun createChannel() {
        uz.hijriy.app.notify.Channels.ensure(this, settings.value.notifySound)
    }

    /** Bildirishnoma yoki vidjetdan ochilganda ko'rsatiladigan sahifa. */
    val pendingRoute = MutableStateFlow<String?>(null)

    companion object {
        lateinit var instance: HijriyApp
            private set
    }
}
