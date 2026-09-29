package uz.hijriy.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.core.Dua
import uz.hijriy.app.core.Duas
import uz.hijriy.app.data.Quran
import uz.hijriy.app.data.TranslationRepo
import uz.hijriy.app.data.UzTranslit
import uz.hijriy.app.ui.theme.QuranFont
import uz.hijriy.app.ui.theme.TitleFont

// ------------------------------ Duolar ------------------------------

@Composable
fun DuasScreen(app: HijriyApp, nav: NavHostController) {
    val quran by app.quran.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val counters = remember { mutableStateMapOf<String, Int>() }
    LaunchedEffect(Unit) { app.ensureQuran() }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Duolar va zikrlar", "Tong, kech va namozdan keyin", onBack = { nav.popBackStack() })
        ScrollableTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background, edgePadding = 12.dp) {
            Duas.sections.forEachIndexed { i, sec -> Tab(tab == i, { tab = i }, text = { Text("${sec.emoji} ${sec.title}") }) }
        }
        val sec = Duas.sections[tab]
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(sec.items, key = { i, d -> "${sec.title}/$i/${d.title}" }) { i, d ->
                val key = "${sec.title}/$i"
                DuaCard(d, quran, counters[key] ?: 0) { counters[key] = ((counters[key] ?: 0) + 1).let { if (it > d.count) 0 else it } }
            }
            item {
                Text(
                    "Zikrni o'qigach kartaga bosing — sanoq oshadi. Hisob to'lganda karta yashil bo'ladi.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun duaArabic(d: Dua, q: Quran?): String {
    if (d.quran.isEmpty()) return d.ar
    if (q == null) return ""
    return d.quran.joinToString("\n\n") { (s, a1, a2) ->
        val sura = q.suras[s - 1]
        val body = (a1..a2).joinToString(" ") { a -> sura.ayahs[a - 1].text + " ۝" + uz.hijriy.app.core.Uz.arabicNumber(a) }
        if (d.quran.size > 1 || s >= 112) "${q.bismillah.text}\n$body" else body
    }
}

@Composable
private fun DuaCard(d: Dua, q: Quran?, done: Int, onTap: () -> Unit) {
    val complete = done >= d.count
    val bg by animateColorAsState(
        if (complete) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainer, label = "bg"
    )
    val progress by animateFloatAsState(done.toFloat() / d.count, label = "p")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(bg).clickable(onClick = onTap).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(d.title, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(
                    if (complete) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                ).padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    if (d.count > 1) "$done / ${d.count}" else if (complete) "✓" else "1 marta",
                    color = if (complete) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        val ar = duaArabic(d, q)
        if (ar.isNotBlank()) {
            VSpace(10.dp)
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                Text(
                    withAyahMarks(ar), fontFamily = QuranFont, fontSize = 24.sp, lineHeight = 46.sp, textAlign = TextAlign.Start,
                    inlineContent = rememberAyahInline(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth()
                )
            }
        }
        VSpace(8.dp)
        Text(d.meaning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        VSpace(4.dp)
        Text("📚 ${d.source}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        if (d.count > 1) {
            VSpace(8.dp)
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.outlineVariant)) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(5.dp).background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

// ------------------------------ Qazo namozlar ------------------------------

@Composable
fun QazoScreen(app: HijriyApp, nav: NavHostController) {
    val counts by app.qazo.counts.collectAsStateWithLifecycle()
    var addDialog by remember { mutableStateOf(false) }
    var resetDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Qazo namozlar", "Jami: ${counts.sum()} ta", onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF37474F), Color(0xFF607D8B)))).padding(18.dp)
                ) {
                    Text("Qolgan qazo namozlar", color = Color.White.copy(alpha = 0.85f))
                    Text("${counts.sum()}", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold, fontFamily = TitleFont)
                    Text(
                        "Har kuni bir kunlik qazo o'qisangiz, taxminan ${(counts.maxOrNull() ?: 0)} kunda tugaydi.",
                        color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            itemsIndexed(app.qazo.names) { i, n ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(n, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).padding(start = 6.dp))
                    FilledTonalIconButton(onClick = { app.qazo.set(i, counts[i] - 1) }) { Icon(Icons.Filled.Remove, "O'qidim") }
                    Text(
                        "${counts[i]}", fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 10.dp).graphicsLayer { }
                    )
                    FilledTonalIconButton(onClick = { app.qazo.set(i, counts[i] + 1) }) { Icon(Icons.Filled.Add, "Qo'shish") }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { addDialog = true }) { Text("Kun / yil qo'shish") }
                    TextButton(onClick = { resetDialog = true }) { Text("Nolga qaytarish") }
                }
                Text(
                    "\"−\" tugmasi — bitta qazo o'qildi. Vitr vojib bo'lgani uchun alohida hisoblanadi.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (addDialog) {
        var n by remember { mutableStateOf("") }
        var years by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { addDialog = false },
            title = { Text("Qazo qo'shish") },
            text = {
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(years, { years = true }, { Text("Yil") })
                        FilterChip(!years, { years = false }, { Text("Kun") })
                    }
                    OutlinedTextField(
                        n, { v -> n = v.filter { it.isDigit() }.take(4) }, label = { Text(if (years) "Necha yil" else "Necha kun") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                    )
                    val add = (n.toIntOrNull() ?: 0) * if (years) 354 else 1
                    Text("Har bir namozga $add ta qo'shiladi" + if (years) " (1 hijriy yil = 354 kun)" else "", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { app.qazo.addAll((n.toIntOrNull() ?: 0) * if (years) 354 else 1); addDialog = false }) { Text("Qo'shish") }
            },
            dismissButton = { TextButton(onClick = { addDialog = false }) { Text("Bekor") } }
        )
    }
    if (resetDialog) {
        AlertDialog(
            onDismissRequest = { resetDialog = false },
            title = { Text("Hisobni nolga qaytarasizmi?") },
            confirmButton = { TextButton(onClick = { app.qazo.reset(); resetDialog = false }) { Text("Ha") } },
            dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("Yo'q") } }
        )
    }
}

// ------------------------------ Qidiruv ------------------------------

data class SearchHit(val sura: Int, val ayah: Int, val snippet: String, val arabic: Boolean)

private val harakat = Regex("[ؐ-ًؚ-ٰٟۖ-ۭـ]")

fun normalizeArabic(s: String): String = harakat.replace(s, "")
    .replace('ٱ', 'ا').replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
    .replace('ى', 'ي').replace('ة', 'ه').replace('ؤ', 'و').replace('ئ', 'ي')

@Composable
fun SearchScreen(app: HijriyApp, nav: NavHostController) {
    val quran by app.quran.collectAsStateWithLifecycle()
    val tr by TranslationRepo.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<SearchHit>?>(null) }
    LaunchedEffect(Unit) { app.ensureQuran() }
    LaunchedEffect(submitted, quran, tr) {
        val q = quran ?: return@LaunchedEffect
        val k = submitted.trim()
        if (k.length < 2) { hits = null; return@LaunchedEffect }
        hits = null
        hits = withContext(Dispatchers.Default) {
            val out = ArrayList<SearchHit>()
            val isAr = k.any { it in '؀'..'ۿ' }
            if (isAr) {
                val nk = normalizeArabic(k)
                for (s in q.suras) for (a in s.ayahs) {
                    if (normalizeArabic(a.text).contains(nk)) out += SearchHit(s.number, a.number, a.text, true)
                    if (out.size >= 300) break
                }
            } else {
                val t = (tr as? TranslationRepo.State.Ready)?.suras
                if (t != null) {
                    val lk = k.lowercase().replace("‘", "'").replace("’", "'")
                    t.forEachIndexed { si, ay ->
                        ay.forEachIndexed { ai, txt ->
                            val lat = UzTranslit.toLatin(txt)
                            if (out.size < 300 && (lat.lowercase().contains(lk) || txt.lowercase().contains(lk))) out += SearchHit(si + 1, ai + 1, lat, false)
                        }
                    }
                }
            }
            out
        }
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Qur'ondan qidirish", "Arabcha matn yoki o'zbekcha tarjima", onBack = { nav.popBackStack() })
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Masalan: رحمة yoki sabr") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = ""; submitted = "" }) { Icon(Icons.Filled.Close, null) } },
            singleLine = true, shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submitted = query }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            )
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { submitted = query }) { Text("Qidirish") }
            HSpace(10.dp)
            if (tr !is TranslationRepo.State.Ready) Text(
                "O'zbekcha qidirish uchun o'qish ekranida tarjimani yuklab oling",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val h = hits
        when {
            submitted.trim().length < 2 -> Unit
            h == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            h.isEmpty() -> Text("Hech narsa topilmadi", modifier = Modifier.padding(24.dp))
            else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("${h.size}${if (h.size >= 300) "+" else ""} ta oyat topildi", style = MaterialTheme.typography.labelLarge) }
                items(h) { hit ->
                    val sura = quran?.suras?.getOrNull(hit.sura - 1)
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { nav.go("reader/${hit.sura}?ayah=${hit.ayah}") }.padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                            HSpace(8.dp)
                            Text("${sura?.uzName ?: hit.sura} ${hit.sura}:${hit.ayah}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (hit.arabic) Text(hit.snippet, fontFamily = QuranFont, fontSize = 22.sp, lineHeight = 40.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                        else Text(hit.snippet, style = MaterialTheme.typography.bodyMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
