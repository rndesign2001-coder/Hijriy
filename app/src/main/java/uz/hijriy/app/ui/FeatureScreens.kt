package uz.hijriy.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.core.Hijri
import uz.hijriy.app.core.HijriDate
import uz.hijriy.app.core.Prayer
import uz.hijriy.app.core.Qibla
import uz.hijriy.app.core.Translit
import uz.hijriy.app.core.Uz
import uz.hijriy.app.core.fmtMin
import uz.hijriy.app.data.AppTime
import uz.hijriy.app.data.HatmPlan
import uz.hijriy.app.ui.theme.LocalExtra
import uz.hijriy.app.ui.theme.QuranFont
import uz.hijriy.app.ui.theme.TitleFont
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private val FGold = Color(0xFFE9C46A)

/** Sarlavhali, naqshli bezak karta (yangi bo'limlar uchun). */
@Composable
fun OrnateCard(colors: List<Color>, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(colors))
            .drawBehind { drawGirih(Color.White.copy(alpha = 0.06f), 54.dp.toPx(), 0.8.dp.toPx()) }
            .border(1.dp, FGold.copy(alpha = 0.4f), shape)
            .padding(18.dp)
    ) { content() }
}

// ------------------------------ Hatm rejasi ------------------------------

@Composable
fun HatmScreen(app: HijriyApp, nav: NavHostController) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    val today = AppTime.today()
    val plan = HatmPlan.of(s, today)
    var days by remember { mutableFloatStateOf(30f) }
    var from by remember { mutableFloatStateOf(s.mushafPage.coerceAtLeast(1).toFloat()) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Hatm rejasi", "Qur'onni maqsadli kunga tugatish", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (plan != null) {
                OrnateCard(listOf(Color(0xFF3E1A10), Color(0xFF7A3418), Color(0xFFC0742C))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(170.dp)) {
                                val st = 14.dp.toPx()
                                drawCircle(Color.White.copy(alpha = 0.15f), size.minDimension / 2 - st / 2, style = Stroke(st))
                                drawArc(
                                    Brush.sweepGradient(listOf(Color(0xFFFFF1C1), FGold, Color(0xFFC8962E), Color(0xFFFFF1C1))),
                                    -90f, 360f * plan.progress, false, Offset(st / 2, st / 2),
                                    androidx.compose.ui.geometry.Size(size.width - st, size.height - st), style = Stroke(st, cap = StrokeCap.Round)
                                )
                                for (j in 1..30) {
                                    val a = Math.toRadians(j * 12.0 - 90)
                                    val r1 = size.minDimension / 2 - st - 6.dp.toPx(); val r2 = r1 - (if (j % 5 == 0) 7 else 3).dp.toPx()
                                    drawLine(Color.White.copy(alpha = 0.4f), Offset(center.x + r1 * cos(a).toFloat(), center.y + r1 * sin(a).toFloat()), Offset(center.x + r2 * cos(a).toFloat(), center.y + r2 * sin(a).toFloat()), 1.2.dp.toPx())
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${(plan.progress * 100).roundToInt()}%", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp)
                                Text("${plan.currentPage}/${HatmPlan.PAGES} bet", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        VSpace(12.dp)
                        Row(Modifier.fillMaxWidth()) {
                            Stat("Bugungi vazifa", "${plan.todayPages} bet", Modifier.weight(1f))
                            Stat("Qolgan kun", "${plan.daysLeft}", Modifier.weight(1f))
                            Stat("Juz/kun", "%.1f".format(plan.todayPages / 20f), Modifier.weight(1f))
                        }
                        VSpace(8.dp)
                        Text("Tugash sanasi: ${Uz.gregorian(plan.end)}", color = FGold, style = MaterialTheme.typography.labelLarge)
                    }
                }
                VSpace(12.dp)
                Button(onClick = { nav.go("mushaf/${plan.currentPage}") }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    HIcon(HI.MUSHAF, MaterialTheme.colorScheme.onPrimary, size = 22.dp); HSpace(8.dp)
                    Text("${plan.currentPage}-betdan davom etish")
                }
                VSpace(8.dp)
                OutlinedButton(onClick = { app.settings.update { it.copy(hatmStart = 0, hatmEnd = 0) } }, modifier = Modifier.fillMaxWidth()) {
                    Text("Rejani bekor qilish")
                }
            } else {
                OrnateCard(listOf(Color(0xFF0F3D2E), Color(0xFF1E6B4F), Color(0xFF2E8B67))) {
                    Column {
                        HIcon(HI.HATM, Color.White, size = 48.dp, accent = FGold)
                        VSpace(8.dp)
                        Text("Yangi reja", color = Color.White, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Mushafni o'qiganingiz sari reja o'zi yangilanadi.", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                VSpace(16.dp)
                Text("Necha kunda tugatasiz: ${days.roundToInt()} kun", style = MaterialTheme.typography.titleMedium)
                Slider(days, { days = it }, valueRange = 7f..365f)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 15, 30, 60, 90).forEach { d -> FilterChip(days.roundToInt() == d, { days = d.toFloat() }, { Text("$d kun") }) }
                }
                VSpace(12.dp)
                Text("Qaysi betdan boshlaysiz: ${from.roundToInt()}", style = MaterialTheme.typography.titleMedium)
                Slider(from, { from = it }, valueRange = 1f..604f)
                VSpace(4.dp)
                val perDay = ((605 - from.roundToInt()) / days).let { kotlin.math.ceil(it).toInt() }
                Text("Kuniga taxminan $perDay bet (≈ ${"%.1f".format(perDay / 20f)} juz)", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                VSpace(16.dp)
                Button(onClick = {
                    val t = AppTime.today().toEpochDay()
                    app.settings.update { it.copy(hatmStart = t, hatmEnd = t + days.roundToInt() - 1, hatmFromPage = from.roundToInt(), mushafPage = from.roundToInt()) }
                }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Rejani boshlash") }
                VSpace(10.dp)
                Text("Masalan: Ramazon oyida 30 kunda — kuniga 1 juz (≈20 bet).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            VSpace(24.dp)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

// ------------------------------ Ramazon ------------------------------

private const val SUHOOR = "نَوَيْتُ أَنْ أَصُومَ صَوْمَ شَهْرِ رَمَضَانَ مِنَ الْفَجْرِ إِلَى الْمَغْرِبِ خَالِصًا لِلَّهِ تَعَالَى"
private const val IFTAR = "اللَّهُمَّ لَكَ صُمْتُ وَبِكَ آمَنْتُ وَعَلَيْكَ تَوَكَّلْتُ وَعَلَى رِزْقِكَ أَفْطَرْتُ فَاغْفِرْ لِي يَا غَفَّارُ مَا قَدَّمْتُ وَمَا أَخَّرْتُ"

@Composable
fun RamadanScreen(app: HijriyApp, nav: NavHostController) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    val now by rememberNow()
    val today = now.toLocalDate()
    val h = Hijri.fromGregorian(today, s.hijriAdjust)
    // Joriy yoki keyingi Ramazonning 1-kuni
    val ramYear = if (h.month > 9) h.year + 1 else h.year
    val start = remember(ramYear, s.hijriAdjust) { Hijri.toGregorian(HijriDate(ramYear, 9, 1), s.hijriAdjust) }
    val len = remember(ramYear) { Hijri.monthLength(ramYear, 9) }
    val inRamadan = h.month == 9
    val t = remember(s, today) { s.times(today) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Ramazon", "${ramYear}-yil hijriy", onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                OrnateCard(listOf(Color(0xFF1A0F3C), Color(0xFF3B1E6B), Color(0xFF7A2E6E))) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        HIcon(HI.RAMADAN, FGold, size = 56.dp, accent = Color.White)
                        VSpace(6.dp)
                        if (inRamadan) {
                            val m = now.hour * 60 + now.minute
                            val fajr = t[Prayer.FAJR]; val mg = t[Prayer.MAGHRIB]
                            val (label, target) = when {
                                m < fajr -> "Saharlik tugashiga" to fajr
                                m < mg -> "Iftorgacha" to mg
                                else -> "Ertangi saharlikkacha" to fajr + 1440
                            }
                            Text("Ramazonning ${h.day}-kuni", color = Color.White, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Text(label, color = Color.White.copy(alpha = 0.8f))
                            Text(Uz.countdown((target - m) * 60L - now.second), color = FGold, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, style = TextStyle(fontFeatureSettings = "tnum"))
                            Row(Modifier.fillMaxWidth()) {
                                Stat("Saharlik", fmtMin(fajr), Modifier.weight(1f))
                                Stat("Iftorlik", fmtMin(mg), Modifier.weight(1f))
                            }
                        } else if (start != null) {
                            val d = start.toEpochDay() - today.toEpochDay()
                            Text("Ramazongacha", color = Color.White.copy(alpha = 0.85f))
                            Text("$d kun", color = FGold, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp)
                            Text("1-Ramazon: ${Uz.gregorianFull(start)}", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                            Text("(taxminiy — oy ko'rinishiga qarab 1 kun farq qilishi mumkin)", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            item { DuaBlock("Saharlik (og'iz yopish) niyati", SUHOOR, "Ramazon oyining ro'zasini tongdan to kun botguncha xolis Alloh uchun tutishni niyat qildim.", s.readingScript == 1) }
            item { DuaBlock("Iftorlik (og'iz ochish) duosi", IFTAR, "Allohim, Sen uchun ro'za tutdim, Senga iymon keltirdim, Senga tavakkul qildim va bergan rizqing bilan iftor qildim. Ey gunohlarni kechiruvchi Zot, oldingi va keyingi gunohlarimni kechir.", s.readingScript == 1) }
            if (start != null) {
                item { SectionTitle("Ramazon taqvimi") }
                items((0 until len).toList()) { i ->
                    val d = start.plusDays(i.toLong())
                    val tt = s.times(d)
                    val isToday = d == today
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${i + 1}", fontWeight = FontWeight.Bold, modifier = Modifier.size(width = 30.dp, height = 22.dp))
                        Text(Uz.gregorian(d).substringBeforeLast(" "), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("Saharlik ${fmtMin(tt[Prayer.FAJR])}", style = MaterialTheme.typography.labelLarge)
                        HSpace(12.dp)
                        Text("Iftor ${fmtMin(tt[Prayer.MAGHRIB])}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DuaBlock(title: String, ar: String, meaning: String, cyr: Boolean) {
    val rd = remember(ar) { Translit.dua(ar) }
    SectionCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        VSpace(8.dp)
        androidx.compose.material3.Text(
            ar, fontFamily = QuranFont, fontSize = 24.sp, lineHeight = 44.sp, textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth(), style = TextStyle(textDirection = androidx.compose.ui.text.style.TextDirection.Rtl)
        )
        VSpace(6.dp)
        androidx.compose.material3.Text(if (cyr) rd.cyrillic else rd.latin, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
        VSpace(6.dp)
        Text(meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ------------------------------ Yaqin masjidlar ------------------------------

data class Mosque(val name: String, val lat: Double, val lon: Double, val km: Double, val bearing: Double)

private suspend fun fetchMosques(lat: Double, lon: Double, radius: Int): List<Mosque> = withContext(Dispatchers.IO) {
    // Maxfiylik: so'rovga ~1 km aniqlikdagi koordinata yuboriladi, masofa telefonda aniq hisoblanadi
    val la = "%.2f".format(java.util.Locale.US, lat); val lo = "%.2f".format(java.util.Locale.US, lon)
    val q = "[out:json][timeout:25];(node[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:$radius,$la,$lo);" +
        "way[\"amenity\"=\"place_of_worship\"][\"religion\"=\"muslim\"](around:$radius,$la,$lo););out center 80;"
    val c = URL("https://overpass-api.de/api/interpreter").openConnection() as HttpURLConnection
    c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 15_000; c.readTimeout = 30_000
    c.setRequestProperty("User-Agent", "HijriyTaqvim/1.0 (Android)")
    c.outputStream.use { it.write(("data=" + URLEncoder.encode(q, "UTF-8")).toByteArray()) }
    val txt = c.inputStream.bufferedReader().use { it.readText() }
    c.disconnect()
    val arr = JSONObject(txt).optJSONArray("elements") ?: return@withContext emptyList()
    (0 until arr.length()).mapNotNull { i ->
        val o = arr.getJSONObject(i)
        val mla = if (o.has("lat")) o.getDouble("lat") else o.optJSONObject("center")?.optDouble("lat") ?: return@mapNotNull null
        val mlo = if (o.has("lon")) o.getDouble("lon") else o.optJSONObject("center")?.optDouble("lon") ?: return@mapNotNull null
        val tags = o.optJSONObject("tags")
        val name = tags?.let { t -> listOf("name:uz", "name", "name:ru", "name:en").firstNotNullOfOrNull { k -> t.optString(k).takeIf { v -> v.isNotBlank() } } } ?: "Masjid"
        val (km, br) = distBearing(lat, lon, mla, mlo)
        Mosque(name, mla, mlo, km, br)
    }.sortedBy { it.km }
}

private fun distBearing(la1: Double, lo1: Double, la2: Double, lo2: Double): Pair<Double, Double> {
    val p1 = Math.toRadians(la1); val p2 = Math.toRadians(la2); val dl = Math.toRadians(lo2 - lo1)
    val a = sin((p2 - p1) / 2).let { it * it } + cos(p1) * cos(p2) * sin(dl / 2).let { it * it }
    val km = 2 * 6371.0 * atan2(sqrt(a), sqrt(1 - a))
    val y = sin(dl) * cos(p2); val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
    return km to (Math.toDegrees(atan2(y, x)) + 360) % 360
}

@Composable
fun MosquesScreen(app: HijriyApp, nav: NavHostController) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var radius by remember { mutableIntStateOf(3000) }
    var list by remember { mutableStateOf<List<Mosque>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(s.lat, s.lon, radius) {
        loading = true; err = null
        runCatching { fetchMosques(s.lat, s.lon, radius) }
            .onSuccess { list = it; if (it.isEmpty()) err = "Bu radiusda masjid topilmadi — radiusni kattalashtiring" }
            .onFailure { err = "Internetga ulanib bo'lmadi" }
        loading = false
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Yaqin masjidlar", s.locName, onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OrnateCard(listOf(Color(0xFF062E2A), Color(0xFF0B4F47), Color(0xFF137A6B))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        MosqueRadar(list.orEmpty(), radius / 1000.0, Qibla.bearing(s.lat, s.lon))
                        VSpace(8.dp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1000, 3000, 5000, 10000).forEach { r ->
                                val sel = r == radius
                                Box(
                                    Modifier.clip(RoundedCornerShape(50)).background(if (sel) FGold else Color.White.copy(alpha = 0.14f))
                                        .clickable { radius = r }.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) { Text("${r / 1000} km", color = if (sel) Color(0xFF0B3B33) else Color.White, style = MaterialTheme.typography.labelLarge) }
                            }
                        }
                    }
                }
            }
            if (loading) item { Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            err?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp)) } }
            items(list.orEmpty()) { m ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable {
                            val uri = Uri.parse("geo:${m.lat},${m.lon}?q=${m.lat},${m.lon}(${Uri.encode(m.name)})")
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(48.dp, 54.dp).clip(ArchShape()).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                        HIcon(HI.MOSQUE, MaterialTheme.colorScheme.primary, size = 30.dp, accent = Color(0xFFC8962E))
                    }
                    HSpace(12.dp)
                    Column(Modifier.weight(1f)) {
                        Text(m.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        Text("${dirName(m.bearing)} tomonda • xaritada ochish", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(if (m.km < 1) "${(m.km * 1000).roundToInt()} m" else "%.1f km".format(m.km), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            item {
                Text("Ma'lumotlar: © OpenStreetMap hissadorlari (ODbL), Overpass API orqali.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

private fun dirName(b: Double): String = listOf("Shimol", "Shimoli-sharq", "Sharq", "Janubi-sharq", "Janub", "Janubi-g'arb", "G'arb", "Shimoli-g'arb")[(((b + 22.5) % 360) / 45).toInt()]

@Composable
private fun MosqueRadar(list: List<Mosque>, radiusKm: Double, qibla: Double) {
    val sweep by androidx.compose.animation.core.rememberInfiniteTransition(label = "radar").animateFloat(
        0f, 360f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(4000, easing = androidx.compose.animation.core.LinearEasing)), label = "sw"
    )
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.25f)) {
        val r = size.height / 2 - 8.dp.toPx(); val c = center
        for (k in 1..3) drawCircle(Color.White.copy(alpha = 0.18f), r * k / 3, c, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
        drawGirih(Color.White.copy(alpha = 0.03f), 40.dp.toPx(), 0.8.dp.toPx())
        // nurlanish
        drawArc(Brush.sweepGradient(listOf(Color.Transparent, Color(0x55E9C46A), Color.Transparent), c), sweep - 40f, 40f, true, Offset(c.x - r, c.y - r), androidx.compose.ui.geometry.Size(2 * r, 2 * r))
        // qibla chizig'i
        val qa = Math.toRadians(qibla - 90)
        drawLine(FGold, c, Offset(c.x + r * cos(qa).toFloat(), c.y + r * sin(qa).toFloat()), 2.dp.toPx(), StrokeCap.Round)
        drawPath(starPath(c.x + r * cos(qa).toFloat(), c.y + r * sin(qa).toFloat(), 7.dp.toPx()), FGold)
        // masjidlar
        list.take(60).forEach { m ->
            val d = (m.km / radiusKm).coerceAtMost(1.0) * r
            val a = Math.toRadians(m.bearing - 90)
            val p = Offset(c.x + (d * cos(a)).toFloat(), c.y + (d * sin(a)).toFloat())
            drawCircle(Color(0x33FFFFFF), 7.dp.toPx(), p)
            drawCircle(Color(0xFF7CFFB2), 3.5.dp.toPx(), p)
        }
        drawCircle(Color.White, 5.dp.toPx(), c)
        drawCircle(Color.White.copy(alpha = 0.3f), 10.dp.toPx(), c)
        // shimol belgisi
        drawLine(Color.White.copy(alpha = 0.6f), Offset(c.x, c.y - r - 6.dp.toPx()), Offset(c.x, c.y - r + 6.dp.toPx()), 2.dp.toPx())
    }
}

// ------------------------------ Namoz kundaligi ------------------------------

private val FARZ = listOf(Prayer.FAJR to 1, Prayer.DHUHR to 2, Prayer.ASR to 4, Prayer.MAGHRIB to 8, Prayer.ISHA to 16)

@Composable
fun JournalScreen(app: HijriyApp, nav: NavHostController) {
    val data by app.journal.state.collectAsStateWithLifecycle()
    val today = AppTime.today()
    var day by remember { mutableStateOf(today) }
    val mask = data[day.toEpochDay()] ?: 0
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Namoz kundaligi", "Har kungi farz namozlar", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // hafta tasmasi
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (6 downTo 0).forEach { k ->
                    val d = today.minusDays(k.toLong())
                    val v = Integer.bitCount(data[d.toEpochDay()] ?: 0)
                    val sel = d == day
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { day = d }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val fg = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        Text(Uz.weekday(d.dayOfWeek).take(3), color = fg.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
                        Text("${d.dayOfMonth}", color = fg, fontWeight = FontWeight.Bold)
                        Text("$v/5", color = if (v == 5) Color(0xFF2E9E6A) else fg.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            VSpace(14.dp)
            Text(if (day == today) "Bugun" else Uz.gregorianFull(day), style = MaterialTheme.typography.titleMedium)
            VSpace(8.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FARZ.forEach { (p, bit) ->
                    val on = mask and bit != 0
                    val shape = ArchShape(0.5f)
                    Column(
                        Modifier.weight(1f).height(108.dp).clip(shape)
                            .background(if (on) Brush.verticalGradient(listOf(Color(0xFF1E8C5E), Color(0xFF0F5A3C))) else Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainer)))
                            .border(1.dp, if (on) FGold else MaterialTheme.colorScheme.outlineVariant, shape)
                            .clickable { app.journal.toggle(day, bit) },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        HIcon(prayerHI.getValue(p), if (on) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, size = 28.dp, accent = FGold)
                        VSpace(6.dp)
                        Text(p.uz, color = if (on) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        Text(if (on) "✓" else "—", color = if (on) FGold else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                }
            }
            VSpace(18.dp)
            // oy xaritasi
            val month = today.withDayOfMonth(1)
            val daysIn = today.lengthOfMonth()
            val total = (1..daysIn).sumOf { Integer.bitCount(data[month.plusDays(it - 1L).toEpochDay()] ?: 0) }
            val passed = today.dayOfMonth * 5
            OrnateCard(listOf(Color(0xFF0F3D2E), Color(0xFF175C45), Color(0xFF2E8B67))) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Uz.gregorian(today).substringAfter(" ").replaceFirstChar { it.uppercase() }, color = Color.White, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("O'qilgan: $total / $passed", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${app.journal.streak(today)}", color = FGold, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                            Text("kun ketma-ket", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    VSpace(10.dp)
                    val offset = month.dayOfWeek.value - 1
                    val cells = offset + daysIn
                    (0 until (cells + 6) / 7).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            (0 until 7).forEach { col ->
                                val i = row * 7 + col - offset
                                Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                    if (i in 0 until daysIn) {
                                        val d = month.plusDays(i.toLong())
                                        val v = Integer.bitCount(data[d.toEpochDay()] ?: 0)
                                        Box(
                                            Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                                .background(if (v == 0) Color.White.copy(alpha = 0.08f) else FGold.copy(alpha = 0.18f + 0.16f * v))
                                                .then(if (d == today) Modifier.border(1.5.dp, Color.White, RoundedCornerShape(8.dp)) else Modifier)
                                                .clickable(enabled = !d.isAfter(today)) { day = d },
                                            contentAlignment = Alignment.Center
                                        ) { Text("${i + 1}", color = if (v >= 4) Color(0xFF1B140A) else Color.White, style = MaterialTheme.typography.labelSmall) }
                                    }
                                }
                            }
                        }
                        VSpace(5.dp)
                    }
                }
            }
            VSpace(10.dp)
            Text(
                "O'tkazib yuborilgan namozlarni \"Qazo\" bo'limida hisoblab boring.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { nav.go("qazo") }
            )
            VSpace(24.dp)
        }
    }
}

// ------------------------------ Mualliflar ------------------------------

@Composable
fun CreditsScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val photos = remember {
        runCatching {
            val a = org.json.JSONArray(ctx.assets.open("photo_credits.json").bufferedReader().use { it.readText() })
            (0 until a.length()).map { a.getJSONObject(it) }
        }.getOrDefault(emptyList())
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Manbalar va mualliflar", onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { SectionTitle("Fon fotosuratlari (Wikimedia Commons)") }
            items(photos) { o ->
                SectionCard(Modifier.fillMaxWidth().clickable {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(o.optString("page")))) }
                }) {
                    androidx.compose.material3.Text(o.optString("title").removePrefix("File:"), style = MaterialTheme.typography.titleSmall)
                    androidx.compose.material3.Text("Muallif: ${o.optString("artist")} • ${o.optString("license")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item { SectionTitle("Ma'lumotlar") }
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    listOf(
                        "Qur'on matni: Tanzil.net (Usmoniy, CC BY 3.0)",
                        "Tajvid belgilari: cpfair/quran-tajweed (CC BY 4.0)",
                        "Mushaf qator tartibi: quran-madina-html (ISC)",
                        "Shrift: Amiri Quran (SIL OFL)",
                        "Ob-havo va shaharlar: Open-Meteo (CC BY 4.0), GeoNames",
                        "Masjidlar: © OpenStreetMap hissadorlari (ODbL)",
                        "Qiroatlar: everyayah.com",
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp)) }
                }
            }
        }
    }
}
