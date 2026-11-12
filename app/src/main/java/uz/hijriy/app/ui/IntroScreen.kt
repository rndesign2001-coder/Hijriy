package uz.hijriy.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hijriy.app.R
import uz.hijriy.app.ui.theme.QuranFont
import uz.hijriy.app.ui.theme.TitleFont
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val GoldA = Color(0xFFF0D27A)
private val GoldB = Color(0xFFC9A227)
private val Cream = Color(0xFFFBF7EC)

private fun seg(p: Float, from: Float, to: Float) = ((p - from) / (to - from)).coerceIn(0f, 1f)
private fun easeOut(t: Float) = 1f - (1f - t) * (1f - t) * (1f - t)
private fun easeInOut(t: Float) = if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).let { it * it * it } / 2f
private fun overshoot(t: Float): Float {
    val s = 1.9f; val x = t - 1f
    return x * x * ((s + 1) * x + s) + 1f
}

/** Oltin zarracha: chetdan aylanib markazdagi halqaga yig'iladi. */
private class Particle(val a0: Float, val r0: Float, val target: Float, val delay: Float, val size: Float, val spin: Float)

private fun particles(n: Int): List<Particle> {
    var s = 12345
    fun rnd(): Float { s = (s * 1103515245 + 12345) and 0x7fffffff; return (s % 10000) / 10000f }
    return List(n) {
        Particle(
            a0 = rnd() * 2f * PI.toFloat(), r0 = 0.75f + rnd() * 0.6f, target = rnd() * 2f * PI.toFloat(),
            delay = rnd() * 0.35f, size = 1.2f + rnd() * 2.6f, spin = (if (rnd() > 0.5f) 1f else -1f) * (1.2f + rnd() * 1.6f)
        )
    }
}

/** 8 qirrali yulduzni tashkil etuvchi ikki kvadrat (har biri alohida kontur). */
private fun khatam(cx: Float, cy: Float, r: Float, rot: Float): List<Path> = (0..1).map { sq ->
    Path().apply {
        for (k in 0..4) {
            val a = Math.toRadians((rot + sq * 45 + k * 90 + 45).toDouble())
            val x = cx + r * cos(a).toFloat(); val y = cy + r * sin(a).toFloat()
            if (k == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
}

/** Konturlarni [t] ulushigacha "chizib" ko'rsatadi (qalam bilan chizilayotgandek). */
private fun DrawScope.drawTrimmed(paths: List<Path>, t: Float, color: Color, width: Float) {
    if (t <= 0f) return
    paths.forEach { path ->
        val pm = PathMeasure().apply { setPath(path, false) }
        val dst = Path()
        pm.getSegment(0f, pm.length * t, dst, true)
        drawPath(dst, color, style = Stroke(width, cap = StrokeCap.Round))
    }
}

/**
 * Ilovaga kirish animatsiyasi (~3.6 soniya), bosilsa darhol o'tkaziladi:
 * yulduzli osmon → oltin chiziqlar bilan islomiy yulduz chiziladi → zarrachalar aylanib yig'iladi →
 * nur portlashi bilan logo chiqadi (yaltirash) → nom harflab paydo bo'ladi → Ka'ba surati ochiladi → kirish.
 */
@Composable
fun IntroScreen(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    var buzzed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(3600, easing = LinearEasing))
        onDone()
    }
    val inf = rememberInfiniteTransition(label = "intro")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(40000, easing = LinearEasing)), label = "spin")
    val twinkle by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "tw")
    val stars = remember { StarField(110, 11) }
    val parts = remember { particles(150) }
    val p = progress.value
    val photo by rememberBgBitmap(R.drawable.bg_nabawi_sunset, 2) // oldindan, fon oqimida o'qiladi

    val skyT = seg(p, 0f, 0.15f)
    val drawT = seg(p, 0.05f, 0.38f)      // yulduz chiziladi
    val partT = seg(p, 0.10f, 0.45f)      // zarrachalar yig'iladi
    val burstT = seg(p, 0.40f, 0.58f)     // nur portlashi
    val logoT = seg(p, 0.42f, 0.66f)
    val shineT = seg(p, 0.58f, 0.80f)
    val titleT = seg(p, 0.55f, 0.80f)
    val arT = seg(p, 0.66f, 0.84f)
    val photoT = seg(p, 0.55f, 0.92f)
    val bismT = seg(p, 0.70f, 0.90f)
    val exitT = seg(p, 0.92f, 1f)
    if (logoT > 0.25f && !buzzed) { buzzed = true; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    Box(
        Modifier
            .fillMaxSize()
            .testTag("intro")
            .graphicsLayer {
                alpha = 1f - easeInOut(exitT)
                val k = 1f + 0.18f * easeInOut(exitT); scaleX = k; scaleY = k
            }
            .background(Color(0xFF041C17))
            .clickable(remember { MutableInteractionSource() }, null) { onDone() },
        contentAlignment = Alignment.Center
    ) {
        // Masjidi Nabaviy surati (bosh sahifa standart foni) — sekin yaqinlashib (Ken Burns), bosh sahifaga uzviy o'tish
        val ph = photo
        if (photoT > 0f && ph != null) {
            Image(
                ph, null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    alpha = 0.85f * easeOut(photoT)
                    val k = 1.25f - 0.15f * easeOut(photoT); scaleX = k; scaleY = k
                }
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xE6041C17), Color(0x99041C17), Color(0xF0041C17)))))
        }
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(center.x, size.height * 0.40f)
            val R = size.minDimension * 0.36f
            // osmon gradiyenti va yulduzlar
            drawRect(Brush.radialGradient(listOf(Color(0xFF0E4A3B).copy(alpha = 0.9f * (1f - photoT * 0.7f)), Color.Transparent), c, size.maxDimension * 0.8f))
            with(stars) { draw(Color.White.copy(alpha = 0.7f * easeOut(skyT) * (1f - 0.6f * photoT)), twinkle, 1f, 1.9f) }
            // fon naqshi (sekin aylanadi)
            drawStarLattice(GoldA.copy(alpha = 0.06f * easeOut(drawT)), size.minDimension / 4.5f, 1.2f, spin, c)
            // markaziy rozetka: uch qatlam yulduz chiziladi va qarama-qarshi aylanadi
            val rot = spin * 3f
            drawTrimmed(khatam(c.x, c.y, R, rot), easeInOut(drawT), GoldA.copy(alpha = 0.95f - 0.45f * logoT), 2.2.dp.toPx())
            drawTrimmed(khatam(c.x, c.y, R * 0.78f, -rot + 22.5f), easeInOut(seg(p, 0.12f, 0.42f)), GoldB.copy(alpha = 0.8f - 0.4f * logoT), 1.6.dp.toPx())
            drawTrimmed(khatam(c.x, c.y, R * 1.22f, rot * 0.5f + 11f), easeInOut(seg(p, 0.18f, 0.48f)), GoldA.copy(alpha = 0.45f - 0.2f * logoT), 1.dp.toPx())
            // tashqi halqa va nuqtalar
            val ringA = easeOut(seg(p, 0.2f, 0.5f))
            drawArc(
                Brush.sweepGradient(listOf(GoldB, GoldA, Color.White, GoldA, GoldB), c), -90f + rot, 360f * ringA, false,
                topLeft = Offset(c.x - R * 1.38f, c.y - R * 1.38f), size = androidx.compose.ui.geometry.Size(R * 2.76f, R * 2.76f),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
            )
            for (i in 0 until 16) {
                val a = Math.toRadians((i * 22.5 - 90 + spin * 2).toDouble())
                val rr = R * 1.38f + 9.dp.toPx()
                drawCircle(GoldA.copy(alpha = ringA * (if (i % 2 == 0) 1f else 0.5f)), (if (i % 2 == 0) 2.6f else 1.6f).dp.toPx(),
                    Offset(c.x + rr * cos(a).toFloat(), c.y + rr * sin(a).toFloat()))
            }
            // oltin zarrachalar — spiral bo'ylab markazga
            parts.forEach { q ->
                val t = seg(partT, q.delay, 1f)
                if (t <= 0f || t >= 1f) return@forEach
                val e = easeInOut(t)
                val ang = q.a0 + q.spin * e * 2.2f
                val rad = (q.r0 * size.maxDimension * 0.6f) * (1f - e) + R * 0.9f * e
                val pos = Offset(c.x + rad * cos(ang), c.y + rad * sin(ang))
                val al = sin(PI * t).toFloat()
                drawCircle(Brush.radialGradient(listOf(GoldA.copy(alpha = al), Color.Transparent), pos, q.size.dp.toPx() * 3f), q.size.dp.toPx() * 3f, pos)
                drawCircle(Color.White.copy(alpha = al), q.size.dp.toPx() * 0.6f, pos)
            }
            // nur portlashi: halqa to'lqini va yorug'lik
            if (burstT > 0f && burstT < 1f) {
                val e = easeOut(burstT)
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f * (1f - e)), GoldA.copy(alpha = 0.4f * (1f - e)), Color.Transparent), c, R * 2.4f * e + 1f), R * 2.4f * e + 1f, c)
                drawCircle(GoldA.copy(alpha = 0.9f * (1f - e)), R * (0.4f + 1.6f * e), c, style = Stroke((6f * (1f - e) + 1f).dp.toPx()))
                // nurlar
                rotate(spin * 4f, c) {
                    for (k in 0 until 12) {
                        val a = k * 30f * (PI.toFloat() / 180f)
                        val l = R * 2.2f * e
                        drawLine(
                            Brush.linearGradient(listOf(GoldA.copy(alpha = 0.6f * (1f - e)), Color.Transparent), c, Offset(c.x + l * cos(a), c.y + l * sin(a))),
                            c, Offset(c.x + l * cos(a), c.y + l * sin(a)), 3.dp.toPx(), StrokeCap.Round
                        )
                    }
                }
            }
            // logo orqasidagi doimiy nur
            drawCircle(Brush.radialGradient(listOf(GoldA.copy(alpha = 0.35f * easeOut(logoT)), Color.Transparent), c, R * 1.3f), R * 1.3f, c)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 60.dp)) {
            // Logo: 3D aylanib, kattalashib chiqadi va ustidan yaltiroq nur o'tadi
            val s = 0.2f + 0.8f * overshoot(logoT)
            val logoShape = androidx.compose.foundation.shape.RoundedCornerShape(44.dp)
            Box(
                Modifier
                    .size(width = 176.dp, height = 184.dp)
                    .graphicsLayer {
                        scaleX = s; scaleY = s; alpha = easeOut(logoT)
                        rotationY = (1f - easeOut(logoT)) * 180f
                        rotationZ = (1f - easeOut(logoT)) * -25f
                        cameraDistance = 16f * density
                    }
                    .shadow(30.dp, logoShape, ambientColor = GoldA, spotColor = GoldA)
                    .clip(logoShape)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        if (shineT > 0f && shineT < 1f) {
                            val x = -size.width + 3f * size.width * easeInOut(shineT)
                            drawRect(
                                Brush.linearGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.75f), Color.Transparent),
                                    Offset(x, 0f), Offset(x + size.width * 0.6f, size.height)
                                ),
                                blendMode = BlendMode.SrcAtop
                            )
                        }
                    }
            ) {
                Image(painterResource(R.drawable.app_logo), contentDescription = "Hijriy Taqvim", modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(30.dp))
            // Nom — harflab, xiralikdan aniqlikka
            val title = "Hijriy Taqvim"
            Row {
                title.forEachIndexed { i, ch ->
                    val t = easeOut(seg(titleT, i * 0.045f, i * 0.045f + 0.45f))
                    androidx.compose.material3.Text(
                        ch.toString(), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = TitleFont,
                        modifier = Modifier
                            .graphicsLayer { alpha = t; translationY = (1f - t) * 26.dp.toPx(); val k = 1.4f - 0.4f * t; scaleX = k; scaleY = k }
                            .blur(((1f - t) * 10f).dp)
                    )
                }
            }
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(width = (200 * easeOut(seg(titleT, 0.4f, 1f))).dp, height = 2.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, GoldA, Color.Transparent)))
            )
            // Arabcha nom — oltin jilo bilan
            androidx.compose.material3.Text(
                "التقويم الهجري",
                fontSize = 30.sp, fontFamily = QuranFont,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        listOf(GoldB, GoldA, Color.White, GoldA, GoldB),
                        Offset(-300f + 900f * arT, 0f), Offset(300f + 900f * arT, 0f)
                    )
                ),
                modifier = Modifier.padding(top = 4.dp).graphicsLayer { alpha = arT; translationY = (1f - easeOut(arT)) * 20.dp.toPx() }
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Basmala — o'ngdan chapga yozilayotgandek ochiladi
            androidx.compose.material3.Text(
                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                color = Cream, fontSize = 27.sp, fontFamily = QuranFont, textAlign = TextAlign.Center,
                modifier = Modifier.drawWithContent {
                    val w = size.width * easeInOut(bismT)
                    clipRect(left = size.width - w) { this@drawWithContent.drawContent() }
                }
            )
            Text(
                "Qur'on  •  Namoz  •  Qibla  •  Taqvim",
                color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp, letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 10.dp).graphicsLayer { alpha = seg(bismT, 0.4f, 1f) }
            )
        }
    }
}
