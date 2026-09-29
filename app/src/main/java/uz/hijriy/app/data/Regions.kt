package uz.hijriy.app.data

import android.content.Context
import org.json.JSONArray
import kotlin.math.cos
import kotlin.math.hypot

data class District(val name: String, val cyr: String, val lat: Double, val lon: Double, val region: String)
data class Region(val name: String, val cyr: String, val districts: List<District>)

object Regions {
    @Volatile
    private var cache: List<Region>? = null

    fun all(context: Context): List<Region> {
        cache?.let { return it }
        val txt = context.assets.open("regions.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(txt)
        val list = (0 until arr.length()).map { i ->
            val r = arr.getJSONObject(i)
            val rn = r.getString("n")
            val ds = r.getJSONArray("d")
            Region(rn, r.getString("c"), (0 until ds.length()).map { k ->
                val d = ds.getJSONObject(k)
                District(d.getString("n"), d.getString("c"), d.getDouble("la"), d.getDouble("lo"), rn)
            })
        }
        cache = list
        return list
    }

    /** Eng yaqin tuman (km bilan). */
    fun nearest(context: Context, lat: Double, lon: Double): Pair<District, Double>? {
        var best: District? = null
        var bestD = Double.MAX_VALUE
        for (r in all(context)) for (d in r.districts) {
            val km = 111.2 * hypot(d.lat - lat, (d.lon - lon) * cos(Math.toRadians(lat)))
            if (km < bestD) {
                bestD = km; best = d
            }
        }
        return best?.let { it to bestD }
    }
}
