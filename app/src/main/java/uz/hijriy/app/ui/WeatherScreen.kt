package uz.hijriy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.WeatherState
import uz.hijriy.app.core.Hijri
import uz.hijriy.app.core.Prayer
import uz.hijriy.app.core.Uz
import uz.hijriy.app.core.fmtMin
import uz.hijriy.app.data.Weather
import uz.hijriy.app.data.WeatherApi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@Composable
fun WeatherScreen(app: HijriyApp, nav: NavHostController) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    val st by app.weather.collectAsStateWithLifecycle()
    LaunchedEffect(s.lat, s.lon) { app.refreshWeather() }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Ob-havo", s.locName, onBack = { nav.popBackStack() }, actions = {
            IconButton(onClick = { app.refreshWeather(force = true) }) { Icon(Icons.Filled.Refresh, "Yangilash") }
        })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 640.dp)) {
                when (val w = st) {
                    is WeatherState.Ready -> WeatherBody(app, w.w, w.stale, w.error)
                    is WeatherState.Failed -> {
                        SectionCard(Modifier.fillMaxWidth()) {
                            Text("🌐", fontSize = 40.sp)
                            Text("Ob-havo ma'lumotini olish uchun internet kerak.", style = MaterialTheme.typography.bodyLarge)
                            VSpace(8.dp)
                            OutlinedButton(onClick = { app.refreshWeather(force = true) }) { Text("Qayta urinish") }
                        }
                        VSpace(12.dp)
                        SunCard(app)
                    }
                    else -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherBody(app: HijriyApp, w: Weather, stale: Boolean, error: String?) {
    val (desc, emo) = WeatherApi.describe(w.code, w.isDay)
    GradientCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${w.temp.roundToInt()}°", color = Color.White, fontSize = 64.sp, style = MaterialTheme.typography.displayLarge)
                Text(desc, color = Color.White, style = MaterialTheme.typography.titleLarge)
                if (!w.feels.isNaN()) Text(
                    "His qilinadi: ${w.feels.roundToInt()}°", color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(emo, fontSize = 72.sp)
        }
        VSpace(14.dp)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Stat("💧 Namlik", "${w.humidity}%")
            Stat("💨 Shamol", "${"%.1f".format(w.wind)} m/s")
            Stat("🧭 Bosim", if (w.pressure.isNaN()) "—" else "${(w.pressure * 0.750062).roundToInt()} mm")
            w.days.firstOrNull()?.let { Stat("🔆 UV", if (it.uv.isNaN()) "—" else "%.0f".format(it.uv)) }
        }
    }
    val updated = Instant.ofEpochMilli(w.fetchedAt).atZone(ZoneId.systemDefault())
    Text(
        "Yangilangan: ${fmtMin(updated.hour * 60 + updated.minute)}" +
            (if (updated.toLocalDate() != LocalDate.now()) ", ${Uz.gregorian(updated.toLocalDate())}" else "") +
            (if (error != null) " • $error" else if (stale) " • saqlangan ma'lumot" else ""),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 6.dp, bottom = 10.dp)
    )
    SunCard(app)
    VSpace(12.dp)
    SectionCard(Modifier.fillMaxWidth(), padding = 10.dp) {
        Text("7 kunlik prognoz", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp))
        val today = LocalDate.now()
        w.days.forEach { d ->
            val (dd, de) = WeatherApi.describe(d.code)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (d.date == today) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1.3f)) {
                    Text(
                        if (d.date == today) "Bugun" else if (d.date == today.plusDays(1)) "Ertaga" else Uz.weekday(d.date.dayOfWeek),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "${d.date.dayOfMonth} ${Uz.MONTHS[d.date.monthValue - 1]} • ${Hijri.fromGregorian(d.date, app.settings.value.hijriAdjust).let { "${it.day} ${it.monthName}" }}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(de, fontSize = 26.sp, modifier = Modifier.padding(horizontal = 6.dp))
                Column(Modifier.weight(1.2f)) {
                    Text(dd, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "💧${if (d.humidity >= 0) "${d.humidity}%" else "—"}  ☔${d.precipProb}%",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "🌅${d.sunrise}  🌇${d.sunset}",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${d.tMax.roundToInt()}° / ${d.tMin.roundToInt()}°",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
    VSpace(8.dp)
    Text(
        "Ma'lumot manbasi: Open-Meteo.com",
        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SunCard(app: HijriyApp) {
    val s = app.settings.value
    val today = LocalDate.now()
    val t = s.times(today)
    val tm = s.times(today.plusDays(1))
    SectionCard(Modifier.fillMaxWidth()) {
        Text("Quyosh", style = MaterialTheme.typography.titleMedium)
        VSpace(8.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("🌅 Chiqishi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmtMin(t.calculated.getValue(Prayer.SUNRISE)), style = MaterialTheme.typography.titleLarge)
            }
            Column {
                Text("🌇 Botishi", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmtMin(Math.round(t.sunsetExact * 60).toInt()), style = MaterialTheme.typography.titleLarge)
            }
            Column {
                Text("☀️ Kun", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Uz.duration(t.dayLengthMinutes), style = MaterialTheme.typography.titleSmall)
            }
        }
        VSpace(6.dp)
        val night = 24 * 60 - t.dayLengthMinutes
        val diff = tm.dayLengthMinutes - t.dayLengthMinutes
        Text(
            "Tun: ${Uz.duration(night)} • Ertaga kun ${if (diff >= 0) "+$diff" else "$diff"} daqiqa",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
