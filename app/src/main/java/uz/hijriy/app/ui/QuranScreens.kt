package uz.hijriy.app.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.core.Uz
import uz.hijriy.app.data.Ayah
import uz.hijriy.app.data.Mushaf
import uz.hijriy.app.data.MushafRepo
import uz.hijriy.app.data.Sura
import uz.hijriy.app.data.TajweedRule
import uz.hijriy.app.ui.theme.LocalExtra
import uz.hijriy.app.ui.theme.QuranFont

// ------------------------------ Suralar ro'yxati ------------------------------

@Composable
fun QuranListScreen(app: HijriyApp, nav: NavHostController) {
    val quran by app.quran.collectAsStateWithLifecycle()
    val s by app.settings.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var byJuz by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { app.ensureQuran() }
    val ctx = LocalContext.current
    val mushaf by produceState<Mushaf?>(null) { value = withContext(Dispatchers.Default) { MushafRepo.load(ctx) } }
    fun open(sura: Int, ayah: Int = 1) {
        val m = mushaf
        if (s.quranMushaf && m != null) nav.go("mushaf/${m.pageOf(sura, ayah)}") else nav.go("reader/$sura?ayah=$ayah")
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Qur'oni Karim", "114 sura • 6236 oyat • 604 sahifa")
        val q = quran
        if (q == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            placeholder = { Text("Sura nomi yoki raqami") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Tozalash") } },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            )
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(selected = !byJuz, onClick = { byJuz = false }, label = { Text("Suralar") })
            FilterChip(selected = byJuz, onClick = { byJuz = true }, label = { Text("Juzlar") })
            Box(Modifier.weight(1f))
            FilterChip(
                selected = !s.quranMushaf, onClick = { app.settings.update { it.copy(quranMushaf = false) } },
                label = { Text("Matn") }
            )
            FilterChip(
                selected = s.quranMushaf, onClick = { app.settings.update { it.copy(quranMushaf = true) } },
                label = { Text("Mushaf") }
            )
        }
        val norm = { x: String -> x.lowercase().replace("'", "").replace("‘", "").replace("’", "").replace("-", "").replace(" ", "") }
        val filtered = remember(q, query) {
            val k = norm(query)
            if (k.isEmpty()) q.suras else q.suras.filter {
                it.number.toString() == k || norm(it.uzName).contains(k) || norm(it.translit).contains(k) || it.arName.contains(query.trim())
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            if (s.lastSura > 0 && query.isEmpty()) {
                item {
                    val ls = q.suras.getOrNull(s.lastSura - 1)
                    if (ls != null) SectionCard(
                        Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        onClick = {
                            if (s.quranMushaf && s.mushafPage > 0) nav.go("mushaf/${s.mushafPage}") else open(ls.number, s.lastAyah)
                        }
                    ) {
                        Text("📖 Davom ettirish", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("${ls.uzName} surasi • ${s.lastAyah}-oyat", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            if (!byJuz) {
                items(filtered, key = { it.number }) { sura ->
                    SuraRow(sura) { open(sura.number) }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            } else {
                itemsIndexed(q.juz) { i, (su, ay) ->
                    val sura = q.suras[su - 1]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { open(su, ay) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NumberBadge(i + 1)
                        HSpace(14.dp)
                        Column(Modifier.weight(1f)) {
                            Text("${i + 1}-juz", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${sura.uzName} surasi, $ay-oyatdan", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("الجزء ${Uz.arabicNumber(i + 1)}", fontFamily = QuranFont, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun NumberBadge(n: Int) {
    Box(
        Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text("$n", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SuraRow(sura: Sura, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NumberBadge(sura.number)
        HSpace(14.dp)
        Column(Modifier.weight(1f)) {
            Text(sura.uzName, style = MaterialTheme.typography.titleMedium)
            Text(
                "${if (sura.meccan) "Makkiy" else "Madaniy"} • ${sura.count} oyat",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(sura.arName, fontFamily = QuranFont, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
    }
}

// ------------------------------ O'qish ekrani ------------------------------

fun ayahAnnotated(a: Ayah, tajweed: Boolean, dark: Boolean, numberColor: Color, withNumber: Boolean = true): AnnotatedString =
    buildAnnotatedString {
        append(a.text)
        if (tajweed) {
            val rules = TajweedRule.entries
            var i = 0
            val ann = a.ann
            while (i + 2 < ann.size) {
                val r = rules.getOrNull(ann[i + 2])
                if (r != null && ann[i] < ann[i + 1] && ann[i + 1] <= a.text.length) {
                    addStyle(SpanStyle(color = Color(if (dark) r.dark else r.light)), ann[i], ann[i + 1])
                }
                i += 3
            }
        }
        if (withNumber) {
            append(" ")
            withStyle(SpanStyle(color = numberColor)) { append("۝" + Uz.arabicNumber(a.number)) }
            append(" ")
        }
    }

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun ReaderScreen(app: HijriyApp, nav: NavHostController, suraNo: Int, startAyah: Int) {
    val quran by app.quran.collectAsStateWithLifecycle()
    val s by app.settings.state.collectAsStateWithLifecycle()
    val dark = LocalExtra.current.dark
    var fullscreen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showLegend by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { app.ensureQuran() }

    val view = LocalView.current
    val activity = LocalContext.current as? Activity
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    DisposableEffect(fullscreen) {
        val w = activity?.window
        if (w != null) {
            val c = WindowCompat.getInsetsController(w, view)
            if (fullscreen) {
                c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                c.hide(WindowInsetsCompat.Type.systemBars())
            } else c.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (w != null) WindowCompat.getInsetsController(w, view).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val q = quran
    if (q == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val sura = q.suras[(suraNo - 1).coerceIn(0, 113)]
    val numberColor = MaterialTheme.colorScheme.primary
    val chunk = 6
    // Yaxlit rejimda oyatlar guruhlanadi
    val groups = remember(sura, s.quranFlow) {
        if (s.quranFlow) sura.ayahs.chunked(chunk) else sura.ayahs.map { listOf(it) }
    }
    val startIndex = remember(sura.number, s.quranFlow) {
        val a = (startAyah - 1).coerceIn(0, sura.count - 1)
        if (a == 0) 0 else 1 + if (s.quranFlow) a / chunk else a
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    LaunchedEffect(sura.number, s.quranFlow) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(800)
            .collect { idx ->
                val ayah = if (idx <= 0) 1 else if (s.quranFlow) (idx - 1) * chunk + 1 else idx
                val cur = app.settings.value
                if (cur.lastSura != sura.number || cur.lastAyah != ayah) {
                    app.settings.update { it.copy(lastSura = sura.number, lastAyah = ayah.coerceIn(1, sura.count)) }
                }
            }
    }

    val arabicStyle = TextStyle(
        fontFamily = QuranFont,
        fontSize = s.quranFont.sp,
        lineHeight = (s.quranFont * 2.05f).sp,
        textAlign = TextAlign.Justify,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(if (dark) MaterialTheme.colorScheme.background else Color(0xFFFFFCF5))
    ) {
        Column(Modifier.fillMaxSize()) {
            if (!fullscreen) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga") }
                        Column(Modifier.weight(1f)) {
                            Text("${sura.number}. ${sura.uzName}", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(
                                "${if (sura.meccan) "Makkiy" else "Madaniy"} • ${sura.count} oyat • ${q.juzOf(sura.number, 1)}-juz",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilterChip(
                            selected = s.tajweed,
                            onClick = { app.settings.update { it.copy(tajweed = !it.tajweed) } },
                            label = { Text("Tajvid") }
                        )
                        IconButton(onClick = {
                            val first = listState.firstVisibleItemIndex
                            val ay = if (first <= 0) 1 else if (s.quranFlow) (first - 1) * chunk + 1 else first
                            val page = MushafRepo.load(app).pageOf(sura.number, ay.coerceIn(1, sura.count))
                            nav.go("mushaf/$page")
                        }) { Icon(Icons.AutoMirrored.Filled.MenuBook, "Mushaf ko'rinishi") }
                        IconButton(onClick = { showSettings = true }) { Icon(Icons.Filled.TextFields, "Ko'rinish") }
                        IconButton(onClick = { fullscreen = true }) { Icon(Icons.Filled.Fullscreen, "To'liq ekran") }
                    }
                }
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item(key = "head") { SuraHeader(sura, q.bismillah, s.tajweed, dark) }
                    itemsIndexed(groups, key = { i, _ -> "g$i" }) { _, g ->
                        val text = remember(g, s.tajweed, dark, numberColor) {
                            if (g.size == 1) ayahAnnotated(g[0], s.tajweed, dark, numberColor)
                            else buildAnnotatedString { g.forEach { append(ayahAnnotated(it, s.tajweed, dark, numberColor)) } }
                        }
                        Column(
                            Modifier.widthIn(max = 900.dp).fillMaxWidth()
                                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { fullscreen = !fullscreen }) }
                        ) {
                            Text(text, style = arabicStyle, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                            if (!s.quranFlow) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        }
                    }
                    item(key = "nav") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 20.dp).navigationBarsPadding(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (sura.number < 114) FilterChip(
                                selected = false,
                                onClick = { nav.navigate("reader/${sura.number + 1}") { popUpTo("quran") } },
                                label = { Text("← ${q.suras[sura.number].uzName}") }
                            ) else HSpace(1.dp)
                            if (sura.number > 1) FilterChip(
                                selected = false,
                                onClick = { nav.navigate("reader/${sura.number - 1}") { popUpTo("quran") } },
                                label = { Text("${q.suras[sura.number - 2].uzName} →") }
                            )
                        }
                    }
                }
            }
        }
        if (fullscreen) {
            IconButton(
                onClick = { fullscreen = false },
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(6.dp)
                    .clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            ) { Icon(Icons.Filled.FullscreenExit, "Chiqish") }
        }
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
                Text("Ko'rinish", style = MaterialTheme.typography.titleLarge)
                VSpace(12.dp)
                Text("Shrift o'lchami: ${s.quranFont.toInt()}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = s.quranFont, onValueChange = { v -> app.settings.update { it.copy(quranFont = v) } },
                    valueRange = 18f..56f, steps = 18
                )
                SettingRow(null, "Tajvid ranglari", "Qoidalar rang bilan ajratiladi", onClick = {
                    app.settings.update { it.copy(tajweed = !it.tajweed) }
                }) { Switch(checked = s.tajweed, onCheckedChange = { v -> app.settings.update { it.copy(tajweed = v) } }) }
                SettingRow(null, "Yaxlit matn", "Oyatlar sahifadagidek ketma-ket", onClick = {
                    app.settings.update { it.copy(quranFlow = !it.quranFlow) }
                }) { Switch(checked = s.quranFlow, onCheckedChange = { v -> app.settings.update { it.copy(quranFlow = v) } }) }
                SettingRow(null, "Tajvid ranglari izohi", onClick = { showSettings = false; showLegend = true })
                Text(
                    "Maslahat: ekranga ikki marta bosing — to'liq ekran rejimi. Telefonni yonboshlatsangiz, matn keng ekranga moslashadi.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
    if (showLegend) {
        ModalBottomSheet(onDismissRequest = { showLegend = false }) {
            LazyColumn(Modifier.padding(horizontal = 20.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                item { Text("Tajvid ranglari", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp)) }
                items(TajweedRule.legend) { r ->
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(Color(if (dark) r.dark else r.light)))
                        HSpace(14.dp)
                        Text(
                            when (r) {
                                TajweedRule.SILENT -> "O'qilmaydigan harflar (hamzai vasl, lomi shamsiya)"
                                TajweedRule.IDGHAAM_MUTAJANISAYN -> "Idg'omi mutajonisayn / mutaqoribayn"
                                else -> r.uz
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                item { VSpace(8.dp); Box(Modifier.navigationBarsPadding()) }
            }
        }
    }
}

@Composable
private fun SuraHeader(sura: Sura, bism: Ayah, tajweed: Boolean, dark: Boolean) {
    val p = LocalExtra.current.palette
    Column(Modifier.fillMaxWidth().widthIn(max = 900.dp).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(p.accent.copy(alpha = 0.12f))
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("سُورَةُ ${sura.arName}", fontFamily = QuranFont, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
        }
        if (sura.hasBismillah) {
            VSpace(10.dp)
            Text(
                ayahAnnotated(bism, tajweed, dark, Color.Unspecified, withNumber = false),
                fontFamily = QuranFont, fontSize = 26.sp, lineHeight = 52.sp,
                color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center
            )
        }
        VSpace(6.dp)
    }
}
