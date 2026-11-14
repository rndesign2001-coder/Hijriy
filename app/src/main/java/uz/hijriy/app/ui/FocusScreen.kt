package uz.hijriy.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.core.Translit
import uz.hijriy.app.data.QuranAudio
import uz.hijriy.app.data.Reciters
import uz.hijriy.app.data.TranslationRepo
import uz.hijriy.app.data.UzTranslit
import uz.hijriy.app.ui.theme.QuranFont
import uz.hijriy.app.ui.theme.TitleFont

private val FocusGold = Color(0xFFE9C46A)

/** Hifz (yodlash) rejimi: ekranda bitta oyat — yirik harf, tajvid ranglari, o'qilishi, tarjima, takrorlab tinglash va yodlash. */
@Composable
fun FocusScreen(app: HijriyApp, nav: NavHostController, suraNo: Int, startAyah: Int) {
    val quran by app.quran.collectAsStateWithLifecycle()
    val s by app.settings.state.collectAsStateWithLifecycle()
    val play by QuranAudio.state.collectAsStateWithLifecycle()
    var showReciter by remember { mutableStateOf(false) }
    if (showReciter) ReciterSheet(app) { showReciter = false }
    val trState by TranslationRepo.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { app.ensureQuran() }
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    val q = quran
    var ayah by remember { mutableIntStateOf(startAyah) }
    var hide by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    val bg = Brush.radialGradient(listOf(Color(0xFF12302A), Color(0xFF081613)), radius = 1600f)
    Box(Modifier.fillMaxSize().background(Color(0xFF081613)).background(bg).drawBehind {
        drawGirih(Color.White.copy(alpha = 0.03f), 70.dp.toPx(), 0.8.dp.toPx())
    }) {
        if (q == null) { CircularProgressIndicator(Modifier.align(Alignment.Center)); return@Box }
        val sura = q.suras[suraNo - 1]
        ayah = ayah.coerceIn(1, sura.ayahs.size)
        // Tinglanayotgan oyatga ergashish
        LaunchedEffect(play.ayah, play.sura) { if (play.active && play.sura == suraNo && play.ayah >= 1) ayah = play.ayah }
        LaunchedEffect(ayah) { reveal = false; app.settings.update { it.copy(lastSura = suraNo, lastAyah = ayah) } }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga", tint = Color.White) }
                Column(Modifier.weight(1f)) {
                    Text("${sura.number}. ${sura.uzName}", color = Color.White, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("$ayah / ${sura.ayahs.size}-oyat", color = Color(0xFF9FB5AC), style = MaterialTheme.typography.labelMedium)
                }
                Pill(if (hide) "Yodlash: yoqiq" else "Yodlash", hide) { hide = !hide }
                Box(Modifier.size(6.dp))
                Pill(if (s.tajweed) "Tajvid" else "Tajvidsiz", s.tajweed) { app.settings.update { it.copy(tajweed = !it.tajweed) } }
                Box(Modifier.size(8.dp))
            }
            // Oyat (suriladi)
            Box(
                Modifier.weight(1f).fillMaxWidth().pointerInput(sura.number) {
                    var dx = 0f
                    detectHorizontalDragGestures(onDragEnd = {
                        // Arabcha tartib: chapga surish — keyingi oyat
                        if (dx > 80) ayah = (ayah - 1).coerceAtLeast(1) else if (dx < -80) ayah = (ayah + 1).coerceAtMost(sura.ayahs.size)
                        dx = 0f
                    }) { _, d -> dx += d }
                },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(ayah, transitionSpec = {
                    val dir = if (targetState > initialState) -1 else 1
                    (slideInHorizontally(tween(320)) { it * dir / 3 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(260)) { -it * dir / 3 } + fadeOut(tween(200)))
                }, label = "ayah") { n ->
                    val a = sura.ayahs[n - 1]
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        AyahStar(n)
                        Box(Modifier.size(14.dp))
                        val annotated = remember(a, s.tajweed) { ayahAnnotated(a, s.tajweed, true, FocusGold, withNumber = false) }
                        val hidden = hide && !reveal
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            androidx.compose.material3.Text(
                                annotated, color = Color(0xFFF2F5F3), fontFamily = QuranFont,
                                fontSize = (s.quranFont + 8f).coerceIn(28f, 60f).sp, lineHeight = ((s.quranFont + 8f).coerceIn(28f, 60f) * 1.9f).sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                                    .then(if (hidden) Modifier.blur(18.dp) else Modifier)
                                    .clickable(enabled = hide) { reveal = !reveal }
                            )
                        }
                        if (hidden) Text("Yoddan o'qing, so'ng tekshirish uchun bosing", color = FocusGold, style = MaterialTheme.typography.labelMedium)
                        Box(Modifier.size(18.dp))
                        if (s.quranReading || hide.not()) {
                            val rd = remember(a) { Translit.ayah(a.text) }
                            androidx.compose.material3.Text(
                                if (s.readingScript == 1) rd.cyrillic else rd.latin,
                                color = Color(0xFFCFE3D8), fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium,
                                fontSize = 18.sp, lineHeight = 27.sp, textAlign = TextAlign.Center
                            )
                        }
                        val tr = (trState as? TranslationRepo.State.Ready)?.suras?.getOrNull(suraNo - 1)?.getOrNull(n - 1)
                        if (tr != null) {
                            Box(Modifier.size(14.dp))
                            Text(
                                if (s.translation == 2 || s.readingScript == 1 || s.script == 1) tr else UzTranslit.toLatin(tr), color = Color(0xFF9FB5AC),
                                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            // O'qilish yozuvi tanlovi
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.Center) {
                listOf(0 to "Lotin", 1 to "Кирилл").forEach { (v, l) ->
                    val sel = s.readingScript == v
                    Box(
                        Modifier.padding(horizontal = 4.dp).clip(RoundedCornerShape(50))
                            .background(if (sel) FocusGold else Color.White.copy(alpha = 0.08f))
                            .clickable { app.settings.update { it.copy(readingScript = v, quranReading = true) } }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) { androidx.compose.material3.Text(l, color = if (sel) Color(0xFF0E3B2E) else Color.White, style = MaterialTheme.typography.labelLarge) }
                }
            }
            Box(Modifier.size(10.dp))
            // Pleyer
            Column(
                Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.06f)).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.12f))) {
                    Box(Modifier.fillMaxWidth(ayah / sura.ayahs.size.toFloat()).height(4.dp).background(FocusGold))
                }
                Box(Modifier.size(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            (Reciters.getOrNull(s.reciter)?.name ?: "") + "  ▾", color = Color(0xFFE9C46A),
                            style = MaterialTheme.typography.labelMedium, maxLines = 1,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showReciter = true }.padding(vertical = 3.dp, horizontal = 2.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Takror:", color = Color.White, style = MaterialTheme.typography.labelMedium)
                            listOf(1, 3, 5, 10).forEach { n ->
                                val sel = s.repeatCount == n
                                Box(
                                    Modifier.padding(start = 4.dp).clip(RoundedCornerShape(50)).background(if (sel) FocusGold else Color.Transparent)
                                        .clickable { app.settings.update { it.copy(repeatCount = n) }; QuranAudio.repeat = n }
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) { Text("$n", color = if (sel) Color(0xFF0E3B2E) else Color.White, style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }
                    IconButton(onClick = { if (play.active && play.sura == suraNo) QuranAudio.prev() else ayah = (ayah - 1).coerceAtLeast(1) }) {
                        Icon(Icons.Filled.SkipPrevious, "Oldingi oyat", tint = Color.White)
                    }
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(FocusGold).clickable {
                            QuranAudio.repeat = s.repeatCount
                            if (play.active && play.sura == suraNo) QuranAudio.togglePause()
                            else QuranAudio.play(suraNo, ayah, sura.ayahs.size, Reciters.getOrElse(s.reciter) { Reciters[0] })
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        if (play.loading && play.sura == suraNo) CircularProgressIndicator(Modifier.size(24.dp), color = Color(0xFF0E3B2E), strokeWidth = 2.dp)
                        else Icon(if (play.playing && play.sura == suraNo) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Tinglash", tint = Color(0xFF0E3B2E))
                    }
                    IconButton(onClick = { if (play.active && play.sura == suraNo) QuranAudio.next() else ayah = (ayah + 1).coerceAtMost(sura.ayahs.size) }) {
                        Icon(Icons.Filled.SkipNext, "Keyingi oyat", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).border(1.dp, FocusGold.copy(alpha = if (on) 1f else 0.4f), RoundedCornerShape(50))
            .background(if (on) FocusGold.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp)
    ) { Text(text, color = FocusGold, style = MaterialTheme.typography.labelMedium) }
}

@Composable
private fun AyahStar(n: Int) {
    Box(Modifier.size(54.dp).drawBehind {
        drawPath(starPath(center.x, center.y, size.minDimension / 2), FocusGold.copy(alpha = 0.18f))
        drawPath(starPath(center.x, center.y, size.minDimension / 2), FocusGold, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
    }, contentAlignment = Alignment.Center) {
        androidx.compose.material3.Text(uz.hijriy.app.core.Uz.arabicNumber(n), color = FocusGold, fontFamily = QuranFont, fontSize = 18.sp)
    }
}
