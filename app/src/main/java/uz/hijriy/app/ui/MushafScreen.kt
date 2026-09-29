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
import androidx.compose.material3.Text
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

private val AYAH_MARK = Regex("۝[٠-٩]+")

@Composable
fun MushafScreen(app: HijriyApp, nav: NavHostController, startPage: Int) {
    val ctx = LocalContext.current
    val quran by app.quran.collectAsStateWithLifecycle()
    val mushaf by produceState<Mushaf?>(null) { value = withContext(Dispatchers.Default) { MushafRepo.load(ctx) } }
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
    val paper = if (dark) MaterialTheme.colorScheme.background else Color(0xFFFFF8E8)

    Box(
        Modifier
            .fillMaxSize()
            .background(paper)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { idx ->
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    // Bosish sahifa ichida ushlanadi — varaqlagich uni "yutib" qo'ymaydi
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag("mushaf_page_${idx + 1}")
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                controls = !controls
                            }
                    ) {
                        MushafPageView(m.pages[idx], q, dark)
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

@Composable
private fun MushafPageView(page: MushafPage, q: Quran, dark: Boolean) {
    val density = LocalDensity.current
    val ink = MaterialTheme.colorScheme.onSurface
    val frame = MaterialTheme.colorScheme.primary
    val accent = LocalExtra.current.palette.accent
    BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp)) {
        val portrait = maxHeight >= maxWidth
        val innerW: Dp = maxWidth - 28.dp
        val headerH = 22.dp
        val footerH = 26.dp
        val availH: Dp = maxHeight - headerH - footerH - 24.dp
        val byWidth = with(density) { innerW.toPx() * 24f / 410f }
        val byHeight = with(density) { (availH / 15).toPx() / 1.78f }
        val fontPx = if (portrait) minOf(byWidth, byHeight) else byWidth
        val lineW: Dp = with(density) { (fontPx * 410f / 24f).toDp() }.coerceAtMost(innerW)
        val lineH: Dp = if (portrait) availH / 15 else with(density) { (fontPx * 1.9f).toDp() }
        val style = TextStyle(
            fontFamily = MushafFont,
            fontSize = with(density) { fontPx.toSp() },
            color = ink,
            textAlign = TextAlign.Center,
        )
        val scroll = rememberScrollState()
        Column(
            Modifier
                .fillMaxSize()
                .then(if (portrait) Modifier else Modifier.verticalScroll(scroll))
                .border(1.5.dp, frame.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                .padding(3.dp)
                .border(0.8.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Yuqori qator: sura nomi va juz
            val sura = q.suras[page.firstSura - 1]
            Row(Modifier.width(lineW).height(headerH), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "الجزء ${Uz.arabicNumber(q.juzOf(page.firstSura, page.firstAyah))}",
                    fontFamily = QuranFont, fontSize = 14.sp, color = frame
                )
                Box(Modifier.weight(1f))
                Text(sura.arName, fontFamily = QuranFont, fontSize = 14.sp, color = frame)
            }
            Column(
                Modifier.then(if (portrait) Modifier.weight(1f) else Modifier),
                verticalArrangement = if (page.number <= 2) Arrangement.Center else Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                page.lines.forEach { l ->
                    Box(Modifier.width(lineW).height(lineH), contentAlignment = Alignment.Center) {
                        when (l.type) {
                            2 -> SuraBanner(q.suras.getOrNull((l.text.toIntOrNull() ?: 1) - 1)?.arName ?: "", style, frame, accent, lineW)
                            else -> MushafTextLine(l, style, lineW, frame)
                        }
                    }
                }
            }
            // Pastki qator: sahifa raqami
            Box(Modifier.width(lineW).height(footerH), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(frame.copy(alpha = if (dark) 0.25f else 0.12f))
                        .padding(horizontal = 14.dp, vertical = 2.dp)
                ) { Text(Uz.arabicNumber(page.number), fontFamily = QuranFont, fontSize = 15.sp, color = frame) }
            }
        }
    }
}

@Composable
private fun MushafTextLine(l: MushafLine, style: TextStyle, lineW: Dp, markColor: Color) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val text: AnnotatedString = remember(l.text, markColor) {
        buildAnnotatedString {
            append(l.text)
            AYAH_MARK.findAll(l.text).forEach { addStyle(SpanStyle(color = markColor), it.range.first, it.range.last + 1) }
        }
    }
    val natural = remember(text, style) {
        measurer.measure(text, style, softWrap = false, maxLines = 1).size.width.toFloat().coerceAtLeast(1f)
    }
    val target = with(density) { lineW.toPx() }
    val scale = if (l.type == 0) target / natural else minOf(1f, target / natural)
    Text(
        text, style = style, softWrap = false, maxLines = 1,
        modifier = Modifier.wrapContentWidth(unbounded = true).graphicsLayer { scaleX = scale }
    )
}

@Composable
private fun SuraBanner(arName: String, style: TextStyle, frame: Color, accent: Color, lineW: Dp) {
    Box(
        Modifier
            .width(lineW)
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.2.dp, frame.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("سُورَةُ $arName", style = style.copy(fontFamily = QuranFont, fontSize = style.fontSize * 0.85f, color = frame))
    }
}
