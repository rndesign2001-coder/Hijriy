package uz.hijriy.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hijriy ikonka to'plami — qo'lda chizilgan, ikki rangli (asosiy chiziq + yumshoq to'ldirish).
 * Shakllar islomiy san'at motivlaridan: mehrob ravog'i, 8 qirrali yulduz, gumbaz, minora, fonus.
 */
enum class HI {
    QURAN, MUSHAF, QIBLA, TASBEH, NAMES, DUA, WALLPAPER, QAZO, CONVERTER, CALENDAR, WEATHER, SETTINGS,
    MOSQUE, HATM, RAMADAN, JOURNAL, SUNRISE, SUNSET, FAJR, DHUHR, ASR, ISHA, HOME, PRAYER, MORE, FOCUS, PHOTO, LOCATION, ZAKAT, KAABA,
}

@Composable
fun HIcon(icon: HI, tint: Color, modifier: Modifier = Modifier, size: Dp = 26.dp, accent: Color = tint) {
    Canvas(modifier.size(size)) { drawHIcon(icon, tint, accent) }
}

fun DrawScope.drawHIcon(icon: HI, tint: Color, accent: Color = tint) {
    val u = size.minDimension / 24f
    val ox = (size.width - 24 * u) / 2; val oy = (size.height - 24 * u) / 2
    fun P(x: Float, y: Float) = Offset(ox + x * u, oy + y * u)
    val st = Stroke(1.7f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val thin = Stroke(1.1f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val soft = accent.copy(alpha = 0.28f)
    fun path(block: Path.() -> Unit) = Path().apply(block)
    fun Path.mt(x: Float, y: Float) = moveTo(ox + x * u, oy + y * u)
    fun Path.lt(x: Float, y: Float) = lineTo(ox + x * u, oy + y * u)
    fun Path.qt(cx: Float, cy: Float, x: Float, y: Float) = quadraticTo(ox + cx * u, oy + cy * u, ox + x * u, oy + y * u)
    fun Path.ct(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) =
        cubicTo(ox + x1 * u, oy + y1 * u, ox + x2 * u, oy + y2 * u, ox + x * u, oy + y * u)
    fun star8(cx: Float, cy: Float, r: Float, color: Color, fill: Boolean = false) {
        val p = starPath(ox + cx * u, oy + cy * u, r * u, 0f)
        if (fill) drawPath(p, color) else drawPath(p, color, style = thin)
    }
    fun crescent(cx: Float, cy: Float, r: Float, color: Color) {
        val p = path {
            addOval(Rect(P(cx - r, cy - r), Size(2 * r * u, 2 * r * u)))
        }
        val cut = path { addOval(Rect(P(cx - r * 0.45f, cy - r * 1.05f), Size(2 * r * u, 2 * r * u))) }
        val res = Path().apply { op(p, cut, androidx.compose.ui.graphics.PathOperation.Difference) }
        drawPath(res, color)
    }
    /** Mehrob ravog'i (uchli ravoq). */
    fun arch(l: Float, r: Float, top: Float, bottom: Float) = path {
        val mid = (l + r) / 2; val sh = top + (r - l) * 0.55f
        mt(l, bottom); lt(l, sh)
        qt(l, top + (sh - top) * 0.25f, mid, top)
        qt(r, top + (sh - top) * 0.25f, r, sh)
        lt(r, bottom); close()
    }
    fun sun(cx: Float, cy: Float, r: Float, rays: Boolean = true) {
        drawCircle(soft, r * u * 1.15f, P(cx, cy))
        drawCircle(tint, r * u, P(cx, cy), style = st)
        if (rays) for (i in 0 until 8) {
            val a = i * PI / 4
            drawLine(tint, P(cx + (r + 1.6f) * cos(a).toFloat(), cy + (r + 1.6f) * sin(a).toFloat()),
                P(cx + (r + 3.2f) * cos(a).toFloat(), cy + (r + 3.2f) * sin(a).toFloat()), 1.5f * u, StrokeCap.Round)
        }
    }

    when (icon) {
        HI.QURAN -> {
            // Ochiq kitob rahl (lavh) ustida
            val l = path { mt(12f, 7f); qt(8f, 4.6f, 3f, 5.5f); lt(3f, 16f); qt(8f, 15f, 12f, 17.5f); close() }
            val r = path { mt(12f, 7f); qt(16f, 4.6f, 21f, 5.5f); lt(21f, 16f); qt(16f, 15f, 12f, 17.5f); close() }
            drawPath(l, soft); drawPath(r, soft)
            drawPath(l, tint, style = st); drawPath(r, tint, style = st)
            drawPath(path { mt(5f, 21f); lt(19f, 15.5f); mt(19f, 21f); lt(5f, 15.5f) }, tint, style = st)
            star8(7.6f, 10.4f, 1.4f, accent, true); star8(16.4f, 10.4f, 1.4f, accent, true)
        }
        HI.MUSHAF -> {
            val b = path { mt(5f, 3.5f); lt(18f, 3.5f); qt(19.5f, 3.5f, 19.5f, 5f); lt(19.5f, 20.5f); lt(6.5f, 20.5f); qt(5f, 20.5f, 5f, 19f); close() }
            drawPath(b, soft); drawPath(b, tint, style = st)
            drawLine(tint, P(5f, 17.5f), P(19.5f, 17.5f), 1.2f * u)
            star8(12.2f, 10.2f, 3.6f, accent); drawCircle(accent, 1.1f * u, P(12.2f, 10.2f))
        }
        HI.QIBLA -> {
            drawCircle(soft, 9.5f * u, P(12f, 12f))
            drawCircle(tint, 9.5f * u, P(12f, 12f), style = thin)
            for (i in 0 until 8) { val a = i * PI / 4; drawCircle(tint, 0.7f * u, P(12f + 7.8f * cos(a).toFloat(), 12f + 7.8f * sin(a).toFloat())) }
            drawPath(path { mt(12f, 13f); lt(9.6f, 19f); lt(12f, 17.4f); lt(14.4f, 19f); close() }, tint.copy(alpha = 0.55f))
            // Ka'ba kubigi strelka uchida
            val k = path { mt(9f, 6.4f); lt(12f, 5f); lt(15f, 6.4f); lt(15f, 10.6f); lt(12f, 12f); lt(9f, 10.6f); close() }
            drawPath(k, tint)
            drawLine(accent, P(9f, 7.8f), P(12f, 9.2f), 1.1f * u); drawLine(accent, P(12f, 9.2f), P(15f, 7.8f), 1.1f * u)
        }
        HI.TASBEH -> {
            for (i in 0 until 11) {
                val a = -PI / 2 + i * (2 * PI / 12)
                drawCircle(if (i % 4 == 0) accent else tint, 1.35f * u, P(12f + 7f * cos(a).toFloat(), 10f + 7f * sin(a).toFloat()))
            }
            drawPath(path { mt(12f, 17f); lt(12f, 19.4f) }, tint, style = st)
            drawPath(path { mt(12f, 19.4f); lt(10.4f, 22.5f); mt(12f, 19.4f); lt(13.6f, 22.5f); mt(12f, 19.4f); lt(12f, 23f) }, accent, style = thin)
        }
        HI.NAMES -> {
            star8(12f, 12f, 10f, tint)
            drawPath(starPath(ox + 12 * u, oy + 12 * u, 6.6f * u, 22.5f), soft)
            drawPath(starPath(ox + 12 * u, oy + 12 * u, 6.6f * u, 22.5f), accent, style = thin)
            drawCircle(tint, 1.6f * u, P(12f, 12f))
        }
        HI.DUA -> {
            val lh = path { mt(11.2f, 21f); lt(11.2f, 11f); qt(11.2f, 6f, 8.2f, 3.5f); qt(6f, 5f, 6.5f, 9f); lt(7.4f, 14f); qt(5f, 15f, 5f, 18f); lt(5f, 21f) }
            val rh = path { mt(12.8f, 21f); lt(12.8f, 11f); qt(12.8f, 6f, 15.8f, 3.5f); qt(18f, 5f, 17.5f, 9f); lt(16.6f, 14f); qt(19f, 15f, 19f, 18f); lt(19f, 21f) }
            drawPath(lh, soft); drawPath(rh, soft)
            drawPath(lh, tint, style = st); drawPath(rh, tint, style = st)
            drawCircle(accent, 1.1f * u, P(12f, 1.8f))
        }
        HI.WALLPAPER, HI.PHOTO -> {
            val a = arch(4f, 20f, 2.5f, 21f)
            drawPath(a, soft); drawPath(a, tint, style = st)
            drawPath(path { mt(4f, 18f); lt(9f, 13f); lt(12.5f, 16f); lt(15f, 13.6f); lt(20f, 18f) }, tint, style = thin)
            crescent(14.5f, 8.6f, 2.4f, accent)
        }
        HI.QAZO -> {
            val a = arch(4.5f, 19.5f, 2.5f, 21f)
            drawPath(a, tint, style = st)
            for (k in 0..2) {
                val y = 10.5f + k * 3.6f
                drawPath(path { mt(7.3f, y); lt(8.4f, y + 1.1f); lt(10.2f, y - 1f) }, accent, style = thin)
                drawLine(tint, P(12f, y), P(16.8f, y), 1.4f * u, StrokeCap.Round)
            }
        }
        HI.CONVERTER -> {
            crescent(8.2f, 9f, 4.6f, tint)
            drawCircle(soft, 4.6f * u, P(15.8f, 15f)); drawCircle(accent, 4.6f * u, P(15.8f, 15f), style = thin)
            drawPath(path { mt(15f, 4.5f); qt(19.5f, 4.5f, 19.5f, 9f); lt(18f, 7.6f); mt(19.5f, 9f); lt(21f, 7.6f) }, tint, style = thin)
            drawPath(path { mt(9f, 19.5f); qt(4.5f, 19.5f, 4.5f, 15f); lt(3f, 16.4f); mt(4.5f, 15f); lt(6f, 16.4f) }, tint, style = thin)
        }
        HI.CALENDAR -> {
            val b = path { mt(3.5f, 6f); lt(20.5f, 6f); lt(20.5f, 20.5f); lt(3.5f, 20.5f); close() }
            drawPath(b, soft); drawPath(b, tint, style = st)
            drawLine(tint, P(3.5f, 10f), P(20.5f, 10f), 1.4f * u)
            drawLine(tint, P(8f, 3.5f), P(8f, 7.5f), 1.7f * u, StrokeCap.Round); drawLine(tint, P(16f, 3.5f), P(16f, 7.5f), 1.7f * u, StrokeCap.Round)
            crescent(11.5f, 15.3f, 3.3f, accent); star8(16.6f, 13.6f, 1.2f, accent, true)
        }
        HI.WEATHER -> {
            sun(9f, 9f, 3.6f)
            val cl = path { mt(8.5f, 20f); qt(5f, 20f, 5.4f, 16.6f); qt(5.8f, 13.6f, 9f, 14f); qt(10.4f, 10.8f, 14f, 11f); qt(17.6f, 11.4f, 18f, 14.6f); qt(21f, 15f, 20.6f, 17.8f); qt(20.2f, 20f, 17.6f, 20f); close() }
            drawPath(cl, Color.White.copy(alpha = 0.001f)); drawPath(cl, accent.copy(alpha = 0.35f)); drawPath(cl, tint, style = st)
        }
        HI.SETTINGS -> {
            drawPath(starPath(ox + 12 * u, oy + 12 * u, 9.5f * u, 0f), soft)
            drawPath(starPath(ox + 12 * u, oy + 12 * u, 9.5f * u, 0f), tint, style = st)
            drawCircle(accent, 3.2f * u, P(12f, 12f), style = st)
        }
        HI.MOSQUE, HI.LOCATION -> {
            if (icon == HI.LOCATION) {
                val pin = path { mt(12f, 22.5f); qt(4f, 14f, 4f, 9.5f); qt(4f, 2f, 12f, 2f); qt(20f, 2f, 20f, 9.5f); qt(20f, 14f, 12f, 22.5f); close() }
                drawPath(pin, soft); drawPath(pin, tint, style = st)
                drawPath(path { mt(8.5f, 12.5f); qt(8.5f, 6.5f, 12f, 6f); qt(15.5f, 6.5f, 15.5f, 12.5f); close() }, accent)
            } else {
                val dome = path { mt(6f, 13f); qt(6f, 6.8f, 12f, 5.8f); qt(18f, 6.8f, 18f, 13f); close() }
                drawPath(dome, soft); drawPath(dome, tint, style = st)
                drawLine(tint, P(12f, 5.8f), P(12f, 3.4f), 1.3f * u)
                crescent(12f, 2.4f, 1.2f, accent)
                drawPath(path { mt(4.5f, 21f); lt(4.5f, 13f); lt(19.5f, 13f); lt(19.5f, 21f) }, tint, style = st)
                drawPath(path { mt(10f, 21f); lt(10f, 17f); qt(12f, 14.6f, 14f, 17f); lt(14f, 21f) }, accent, style = thin)
                drawPath(path { mt(1.5f, 21f); lt(1.5f, 9f); lt(2.6f, 6.8f); lt(3.7f, 9f); lt(3.7f, 21f) }, tint, style = thin)
                drawPath(path { mt(20.3f, 21f); lt(20.3f, 9f); lt(21.4f, 6.8f); lt(22.5f, 9f); lt(22.5f, 21f) }, tint, style = thin)
                drawLine(tint, P(1f, 21.2f), P(23f, 21.2f), 1.5f * u, StrokeCap.Round)
            }
        }
        HI.HATM -> {
            drawArc(tint.copy(alpha = 0.22f), 0f, 360f, false, P(2.5f, 2.5f), Size(19f * u, 19f * u), style = st)
            drawArc(accent, -90f, 250f, false, P(2.5f, 2.5f), Size(19f * u, 19f * u), style = Stroke(2.2f * u, cap = StrokeCap.Round))
            val l = path { mt(12f, 9f); qt(9.5f, 7.6f, 7f, 8f); lt(7f, 15.5f); qt(9.5f, 15f, 12f, 16.4f); close() }
            val r = path { mt(12f, 9f); qt(14.5f, 7.6f, 17f, 8f); lt(17f, 15.5f); qt(14.5f, 15f, 12f, 16.4f); close() }
            drawPath(l, tint, style = thin); drawPath(r, tint, style = thin)
        }
        HI.RAMADAN -> {
            // Fonus (chiroq)
            drawLine(tint, P(12f, 1.5f), P(12f, 4f), 1.3f * u)
            drawPath(path { mt(8f, 6.5f); lt(12f, 4f); lt(16f, 6.5f); close() }, tint)
            val body = path { mt(8f, 7f); lt(16f, 7f); lt(17.5f, 12f); lt(16f, 18.5f); lt(8f, 18.5f); lt(6.5f, 12f); close() }
            drawPath(body, accent.copy(alpha = 0.35f)); drawPath(body, tint, style = st)
            drawPath(path { mt(12f, 7f); lt(12f, 18.5f); mt(9.2f, 7f); lt(9f, 18.5f); mt(14.8f, 7f); lt(15f, 18.5f) }, tint, style = thin)
            drawCircle(Color(0xFFFFD86B), 1.5f * u, P(12f, 13f))
            drawPath(path { mt(9f, 18.5f); lt(10f, 21.5f); lt(14f, 21.5f); lt(15f, 18.5f) }, tint, style = st)
        }
        HI.JOURNAL -> {
            // Joynamoz
            val mat = path { mt(6f, 22f); lt(6f, 7f); qt(6f, 2f, 12f, 2f); qt(18f, 2f, 18f, 7f); lt(18f, 22f); close() }
            drawPath(mat, soft); drawPath(mat, tint, style = st)
            val inner = arch(8.6f, 15.4f, 5f, 19.5f)
            drawPath(inner, accent, style = thin)
            star8(12f, 13.5f, 2f, accent, true)
            for (x in listOf(7.5f, 10f, 12.5f, 15f)) drawLine(tint, P(x + 0.5f, 22f), P(x + 0.5f, 23.4f), 1f * u)
        }
        HI.SUNRISE, HI.SUNSET -> {
            drawArc(soft, 180f, 180f, true, P(5f, 9f), Size(14f * u, 14f * u))
            drawArc(tint, 180f, 180f, false, P(5f, 9f), Size(14f * u, 14f * u), style = st)
            drawLine(tint, P(2f, 16f), P(22f, 16f), 1.7f * u, StrokeCap.Round)
            drawLine(tint.copy(alpha = 0.6f), P(5f, 19.5f), P(19f, 19.5f), 1.3f * u, StrokeCap.Round)
            val up = icon == HI.SUNRISE
            drawPath(path {
                if (up) { mt(12f, 7f); lt(12f, 1.8f); mt(9.6f, 4.2f); lt(12f, 1.8f); lt(14.4f, 4.2f) }
                else { mt(12f, 1.8f); lt(12f, 7f); mt(9.6f, 4.6f); lt(12f, 7f); lt(14.4f, 4.6f) }
            }, accent, style = st)
        }
        HI.FAJR -> {
            drawLine(tint, P(2f, 18f), P(22f, 18f), 1.7f * u, StrokeCap.Round)
            drawArc(soft, 180f, 180f, true, P(6f, 13f), Size(12f * u, 10f * u))
            crescent(12f, 8.5f, 4.5f, tint); star8(18f, 5f, 1.4f, accent, true)
            drawLine(tint.copy(alpha = 0.5f), P(6f, 21f), P(18f, 21f), 1.2f * u, StrokeCap.Round)
        }
        HI.DHUHR -> sun(12f, 12f, 4.6f)
        HI.ASR -> {
            sun(9.5f, 9f, 3.8f)
            drawLine(tint, P(3f, 20f), P(21f, 20f), 1.7f * u, StrokeCap.Round)
            drawPath(path { mt(15f, 20f); lt(15f, 14f); lt(16f, 13f); lt(17f, 14f); lt(17f, 20f) }, tint, style = thin)
            drawLine(accent, P(17f, 20f), P(22.5f, 22.4f), 1.4f * u, StrokeCap.Round)
        }
        HI.ISHA -> {
            crescent(11f, 12f, 8f, tint)
            star8(18f, 7f, 1.6f, accent, true); star8(19.5f, 15f, 1f, accent, true); star8(14.5f, 3.6f, 0.9f, accent, true)
        }
        HI.HOME -> {
            val a = arch(4f, 20f, 2f, 21.5f)
            drawPath(a, soft); drawPath(a, tint, style = st)
            drawPath(arch(9.5f, 14.5f, 11f, 21.5f), accent, style = thin)
            star8(12f, 7.6f, 1.6f, accent, true)
        }
        HI.PRAYER -> {
            val a = arch(4f, 20f, 2f, 22f)
            drawPath(a, soft); drawPath(a, tint, style = st)
            drawCircle(tint, 5f * u, P(12f, 14f), style = thin)
            drawPath(path { mt(12f, 11f); lt(12f, 14f); lt(14.2f, 15.4f) }, accent, style = st)
        }
        HI.MORE -> {
            for ((x, y) in listOf(7f to 7f, 17f to 7f, 7f to 17f)) {
                drawPath(starPath(ox + x * u, oy + y * u, 4.4f * u, 0f), soft)
                drawPath(starPath(ox + x * u, oy + y * u, 4.4f * u, 0f), tint, style = thin)
            }
            drawPath(starPath(ox + 17 * u, oy + 17 * u, 4.4f * u, 0f), accent)
        }
        HI.ZAKAT -> {
            // Tangalar ustida o'sayotgan nihol — barakali mol
            for ((k, y) in listOf(0 to 19.5f, 1 to 16.5f, 2 to 13.5f)) {
                val e = Rect(P(4.5f + k * 0.0f, y - 1.6f), Size(11f * u, 3.6f * u))
                drawOval(if (k == 2) accent.copy(alpha = 0.5f) else soft, e.topLeft, e.size)
                drawOval(tint, e.topLeft, e.size, style = thin)
            }
            drawPath(path { mt(17f, 21f); lt(17f, 10f) }, tint, style = st)
            drawPath(path { mt(17f, 13.5f); qt(13.5f, 12.5f, 13.6f, 9f); qt(17f, 9.5f, 17f, 13.5f); close() }, accent)
            drawPath(path { mt(17f, 11.5f); qt(20.5f, 10.5f, 20.6f, 6.5f); qt(17.2f, 7f, 17f, 11.5f); close() }, accent)
        }
        HI.KAABA -> {
            val top = path { mt(4f, 7f); lt(12f, 3.5f); lt(20f, 7f); lt(12f, 10.5f); close() }
            val left = path { mt(4f, 7f); lt(12f, 10.5f); lt(12f, 21f); lt(4f, 17.5f); close() }
            val right = path { mt(20f, 7f); lt(12f, 10.5f); lt(12f, 21f); lt(20f, 17.5f); close() }
            drawPath(left, tint); drawPath(right, tint.copy(alpha = 0.8f)); drawPath(top, tint.copy(alpha = 0.6f))
            drawPath(path { mt(4f, 10f); lt(12f, 13.5f); lt(20f, 10f) }, accent, style = Stroke(1.4f * u))
            drawPath(path { mt(13.6f, 21f); lt(13.6f, 15.8f); lt(16.4f, 14.6f); lt(16.4f, 19.8f) }, accent, style = thin)
        }
        HI.FOCUS -> {
            drawPath(path { mt(2f, 12f); qt(12f, 2f, 22f, 12f); qt(12f, 22f, 2f, 12f); close() }, soft)
            drawPath(path { mt(2f, 12f); qt(12f, 2f, 22f, 12f); qt(12f, 22f, 2f, 12f); close() }, tint, style = st)
            drawPath(starPath(ox + 12 * u, oy + 12 * u, 3.6f * u, 0f), accent)
        }
    }
}

/** Mehrob ravog'i shakli (uchli ravoq) — plitkalar va tugmalar uchun. */
class ArchShape(private val shoulder: Float = 0.42f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width; val h = size.height
        val sh = (w * shoulder).coerceAtMost(h * 0.6f)
        val r = w * 0.16f
        val p = Path().apply {
            moveTo(0f, h - r)
            lineTo(0f, sh)
            quadraticTo(0f, sh * 0.22f, w / 2, 0f)
            quadraticTo(w, sh * 0.22f, w, sh)
            lineTo(w, h - r)
            quadraticTo(w, h, w - r, h)
            lineTo(r, h)
            quadraticTo(0f, h, 0f, h - r)
            close()
        }
        return Outline.Generic(p)
    }
}

/** Butun maydonga nozik islomiy geometrik naqsh (8 qirrali yulduzlar va ularni tutashtiruvchi tasma). */
fun DrawScope.drawGirih(color: Color, cell: Float, stroke: Float = 1f, offset: Offset = Offset.Zero) {
    val st = Stroke(stroke)
    var y = -cell + (offset.y % cell)
    while (y < size.height + cell) {
        var x = -cell + (offset.x % cell)
        while (x < size.width + cell) {
            val cx = x + cell / 2; val cy = y + cell / 2
            drawPath(starPath(cx, cy, cell * 0.36f, 22.5f), color, style = st)
            drawPath(starPath(cx, cy, cell * 0.20f, 0f), color, style = st)
            // burchakdagi romb va tasmalar
            drawPath(starPath(x, y, cell * 0.12f, 0f), color, style = st)
            for (k in 0 until 4) {
                val a = Math.toRadians(45.0 + 90.0 * k)
                val sx = cx + cell * 0.36f * cos(a).toFloat(); val sy = cy + cell * 0.36f * sin(a).toFloat()
                val ex = cx + cell * 0.62f * cos(a).toFloat(); val ey = cy + cell * 0.62f * sin(a).toFloat()
                drawLine(color, Offset(sx, sy), Offset(ex, ey), stroke)
            }
            x += cell
        }
        y += cell
    }
}
