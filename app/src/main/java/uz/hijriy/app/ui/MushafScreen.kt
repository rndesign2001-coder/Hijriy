package uz.hijriy.app.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.core.Uz
import uz.hijriy.app.data.Mushaf
import uz.hijriy.app.data.MushafLine
import uz.hijriy.app.data.MushafPage
import uz.hijriy.app.data.MushafRepo
import uz.hijriy.app.data.Quran
import uz.hijriy.app.ui.theme.LocalExtra
import uz.hijriy.app.ui.theme.MushafFont
import uz.hijriy.app.ui.theme.QuranFont


@Composable
fun MushafScreen(app: HijriyApp, nav: NavHostController, startPage: Int) {
    val ctx = LocalContext.current
    val quran by app.quran.collectAsStateWithLifecycle()
    val mushaf by produceState<Mushaf?>(null) { value = withContext(Dispatchers.Default) { MushafRepo.load(ctx) } }
    val taj by produceState<List<List<IntArray>>?>(null) { value = withContext(Dispatchers.Default) { runCatching { uz.hijriy.app.data.MushafTajweed.load(ctx) }.getOrNull() } }
    val st by app.settings.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { app.ensureQuran() }
    var controls by remember { mutableStateOf(false) }

    // Doim yoqilgan ekran va to'liq ekran rejimi (panel ochilganda tizim panellari ko'rinadi)
    val view = LocalView.current
    val activity = ctx as? Activity
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    DisposableEffect(controls) {
        val w = activity?.window
        if (w != null) {
            val c = WindowCompat.getInsetsController(w, view)
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (controls) c.show(WindowInsetsCompat.Type.systemBars()) else c.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (w != null) WindowCompat.getInsetsController(w, view).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val m = mushaf
    val q = quran
    if (m == null || q == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val pager = rememberPagerState(initialPage = (startPage - 1).coerceIn(0, m.pageCount - 1)) { m.pageCount }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager) {
        snapshotFlow { pager.settledPage }.distinctUntilChanged().collect { idx ->
            val p = m.pages[idx]
            app.settings.update { it.copy(mushafPage = idx + 1, lastSura = p.firstSura, lastAyah = p.firstAyah) }
        }
    }
    val dark = LocalExtra.current.dark
    val paper = if (dark) Color(0xFF14130F) else Color(0xFFF3EBD2)

    Box(
        Modifier
            .fillMaxSize()
            .background(paper)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { idx ->
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    // Kitob varag'i effekti: sahifalar joyida turadi, joriy varaq o'ng (muqova) cheti atrofida ag'dariladi
                    val d = idx - (pager.currentPage + pager.currentPageOffsetFraction)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .zIndex(if (d <= 0f) 1f else 0f)
                            .graphicsLayer {
                                val dd = d.coerceIn(-1f, 1f)
                                translationX = size.width * dd
                                cameraDistance = 18f * density
                                if (dd < 0f) {
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
                                    rotationY = dd * 100f
                                    alpha = if (dd < -0.92f) 0f else 1f
                                }
                            }
                            .drawWithContent {
                                drawContent()
                                val dd = d.coerceIn(-1f, 1f)
                                // varaq soyasi
                                if (dd > 0f) drawRect(Color.Black.copy(alpha = 0.35f * dd))
                                if (dd < 0f) drawRect(
                                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f * -dd)))
                                )
                            }
                            .background(paper)
                            .testTag("mushaf_page_${idx + 1}")
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                controls = !controls
                            }
                    ) {
                        MushafPageView(m.pages[idx], q, dark, if (st.tajweed) taj?.getOrNull(idx) else null)
                    }
                }
            }
        }

        val cur = m.pages[pager.currentPage]
        AnimatedVisibility(controls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter)) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f), shadowElevation = 4.dp) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga") }
                    Column(Modifier.weight(1f)) {
                        val s = q.suras[cur.firstSura - 1]
                        Text("${s.number}. ${s.uzName}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${cur.number}-sahifa • ${q.juzOf(cur.firstSura, cur.firstAyah)}-juz",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val bms by app.bookmarks.list.collectAsStateWithLifecycle()
                    val marked = bms.any { it.page == cur.number }
                    IconButton(onClick = { app.bookmarks.togglePage(cur.number, cur.firstSura, cur.firstAyah) }) {
                        Icon(if (marked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder, "Xatcho'p", tint = if (marked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { nav.go("reader/${cur.firstSura}?ayah=${cur.firstAyah}") }) {
                        Icon(Icons.AutoMirrored.Filled.Notes, "Matn ko'rinishi")
                    }
                }
            }
        }
        AnimatedVisibility(controls, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomCenter)) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.97f), shadowElevation = 4.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    var slider by remember(pager.currentPage) { mutableFloatStateOf((pager.currentPage + 1).toFloat()) }
                    Text(
                        "Sahifa: ${slider.toInt()} / ${m.pageCount}",
                        style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Slider(
                            value = slider, onValueChange = { slider = it },
                            onValueChangeFinished = { scope.launch { pager.scrollToPage(slider.toInt() - 1) } },
                            valueRange = 1f..m.pageCount.toFloat()
                        )
                    }
                    Text(
                        "Varaqlash uchun suring • Panelni yashirish uchun bir marta bosing",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

/** Klassik tajvidli mushaf ranglari (Dor al-Ma'rifa uslubiga yaqin). */
fun mushafTajColor(rule: Int, dark: Boolean): Color? {
    val r = uz.hijriy.app.data.TajweedRule.entries.getOrNull(rule) ?: return null
    return when (r) {
        uz.hijriy.app.data.TajweedRule.HAMZAT_WASL, uz.hijriy.app.data.TajweedRule.LAM_SHAMSIYYAH, uz.hijriy.app.data.TajweedRule.SILENT,
        uz.hijriy.app.data.TajweedRule.IDGHAAM_NO_GHUNNAH, uz.hijriy.app.data.TajweedRule.IDGHAAM_MUTAJANISAYN,
        uz.hijriy.app.data.TajweedRule.IDGHAAM_MUTAQARIBAYN -> if (dark) Color(0xFF8E9590) else Color(0xFF9AA0A6)
        uz.hijriy.app.data.TajweedRule.MADD_2 -> if (dark) Color(0xFFD9B45A) else Color(0xFFB8892B)
        uz.hijriy.app.data.TajweedRule.MADD_246, uz.hijriy.app.data.TajweedRule.MADD_MUNFASIL -> if (dark) Color(0xFFFF9F5A) else Color(0xFFE0701B)
        uz.hijriy.app.data.TajweedRule.MADD_MUTTASIL -> if (dark) Color(0xFFFF6FA8) else Color(0xFFD6197A)
        uz.hijriy.app.data.TajweedRule.MADD_6 -> if (dark) Color(0xFFFF5A5A) else Color(0xFFB3001B)
        uz.hijriy.app.data.TajweedRule.GHUNNAH, uz.hijriy.app.data.TajweedRule.IKHFA, uz.hijriy.app.data.TajweedRule.IKHFA_SHAFAWI,
        uz.hijriy.app.data.TajweedRule.IQLAB, uz.hijriy.app.data.TajweedRule.IDGHAAM_GHUNNAH, uz.hijriy.app.data.TajweedRule.IDGHAAM_SHAFAWI ->
            if (dark) Color(0xFF4FD08C) else Color(0xFF13894A)
        uz.hijriy.app.data.TajweedRule.QALQALAH -> if (dark) Color(0xFF6CC4FF) else Color(0xFF1C8AD1)
    }
}

private val mushafLegend = listOf(
    Color(0xFFB3001B) to "Madd 6", Color(0xFFD6197A) to "Madd 4–5", Color(0xFFE0701B) to "Madd 2–4–6",
    Color(0xFFB8892B) to "Madd 2", Color(0xFF13894A) to "G'unna, ixfo", Color(0xFF9AA0A6) to "Idg'om, o'qilmaydi",
    Color(0xFF1C8AD1) to "Qalqala",
)

@Composable
private fun MushafPageView(page: MushafPage, q: Quran, dark: Boolean, spans: List<IntArray>?) {
    val density = LocalDensity.current
    val ink = if (dark) Color(0xFFEDE6D2) else Color(0xFF1A140A)
    val band = if (dark) Color(0xFF1F5C55) else Color(0xFF1E6F66)
    val gold = Color(0xFFC9A24A)
    val sheet = if (dark) Color(0xFF1C1A15) else Color(0xFFFFFBEA)
    val inline = rememberAyahInline(if (dark) gold else band)
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 4.dp)) {
        val portrait = maxHeight >= maxWidth
        val frameW = 9.dp
        val innerW: Dp = maxWidth - frameW * 2 - 16.dp
        val headerH = 30.dp
        val footerH = if (spans != null) 44.dp else 8.dp
        val numberH = 30.dp
        val availH: Dp = maxHeight - headerH - footerH - numberH - frameW * 2 - 18.dp
        val byWidth = with(density) { innerW.toPx() * 24f / 410f }
        val byHeight = with(density) { (availH / 15).toPx() / 1.78f }
        val fontPx = if (portrait) minOf(byWidth, byHeight) else byWidth
        val lineW: Dp = with(density) { (fontPx * 410f / 24f).toDp() }.coerceAtMost(innerW)
        val lineH: Dp = if (portrait) availH / 15 else with(density) { (fontPx * 1.9f).toDp() }
        val style = TextStyle(fontFamily = MushafFont, fontSize = with(density) { fontPx.toSp() }, color = ink, textAlign = TextAlign.Center)
        val scroll = rememberScrollState()
        Column(
            Modifier.fillMaxSize().then(if (portrait) Modifier else Modifier.verticalScroll(scroll)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val sura = q.suras[page.firstSura - 1]
            // Bezakli ramka: tashqi oltin chiziq, naqshli feruza tasma, ichki chiziqlar
            Column(
                Modifier.then(if (portrait) Modifier.weight(1f) else Modifier).fillMaxWidth()
                    .drawBehind {
                        val fw = frameW.toPx()
                        drawRect(band)
                        clipRect(0f, 0f, size.width, size.height) {
                            drawGirih(gold.copy(alpha = 0.55f), fw * 2.2f, 0.7.dp.toPx())
                        }
                        drawRect(gold, style = Stroke(1.6.dp.toPx()))
                        drawRect(sheet, topLeft = androidx.compose.ui.geometry.Offset(fw, fw), size = androidx.compose.ui.geometry.Size(size.width - 2 * fw, size.height - 2 * fw))
                        drawRect(gold, topLeft = androidx.compose.ui.geometry.Offset(fw, fw), size = androidx.compose.ui.geometry.Size(size.width - 2 * fw, size.height - 2 * fw), style = Stroke(1.2.dp.toPx()))
                        val g = fw + 3.dp.toPx()
                        drawRect(band.copy(alpha = 0.6f), topLeft = androidx.compose.ui.geometry.Offset(g, g), size = androidx.compose.ui.geometry.Size(size.width - 2 * g, size.height - 2 * g), style = Stroke(0.7.dp.toPx()))
                        // burchak yulduzlari
                        listOf(androidx.compose.ui.geometry.Offset(fw / 2, fw / 2), androidx.compose.ui.geometry.Offset(size.width - fw / 2, fw / 2),
                            androidx.compose.ui.geometry.Offset(fw / 2, size.height - fw / 2), androidx.compose.ui.geometry.Offset(size.width - fw / 2, size.height - fw / 2)).forEach {
                            drawPath(starPath(it.x, it.y, fw * 0.9f), gold)
                        }
                    }
                    .padding(frameW + 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Yuqori qator: juz va sura kartushlari
                Row(Modifier.width(lineW).height(headerH), verticalAlignment = Alignment.CenterVertically) {
                    Cartouche("الجزء ${Uz.arabicNumber(q.juzOf(page.firstSura, page.firstAyah))}", band, gold)
                    Box(Modifier.weight(1f))
                    Cartouche("سورة ${sura.arName}", band, gold)
                }
                Column(
                    Modifier.then(if (portrait) Modifier.weight(1f) else Modifier),
                    verticalArrangement = if (page.number <= 2) Arrangement.Center else Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    page.lines.forEachIndexed { li, l ->
                        Box(Modifier.width(lineW).height(lineH), contentAlignment = Alignment.Center) {
                            when (l.type) {
                                2 -> SuraBanner(q.suras.getOrNull((l.text.toIntOrNull() ?: 1) - 1)?.arName ?: "", style, band, gold, lineW)
                                else -> MushafTextLine(l, style, lineW, inline, spans?.getOrNull(li), dark)
                            }
                        }
                    }
                }
                if (spans != null) {
                    // Tajvid izohi
                    Column(Modifier.width(lineW).height(footerH).padding(top = 4.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                        mushafLegend.chunked(4).forEach { row ->
                            Row(Modifier.fillMaxWidth().height(17.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                row.forEach { (c, t) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(7.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c))
                                        Box(Modifier.width(3.dp))
                                        androidx.compose.material3.Text(t, fontSize = 8.5.sp, lineHeight = 10.sp, color = ink.copy(alpha = 0.75f), maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Sahifa raqami
            Box(Modifier.fillMaxWidth().height(numberH), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Text(Uz.arabicNumber(page.number), fontFamily = QuranFont, fontSize = 14.sp, lineHeight = 18.sp, color = if (dark) gold else band)
            }
        }
    }
}

@Composable
private fun Cartouche(text: String, band: Color, gold: Color) {
    Box(
        Modifier.height(26.dp).clip(RoundedCornerShape(50)).background(band.copy(alpha = 0.10f))
            .border(1.dp, gold, RoundedCornerShape(50)).padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text, fontFamily = QuranFont, fontSize = 11.sp, color = band, maxLines = 1,
            style = TextStyle(lineHeight = 14.sp, lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.Both))
        )
    }
}

@Composable
private fun MushafTextLine(
    l: MushafLine, style: TextStyle, lineW: Dp, inline: Map<String, androidx.compose.foundation.text.InlineTextContent>,
    spans: IntArray?, dark: Boolean,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val built = remember(l.text, spans, dark) {
        val ranges = mutableListOf<AnnotatedString.Range<androidx.compose.ui.text.Placeholder>>()
        withAyahMarksColored(l.text, spans, { mushafTajColor(it, dark) }, ranges) to ranges.toList()
    }
    val text = built.first
    val natural = remember(text, style) {
        measurer.measure(text, style, softWrap = false, maxLines = 1, placeholders = built.second).size.width.toFloat().coerceAtLeast(1f)
    }
    val target = with(density) { lineW.toPx() }
    val scale = if (l.type == 0) target / natural else minOf(1f, target / natural)
    Text(
        text, style = style, softWrap = false, maxLines = 1, inlineContent = inline,
        modifier = Modifier.wrapContentWidth(unbounded = true).graphicsLayer { scaleX = scale }
    )
}

@Composable
private fun SuraBanner(arName: String, style: TextStyle, band: Color, gold: Color, lineW: Dp) {
    Box(
        Modifier
            .width(lineW)
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Brush.horizontalGradient(listOf(band, band.copy(alpha = 0.85f), band)))
            .drawBehind { drawGirih(gold.copy(alpha = 0.35f), size.height * 0.9f, 0.6.dp.toPx()) }
            .border(1.5.dp, gold, RoundedCornerShape(6.dp))
            .padding(vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFFBEA)).border(1.dp, gold, RoundedCornerShape(50)).padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("سُورَةُ $arName", style = style.copy(fontFamily = QuranFont, fontSize = style.fontSize * 0.8f, color = band))
        }
    }
}
