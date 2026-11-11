package uz.hijriy.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Dunyodagi shahar: nomi, davlati, koordinatasi va vaqt mintaqasi. */
data class City(
    val name: String,
    val country: String,
    val cc: String,
    val lat: Double,
    val lon: Double,
    val tz: String,
    val admin: String = "",
)

/** Shahar qidirish — Open-Meteo Geocoding (GeoNames asosida, bepul, kalitsiz). */
object Geo {
    /** Internetsiz ham tanlash mumkin bo'lgan mashhur shaharlar. */
    val popular = listOf(
        City("Makka", "Saudiya Arabistoni", "SA", 21.4225, 39.8262, "Asia/Riyadh"),
        City("Madina", "Saudiya Arabistoni", "SA", 24.4672, 39.6111, "Asia/Riyadh"),
        City("Istanbul", "Turkiya", "TR", 41.0082, 28.9784, "Europe/Istanbul"),
        City("Moskva", "Rossiya", "RU", 55.7558, 37.6173, "Europe/Moscow"),
        City("Sankt-Peterburg", "Rossiya", "RU", 59.9343, 30.3351, "Europe/Moscow"),
        City("Qozon", "Rossiya", "RU", 55.7963, 49.1088, "Europe/Moscow"),
        City("Dubay", "BAA", "AE", 25.2048, 55.2708, "Asia/Dubai"),
        City("Almati", "Qozog'iston", "KZ", 43.2389, 76.8897, "Asia/Almaty"),
        City("Bishkek", "Qirg'iziston", "KG", 42.8746, 74.5698, "Asia/Bishkek"),
        City("O'sh", "Qirg'iziston", "KG", 40.5283, 72.7985, "Asia/Bishkek"),
        City("Dushanbe", "Tojikiston", "TJ", 38.5598, 68.7870, "Asia/Dushanbe"),
        City("Seul", "Janubiy Koreya", "KR", 37.5665, 126.9780, "Asia/Seoul"),
        City("London", "Buyuk Britaniya", "GB", 51.5074, -0.1278, "Europe/London"),
        City("Berlin", "Germaniya", "DE", 52.5200, 13.4050, "Europe/Berlin"),
        City("Nyu-York", "AQSh", "US", 40.7128, -74.0060, "America/New_York"),
        City("Qohira", "Misr", "EG", 30.0444, 31.2357, "Africa/Cairo"),
    )

    suspend fun search(query: String, lang: String = "ru"): List<City> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val url = URL("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=15&language=$lang&format=json")
        val c = url.openConnection() as HttpURLConnection
        c.connectTimeout = 12_000; c.readTimeout = 15_000
        try {
            val txt = c.inputStream.bufferedReader().use { it.readText() }
            parse(txt)
        } finally { c.disconnect() }
    }

    fun parse(json: String): List<City> {
        val arr = JSONObject(json).optJSONArray("results") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            City(
                name = o.optString("name"),
                country = o.optString("country"),
                cc = o.optString("country_code"),
                lat = o.optDouble("latitude"),
                lon = o.optDouble("longitude"),
                tz = o.optString("timezone"),
                admin = o.optString("admin1"),
            )
        }.filter { it.tz.isNotBlank() && !it.lat.isNaN() }
    }
}
