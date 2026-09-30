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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import uz.hijriy.app.core.Qibla
import uz.hijriy.app.ui.theme.LocalExtra
import kotlin.math.cos

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

private val headerText = TextStyle(shadow = Shadow(Color(0x55000000), Offset(0f, 2f), 8f))
private val Gold = Color(0xFFE9C46A)
private val GoldDeep = Color(0xFFC8962E)
private val HeroOverlap = 40.dp

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
    val prevAt = pn.prevAt ?: today.minusDays(1).atStartOfDay()
        .plusMinutes(remember(s, today) { s.times(today.minusDays(1)) }[Prayer.ISHA].toLong())
    val total = Duration.between(prevAt, pn.nextAt).seconds.coerceAtLeast(1)
    val progress = ((total - left).toFloat() / total).coerceIn(0f, 1f)

    LaunchedEffect(s.lat, s.lon) { app.refreshWeather() }
    val view = androidx.compose.ui.platform.LocalView.current
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    androidx.compose.runtime.DisposableEffect(Unit) {
        val c = activity?.window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        val prev = c?.isAppearanceLightStatusBars
        c?.isAppearanceLightStatusBars = false
        onDispose { if (prev != null) c?.isAppearanceLightStatusBars = prev }
    }
    val go: (String) -> Unit = { r ->
        if (r == "quran" || r == "calendar") nav.navigate(r) { launchSingleTop = true; restoreState = true } else nav.go(r)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SkyHeader(phase, times, minute, now) {
            Column(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp).padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- yuqori qator ---
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Assalomu alaykum", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium.merge(headerText))
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).clickable { nav.go("location") }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.LocationOn, null, tint = Gold, modifier = Modifier.size(18.dp))
                            HSpace(3.dp)
                            Text(
                                s.locName, color = Color.White, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, style = headerText, modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                        }
                    }
                    GlassIcon(Icons.Filled.Explore, "Qibla") { nav.go("qibla") }
                    HSpace(8.dp)
                    GlassIcon(Icons.Filled.Settings, "Sozlamalar") { nav.go("settings") }
                }
                VSpace(18.dp)
                // --- sana ---
                Text(
                    "${hijri.day} ${hijri.monthName} ${hijri.year}",
                    color = Color.White, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 27.sp, style = headerText
                )
                Text(
                    "${Uz.arabicNumber(hijri.day)} ${hijri.monthNameAr} ${Uz.arabicNumber(hijri.year)} هـ",
                    color = Gold, fontFamily = QuranFont, fontSize = 19.sp, style = headerText
                )
                Text(Uz.gregorianFull(today), color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium.merge(headerText))
                IslamicDays.find(hijri)?.let {
                    VSpace(8.dp)
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Gold.copy(alpha = 0.22f))
                            .border(1.dp, Gold.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Gold, modifier = Modifier.size(14.dp))
                        HSpace(6.dp)
                        Text(it.name, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
                VSpace(14.dp)
                CountdownRing(pn, left, progress) { nav.go("prayer") }
                VSpace(10.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroChip("Hozir: ${(pn.current ?: Prayer.ISHA).uz}", Color(0xFF7CFFB2))
                    HeroChip(phase.title, Gold)
                }
                VSpace(HeroOverlap + 92.dp)   // quyosh yo'li va masjid silueti uchun joy
            }
        }

        Column(Modifier.padding(horizontal = 16.dp).offset(y = -HeroOverlap)) {
            PrayerTimeline(times, pn, now) { nav.go("prayer") }
            if (hijri.month == 9) {
                VSpace(14.dp)
                RamadanCard(hijri.day, times[Prayer.FAJR], times[Prayer.MAGHRIB], now)
            }
            VSpace(14.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                WeatherCard(weather, times, Modifier.weight(1f).fillMaxHeight()) { nav.go("weather") }
                QiblaCard(Qibla.bearing(s.lat, s.lon), Modifier.weight(1f).fillMaxHeight()) { nav.go("qibla") }
            }
            VSpace(14.dp)
            NamesCard(now.toEpochSecond(ZoneOffset.UTC)) { nav.go("names") }
            if (s.lastSura > 0) {
                val q by app.quran.collectAsStateWithLifecycle()
                q?.suras?.getOrNull(s.lastSura - 1)?.let { sura ->
                    VSpace(14.dp)
                    ContinueCard(
                        "${sura.number}. ${sura.uzName} surasi",
                        "${s.lastAyah}-oyat" + if (s.quranMushaf && s.mushafPage > 0) " • ${s.mushafPage}-sahifa" else "",
                        sura.arName
                    ) {
                        if (s.quranMushaf && s.mushafPage > 0) nav.go("mushaf/${s.mushafPage}")
                        else nav.go("reader/${sura.number}?ayah=${s.lastAyah}")
                    }
                }
            }
            VSpace(22.dp)
            SectionHeader("Xizmatlar", "Barchasi") { nav.go("more") }
            VSpace(10.dp)
            val tiles = listOf(
                HomeTile("Qur'on", Icons.AutoMirrored.Filled.MenuBook, "quran", Color(0xFF0F8A66)),
                HomeTile("Mushaf", Icons.Filled.AutoStories, "mushaf/${if (s.mushafPage > 0) s.mushafPage else 1}", Color(0xFFB7791F)),
                HomeTile("Qibla", Icons.Filled.Explore, "qibla", Color(0xFF00838F)),
                HomeTile("Tasbeh", Icons.Filled.Fingerprint, "tasbeh", Color(0xFF7B3FA0)),
                HomeTile("99 ism", Icons.Filled.AutoAwesome, "names", Color(0xFF3949AB)),
                HomeTile("Duolar", Icons.Filled.VolunteerActivism, "duas", Color(0xFF2E7D32)),
                HomeTile("Rasm", Icons.Filled.Image, "wallpaper", Color(0xFFD81B60)),
                HomeTile("Qazo", Icons.Filled.Checklist, "qazo", Color(0xFF5D6D7E)),
                HomeTile("Konvertor", Icons.Filled.SwapHoriz, "converter", Color(0xFF1E6FD9)),
                HomeTile("Taqvim", Icons.Filled.CalendarMonth, "calendar", Color(0xFFE0592A)),
                HomeTile("Ob-havo", Icons.Filled.WbSunny, "weather", Color(0xFF0288D1)),
                HomeTile("Sozlamalar", Icons.Filled.Settings, "settings", Color(0xFF607D8B)),
            )
            Column(
                Modifier.fillMaxWidth().premiumCard().padding(vertical = 14.dp, horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                tiles.chunked(4).forEachIndexed { r, row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEachIndexed { c, t -> AnimatedTile(t, r * 4 + c, Modifier.weight(1f)) { go(t.route) } }
                    }
                }
            }
            VSpace(8.dp)
        }
    }
}

/** Yumshoq soyali, nozik chegarali premium karta foni. */
@Composable
private fun Modifier.premiumCard(radius: Int = 26): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    val dark = LocalExtra.current.dark
    return this
        .shadow(if (dark) 0.dp else 10.dp, shape, ambientColor = Color(0x22000000), spotColor = Color(0x33000000))
        .clip(shape)
        .background(if (dark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (dark) 0.35f else 0.45f), shape)
}

@Composable
private fun SectionHeader(title: String, action: String?, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(Brush.verticalGradient(listOf(Gold, GoldDeep))))
        HSpace(8.dp)
        Text(title, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f))
        if (action != null) Row(
            Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(action, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun GlassIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(42.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.16f))
            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, desc, tint = Color.White, modifier = Modifier.size(22.dp)) }
}

@Composable
private fun HeroChip(text: String, dot: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        HSpace(6.dp)
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
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
    val stars = remember { StarField(70, 5) }
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
    Box(Modifier.fillMaxWidth()) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(Brush.verticalGradient(listOf(c0, c1, c2)))
            // yuqoridan yumshoq nur
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), Offset(size.width * 0.5f, size.height * 0.36f), size.width * 0.7f),
                size.width * 0.7f, Offset(size.width * 0.5f, size.height * 0.36f)
            )
            drawStarLattice(Color.White.copy(alpha = 0.05f), size.width / 5.5f, 1.2f, drift, Offset(size.width * 0.8f, size.height * 0.2f))
            if (phase.night) with(stars) { draw(Color.White.copy(alpha = 0.85f), tw, 0.7f, 1.7f) }
            // quyosh / oy yoyi (karta ustida)
            val overlap = HeroOverlap.toPx()
            val l = size.width * 0.06f; val r = size.width * 0.94f
            val base = size.height - overlap - 6.dp.toPx(); val h = 66.dp.toPx()
            val x = l + (r - l) * frac
            val y = base - h * sin(PI * frac).toFloat()
            drawArc(
                Color.White.copy(alpha = 0.22f), 180f, 180f, false,
                topLeft = Offset(l, base - h), size = androidx.compose.ui.geometry.Size(r - l, h * 2),
                style = Stroke(1.2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 10f)))
            )
            val bodyR = 13.dp.toPx()
            if (isDay) {
                drawCircle(Brush.radialGradient(listOf(Color(0x99FFF3B0), Color.Transparent), Offset(x, y), bodyR * 3.2f * glow), bodyR * 3.2f * glow, Offset(x, y))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF8D6), Color(0xFFFFC93C)), Offset(x, y), bodyR), bodyR, Offset(x, y))
            } else {
                drawCircle(Brush.radialGradient(listOf(Color(0x66E8EEFF), Color.Transparent), Offset(x, y), bodyR * 2.8f * glow), bodyR * 2.8f * glow, Offset(x, y))
                drawPath(crescentPath(x, y, bodyR), Color(0xFFFFF4CF))
            }
            // masjid silueti
            val mw = size.width * 0.94f
            drawPath(mosquePath(mw, size.height + 2f, 76.dp.toPx(), (size.width - mw) / 2), Color.Black.copy(alpha = 0.26f))
            drawRect(Color.Black.copy(alpha = 0.26f), Offset(0f, size.height - overlap), androidx.compose.ui.geometry.Size(size.width, overlap))
        }
        content()
    }
}

/** Keyingi namozgacha qolgan vaqt — oltin halqa, soat belgilari va porlab turuvchi nuqta. */
@Composable
private fun CountdownRing(pn: PrayerNow, left: Long, progress: Float, onClick: () -> Unit) {
    val p by animateFloatAsState(progress, tween(900, easing = FastOutSlowInEasing), label = "ring")
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    val inf = rememberInfiniteTransition(label = "ringInf")
    val pulse by inf.animateFloat(0.55f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "pulse")
    Box(
        Modifier.size(226.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val r = size.minDimension / 2f - 16.dp.toPx()
            val c = center
            // ichki shisha doira
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.04f)), c, r), r - stroke, c)
            drawCircle(Color.White.copy(alpha = 0.18f), r - stroke, c, style = Stroke(1.dp.toPx()))
            // soat belgilari
            for (i in 0 until 60) {
                val a = Math.toRadians(i * 6.0 - 90)
                val major = i % 5 == 0
                val r1 = r + stroke / 2 + 4.dp.toPx()
                val r2 = r1 + if (major) 7.dp.toPx() else 3.5.dp.toPx()
                val on = i / 60f <= p * appear.value
                drawLine(
                    (if (on) Gold else Color.White).copy(alpha = if (major) 0.85f else 0.45f),
                    Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                    Offset(c.x + r2 * cos(a).toFloat(), c.y + r2 * sin(a).toFloat()),
                    if (major) 2.dp.toPx() else 1.dp.toPx(), cap = StrokeCap.Round
                )
            }
            // yo'l
            drawCircle(Color.White.copy(alpha = 0.16f), r, c, style = Stroke(stroke))
            // taraqqiyot
            val sweep = 360f * p * appear.value
            rotate(-90f, c) {
                drawArc(
                    Brush.sweepGradient(listOf(Color(0xFFFFF1C1), Gold, GoldDeep, Color(0xFFFFF1C1)), c),
                    0f, sweep.coerceAtLeast(0.5f), false,
                    topLeft = Offset(c.x - r, c.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            val ea = Math.toRadians(sweep - 90.0)
            val ep = Offset(c.x + r * cos(ea).toFloat(), c.y + r * sin(ea).toFloat())
            drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = 0.7f * pulse), Color.Transparent), ep, stroke * 2.2f), stroke * 2.2f, ep)
            drawCircle(Color.White, stroke * 0.55f, ep)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Keyingi namoz", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium.merge(headerText))
            Text(pn.next.uz, color = Gold, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 24.sp, style = headerText)
            Text(
                Uz.countdown(left), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 34.sp,
                letterSpacing = 0.5.sp, style = headerText.merge(TextStyle(fontFeatureSettings = "tnum"))
            )
            VSpace(2.dp)
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text("${fmtMin(pn.nextAt.hour * 60 + pn.nextAt.minute)} da", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ------------------------------ Namozlar jadvali ------------------------------

private val prayerIcons = mapOf(
    Prayer.FAJR to Icons.Filled.WbTwilight, Prayer.SUNRISE to Icons.Filled.WbSunny, Prayer.DHUHR to Icons.Filled.LightMode,
    Prayer.ASR to Icons.Filled.Brightness5, Prayer.MAGHRIB to Icons.Filled.Brightness4, Prayer.ISHA to Icons.Filled.Bedtime
)

@Composable
private fun PrayerTimeline(times: DayTimes, pn: PrayerNow, now: LocalDateTime, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "tl")
    val glow by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "g")
    val primary = MaterialTheme.colorScheme.primary
    val ex = LocalExtra.current
    Column(Modifier.fillMaxWidth().premiumCard(28).clickable(onClick = onClick).padding(top = 14.dp, bottom = 12.dp)) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Bugungi namozlar", fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(Uz.gregorianFull(now.toLocalDate()).substringBefore(","), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
        VSpace(10.dp)
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp)) {
            Prayer.entries.forEach { p ->
                val active = p == pn.current
                val next = p == pn.next
                val bg by animateColorAsState(if (active) primary else Color.Transparent, tween(500), label = "bg")
                Column(
                    Modifier.weight(1f).padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (active) Brush.verticalGradient(listOf(bg, lerp(bg, Color.Black, 0.18f)))
                            else Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .border(
                            if (next) 1.5.dp else 0.dp,
                            if (next) Gold.copy(alpha = 0.45f + 0.55f * glow) else Color.Transparent,
                            RoundedCornerShape(18.dp)
                        )
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val fg = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                    val iconTint = when {
                        active -> Gold
                        next -> GoldDeep
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    }
                    Icon(prayerIcons.getValue(p), null, tint = iconTint, modifier = Modifier.size(22.dp))
                    VSpace(4.dp)
                    Text(p.uz, color = fg.copy(alpha = if (active) 0.9f else 0.7f), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    Text(
                        fmtMin(times[p]), color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                    Box(
                        Modifier.padding(top = 3.dp).size(5.dp).clip(CircleShape)
                            .background(if (times.isAdjusted(p)) (if (active) Gold else MaterialTheme.colorScheme.secondary) else Color.Transparent)
                    )
                }
            }
        }
        // kun chizig'i: bomdoddan xuftongacha
        val fajr = times[Prayer.FAJR].toFloat(); val isha = times[Prayer.ISHA].toFloat()
        val m = now.hour * 60 + now.minute + now.second / 60f
        val dayFrac = ((m - fajr) / (isha - fajr)).coerceIn(0f, 1f)
        Box(Modifier.padding(horizontal = 18.dp).padding(top = 6.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
            Box(Modifier.fillMaxWidth(dayFrac).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Brush.horizontalGradient(listOf(primary, ex.palette.accent, Gold))))
        }
    }
}

// ------------------------------ Kartalar ------------------------------

@Composable
private fun GradientTile(colors: List<Color>, modifier: Modifier, onClick: (() -> Unit)?, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(dampingRatio = 0.55f), label = "gp")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(8.dp, shape, ambientColor = colors.last().copy(alpha = 0.4f), spotColor = colors.first().copy(alpha = 0.5f))
            .clip(shape)
            .background(Brush.linearGradient(colors))
            .then(if (onClick != null) Modifier.clickable(src, null, onClick = onClick) else Modifier)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.10f), size.width * 0.55f, Offset(size.width * 1.02f, -size.height * 0.05f))
            drawCircle(Color.White.copy(alpha = 0.07f), size.width * 0.35f, Offset(size.width * 0.95f, size.height * 1.05f))
        }
        Box(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun WeatherCard(weather: WeatherState, times: DayTimes, modifier: Modifier, onClick: () -> Unit) {
    GradientTile(listOf(Color(0xFF1565C0), Color(0xFF3D8BEB), Color(0xFF6EC6F5)), modifier, onClick) {
        Column {
            Text("Ob-havo", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
            when (weather) {
                is WeatherState.Ready -> {
                    val (desc, emo) = WeatherApi.describe(weather.w.code, weather.w.isDay)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${weather.w.temp.roundToInt()}°", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        HSpace(6.dp)
                        Text(emo, fontSize = 26.sp)
                    }
                    Text(desc, color = Color.White, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Namlik ${weather.w.humidity}%", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                }
                is WeatherState.Failed -> {
                    Text("—°", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Text("Internet kerak", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    Text("…", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    Text("Yuklanmoqda", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                }
            }
            VSpace(8.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ArrowUpward, null, tint = Gold, modifier = Modifier.size(13.dp))
                Text(fmtMin(times.calculated.getValue(Prayer.SUNRISE)), color = Color.White, style = MaterialTheme.typography.labelMedium)
                HSpace(8.dp)
                Icon(Icons.Filled.ArrowDownward, null, tint = Color(0xFFFFB38A), modifier = Modifier.size(13.dp))
                Text(fmtMin((times.sunsetExact * 60).roundToInt()), color = Color.White, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun QiblaCard(bearing: Double, modifier: Modifier, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "qc")
    val sway by inf.animateFloat(-6f, 6f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "sway")
    GradientTile(listOf(Color(0xFF064E3B), Color(0xFF0B7A5C), Color(0xFF22A884)), modifier, onClick) {
        Column {
            Text("Qibla", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${bearing.roundToInt()}°", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("shimoldan", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                }
                Canvas(Modifier.size(62.dp)) {
                    val r = size.minDimension / 2f
                    drawCircle(Color.White.copy(alpha = 0.14f), r)
                    drawCircle(Gold.copy(alpha = 0.8f), r - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                    for (i in 0 until 4) {
                        val a = Math.toRadians(i * 90.0 - 90)
                        drawCircle(Color.White.copy(alpha = 0.7f), 1.8.dp.toPx(), Offset(center.x + (r - 6.dp.toPx()) * cos(a).toFloat(), center.y + (r - 6.dp.toPx()) * sin(a).toFloat()))
                    }
                    rotate(bearing.toFloat() + sway, center) {
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(center.x, center.y - r * 0.78f)
                            lineTo(center.x + r * 0.16f, center.y)
                            lineTo(center.x, center.y + r * 0.5f)
                            lineTo(center.x - r * 0.16f, center.y)
                            close()
                        }
                        drawPath(path, Brush.verticalGradient(listOf(Gold, GoldDeep), center.y - r, center.y + r))
                        drawCircle(Color.White, 2.5.dp.toPx(), center)
                    }
                }
            }
            VSpace(8.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Kompasni ochish", color = Color.White, style = MaterialTheme.typography.labelMedium)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Gold, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun NamesCard(epoch: Long, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    val drift by rememberInfiniteTransition(label = "nc").animateFloat(0f, 360f, infiniteRepeatable(tween(120000, easing = LinearEasing)), label = "d")
    Column(
        Modifier.fillMaxWidth()
            .shadow(8.dp, shape, spotColor = Color(0x66201040))
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF14123A), Color(0xFF2A1F5C), Color(0xFF4A2A6E))))
            .drawBehind {
                drawStarLattice(Gold.copy(alpha = 0.07f), size.width / 4.5f, 1.1f, drift, Offset(size.width * 0.85f, size.height * 0.5f))
            }
            .border(1.dp, Gold.copy(alpha = 0.35f), shape)
            .padding(horizontal = 6.dp, vertical = 10.dp)
    ) {
        Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Gold, modifier = Modifier.size(16.dp))
            HSpace(6.dp)
            Text("Asmaul husna", color = Gold, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("99 ism ›", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium, modifier = Modifier.clickable(onClick = onClick))
        }
        VSpace(4.dp)
        NameTicker(epoch, onClick)
    }
}

// ------------------------------ Plitkalar ------------------------------

private data class HomeTile(val label: String, val icon: ImageVector, val route: String, val color: Color)

@Composable
private fun AnimatedTile(t: HomeTile, index: Int, modifier: Modifier, onClick: () -> Unit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(450, delayMillis = 45 * index, easing = FastOutSlowInEasing)) }
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.5f), label = "press")
    val dark = LocalExtra.current.dark
    Column(
        modifier
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 24.dp.toPx()
                scaleX = scale; scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(src, null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val tint = if (dark) lerp(t.color, Color.White, 0.35f) else t.color
        Box(
            Modifier.size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(t.color.copy(alpha = if (dark) 0.30f else 0.16f), t.color.copy(alpha = if (dark) 0.14f else 0.06f))))
                .border(1.dp, t.color.copy(alpha = if (dark) 0.35f else 0.18f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(t.icon, null, tint = tint, modifier = Modifier.size(26.dp)) }
        VSpace(7.dp)
        Text(t.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ContinueCard(title: String, sub: String, arName: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, spotColor = Color(0x66A0701A))
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF3E2A0C), Color(0xFF7A5418), Color(0xFFB8862E))))
            .border(1.dp, Gold.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = Gold, modifier = Modifier.size(26.dp)) }
        HSpace(12.dp)
        Column(Modifier.weight(1f)) {
            Text("Davom ettirish", color = Gold, style = MaterialTheme.typography.labelLarge)
            Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
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
