package uz.hijriy.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.WeatherState
import uz.hijriy.app.core.DayTimes
import uz.hijriy.app.core.Hijri
import uz.hijriy.app.core.IslamicDays
import uz.hijriy.app.core.Prayer
import uz.hijriy.app.core.Uz
import uz.hijriy.app.core.fmtMin
import uz.hijriy.app.data.Settings
import uz.hijriy.app.data.WeatherApi
import uz.hijriy.app.ui.theme.QuranFont
import uz.hijriy.app.ui.theme.TitleFont
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

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

// ------------------------------ Osmon fazasi ------------------------------

enum class SkyPhase(val title: String, val colors: List<Color>, val night: Boolean) {
    NIGHT("Tun", listOf(Color(0xFF070B24), Color(0xFF1B1F5E), Color(0xFF2F2A78)), true),
    DAWN("Tong", listOf(Color(0xFF2A2E7A), Color(0xFF8E4FA3), Color(0xFFF2A07B)), true),
    MORNING("Tong otdi", listOf(Color(0xFF1560C9), Color(0xFF3C8FE8), Color(0xFF8CCBF5)), false),
    DAY("Kunduz", listOf(Color(0xFF0B63CE), Color(0xFF2E8BEA), Color(0xFF6FC3F7)), false),
    AFTERNOON("Asr", listOf(Color(0xFFB8451F), Color(0xFFE07A2E), Color(0xFFF4B45A)), false),
    SUNSET("Shom", listOf(Color(0xFF3B1E6B), Color(0xFFB3395F), Color(0xFFF08A4B)), true),
}

fun skyPhase(t: DayTimes, minute: Int): SkyPhase {
    val f = t[Prayer.FAJR]; val sr = t.calculated.getValue(Prayer.SUNRISE); val asr = t[Prayer.ASR]
    val ss = (t.sunsetExact * 60).roundToInt(); val isha = t[Prayer.ISHA]
    return when {
        minute < f || minute >= isha -> SkyPhase.NIGHT
        minute < sr -> SkyPhase.DAWN
        minute < sr + 90 -> SkyPhase.MORNING
        minute < asr -> SkyPhase.DAY
        minute < ss - 25 -> SkyPhase.AFTERNOON
        else -> SkyPhase.SUNSET
    }
}

private val headerText = TextStyle(shadow = Shadow(Color(0x66000000), Offset(0f, 2f), 6f))

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
    val minute = now.hour * 60 + now.minute
    val phase = skyPhase(times, minute)

    LaunchedEffect(s.lat, s.lon) { app.refreshWeather() }
    val view = androidx.compose.ui.platform.LocalView.current
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    androidx.compose.runtime.DisposableEffect(Unit) {
        val c = activity?.window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        val prev = c?.isAppearanceLightStatusBars
        c?.isAppearanceLightStatusBars = false
        onDispose { if (prev != null) c?.isAppearanceLightStatusBars = prev }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SkyHeader(phase, times, minute, now) {
            Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 10.dp)) {
                // Joylashuv va sana
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.16f))
                            .clickable { nav.go("location") }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.LocationOn, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        HSpace(4.dp)
                        Text(s.locName, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.weight(1f))
                    Text(phase.title, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge.merge(headerText))
                }
                VSpace(16.dp)
                Text(
                    "${hijri.day} ${hijri.monthName} ${hijri.year}",
                    color = Color.White, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 30.sp, style = headerText
                )
                Text(
                    "${Uz.arabicNumber(hijri.day)} ${hijri.monthNameAr} ${Uz.arabicNumber(hijri.year)} هـ",
                    color = Color(0xFFFFE6A3), fontFamily = QuranFont, fontSize = 20.sp, style = headerText
                )
                Text(Uz.gregorianFull(today), color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodyMedium.merge(headerText))
                IslamicDays.find(hijri)?.let {
                    VSpace(6.dp)
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(Color(0x33FFE6A3)).padding(horizontal = 12.dp, vertical = 4.dp)
                    ) { Text("✨ ${it.name}", color = Color.White, style = MaterialTheme.typography.labelLarge) }
                }
                VSpace(18.dp)
                NextPrayerGlass(pn, left)
                VSpace(12.dp)
                NameTicker(now.toEpochSecond(ZoneOffset.UTC)) { nav.go("names") }
                VSpace(58.dp)   // masjid silueti uchun joy
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            if (hijri.month == 9) {
                VSpace(14.dp)
                RamadanCard(hijri.day, times[Prayer.FAJR], times[Prayer.MAGHRIB], now)
            }
            VSpace(14.dp)
            PrayerStrip(times, pn) { nav.go("prayer") }
            VSpace(14.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeatherMini(weather, Modifier.weight(1f)) { nav.go("weather") }
                SunMini(times, Modifier.weight(1f))
            }
            VSpace(20.dp)
            Text("Bo'limlar", fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            VSpace(10.dp)
            val tiles = listOf(
                HomeTile("Qibla", Icons.Filled.Explore, "qibla", Color(0xFF00897B), Color(0xFF26C6DA)),
                HomeTile("Qur'on", Icons.AutoMirrored.Filled.MenuBook, "quran", Color(0xFF0F7B5F), Color(0xFF43C59E)),
                HomeTile("Mushaf", Icons.Filled.AutoStories, "mushaf/${if (s.mushafPage > 0) s.mushafPage else 1}", Color(0xFF8A5A12), Color(0xFFE0A93B)),
                HomeTile("Tasbeh", Icons.Filled.Fingerprint, "tasbeh", Color(0xFF6A1B9A), Color(0xFFAB47BC)),
                HomeTile("99 ism", Icons.Filled.AutoAwesome, "names", Color(0xFF283593), Color(0xFF5C6BC0)),
                HomeTile("Duolar", Icons.Filled.VolunteerActivism, "duas", Color(0xFF2E7D32), Color(0xFF81C784)),
                HomeTile("Rasm", Icons.Filled.Image, "wallpaper", Color(0xFFC2185B), Color(0xFFF06292)),
                HomeTile("Qazo", Icons.Filled.Checklist, "qazo", Color(0xFF455A64), Color(0xFF90A4AE)),
                HomeTile("Konvertor", Icons.Filled.SwapHoriz, "converter", Color(0xFF1565C0), Color(0xFF42A5F5)),
                HomeTile("Taqvim", Icons.Filled.CalendarMonth, "calendar", Color(0xFFD84315), Color(0xFFFF8A65)),
                HomeTile("Ob-havo", Icons.Filled.WbSunny, "weather", Color(0xFF0277BD), Color(0xFF4FC3F7)),
                HomeTile("Sozlamalar", Icons.Filled.Settings, "settings", Color(0xFF546E7A), Color(0xFF78909C)),
            )
            tiles.chunked(3).forEachIndexed { r, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                    row.forEachIndexed { c, t ->
                        AnimatedTile(t, r * 3 + c, Modifier.weight(1f)) {
                            if (t.route == "quran" || t.route == "calendar") {
                                nav.navigate(t.route) { launchSingleTop = true; restoreState = true }
                            } else nav.go(t.route)
                        }
                    }
                }
            }
            if (s.lastSura > 0) {
                val q by app.quran.collectAsStateWithLifecycle()
                q?.suras?.getOrNull(s.lastSura - 1)?.let { sura ->
                    ContinueCard(
                        "${sura.number}. ${sura.uzName} surasi, ${s.lastAyah}-oyat" + if (s.quranMushaf && s.mushafPage > 0) " • ${s.mushafPage}-sahifa" else "",
                        sura.arName
                    ) {
                        if (s.quranMushaf && s.mushafPage > 0) nav.go("mushaf/${s.mushafPage}")
                        else nav.go("reader/${sura.number}?ayah=${s.lastAyah}")
                    }
                    VSpace(12.dp)
                }
            }
            VSpace(12.dp)
        }
    }
}

// ------------------------------ Osmonli sarlavha ------------------------------

@Composable
private fun SkyHeader(phase: SkyPhase, t: DayTimes, minute: Int, now: LocalDateTime, content: @Composable () -> Unit) {
    val c0 by animateColorAsState(phase.colors[0], tween(1500), label = "c0")
    val c1 by animateColorAsState(phase.colors[1], tween(1500), label = "c1")
    val c2 by animateColorAsState(phase.colors[2], tween(1500), label = "c2")
    val inf = rememberInfiniteTransition(label = "sky")
    val tw by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "tw")
    val glow by inf.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val drift by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(90000, easing = LinearEasing)), label = "drift")
    val stars = remember { StarField(60, 5) }
    val sunrise = t.calculated.getValue(Prayer.SUNRISE).toFloat()
    val sunset = (t.sunsetExact * 60).toFloat()
    val sec = minute + now.second / 60f
    val isDay = sec in sunrise..sunset
    val frac = if (isDay) (sec - sunrise) / (sunset - sunrise)
    else {
        val night = 1440f - (sunset - sunrise)
        val since = if (sec > sunset) sec - sunset else sec + 1440f - sunset
        (since / night).coerceIn(0f, 1f)
    }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.verticalGradient(listOf(c0, c1, c2)))
            // nozik islomiy naqsh
            drawStarLattice(Color.White.copy(alpha = 0.05f), size.width / 5.5f, 1.2f, drift, Offset(size.width * 0.8f, size.height * 0.2f))
            if (phase.night) with(stars) { draw(Color.White.copy(alpha = 0.85f), tw, 0.7f, 1.7f) }
            // quyosh / oy yoyi
            val l = size.width * 0.08f; val r = size.width * 0.92f
            val base = size.height - 58.dp.toPx(); val h = size.height * 0.34f
            val x = l + (r - l) * frac
            val y = base - h * sin(PI * frac).toFloat()
            drawArc(
                Color.White.copy(alpha = 0.18f), 180f, 180f, false,
                topLeft = Offset(l, base - h), size = androidx.compose.ui.geometry.Size(r - l, h * 2),
                style = Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            )
            val bodyR = 16.dp.toPx()
            if (isDay) {
                drawCircle(Brush.radialGradient(listOf(Color(0x99FFF3B0), Color.Transparent), Offset(x, y), bodyR * 3.2f * glow), bodyR * 3.2f * glow, Offset(x, y))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF8D6), Color(0xFFFFC93C)), Offset(x, y), bodyR), bodyR, Offset(x, y))
            } else {
                drawCircle(Brush.radialGradient(listOf(Color(0x66E8EEFF), Color.Transparent), Offset(x, y), bodyR * 2.8f * glow), bodyR * 2.8f * glow, Offset(x, y))
                drawPath(crescentPath(x, y, bodyR), Color(0xFFFFF4CF))
            }
            // masjid silueti
            val mw = size.width * 0.9f
            drawPath(mosquePath(mw, size.height + 2f, 64.dp.toPx(), (size.width - mw) / 2), Color.Black.copy(alpha = 0.28f))
            drawRect(Color.Black.copy(alpha = 0.28f), Offset(0f, size.height - 10.dp.toPx()), androidx.compose.ui.geometry.Size(size.width, 12.dp.toPx()))
        }
        content()
    }
}

@Composable
private fun NextPrayerGlass(pn: PrayerNow, left: Long) {
    val inf = rememberInfiniteTransition(label = "np")
    val pulse by inf.animateFloat(0.6f, 1f, infiniteRepeatable(tween(1200), RepeatMode.Reverse), label = "pulse")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).graphicsLayer { alpha = pulse }.clip(CircleShape).background(Color(0xFF7CFFB2)))
            HSpace(8.dp)
            Text("Keyingi namoz: ${pn.next.uz}", color = Color.White, style = MaterialTheme.typography.titleMedium.merge(headerText))
            Box(Modifier.weight(1f))
            Text(fmtMin(pn.nextAt.hour * 60 + pn.nextAt.minute), color = Color.White, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        Text(
            Uz.countdown(left), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 44.sp,
            letterSpacing = 1.sp, style = headerText
        )
        if (pn.prevAt != null) {
            val total = Duration.between(pn.prevAt, pn.nextAt).seconds.coerceAtLeast(1)
            val done by animateFloatAsState(((total - left).toFloat() / total).coerceIn(0f, 1f), tween(800), label = "done")
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.2f))) {
                Box(
                    Modifier.fillMaxWidth(done).height(6.dp).clip(RoundedCornerShape(3.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFFE6A3), Color(0xFFFFC93C))))
                )
            }
        }
    }
}

// ------------------------------ Namozlar qatori ------------------------------

private val prayerIcons = mapOf(
    Prayer.FAJR to "🌄", Prayer.SUNRISE to "🌅", Prayer.DHUHR to "☀️",
    Prayer.ASR to "🌤️", Prayer.MAGHRIB to "🌇", Prayer.ISHA to "🌙"
)

@Composable
private fun PrayerStrip(times: DayTimes, pn: PrayerNow, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "strip")
    val glow by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "g")
    val primary = MaterialTheme.colorScheme.primary
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Prayer.entries.forEach { p ->
            val active = p == pn.current
            val next = p == pn.next
            Column(
                Modifier
                    .width(86.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (active) Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.75f)))
                        else Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainer))
                    )
                    .border(
                        if (next) 2.dp else 0.dp,
                        if (next) Color(0xFFFFC93C).copy(alpha = 0.5f + 0.5f * glow) else Color.Transparent,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable(onClick = onClick)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val fg = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                Text(prayerIcons.getValue(p), fontSize = 22.sp)
                Text(p.uz, color = fg.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                Text(fmtMin(times[p]), color = fg, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (times.isAdjusted(p)) Text("•", color = if (active) fg else MaterialTheme.colorScheme.secondary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun WeatherMini(weather: WeatherState, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1E88E5), Color(0xFF4FC3F7))))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        when (weather) {
            is WeatherState.Ready -> {
                val (desc, emo) = WeatherApi.describe(weather.w.code, weather.w.isDay)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emo, fontSize = 30.sp)
                    HSpace(8.dp)
                    Text("${weather.w.temp.roundToInt()}°", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
                Text(desc, color = Color.White, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "💧 ${weather.w.humidity}%  💨 ${"%.1f".format(weather.w.wind)} m/s",
                    color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall
                )
            }
            is WeatherState.Failed -> {
                Text("🌐", fontSize = 30.sp)
                Text("Ob-havo", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("Internet kerak", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
            }
            else -> {
                Text("🌤️", fontSize = 30.sp)
                Text("Yuklanmoqda…", color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SunMini(times: DayTimes, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF57C00), Color(0xFFFFB74D))))
            .padding(16.dp)
    ) {
        Text("🌅  ${fmtMin(times.calculated.getValue(Prayer.SUNRISE))}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        VSpace(4.dp)
        Text("🌇  ${fmtMin((times.sunsetExact * 60).roundToInt())}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        VSpace(4.dp)
        Text("Kun: ${Uz.duration(times.dayLengthMinutes)}", color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodySmall)
    }
}

// ------------------------------ Plitkalar ------------------------------

private data class HomeTile(val label: String, val icon: ImageVector, val route: String, val c1: Color, val c2: Color)

@Composable
private fun AnimatedTile(t: HomeTile, index: Int, modifier: Modifier, onClick: () -> Unit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(450, delayMillis = 60 * index, easing = FastOutSlowInEasing)) }
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f), label = "press")
    Column(
        modifier
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 40.dp.toPx()
                scaleX = scale * (0.85f + 0.15f * appear.value); scaleY = scaleX
            }
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(src, null, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(t.c1, t.c2))),
            contentAlignment = Alignment.Center
        ) { Icon(t.icon, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        VSpace(8.dp)
        Text(t.label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun ContinueCard(title: String, arName: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF0B5D48), Color(0xFF16946F))))
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("📖 Davom ettirish", color = Color(0xFFFFE6A3), style = MaterialTheme.typography.labelLarge)
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        Text(arName, fontFamily = QuranFont, fontSize = 26.sp, color = Color.White)
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
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF311B92), Color(0xFF6A1B9A), Color(0xFFAD1457))))
            .padding(16.dp)
    ) {
        Text("🌙 Ramazon muborak! $day-kun", color = Color.White, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        VSpace(8.dp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Saharlik", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                Text(fmtMin(fajr), color = Color.White, style = MaterialTheme.typography.headlineSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                Text(Uz.countdown(left), color = Color(0xFFFFE6A3), style = MaterialTheme.typography.titleLarge)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Iftorlik", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                Text(fmtMin(maghrib), color = Color.White, style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}
