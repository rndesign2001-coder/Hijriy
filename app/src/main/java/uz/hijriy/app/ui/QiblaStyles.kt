package uz.hijriy.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uz.hijriy.app.ui.theme.TitleFont
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Gold = Color(0xFFD4A72C)
private val GoldLight = Color(0xFFF3D57E)

/** Ka'ba belgisi. */
fun DrawScope.drawKaaba(c: Offset, s: Float) {
    val tl = Offset(c.x - s / 2, c.y - s / 2)
    drawRoundRect(Color(0xFF111111), tl, Size(s, s), CornerRadius(s * 0.08f))
    drawRect(Gold, Offset(tl.x, tl.y + s * 0.22f), Size(s, s * 0.12f))
    drawRect(Gold, Offset(c.x + s * 0.12f, tl.y + s * 0.5f), Size(s * 0.2f, s * 0.42f))
}

private fun polar(c: Offset, r: Float, deg: Float) =
    Offset(c.x + r * cos(Math.toRadians((deg - 90).toDouble())).toFloat(), c.y + r * sin(Math.toRadians((deg - 90).toDouble())).toFloat())

/** Islomiy uslub: zumrad disk, oltin naqshli aylanuvchi siferblat, Ka'ba belgisi. */
@Composable
fun CompassIslamic(heading: Float, qibla: Float, aligned: Boolean) {
    val inf = rememberInfiniteTransition(label = "isl")
    val pulse by inf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p")
    Canvas(Modifier.fillMaxSize()) {
        val c = center; val r = size.minDimension / 2
        if (aligned) drawCircle(Brush.radialGradient(listOf(GoldLight.copy(alpha = 0.55f * pulse), Color.Transparent), c, r), r, c)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF16946F), Color(0xFF0A4A37)), c, r * 0.94f), r * 0.94f, c)
        drawCircle(Gold, r * 0.94f, c, style = Stroke(r * 0.03f))
        drawCircle(GoldLight, r * 0.86f, c, style = Stroke(r * 0.008f))
        rotate(-heading, c) {
            // naqsh: rozetka
            for (k in 0 until 3) drawPath(starPath(c.x, c.y, r * (0.72f - k * 0.17f), k * 15f), GoldLight.copy(alpha = 0.55f - k * 0.1f), style = Stroke(r * 0.008f))
            for (i in 0 until 72) {
                val long = i % 9 == 0
                drawLine(
                    if (i == 0) Color(0xFFFF6B6B) else GoldLight.copy(alpha = if (long) 1f else 0.6f),
                    polar(c, r * 0.86f, i * 5f), polar(c, r * (if (long) 0.76f else 0.81f), i * 5f),
                    strokeWidth = if (long) r * 0.014f else r * 0.006f, cap = StrokeCap.Round
                )
            }
            // qibla nuri
            drawLine(
                Brush.linearGradient(listOf(Color.Transparent, GoldLight), c, polar(c, r * 0.7f, qibla)),
                c, polar(c, r * 0.7f, qibla), strokeWidth = r * 0.05f, cap = StrokeCap.Round
            )
            val kp = polar(c, r * 0.76f, qibla)
            drawCircle(Color(0xFFFBF6EA), r * 0.1f, kp)
            drawCircle(Gold, r * 0.1f, kp, style = Stroke(r * 0.012f))
            rotate(heading, kp) { drawKaaba(kp, r * 0.1f) }
        }
        // telefon ko'rsatkichi: yuqoriga qaragan hilol-uchli minora
        val tip = Offset(c.x, c.y - r * 0.98f)
        val p = Path().apply {
            moveTo(tip.x, tip.y); lineTo(c.x - r * 0.045f, c.y - r * 0.84f); lineTo(c.x + r * 0.045f, c.y - r * 0.84f); close()
        }
        drawPath(p, if (aligned) Color(0xFF7CFFB2) else Color.White)
        drawCircle(Color(0xFFFBF6EA), r * 0.13f, c)
        drawPath(crescentPath(c.x - r * 0.01f, c.y, r * 0.075f), Gold)
    }
}

/** Zamonaviy uslub: qorong'i shisha disk, neon yoy qancha burilish kerakligini ko'rsatadi. */
@Composable
fun CompassModern(heading: Float, qibla: Float, aligned: Boolean) {
    val inf = rememberInfiniteTransition(label = "mod")
    val pulse by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "p")
    var diff = qibla - heading
    while (diff > 180) diff -= 360f
    while (diff < -180) diff += 360f
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center; val r = size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(Color(0xFF1C2230), Color(0xFF0B0E14)), c, r), r * 0.96f, c)
            drawCircle(Color.White.copy(alpha = 0.08f), r * 0.96f, c, style = Stroke(r * 0.02f))
            rotate(-heading, c) {
                for (i in 0 until 36) {
                    drawLine(
                        Color.White.copy(alpha = if (i % 9 == 0) 0.9f else 0.25f),
                        polar(c, r * 0.9f, i * 10f), polar(c, r * (if (i % 9 == 0) 0.8f else 0.85f), i * 10f),
                        strokeWidth = r * 0.012f, cap = StrokeCap.Round
                    )
                }
                drawCircle(Color(0xFFFF4D6D), r * 0.03f, polar(c, r * 0.74f, 0f))
                drawCircle(Brush.radialGradient(listOf(Color(0xFF00E5FF), Color.Transparent), polar(c, r * 0.9f, qibla), r * 0.12f), r * 0.12f, polar(c, r * 0.9f, qibla))
                drawCircle(Color(0xFF00E5FF), r * 0.035f, polar(c, r * 0.9f, qibla))
            }
            // Burilish yoyi (yuqoridan qibla tomonga)
            val sw = r * 0.06f
            val arcR = r * 0.68f
            drawArc(
                Brush.sweepGradient(listOf(Color(0xFF00E5FF), Color(0xFFB388FF), Color(0xFFFF4D6D), Color(0xFF00E5FF)), c),
                -90f, if (aligned) 360f else diff, false,
                topLeft = Offset(c.x - arcR, c.y - arcR), size = Size(arcR * 2, arcR * 2),
                style = Stroke(sw, cap = StrokeCap.Round),
                alpha = if (aligned) pulse else 1f
            )
            drawLine(Color.White, Offset(c.x, c.y - r * 0.96f), Offset(c.x, c.y - r * 0.78f), strokeWidth = r * 0.02f, cap = StrokeCap.Round)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (aligned) "✓" else "${abs(diff).roundToInt()}°",
                color = if (aligned) Color(0xFF7CFFB2) else Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold, fontFamily = TitleFont
            )
            Text(
                if (aligned) "Qibla" else if (diff > 0) "o'ngga" else "chapga",
                color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp
            )
        }
    }
}
