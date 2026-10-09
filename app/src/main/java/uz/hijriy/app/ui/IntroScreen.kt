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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.hijriy.app.R
import uz.hijriy.app.ui.theme.QuranFont

private val GoldA = Color(0xFFF0D27A)
private val GoldB = Color(0xFFC9A227)
private val Cream = Color(0xFFFBF7EC)

private fun seg(p: Float, from: Float, to: Float) = ((p - from) / (to - from)).coerceIn(0f, 1f)
private fun easeOut(t: Float) = 1f - (1f - t) * (1f - t) * (1f - t)
private fun overshoot(t: Float): Float {
    val s = 1.7f; val x = t - 1f
    return x * x * ((s + 1) * x + s) + 1f
}

/** Ilovaga kirish animatsiyasi (~3 soniya). Bosilsa darhol o'tkazib yuboriladi. */
@Composable
fun IntroScreen(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(3000, easing = LinearEasing))
        onDone()
    }
    val inf = rememberInfiniteTransition(label = "intro")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(60000, easing = LinearEasing)), label = "spin")
    val twinkle by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "tw")
    val stars = remember { StarField(70, 11) }
    val p = progress.value

    val logoT = seg(p, 0.05f, 0.40f)
    val ringT = seg(p, 0.12f, 0.55f)
    val titleT = seg(p, 0.35f, 0.60f)
    val arT = seg(p, 0.48f, 0.70f)
    val bismT = seg(p, 0.60f, 0.85f)
    val exitT = seg(p, 0.90f, 1f)

    Box(
        Modifier
            .fillMaxSize()
            .testTag("intro")
            .graphicsLayer { alpha = 1f - exitT }
            .background(Brush.verticalGradient(listOf(Color(0xFF062B22), Color(0xFF0B5D48), Color(0xFF073528))))
            .clickable(remember { MutableInteractionSource() }, null) { onDone() },
        contentAlignment = Alignment.Center
    ) {
        // Fon: aylanib turuvchi naqsh, yulduzlar va nur
        Canvas(Modifier.fillMaxSize()) {
            with(stars) { draw(Color.White.copy(alpha = 0.55f * easeOut(seg(p, 0f, 0.3f))), twinkle, 1f, 1.8f) }
            drawStarLattice(GoldA.copy(alpha = 0.07f + 0.05f * easeOut(logoT)), size.minDimension / 4.2f, 1.4f, spin, center)
            drawCircle(
                Brush.radialGradient(
                    listOf(GoldA.copy(alpha = 0.35f * easeOut(logoT)), Color.Transparent),
                    center = Offset(center.x, size.height * 0.4f), radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f, center = Offset(center.x, size.height * 0.4f)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(290.dp)) {
                // Oltin halqa chiziladi
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2 - 6.dp.toPx()
                    drawArc(
                        Brush.sweepGradient(listOf(GoldB, GoldA, GoldB, GoldA, GoldB)), -90f, 360f * easeOut(ringT), false,
                        topLeft = Offset(center.x - r, center.y - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    for (i in 0 until 8) {
                        val a = Math.toRadians((i * 45 - 90 + spin * 2).toDouble())
                        val rr = r + 10.dp.toPx()
                        drawCircle(GoldA.copy(alpha = easeOut(ringT)), 2.5.dp.toPx(),
                            Offset(center.x + rr * kotlin.math.cos(a).toFloat(), center.y + rr * kotlin.math.sin(a).toFloat()))
                    }
                }
                val s = 0.35f + 0.65f * overshoot(logoT)
                val logoShape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
                Image(
                    painterResource(R.drawable.app_logo), contentDescription = "Hijriy Taqvim",
                    modifier = Modifier
                        .size(width = 192.dp, height = 201.dp)
                        .graphicsLayer {
                            scaleX = s; scaleY = s; alpha = logoT
                            rotationY = (1f - easeOut(logoT)) * 70f
                            cameraDistance = 14f * density
                        }
                        .shadow(28.dp, logoShape, ambientColor = GoldA, spotColor = GoldA)
                        .clip(logoShape)
                )
            }
            Spacer(Modifier.height(26.dp))
            Text(
                "Hijriy Taqvim",
                color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, fontFamily = uz.hijriy.app.ui.theme.TitleFont,
                modifier = Modifier.graphicsLayer {
                    alpha = titleT; translationY = (1f - easeOut(titleT)) * 40.dp.toPx()
                    val k = 0.9f + 0.1f * easeOut(titleT); scaleX = k; scaleY = k
                }
            )
            Box(
                Modifier
                    .padding(top = 6.dp)
                    .size(width = (180 * easeOut(titleT)).dp, height = 2.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, GoldA, Color.Transparent)))
            )
            Text(
                "التقويم الهجري",
                color = GoldA, fontSize = 30.sp, fontFamily = QuranFont,
                modifier = Modifier.padding(top = 4.dp).graphicsLayer { alpha = arT; translationY = (1f - easeOut(arT)) * 24.dp.toPx() }
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 36.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                color = Cream, fontSize = 26.sp, fontFamily = QuranFont, textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = bismT; translationY = (1f - easeOut(bismT)) * 16.dp.toPx() }
            )
            Text(
                "Qur'on  •  Qibla  •  Kalendar  •  Namoz vaqtlari",
                color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, letterSpacing = 1.5.sp,
                modifier = Modifier.padding(top = 8.dp).graphicsLayer { alpha = bismT }
            )
        }
    }
}
