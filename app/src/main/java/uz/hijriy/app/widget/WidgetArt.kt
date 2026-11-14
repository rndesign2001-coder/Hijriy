package uz.hijriy.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import uz.hijriy.app.ui.drawGirih
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** Vidjet uchun rasmlar: naqshli fon, quyosh yo'li va ikonkalar (Compose chizish API'si bitmapga). */
object WidgetArt {
    private fun render(context: Context, w: Int, h: Int, block: DrawScope.() -> Unit): Bitmap {
        val img = ImageBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1))
        val d = Density(context.resources.displayMetrics.density)
        CanvasDrawScope().draw(d, LayoutDirection.Ltr, Canvas(img), Size(w.toFloat(), h.toFloat()), block)
        return img.asAndroidBitmap()
    }

    fun background(context: Context, wDp: Int, hDp: Int, sunrise: Int, sunset: Int, minute: Float): Bitmap {
        val dens = context.resources.displayMetrics.density
        // Xotira chegarasi: juda katta bo'lmasin
        val scale = minOf(dens, 1100f / wDp.coerceAtLeast(1))
        val w = (wDp * scale).roundToInt(); val h = (hDp * scale).roundToInt()
        val isDay = minute in sunrise.toFloat()..sunset.toFloat()
        return render(context, w, h) {
            val r = 24f * scale
            val shape = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }
            clipPath(shape) {
                val colors = if (isDay) listOf(Color(0xFF0B5D48), Color(0xFF0F7B5F), Color(0xFF1C6E86))
                else listOf(Color(0xFF0B1030), Color(0xFF1C2459), Color(0xFF3A2C6E))
                drawRect(Brush.linearGradient(colors, Offset.Zero, Offset(size.width, size.height)))
                drawGirih(Color.White.copy(alpha = 0.07f), 46f * scale, 0.9f * scale)
                // Quyosh yo'li: pastki kataklar ustida
                val l = 18f * scale; val rr = size.width - 18f * scale
                val horizon = size.height - 78f * scale
                val top = 50f * scale
                val hh = (horizon - top).coerceAtLeast(10f)
                drawLine(Color.White.copy(alpha = 0.35f), Offset(l, horizon), Offset(rr, horizon), 1f * scale)
                val path = Path()
                for (k in 0..60) {
                    val f = k / 60f
                    val x = l + (rr - l) * f; val y = horizon - hh * sin(PI * f).toFloat()
                    if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, Color.White.copy(alpha = 0.3f), style = Stroke(1.3f * scale, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f * scale, 6f * scale))))
                drawLine(Color(0xFFF3D57E), Offset(l, horizon - 6 * scale), Offset(l, horizon + 3 * scale), 2f * scale, StrokeCap.Round)
                drawLine(Color(0xFFFFB38A), Offset(rr, horizon - 6 * scale), Offset(rr, horizon + 3 * scale), 2f * scale, StrokeCap.Round)
                if (isDay) {
                    val f = (minute - sunrise) / (sunset - sunrise)
                    val c = Offset(l + (rr - l) * f, horizon - hh * sin(PI * f).toFloat())
                    drawCircle(Brush.radialGradient(listOf(Color(0x99FFF3B0), Color.Transparent), c, 18f * scale), 18f * scale, c)
                    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFFBE6), Color(0xFFFFC93C)), c, 7f * scale), 7f * scale, c)
                }
                drawRoundRect(Color(0x55E9C46A), cornerRadius = CornerRadius(r), style = Stroke(1.2f * scale))
            }
        }
    }

    /** Fon ustidagi naqsh: girih, oltin hoshiya; tunda — oy va yulduzlar. */
    fun overlay(context: Context, wDp: Int, hDp: Int, night: Boolean): Bitmap {
        val dens = context.resources.displayMetrics.density
        val scale = minOf(dens, 700f / wDp.coerceAtLeast(1))
        val w = (wDp * scale).roundToInt(); val h = (hDp * scale).roundToInt()
        return render(context, w, h) {
            val r = 24f * scale
            val shape = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, CornerRadius(r))) }
            clipPath(shape) {
                drawGirih(Color.White.copy(alpha = 0.06f), 40f * scale, 0.8f * scale)
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent), 0f, size.height * 0.5f))
                if (night) {
                    // Yarim oy (o'ng yuqori burchakda)
                    val c = Offset(size.width - 26f * scale, 26f * scale); val rad = 12f * scale
                    drawCircle(Brush.radialGradient(listOf(Color(0x55F3D57E), Color.Transparent), c, rad * 2.4f), rad * 2.4f, c)
                    val full = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, rad)) }
                    val cut = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(c.x + rad * 0.55f, c.y - rad * 0.3f), rad * 0.95f)) }
                    val moon = Path().apply { op(full, cut, androidx.compose.ui.graphics.PathOperation.Difference) }
                    drawPath(moon, Color(0xFFF3D57E))
                    val stars = listOf(0.12f to 0.18f, 0.22f to 0.62f, 0.85f to 0.72f, 0.65f to 0.12f, 0.08f to 0.85f)
                    for ((fx, fy) in stars) drawCircle(Color.White.copy(alpha = 0.5f), 1.3f * scale, Offset(size.width * fx, size.height * fy))
                }
                drawRoundRect(Color(0x55E9C46A), cornerRadius = CornerRadius(r), style = Stroke(1.2f * scale))
            }
        }
    }
}
