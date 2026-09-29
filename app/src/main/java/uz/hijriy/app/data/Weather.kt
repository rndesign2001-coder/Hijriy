package uz.hijriy.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.util.Locale

data class DailyWeather(
    val date: LocalDate,
    val code: Int,
    val tMax: Double,
    val tMin: Double,
    val precipProb: Int,
    val humidity: Int,
    val windMax: Double,
    val uv: Double,
    val sunrise: String,
    val sunset: String,
)

data class Weather(
    val temp: Double,
    val feels: Double,
    val humidity: Int,
    val wind: Double,
    val pressure: Double,
    val code: Int,
    val isDay: Boolean,
    val days: List<DailyWeather>,
    val fetchedAt: Long,
    val lat: Double,
    val lon: Double,
)

object WeatherApi {

    fun url(lat: Double, lon: Double): String = String.format(
        Locale.US,
        "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,pressure_msl,wind_speed_10m" +
            "&hourly=relative_humidity_2m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max,wind_speed_10m_max" +
            "&timezone=auto&forecast_days=7&wind_speed_unit=ms",
        lat, lon
    )

    suspend fun fetch(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        val c = URL(url(lat, lon)).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 15000
        c.setRequestProperty("User-Agent", "HijriyTaqvim/1.0 (Android)")
        try {
            if (c.responseCode != 200) throw RuntimeException("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    fun parse(json: String, fetchedAt: Long, lat: Double, lon: Double): Weather {
        val j = JSONObject(json)
        val cur = j.getJSONObject("current")
        val d = j.getJSONObject("daily")
        val times = d.getJSONArray("time")
        // Kunlik o'rtacha namlik — soatlik ma'lumotdan
        val humByDay = HashMap<String, MutableList<Int>>()
        j.optJSONObject("hourly")?.let { h ->
            val ht = h.getJSONArray("time")
            val hv = h.getJSONArray("relative_humidity_2m")
            for (i in 0 until ht.length()) {
                if (hv.isNull(i)) continue
                humByDay.getOrPut(ht.getString(i).take(10)) { mutableListOf() }.add(hv.getInt(i))
            }
        }
        fun dbl(name: String, i: Int) = d.optJSONArray(name)?.let { if (it.isNull(i)) Double.NaN else it.getDouble(i) } ?: Double.NaN
        val days = (0 until times.length()).map { i ->
            val ds = times.getString(i)
            DailyWeather(
                date = LocalDate.parse(ds),
                code = d.getJSONArray("weather_code").optInt(i, 0),
                tMax = dbl("temperature_2m_max", i),
                tMin = dbl("temperature_2m_min", i),
                precipProb = d.optJSONArray("precipitation_probability_max")?.optInt(i, 0) ?: 0,
                humidity = humByDay[ds]?.let { l -> l.sum() / l.size } ?: -1,
                windMax = dbl("wind_speed_10m_max", i),
                uv = dbl("uv_index_max", i),
                sunrise = d.getJSONArray("sunrise").getString(i).substringAfter('T'),
                sunset = d.getJSONArray("sunset").getString(i).substringAfter('T'),
            )
        }
        return Weather(
            temp = cur.getDouble("temperature_2m"),
            feels = cur.optDouble("apparent_temperature", Double.NaN),
            humidity = cur.optInt("relative_humidity_2m", -1),
            wind = cur.optDouble("wind_speed_10m", Double.NaN),
            pressure = cur.optDouble("pressure_msl", Double.NaN),
            code = cur.optInt("weather_code", 0),
            isDay = cur.optInt("is_day", 1) == 1,
            days = days,
            fetchedAt = fetchedAt,
            lat = lat,
            lon = lon,
        )
    }

    /** WMO ob-havo kodlari — o'zbekcha tavsif va belgi. */
    fun describe(code: Int, isDay: Boolean = true): Pair<String, String> = when (code) {
        0 -> "Ochiq osmon" to (if (isDay) "☀️" else "🌙")
        1 -> "Asosan ochiq" to (if (isDay) "🌤️" else "🌙")
        2 -> "Qisman bulutli" to "⛅"
        3 -> "Bulutli" to "☁️"
        45, 48 -> "Tuman" to "🌫️"
        51, 53, 55 -> "Mayda yomg'ir" to "🌦️"
        56, 57 -> "Muzlagan shivalama" to "🌧️"
        61 -> "Yengil yomg'ir" to "🌦️"
        63 -> "Yomg'ir" to "🌧️"
        65 -> "Kuchli yomg'ir" to "🌧️"
        66, 67 -> "Muzli yomg'ir" to "🌧️"
        71 -> "Yengil qor" to "🌨️"
        73 -> "Qor" to "🌨️"
        75 -> "Kuchli qor" to "❄️"
        77 -> "Qor donachalari" to "🌨️"
        80, 81 -> "Jala" to "🌦️"
        82 -> "Kuchli jala" to "⛈️"
        85, 86 -> "Qor aralash jala" to "🌨️"
        95 -> "Momaqaldiroq" to "⛈️"
        96, 99 -> "Do'l bilan momaqaldiroq" to "⛈️"
        else -> "Noma'lum" to "🌡️"
    }
}
