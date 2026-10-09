package uz.hijriy.app.ui

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.core.AllahName
import uz.hijriy.app.core.AsmaulHusna
import uz.hijriy.app.ui.theme.LocalExtra
import uz.hijriy.app.ui.theme.QuranFont

// ------------------------------ Allohning 99 ismi ------------------------------

@Composable
fun NamesScreen(nav: NavHostController) {
    var query by remember { mutableStateOf("") }
    val norm = { x: String -> x.lowercase().replace("'", "").replace("-", "").replace(" ", "") }
    val list = remember(query) {
        val k = norm(query)
        if (k.isEmpty()) AsmaulHusna.names
        else AsmaulHusna.names.filter { norm(it.uz).contains(k) || norm(it.meaning).contains(k) || it.ar.contains(query.trim()) || it.n.toString() == k }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Allohning 99 ismi", "Asmaul Husna", onBack = { nav.popBackStack() })
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Ism yoki ma'nosi bo'yicha qidirish") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, null) } },
            singleLine = true, shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            )
        )
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(
                    "Rasululloh ﷺ: «Albatta, Allohning to'qson to'qqizta ismi bor. Kim ularni yod olsa (ma'nosini anglab, amal qilsa), jannatga kiradi». (Buxoriy, Muslim)",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(list, key = { it.n }) { NameCard(it) }
        }
    }
}

@Composable
private fun NameCard(n: AllahName) {
    SectionCard(Modifier.fillMaxWidth(), padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Text("${n.n}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
            HSpace(12.dp)
            Column(Modifier.weight(1f)) {
                Text(n.uz, style = MaterialTheme.typography.titleMedium)
                Text(n.meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HSpace(8.dp)
            Text(n.ar, fontFamily = QuranFont, fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Bosh sahifadagi aylanib turadigan ism (har 6 soniyada almashadi). */
@Composable
fun NameTicker(epochSeconds: Long, onClick: () -> Unit) {
    val idx = ((epochSeconds / 6) % 99).toInt()
    val name = AsmaulHusna.names[idx]
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = name,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "name",
            modifier = Modifier.weight(1f)
        ) { n ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${n.n}. ${n.uz}", color = Color.White, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text(
                        n.meaning, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                }
                HSpace(8.dp)
                Text(n.ar, fontFamily = QuranFont, fontSize = 24.sp, color = Color(0xFFFFE6A3))
            }
        }
    }
}

// ------------------------------ Tasbeh ------------------------------

data class Dhikr(val ar: String, val uz: String, val meaning: String, val target: Int)

val Dhikrs = listOf(
    Dhikr("سُبْحَانَ اللّٰهِ", "Subhanalloh", "Alloh barcha nuqsonlardan pokdir", 33),
    Dhikr("اَلْحَمْدُ لِلّٰهِ", "Alhamdulillah", "Barcha hamdlar Allohgadir", 33),
    Dhikr("اَللّٰهُ أَكْبَرُ", "Allohu akbar", "Alloh eng buyukdir", 34),
    Dhikr("لَا إِلٰهَ إِلَّا اللّٰهُ", "La ilaha illalloh", "Allohdan o'zga iloh yo'q", 100),
    Dhikr("أَسْتَغْفِرُ اللّٰهَ", "Astag'firulloh", "Allohdan mag'firat so'rayman", 100),
    Dhikr("سُبْحَانَ اللّٰهِ وَبِحَمْدِهِ", "Subhanallohi va bihamdihi", "Allohni hamdi bilan poklab yod etaman", 100),
    Dhikr("اَللّٰهُمَّ صَلِّ عَلَى مُحَمَّدٍ", "Allohumma solli 'ala Muhammad", "Allohim, Muhammadga salovot yo'lla", 100),
    Dhikr("لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللّٰهِ", "La havla va la quvvata illa billah", "Kuch-quvvat faqat Alloh bilandir", 100),
)

@Composable
fun TasbehScreen(app: HijriyApp, nav: NavHostController) {
    val ctx = LocalContext.current
    val store = app.settings
    var sel by remember { mutableIntStateOf(store.getString("tasbeh_sel")?.toIntOrNull()?.coerceIn(0, Dhikrs.lastIndex) ?: 0) }
    var count by remember { mutableIntStateOf(store.getString("tasbeh_count")?.toIntOrNull() ?: 0) }
    var laps by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(store.getString("tasbeh_total")?.toIntOrNull() ?: 0) }
    var afterPrayer by remember { mutableStateOf(store.getString("tasbeh_seq") == "1") }
    var vibrate by remember { mutableStateOf(store.getString("tasbeh_vib") != "0") }
    val d = Dhikrs[sel]

    fun buzz(ms: Long) {
        if (!vibrate) return
        runCatching {
            val v = ctx.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= 26) v?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    fun save() {
        store.putString("tasbeh_sel", sel.toString())
        store.putString("tasbeh_count", count.toString())
        store.putString("tasbeh_total", total.toString())
    }
    fun tap() {
        count++; total++
        if (count >= d.target) {
            buzz(300)
            if (afterPrayer && sel < 2) {
                sel++; count = 0
            } else {
                laps++; count = 0
            }
        } else buzz(18)
        save()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Tasbeh", "Jami zikrlar: $total", onBack = { nav.popBackStack() }, actions = {
            IconButton(onClick = { count = 0; laps = 0; save() }) { Icon(Icons.Filled.Refresh, "Nolga qaytarish") }
        })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Dhikrs.forEachIndexed { i, x ->
                    FilterChip(
                        selected = sel == i,
                        onClick = { sel = i; count = 0; laps = 0; save() },
                        label = { Text(x.uz) }
                    )
                }
            }
            VSpace(18.dp)
            AnimatedContent(targetState = d, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "dhikr") { x ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(x.ar, fontFamily = QuranFont, fontSize = 34.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    Text(x.uz, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(x.meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            VSpace(22.dp)
            val ex = LocalExtra.current
            val ring = MaterialTheme.colorScheme.primary
            val track = MaterialTheme.colorScheme.surfaceContainerHigh
            Box(
                Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth(0.82f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(ex.headerGradient))
                    .clickable { tap() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                    val sw = size.minDimension * 0.045f
                    drawArc(track.copy(alpha = 0.35f), 0f, 360f, false, style = Stroke(sw), topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw))
                    drawArc(
                        ex.palette.accent, -90f, 360f * count / d.target, false,
                        style = Stroke(sw, cap = StrokeCap.Round), topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$count", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                    Text("/ ${d.target}", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium)
                    if (laps > 0) Text("Davra: $laps", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                }
            }
            Text("Doira ustiga bosing", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp))
            VSpace(16.dp)
            SectionCard(Modifier.fillMaxWidth(), padding = 8.dp) {
                SettingRow(null, "Namozdan keyingi tasbeh", "33 Subhanalloh → 33 Alhamdulillah → 34 Allohu akbar, avtomatik o'tadi") {
                    Switch(checked = afterPrayer, onCheckedChange = {
                        afterPrayer = it; store.putString("tasbeh_seq", if (it) "1" else "0")
                        if (it) { sel = 0; count = 0; laps = 0; save() }
                    })
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingRow(Icons.Filled.Vibration, "Tebranish", "Har bosishda va davra tugaganda") {
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it; store.putString("tasbeh_vib", if (it) "1" else "0") })
                }
            }
            VSpace(24.dp)
        }
    }
}
