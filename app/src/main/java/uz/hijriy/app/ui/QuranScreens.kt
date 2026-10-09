package uz.hijriy.app.ui

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import uz.hijriy.app.data.QuranAudio
import uz.hijriy.app.data.Reciters
import uz.hijriy.app.data.TranslationRepo
import uz.hijriy.app.data.UzTranslit
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
import androidx.compose.foundation.layout.width
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
    var listTab by remember { mutableStateOf(0) }
    val bookmarks by app.bookmarks.list.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { app.ensureQuran() }
    val ctx = LocalContext.current
    val mushaf by produceState<Mushaf?>(null) { value = withContext(Dispatchers.Default) { MushafRepo.load(ctx) } }
    fun open(sura: Int, ayah: Int = 1) {
        val m = mushaf
        if (s.quranMushaf && m != null) nav.go("mushaf/${m.pageOf(sura, ayah)}") else nav.go("reader/$sura?ayah=$ayah")
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Qur'oni Karim", "114 sura • 6236 oyat • 604 sahifa", actions = {
            IconButton(onClick = { nav.go("search") }) { Icon(Icons.Filled.Search, "Oyatlardan qidirish") }
        })
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
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(selected = listTab == 0, onClick = { listTab = 0 }, label = { Text("Suralar") })
            FilterChip(selected = listTab == 1, onClick = { listTab = 1 }, label = { Text("Juzlar") })
            FilterChip(selected = listTab == 2, onClick = { listTab = 2 }, label = { Text("🔖 Xatcho'plar") })
            Box(Modifier.width(8.dp))
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
            if (listTab == 2) {
                if (bookmarks.isEmpty()) item {
                    Text(
                        "Hozircha xatcho'p yo'q. O'qish ekranida oyatni uzoq bosing yoki mushafda 🔖 tugmasini bosing.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
                items(bookmarks, key = { "${it.sura}/${it.ayah}/${it.page}" }) { b ->
                    val bs = q.suras[b.sura - 1]
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .clickable { if (b.isPage) nav.go("mushaf/${b.page}") else nav.go("reader/${b.sura}?ayah=${b.ayah}") }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (b.isPage) "📄" else "🔖", fontSize = 24.sp)
                        HSpace(12.dp)
                        Column(Modifier.weight(1f)) {
                            Text(if (b.isPage) "Mushaf, ${b.page}-sahifa" else "${bs.uzName} surasi, ${b.ayah}-oyat", style = MaterialTheme.typography.titleMedium)
                            Text(bs.ayahs[b.ayah - 1].text, fontFamily = QuranFont, fontSize = 18.sp, maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { app.bookmarks.remove(b) }) { Icon(Icons.Filled.Close, "O'chirish") }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            } else if (listTab == 0) {
                items(filtered, key = { it.number }) { sura ->
                    SuraRow(sura) { open(sura.number) }
                    VSpace(10.dp)
                }
            } else {
                itemsIndexed(q.juz) { i, (su, ay) ->
                    val sura = q.suras[su - 1]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable { open(su, ay) }
                            .padding(vertical = 16.dp, horizontal = 14.dp),
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
                        Text("الجزء ${Uz.arabicNumber(i + 1)}", fontFamily = QuranFont, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    VSpace(10.dp)
                }
            }
        }
    }
}

@Composable
private fun NumberBadge(n: Int) {
    val gold = Color(0xFFD4AF37)
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            .border(2.dp, gold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("$n", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = gold)
    }
}

@Composable
private fun SuraRow(sura: Sura, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NumberBadge(sura.number)
        HSpace(16.dp)
        Column(Modifier.weight(1f)) {
            Text(sura.uzName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "${if (sura.meccan) "Makka" else "Madina"}, ${sura.count} oyat",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(sura.arName, fontFamily = QuranFont, fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
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
            appendAyahMark(a.number)
            append(" ")
        }
    }

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(app: HijriyApp, nav: NavHostController, suraNo: Int, startAyah: Int) {
    val quran by app.quran.collectAsStateWithLifecycle()
    val s by app.settings.state.collectAsStateWithLifecycle()
    val dark = LocalExtra.current.dark
    var fullscreen by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showLegend by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { app.ensureQuran() }
    val play by QuranAudio.state.collectAsStateWithLifecycle()
    val trState by TranslationRepo.state.collectAsStateWithLifecycle()
    val bookmarks by app.bookmarks.list.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    DisposableEffect(Unit) { onDispose { QuranAudio.stop() } }

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

    LaunchedEffect(play.ayah, play.sura) {
        if (play.active && play.sura == sura.number && play.ayah > 0) {
            val idx = 1 + if (s.quranFlow) (play.ayah - 1) / chunk else play.ayah - 1
            runCatching { listState.animateScrollToItem(idx) }
        }
    }

    val ayahInline = rememberAyahInline(numberColor)
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
                                "${if (sura.meccan) "Makka" else "Madina"}, ${sura.count} oyat",
                                maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        IconButton(onClick = {
                            if (play.active && play.sura == sura.number) QuranAudio.togglePause()
                            else {
                                val first = listState.firstVisibleItemIndex
                                val ay = if (first <= 0 || s.quranFlow) 1 else first
                                QuranAudio.play(sura.number, ay, sura.count, Reciters[s.reciter.coerceIn(0, Reciters.lastIndex)])
                            }
                        }) {
                            Icon(if (play.playing && play.sura == sura.number) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Tilovat")
                        }
                        IconButton(onClick = { showSettings = true }) { Icon(Icons.Filled.TextFields, "Ko'rinish") }
                        IconButton(onClick = { fullscreen = true }) { Icon(Icons.Filled.Fullscreen, "To'liq ekran") }
                    }
                }
                if (!s.quranFlow) ReaderModeBar(
                    reading = s.quranReading, translation = s.translation > 0,
                    onArabicOnly = { app.settings.update { it.copy(quranReading = false, translation = 0) } },
                    onReading = { app.settings.update { it.copy(quranReading = !it.quranReading) } },
                    onTranslation = {
                        app.settings.update { it.copy(translation = if (it.translation > 0) 0 else if (it.script == 1) 2 else 1) }
                        if (trState !is TranslationRepo.State.Ready && trState !is TranslationRepo.State.Downloading) scope.launch { TranslationRepo.download(ctx) }
                    },
                    onSmaller = { app.settings.update { it.copy(quranFont = (it.quranFont - 2f).coerceAtLeast(18f)) } },
                    onBigger = { app.settings.update { it.copy(quranFont = (it.quranFont + 2f).coerceAtMost(56f)) } },
                )
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = if (s.quranFlow) 18.dp else 12.dp, end = if (s.quranFlow) 18.dp else 12.dp, top = 8.dp, bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (s.quranFlow) 0.dp else 12.dp),
                ) {
                    item(key = "head") { SuraHeader(sura, q.bismillah, s.tajweed, dark, s.quranReading && !s.quranFlow) }
                    itemsIndexed(groups, key = { i, _ -> "g$i" }) { _, g ->
                        val text = remember(g, s.tajweed, dark, numberColor, s.quranFlow) {
                            if (g.size == 1) ayahAnnotated(g[0], s.tajweed, dark, numberColor, withNumber = s.quranFlow)
                            else buildAnnotatedString { g.forEach { append(ayahAnnotated(it, s.tajweed, dark, numberColor)) } }
                        }
                        val playingHere = play.active && play.sura == sura.number && g.any { it.number == play.ayah }
                        val marked = !s.quranFlow && bookmarks.any { !it.isPage && it.sura == sura.number && it.ayah == g[0].number }
                        val hl by androidx.compose.animation.animateColorAsState(
                            if (playingHere) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent, label = "hl"
                        )
                        val toggleMark = {
                            app.bookmarks.toggleAyah(sura.number, g[0].number)
                            android.widget.Toast.makeText(ctx, uiText(ctx, if (app.bookmarks.hasAyah(sura.number, g[0].number)) "🔖 ${sura.uzName} ${g[0].number}-oyat xatcho'pga qo'shildi" else "Xatcho'p olib tashlandi"),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        if (s.quranFlow) {
                            Column(
                                Modifier.widthIn(max = 900.dp).fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(hl)
                                    .combinedClickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null, onClick = {}, onDoubleClick = { fullscreen = !fullscreen },
                                    )
                                    .padding(horizontal = 6.dp)
                            ) {
                                Text(text, style = arabicStyle, inlineContent = ayahInline, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                            }
                        } else {
                            val a = g[0]
                            val trList = (trState as? TranslationRepo.State.Ready)?.suras?.getOrNull(sura.number - 1)
                            val trText = if (s.translation > 0 && trList != null) trList.getOrNull(a.number - 1)?.let { raw ->
                                if (s.translation == 1) UzTranslit.toLatin(raw) else raw
                            } else null
                            val rd = if (s.quranReading) remember(a) { uz.hijriy.app.core.Translit.ayah(a.text) } else null
                            val cyr = LocalCyr.current
                            AyahCard(
                                number = a.number, arabic = text, arabicStyle = arabicStyle, inline = ayahInline,
                                reading = rd, translation = trText, fontSize = s.quranFont, marked = marked,
                                playing = play.playing && playingHere, highlight = hl,
                                onPlay = {
                                    if (playingHere && play.active) QuranAudio.togglePause()
                                    else QuranAudio.play(sura.number, a.number, sura.count, Reciters[s.reciter.coerceIn(0, Reciters.lastIndex)])
                                },
                                onCopy = {
                                    val body = ayahShareText(sura, a, rd, trText, cyr)
                                    (ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                                        .setPrimaryClip(android.content.ClipData.newPlainText("oyat", body))
                                    android.widget.Toast.makeText(ctx, uiText(ctx, "Nusxa olindi"), android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onShare = {
                                    val body = ayahShareText(sura, a, rd, trText, cyr)
                                    val i = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
                                        .putExtra(android.content.Intent.EXTRA_TEXT, body)
                                    ctx.startActivity(android.content.Intent.createChooser(i, uiText(ctx, "Ulashish")))
                                },
                                onBookmark = toggleMark,
                                onDoubleTap = { fullscreen = !fullscreen },
                            )
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
        if (play.active && play.sura == sura.number) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(12.dp).fillMaxWidth()
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Reciters[s.reciter.coerceIn(0, Reciters.lastIndex)].name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                        Text(
                            when {
                                play.error != null -> play.error!!
                                play.loading -> "Yuklanmoqda…"
                                play.ayah == 0 -> "Bismillah"
                                else -> "${sura.uzName}, ${play.ayah}-oyat"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { QuranAudio.prev() }) { Icon(Icons.Filled.SkipPrevious, "Oldingi") }
                    IconButton(onClick = { QuranAudio.togglePause() }) {
                        if (play.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        else Icon(if (play.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Ijro")
                    }
                    IconButton(onClick = { QuranAudio.next() }) { Icon(Icons.Filled.SkipNext, "Keyingi") }
                    IconButton(onClick = { QuranAudio.stop() }) { Icon(Icons.Filled.Close, "To'xtatish") }
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
                SettingRow(null, "O'qilishi (lotin / kirill)", "Har oyat ostida talaffuzi — yozuv sozlamasiga mos", onClick = {
                    app.settings.update { it.copy(quranReading = !it.quranReading) }
                }) { Switch(checked = s.quranReading, onCheckedChange = { v -> app.settings.update { it.copy(quranReading = v) } }) }
                Text(
                    "O'qilishi tajvid qoidalari asosida taxminiy berilgan. To'g'ri talaffuzni ustozdan o'rganing.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )
                SettingRow(null, "Tajvid ranglari izohi", onClick = { showSettings = false; showLegend = true })
                VSpace(8.dp)
                Text("O'zbekcha tarjima (Muhammad Sodiq Muhammad Yusuf)", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Yo'q", "Lotin", "Kirill").forEachIndexed { i, t ->
                        FilterChip(selected = s.translation == i, onClick = { app.settings.update { it.copy(translation = i) } }, label = { Text(t) })
                    }
                }
                when (val st = trState) {
                    is TranslationRepo.State.Ready -> Text("✓ Tarjima telefonda saqlangan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    is TranslationRepo.State.Downloading -> {
                        Text("Yuklanmoqda… ${st.percent}%", style = MaterialTheme.typography.bodySmall)
                        androidx.compose.material3.LinearProgressIndicator(progress = { st.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    }
                    else -> {
                        if (st is TranslationRepo.State.Failed) Text(st.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        androidx.compose.material3.OutlinedButton(onClick = { scope.launch { TranslationRepo.download(ctx) } }) {
                            Text("Tarjimani yuklab olish (~4 MB, bir marta)")
                        }
                    }
                }
                VSpace(10.dp)
                Text("Qori (tilovat internet orqali)", style = MaterialTheme.typography.titleSmall)
                Reciters.forEachIndexed { i, r ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .clickable { app.settings.update { it.copy(reciter = i) }; if (play.active) QuranAudio.stop() }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(selected = s.reciter == i, onClick = { app.settings.update { it.copy(reciter = i) } })
                        Text(r.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    "Maslahat: oyatga ikki marta bosing — to'liq ekran; uzoq bosing — xatcho'p. Telefonni yonboshlatsangiz, matn keng ekranga moslashadi.",
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
private fun SuraHeader(sura: Sura, bism: Ayah, tajweed: Boolean, dark: Boolean, reading: Boolean = false) {
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
            if (reading) {
                val rd = remember(bism) { uz.hijriy.app.core.Translit.ayah(bism.text) }
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { ReadingText(rd) }
            }
        }
        VSpace(6.dp)
    }
}


private fun ayahShareText(sura: Sura, a: Ayah, rd: uz.hijriy.app.core.Translit.Result?, tr: String?, cyr: Boolean): String = buildString {
    append(a.text).append(" ﴿").append(a.number).append("﴾")
    if (rd != null) append("\n\n").append(if (cyr) rd.cyrillic else rd.latin)
    if (tr != null) append("\n\n").append(tr)
    append("\n\n— ").append(sura.uzName).append(", ").append(a.number).append(if (cyr) "-оят" else "-oyat")
}

/** Ko'rinish tanlovi: faqat arabcha / o'qilishi / tarjima va shrift o'lchami. */
@Composable
private fun ReaderModeBar(
    reading: Boolean, translation: Boolean,
    onArabicOnly: () -> Unit, onReading: () -> Unit, onTranslation: () -> Unit,
    onSmaller: () -> Unit, onBigger: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)
            .horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp).padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(selected = !reading && !translation, onClick = onArabicOnly, label = { Text("Arabcha") })
        FilterChip(selected = reading, onClick = onReading, label = { Text("O'qilishi") })
        FilterChip(selected = translation, onClick = onTranslation, label = { Text("Tarjima") })
        FontStepButton("A−", "Kichraytirish", onSmaller)
        FontStepButton("A+", "Kattalashtirish", onBigger)
    }
}

@Composable
private fun FontStepButton(label: String, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(width = 44.dp, height = 34.dp).clip(RoundedCornerShape(10.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .clickable(onClickLabel = desc, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { androidx.compose.material3.Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
}

/** Bir oyat kartasi: raqam, amallar, arabcha matn, o'qilishi va tarjimasi. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AyahCard(
    number: Int, arabic: AnnotatedString, arabicStyle: TextStyle, inline: Map<String, androidx.compose.foundation.text.InlineTextContent>,
    reading: uz.hijriy.app.core.Translit.Result?, translation: String?, fontSize: Float,
    marked: Boolean, playing: Boolean, highlight: Color,
    onPlay: () -> Unit, onCopy: () -> Unit, onShare: () -> Unit, onBookmark: () -> Unit, onDoubleTap: () -> Unit,
) {
    val gold = Color(0xFFD4AF37)
    val cyr = LocalCyr.current
    val sub = (fontSize * 0.62f).coerceIn(14f, 30f)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            Modifier.widthIn(max = 900.dp).fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .background(highlight)
                .combinedClickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null, onClick = {}, onDoubleClick = onDoubleTap, onLongClick = onBookmark,
                )
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 16.dp)
                .semantics(mergeDescendants = false) { testTag = "ayah_$number" }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(2.dp, gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) { androidx.compose.material3.Text("$number", color = gold, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall) }
                Box(Modifier.weight(1f))
                IconButton(onClick = onPlay) { Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Oyat tilovati", tint = gold) }
                IconButton(onClick = onCopy) { Icon(Icons.Filled.ContentCopy, "Nusxa olish", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onShare) { Icon(Icons.Filled.Share, "Ulashish", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = onBookmark) {
                    Icon(if (marked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Xatcho'p", tint = gold)
                }
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    arabic, style = arabicStyle.copy(textAlign = TextAlign.Start), inlineContent = inline,
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 6.dp, bottom = 4.dp)
                )
            }
            if (reading != null) {
                androidx.compose.material3.Text(
                    if (cyr) reading.cyrillic else reading.latin,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = sub.sp, lineHeight = (sub * 1.45f).sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp, top = 6.dp)
                )
            }
            if (translation != null) {
                androidx.compose.material3.Text(
                    translation,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = sub.sp, lineHeight = (sub * 1.45f).sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    modifier = Modifier.padding(end = 8.dp, top = 8.dp)
                )
            }
        }
    }
}
