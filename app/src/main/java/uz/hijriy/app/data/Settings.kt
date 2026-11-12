package uz.hijriy.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import uz.hijriy.app.core.CalcMethod
import uz.hijriy.app.core.CalcSettings
import uz.hijriy.app.core.DayTimes
import uz.hijriy.app.core.Prayer
import uz.hijriy.app.core.PrayerAdjust
import uz.hijriy.app.core.PrayerCalc
import java.time.LocalDate
import java.time.ZoneId

enum class ThemeMode(val title: String) { SYSTEM("Tizim bo'yicha"), LIGHT("Kunduzgi"), DARK("Tungi") }

data class Settings(
    // Joylashuv
    val locName: String = "Toshkent shahri",
    val locRegion: String = "Toshkent shahri",
    val lat: Double = 41.3111,
    val lon: Double = 69.2797,
    val fromGps: Boolean = false,
    /** Tanlangan shaharning vaqt mintaqasi (IANA, masalan "Europe/Istanbul"); bo'sh — O'zbekiston/telefon. */
    val tz: String = "",
    /** Davlat kodi (ISO-2): hisoblash usulini avtomatik tanlash uchun. */
    val country: String = "UZ",
    /** Davlatga qarab hisoblash usulini avtomatik tanlash. */
    val autoMethod: Boolean = true,
    // Namoz hisobi
    val method: CalcMethod = CalcMethod.UZBEKISTAN,
    val hanafi: Boolean = true,
    val customFajr: Double = 15.5,
    val customIsha: Double = 15.5,
    val adjust: Map<Prayer, PrayerAdjust> = emptyMap(),
    // Hijriy tuzatish (kun)
    val hijriAdjust: Int = 0,
    // Ko'rinish
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: Int = 0,
    // Qur'on
    val quranFont: Float = 30f,
    val tajweed: Boolean = true,
    val quranFlow: Boolean = false,
    /** Qur'on ro'yxatidan ochilganda mushaf (sahifa) ko'rinishi. */
    val quranMushaf: Boolean = false,
    val lastSura: Int = 0,
    val lastAyah: Int = 1,
    /** Mushaf rejimida oxirgi ochilgan sahifa (0 — hali ochilmagan). */
    val mushafPage: Int = 0,
    // Bildirishnoma
    val notifyEnabled: Boolean = false,
    val notifyPrayers: Set<Prayer> = setOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA),
    val notifyBefore: Int = 0,
    /** Tanlangan eslatma ovozi (content:// URI), bo'sh — standart. */
    val notifySound: String = "",
    /** Juma kuni ertalab eslatma (Juma namozi, Kahf surasi). */
    val fridayReminder: Boolean = false,
    // Qibla ko'rinishi: 0 — klassik, 1 — islomiy, 2 — zamonaviy
    val qiblaStyle: Int = 1,
    // Tarjima: 0 — yo'q, 1 — lotin, 2 — kirill
    val translation: Int = 0,
    // Qori (Reciters ro'yxatidagi indeks)
    val reciter: Int = 0,
    // Interfeys yozuvi: 0 — lotin, 1 — kirill
    val script: Int = 0,
    // O'qilishi (transliteratsiya): Qur'onda va duolarda
    val quranReading: Boolean = false,
    val duaReading: Boolean = true,
    /** O'qilish yozuvi: 0 — lotin, 1 — kirill (interfeys yozuvidan alohida). */
    val readingScript: Int = 0,
    /** Bosh sahifa foni: 0 — jonli osmon, 1.. — fotosuratlar. */
    val bgIndex: Int = 4,
    /** Qur'on ko'rinishi: 0 — oyat kartalari, 1 — tajvidli mushaf, 2 — fokus. */
    val quranView: Int = 0,
    // Hatm rejasi: boshlangan kun (epochDay), maqsad kuni, boshlang'ich sahifa (0 — reja yo'q)
    val hatmStart: Long = 0,
    val hatmEnd: Long = 0,
    val hatmFromPage: Int = 1,
    /** Fokus rejimida har oyatni takrorlash soni. */
    val repeatCount: Int = 1,
) {
    val zone: java.time.ZoneId
        get() = when {
            tz.isNotBlank() -> runCatching { java.time.ZoneId.of(tz) }.getOrDefault(ZoneId.systemDefault())
            !fromGps -> ZoneId.of("Asia/Tashkent")
            else -> ZoneId.systemDefault()
        }

    val calc get() = CalcSettings(method, hanafi, customFajr, customIsha)

    /** Tanlangan joy vaqt mintaqasining UTC dan farqi (soat). */
    fun tzHours(date: LocalDate): Double = zone.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0

    fun times(date: LocalDate): DayTimes = PrayerCalc.compute(date, lat, lon, tzHours(date), calc, adjust)

    fun toJson(): String = JSONObject().apply {
        put("locName", locName); put("locRegion", locRegion); put("lat", lat); put("lon", lon); put("fromGps", fromGps)
        put("tz", tz); put("country", country); put("autoMethod", autoMethod)
        put("readingScript", readingScript); put("bgIndex2", bgIndex); put("quranView", quranView)
        put("hatmStart", hatmStart); put("hatmEnd", hatmEnd); put("hatmFromPage", hatmFromPage); put("repeatCount", repeatCount)
        put("method", method.name); put("hanafi", hanafi); put("customFajr", customFajr); put("customIsha", customIsha)
        put("adjust", JSONObject().apply {
            adjust.forEach { (p, a) -> if (!a.isDefault) put(p.name, JSONObject().put("o", a.offset).put("f", a.fixed)) }
        })
        put("hijriAdjust", hijriAdjust); put("themeMode", themeMode.name); put("palette", palette)
        put("quranFont", quranFont.toDouble()); put("tajweed", tajweed); put("quranFlow", quranFlow); put("quranMushaf", quranMushaf)
        put("lastSura", lastSura); put("lastAyah", lastAyah); put("mushafPage", mushafPage)
        put("notifyEnabled", notifyEnabled); put("notifyPrayers", notifyPrayers.joinToString(",") { it.name })
        put("notifyBefore", notifyBefore); put("notifySound", notifySound); put("fridayReminder", fridayReminder)
        put("qiblaStyle", qiblaStyle); put("translation", translation); put("reciter", reciter); put("script", script); put("quranReading", quranReading); put("duaReading", duaReading)
    }.toString()

    companion object {
        fun fromJson(s: String?): Settings {
            if (s.isNullOrBlank()) return Settings()
            return try {
                val j = JSONObject(s)
                val d = Settings()
                val adj = mutableMapOf<Prayer, PrayerAdjust>()
                j.optJSONObject("adjust")?.let { a ->
                    for (k in a.keys()) {
                        val p = runCatching { Prayer.valueOf(k) }.getOrNull() ?: continue
                        val o = a.getJSONObject(k)
                        adj[p] = PrayerAdjust(o.optInt("o", 0), o.optInt("f", -1))
                    }
                }
                Settings(
                    locName = j.optString("locName", d.locName),
                    locRegion = j.optString("locRegion", d.locRegion),
                    lat = j.optDouble("lat", d.lat),
                    lon = j.optDouble("lon", d.lon),
                    fromGps = j.optBoolean("fromGps", false),
                    method = runCatching { CalcMethod.valueOf(j.optString("method")) }.getOrDefault(d.method),
                    hanafi = j.optBoolean("hanafi", true),
                    customFajr = j.optDouble("customFajr", d.customFajr),
                    customIsha = j.optDouble("customIsha", d.customIsha),
                    adjust = adj,
                    hijriAdjust = j.optInt("hijriAdjust", 0),
                    themeMode = runCatching { ThemeMode.valueOf(j.optString("themeMode")) }.getOrDefault(ThemeMode.SYSTEM),
                    palette = j.optInt("palette", 0),
                    quranFont = j.optDouble("quranFont", 30.0).toFloat(),
                    tajweed = j.optBoolean("tajweed", true),
                    quranFlow = j.optBoolean("quranFlow", false),
                    quranMushaf = j.optBoolean("quranMushaf", false),
                    lastSura = j.optInt("lastSura", 0),
                    lastAyah = j.optInt("lastAyah", 1),
                    mushafPage = j.optInt("mushafPage", 0),
                    notifyEnabled = j.optBoolean("notifyEnabled", false),
                    notifyPrayers = j.optString("notifyPrayers", "").split(",")
                        .mapNotNull { runCatching { Prayer.valueOf(it) }.getOrNull() }.toSet()
                        .ifEmpty { d.notifyPrayers },
                    notifyBefore = j.optInt("notifyBefore", 0),
                    notifySound = j.optString("notifySound", ""),
                    fridayReminder = j.optBoolean("fridayReminder", false),
                    qiblaStyle = j.optInt("qiblaStyle", 1),
                    translation = j.optInt("translation", 0),
                    reciter = j.optInt("reciter", 0),
                    script = j.optInt("script", 0),
                    quranReading = j.optBoolean("quranReading", false),
                    duaReading = j.optBoolean("duaReading", true),
                    tz = j.optString("tz", ""),
                    country = j.optString("country", "UZ"),
                    autoMethod = j.optBoolean("autoMethod", true),
                    readingScript = j.optInt("readingScript", j.optInt("script", 0)),
                    bgIndex = j.optInt("bgIndex2", 4), // standart: Masjidi Nabaviy — quyosh botishi
                    quranView = j.optInt("quranView", if (j.optBoolean("quranMushaf", false)) 1 else 0),
                    hatmStart = j.optLong("hatmStart", 0),
                    hatmEnd = j.optLong("hatmEnd", 0),
                    hatmFromPage = j.optInt("hatmFromPage", 1),
                    repeatCount = j.optInt("repeatCount", 1),
                )
            } catch (e: Exception) {
                Settings()
            }
        }
    }
}

class SettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hijriy", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(Settings.fromJson(prefs.getString("settings", null)).also { AppTime.zone = it.zone })
    val state: StateFlow<Settings> = _state.asStateFlow()
    val value: Settings get() = _state.value

    fun update(block: (Settings) -> Settings) {
        val n = block(_state.value)
        _state.value = n
        AppTime.zone = n.zone
        prefs.edit().putString("settings", n.toJson()).apply()
    }

    /** Foydalanuvchi joylashuvni o'zi tanlaganmi (yangi o'rnatilganda — yo'q, so'raladi). */
    private val _locChosen = MutableStateFlow(prefs.getBoolean("locChosen", prefs.contains("settings")))
    val locChosen: StateFlow<Boolean> = _locChosen.asStateFlow()
    fun markLocChosen() {
        if (_locChosen.value) return
        _locChosen.value = true
        prefs.edit().putBoolean("locChosen", true).apply()
    }

    fun getString(key: String): String? = prefs.getString(key, null)
    fun putString(key: String, v: String) = prefs.edit().putString(key, v).apply()

    companion object {
        /** Bildirishnoma receiver'lari uchun (ilova ishlamayotganda ham). */
        fun read(context: Context): Settings =
            Settings.fromJson(context.getSharedPreferences("hijriy", Context.MODE_PRIVATE).getString("settings", null))
    }
}

/** Ilova soati: tanlangan shahar vaqt mintaqasida (xorijdagi shahar tanlansa ham to'g'ri hisob). */
object AppTime {
    @Volatile var zone: ZoneId = ZoneId.of("Asia/Tashkent")
    fun now(): java.time.LocalDateTime = java.time.LocalDateTime.now(zone)
    fun today(): LocalDate = LocalDate.now(zone)
}
