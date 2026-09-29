package uz.hijriy.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 8 qirrali yulduz (Rub al-hizb) konturi. */
fun starPath(cx: Float, cy: Float, r: Float, rotDeg: Float = 0f): Path {
    val p = Path()
    for (sq in 0..1) {
        val base = rotDeg + sq * 45f
        for (i in 0..3) {
            val a = Math.toRadians((base + 90 * i).toDouble())
            val x = cx + r * cos(a).toFloat()
            val y = cy + r * sin(a).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
    }
    return p
}

/** Butun maydonni qoplaydigan geometrik islomiy naqsh. */
fun DrawScope.drawStarLattice(color: Color, cell: Float, stroke: Float, rotation: Float = 0f, pivot: Offset = center) {
    rotate(rotation, pivot) {
        val ext = size.maxDimension * 0.75f
        var y = pivot.y - ext
        var row = 0
        while (y < pivot.y + ext) {
            var x = pivot.x - ext + if (row % 2 == 0) 0f else cell / 2
            while (x < pivot.x + ext) {
                drawPath(starPath(x, y, cell * 0.42f), color, style = Stroke(stroke))
                drawCircle(color, cell * 0.14f, Offset(x, y), style = Stroke(stroke * 0.8f))
                x += cell
            }
            y += cell * 0.5f
            row++
        }
    }
}

/** Masjid silueti: markaziy gumbaz, ikki yon gumbaz va ikki minora. [base] — pastki chiziq. */
fun mosquePath(w: Float, base: Float, h: Float, left: Float = 0f): Path {
    val p = Path()
    val u = w / 100f
    fun X(v: Float) = left + v * u
    fun Y(v: Float) = base - v * h / 100f
    p.moveTo(X(0f), Y(0f))
    p.lineTo(X(0f), Y(14f))
    // chap minora
    p.lineTo(X(10f), Y(14f)); p.lineTo(X(10f), Y(62f)); p.lineTo(X(9f), Y(64f)); p.lineTo(X(11.5f), Y(80f))
    p.lineTo(X(14f), Y(64f)); p.lineTo(X(13f), Y(62f)); p.lineTo(X(13f), Y(22f))
    // chap kichik gumbaz
    p.lineTo(X(20f), Y(22f)); p.quadraticTo(X(20f), Y(38f), X(28f), Y(40f)); p.quadraticTo(X(36f), Y(38f), X(36f), Y(22f))
    // markaziy gumbaz
    p.lineTo(X(36f), Y(30f)); p.lineTo(X(35f), Y(30f))
    p.cubicTo(X(35f), Y(52f), X(46f), Y(58f), X(50f), Y(62f))
    p.lineTo(X(50f), Y(70f)); p.lineTo(X(50.8f), Y(70f)); p.lineTo(X(50.8f), Y(62f))
    p.cubicTo(X(54f), Y(58f), X(65f), Y(52f), X(65f), Y(30f))
    p.lineTo(X(64f), Y(30f)); p.lineTo(X(64f), Y(22f))
    // o'ng kichik gumbaz
    p.quadraticTo(X(64f), Y(38f), X(72f), Y(40f)); p.quadraticTo(X(80f), Y(38f), X(80f), Y(22f))
    p.lineTo(X(87f), Y(22f)); p.lineTo(X(87f), Y(62f)); p.lineTo(X(86f), Y(64f)); p.lineTo(X(88.5f), Y(80f))
    p.lineTo(X(91f), Y(64f)); p.lineTo(X(90f), Y(62f)); p.lineTo(X(90f), Y(14f))
    p.lineTo(X(100f), Y(14f)); p.lineTo(X(100f), Y(0f))
    p.close()
    return p
}

/** Hilol (yarim oy). */
fun crescentPath(cx: Float, cy: Float, r: Float): Path {
    val outer = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(cx, cy), r)) }
    val inner = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(cx + r * 0.42f, cy - r * 0.22f), r * 0.86f)) }
    return Path().apply { op(outer, inner, androidx.compose.ui.graphics.PathOperation.Difference) }
}

/** Takrorlanadigan "tasodifiy" yulduzlar joylashuvi (har chizishda bir xil). */
class StarField(n: Int, seed: Int = 7) {
    val pts: List<Triple<Float, Float, Float>> = run {
        var s = seed.toLong()
        fun rnd(): Float { s = (s * 1103515245 + 12345) and 0x7fffffff; return (s % 10000) / 10000f }
        List(n) { Triple(rnd(), rnd(), rnd()) }
    }

    fun DrawScope.draw(color: Color, phase: Float, heightFraction: Float = 1f, maxR: Float = 2.4f) {
        for ((x, y, k) in pts) {
            val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin((phase * 2 * PI + k * 20).toFloat()))
            drawCircle(color.copy(alpha = color.alpha * tw), 0.6f + k * maxR, Offset(x * size.width, y * size.height * heightFraction))
        }
    }
}

fun Size.minHalf() = minDimension / 2f
