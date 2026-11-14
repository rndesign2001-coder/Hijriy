package uz.hijriy.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.R
import uz.hijriy.app.data.QuranAudio

private val PlGreen = listOf(Color(0xFF0A3A2C), Color(0xFF0E5A44), Color(0xFF14705A))
private val Gold = Color(0xFFF3D57E)
private val GoldDeep = Color(0xFFD4A93E)
private val Ink = Color(0xFF0B3D2E)

/**
 * Qur'on tilovati pleyeri — o'quv ekranida ham, ilovaning boshqa bo'limlarida ham bir xil ko'rinish:
 * yashil-oltin kartochka, Qur'on muqovasi, sura/oyat, qori (bosilsa almashtiriladi), oyat bo'yicha jarayon chizig'i.
 */
@Composable
fun QuranPlayerBar(app: HijriyApp, modifier: Modifier = Modifier, onOpen: (() -> Unit)? = null) {
    val play by QuranAudio.state.collectAsStateWithLifecycle()
    val q by app.quran.collectAsStateWithLifecycle()
    var showReciter by rememberSaveable { mutableStateOf(false) }
    if (!play.active) return
    val sura = q?.suras?.getOrNull(play.sura - 1)
    val count = sura?.count ?: 0
    val progress by animateFloatAsState(if (count > 0) play.ayah / count.toFloat() else 0f, tween(500), label = "pl")
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier
            .fillMaxWidth()
            .shadow(10.dp, shape, ambientColor = Color(0x55000000), spotColor = Color(0x880B3D2E))
            .clip(shape)
            .background(Brush.linearGradient(PlGreen))
            .drawBehind { drawGirih(Color.White.copy(alpha = 0.06f), 40.dp.toPx(), 0.7.dp.toPx()) }
            .border(1.dp, Gold.copy(alpha = 0.45f), shape)
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
    ) {
        Column {
            Row(
                Modifier.padding(start = 10.dp, end = 6.dp, top = 9.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cover(play.playing && !play.loading)
                Column(
                    Modifier.weight(1f).padding(start = 10.dp, end = 4.dp)
                ) {
                    Text(
                        when {
                            sura == null -> "Qur'oni Karim"
                            play.ayah == 0 -> "${sura.uzName} · Bismillah"
                            else -> "${sura.uzName} · ${play.ayah}-oyat"
                        },
                        color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        Modifier.padding(top = 1.dp).clip(RoundedCornerShape(8.dp)).clickable { showReciter = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            when {
                                play.error != null -> play.error!!
                                play.loading -> "Yuklanmoqda…"
                                else -> QuranAudio.reciterName + "  ▾"
                            },
                            color = if (play.error != null) Color(0xFFFFB4A8) else Gold, fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                RoundBtn(R.drawable.ic_pl_prev, "Oldingi", 36.dp, ghost = true) { QuranAudio.prev() }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier.size(46.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Gold, GoldDeep)))
                        .clickable { QuranAudio.togglePause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (play.loading) CircularProgressIndicator(Modifier.size(22.dp), color = Ink, strokeWidth = 2.5.dp)
                    else Icon(
                        painterResource(if (play.playing) R.drawable.ic_pl_pause else R.drawable.ic_pl_play),
                        if (play.playing) "To'xtatish" else "Davom etish", tint = Ink, modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                RoundBtn(R.drawable.ic_pl_next, "Keyingi", 36.dp, ghost = true) { QuranAudio.next() }
                RoundBtn(R.drawable.ic_pl_close, "Yopish", 32.dp, ghost = false) { QuranAudio.stop() }
            }
            // Oyat bo'yicha jarayon
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.18f))
                ) {
                    Box(
                        Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(Brush.horizontalGradient(listOf(GoldDeep, Gold)))
                    )
                }
                if (count > 0) Text(
                    "${play.ayah}/$count", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
    if (showReciter) ReciterSheet(app) { showReciter = false }
}

@Composable
private fun Cover(playing: Boolean) {
    val t = rememberInfiniteTransition(label = "cover")
    val pulse by t.animateFloat(1f, 1.12f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p")
    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
        if (playing) Box(
            Modifier.size(46.dp).scale(pulse).clip(RoundedCornerShape(14.dp))
                .background(Gold.copy(alpha = 0.18f))
        )
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .border(1.dp, Gold.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(painterResource(R.drawable.ic3d_quran), null, Modifier.size(34.dp))
        }
    }
}

@Composable
private fun RoundBtn(icon: Int, desc: String, size: Dp, ghost: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape)
            .then(if (ghost) Modifier.background(Color.White.copy(alpha = 0.14f)) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(painterResource(icon), desc, tint = Color.White.copy(alpha = if (ghost) 1f else 0.75f), modifier = Modifier.size(size * 0.5f))
    }
}
