package uz.hijriy.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.WeatherState
import uz.hijriy.app.core.Hijri
import uz.hijriy.app.core.IslamicDays
import uz.hijriy.app.core.Prayer
import uz.hijriy.app.core.Uz
import uz.hijriy.app.core.fmtMin
import uz.hijriy.app.data.Settings
import uz.hijriy.app.data.WeatherApi
import uz.hijriy.app.ui.theme.LocalExtra
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.roundToInt

data class PrayerNow(val current: Prayer?, val next: Prayer, val nextAt: LocalDateTime, val prevAt: LocalDateTime?)

/** Hozirgi va keyingi namozni aniqlaydi (Quyosh — bomdod vaqti tugashi sifatida). */
fun prayerNow(s: Settings, now: LocalDateTime): PrayerNow {
    val date = now.toLocalDate()
    val t = s.times(date)
    val minuteNow = now.hour * 60 + now.minute
    val order = Prayer.entries
    var current: Prayer? = null
    var prevAt: LocalDateTime? = null
    for (p in order) if (t[p] <= minuteNow) {
        current = p; prevAt = date.atStartOfDay().plusMinutes(t[p].toLong())
    }
    val nextP = order.firstOrNull { t[it] > minuteNow }
    return if (nextP != null) {
        PrayerNow(current, nextP, date.atStartOfDay().plusMinutes(t[nextP].toLong()), prevAt)
    } else {
        val tm = s.times(date.plusDays(1))
        PrayerNow(current, Prayer.FAJR, date.plusDays(1).atStartOfDay().plusMinutes(tm[Prayer.FAJR].toLong()), prevAt)
    }
}

@Composable
fun HomeScreen(app: HijriyApp, nav: NavHostController) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    val weather by app.weather.collectAsStateWithLifecycle()
    val now by rememberNow()
    val today = now.toLocalDate()
    val times = remember(s, today) { s.times(today) }
    val hijri = remember(s.hijriAdjust, today) { Hijri.fromGregorian(today, s.hijriAdjust) }
    val pn = prayerNow(s, now)
    val left = Duration.between(now, pn.nextAt).seconds
    val onHeader = Color.White

    LaunchedEffect(s.lat, s.lon) { app.refreshWeather() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        VSpace(8.dp)
        GradientCard(Modifier.fillMaxWidth()) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { nav.go("location") }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.LocationOn, null, tint = onHeader.copy(alpha = 0.9f), modifier = Modifier.size(18.dp))
                HSpace(6.dp)
                Text(
                    s.locName + if (s.locRegion.isNotBlank() && s.locRegion != s.locName) ", ${s.locRegion}" else "",
                    color = onHeader.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            VSpace(10.dp)
            Text(Uz.gregorianFull(today), color = onHeader.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
            Text(
                "${hijri.day} ${hijri.monthName} ${hijri.year} h.",
                color = onHeader, style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "${Uz.arabicNumber(hijri.day)} ${hijri.monthNameAr} ${Uz.arabicNumber(hijri.year)}",
                color = onHeader.copy(alpha = 0.85f),
                style = uz.hijriy.app.ui.theme.ArabicTitle.copy(fontSize = 20.sp)
            )
            IslamicDays.find(hijri)?.let {
                VSpace(6.dp)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) { Text("✨ ${it.name}", color = onHeader, style = MaterialTheme.typography.labelLarge) }
            }
            VSpace(18.dp)
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Keyingi: ${pn.next.uz}", color = onHeader.copy(alpha = 0.85f), style = MaterialTheme.typography.titleMedium)
                    Text(
                        Uz.countdown(left), color = onHeader,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, fontSize = 40.sp)
                    )
                }
                Text(
                    fmtMin(pn.nextAt.hour * 60 + pn.nextAt.minute), color = onHeader,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            if (pn.prevAt != null) {
                val total = Duration.between(pn.prevAt, pn.nextAt).seconds.coerceAtLeast(1)
                val done = (total - left).toFloat() / total
                VSpace(10.dp)
                LinearProgressIndicator(
                    progress = { done.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = LocalExtra.current.palette.accent,
                    trackColor = Color.White.copy(alpha = 0.18f),
                )
            }
            VSpace(14.dp)
            NameTicker(now.toEpochSecond(java.time.ZoneOffset.UTC)) { nav.go("names") }
        }

        // Ramazon oyida: saharlik va iftorlik
        if (hijri.month == 9) {
            VSpace(12.dp)
            RamadanCard(hijri.day, times[Prayer.FAJR], times[Prayer.MAGHRIB], now)
        }

        VSpace(14.dp)
        // Bugungi namoz vaqtlari — ixcham
        SectionCard(onClick = { nav.go("prayer") }, padding = 12.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Prayer.entries.forEach { p ->
                    val active = p == pn.current
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val c = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        Text(p.uz, style = MaterialTheme.typography.labelMedium, color = c.copy(alpha = 0.8f), maxLines = 1)
                        VSpace(2.dp)
                        Text(fmtMin(times[p]), style = MaterialTheme.typography.titleSmall, color = c)
                        if (times.isAdjusted(p)) Text("•", color = if (active) c else MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
                    }
                }
            }
        }

        VSpace(12.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Ob-havo
            SectionCard(Modifier.weight(1f), onClick = { nav.go("weather") }) {
                when (val w = weather) {
                    is WeatherState.Ready -> {
                        val (desc, emo) = WeatherApi.describe(w.w.code, w.w.isDay)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emo, fontSize = 30.sp)
                            HSpace(8.dp)
                            Text("${w.w.temp.roundToInt()}°", style = MaterialTheme.typography.headlineMedium)
                        }
                        Text(desc, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Namlik ${w.w.humidity}% • ${"%.1f".format(w.w.wind)} m/s",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    WeatherState.Loading, WeatherState.Idle -> {
                        Text("🌤️", fontSize = 30.sp)
                        Text("Ob-havo yuklanmoqda…", style = MaterialTheme.typography.bodyMedium)
                    }
                    is WeatherState.Failed -> {
                        Text("🌐", fontSize = 30.sp)
                        Text("Ob-havo", style = MaterialTheme.typography.titleMedium)
                        Text("Internet kerak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // Quyosh
            SectionCard(Modifier.weight(1f)) {
                Text("🌅  ${fmtMin(times.calculated.getValue(Prayer.SUNRISE))}", style = MaterialTheme.typography.titleMedium)
                VSpace(4.dp)
                Text("🌇  ${fmtMin(Math.round(times.sunsetExact * 60).toInt())}", style = MaterialTheme.typography.titleMedium)
                VSpace(4.dp)
                Text(
                    "Kun: ${Uz.duration(times.dayLengthMinutes)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        VSpace(12.dp)
        val tiles = listOf(
            Triple("Qibla", Icons.Filled.Explore, "qibla"),
            Triple("Qur'on", Icons.AutoMirrored.Filled.MenuBook, "quran"),
            Triple("Mushaf", Icons.Filled.AutoStories, "mushaf/${if (s.mushafPage > 0) s.mushafPage else 1}"),
            Triple("Tasbeh", Icons.Filled.Fingerprint, "tasbeh"),
            Triple("99 ism", Icons.Filled.AutoAwesome, "names"),
            Triple("Konvertor", Icons.Filled.SwapHoriz, "converter"),
            Triple("Taqvim", Icons.Filled.CalendarMonth, "calendar"),
            Triple("Ob-havo", Icons.Filled.WbSunny, "weather"),
            Triple("Sozlamalar", Icons.Filled.Settings, "settings"),
        )
        tiles.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                row.forEach { (label, icon, route) ->
                    Tile(label, icon, Modifier.weight(1f)) {
                        if (route == "quran" || route == "calendar") {
                            nav.navigate(route) { launchSingleTop = true; restoreState = true }
                        } else nav.go(route)
                    }
                }
            }
        }
        if (s.lastSura > 0) {
            val q by app.quran.collectAsStateWithLifecycle()
            q?.suras?.getOrNull(s.lastSura - 1)?.let { sura ->
                SectionCard(Modifier.fillMaxWidth(), onClick = {
                    if (s.quranMushaf && s.mushafPage > 0) nav.go("mushaf/${s.mushafPage}")
                    else nav.go("reader/${sura.number}?ayah=${s.lastAyah}")
                }) {
                    Text("Davom ettirish", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("${sura.number}. ${sura.uzName} surasi, ${s.lastAyah}-oyat", style = MaterialTheme.typography.titleMedium)
                }
                VSpace(12.dp)
            }
        }
        VSpace(8.dp)
    }
}

@Composable
private fun Tile(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    SectionCard(modifier, onClick = onClick, padding = 12.dp) {
        Column(Modifier.fillMaxWidth().widthIn(min = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(icon, size = 44.dp)
            VSpace(8.dp)
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun RamadanCard(day: Int, fajr: Int, maghrib: Int, now: LocalDateTime) {
    val minuteNow = now.hour * 60 + now.minute
    val (label, target) = when {
        minuteNow < fajr -> "Saharlikkacha" to fajr
        minuteNow < maghrib -> "Iftorgacha" to maghrib
        else -> "Ertangi saharlikkacha" to fajr + 24 * 60
    }
    val left = (target - minuteNow) * 60L - now.second
    SectionCard(Modifier.fillMaxWidth()) {
        Text("🌙 Ramazon muborak! $day-kun", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        VSpace(8.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Saharlik (og'iz yopish)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmtMin(fajr), style = MaterialTheme.typography.headlineSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Uz.countdown(left), style = MaterialTheme.typography.titleMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Iftorlik (og'iz ochish)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(fmtMin(maghrib), style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
