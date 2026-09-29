package uz.hijriy.app.ui

import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import uz.hijriy.app.core.Uz
import uz.hijriy.app.ui.theme.QuranFont
import kotlin.math.cos
import kotlin.math.sin

/**
 * Oyat oxiri belgisi — shriftga bog'liq bo'lmagan, o'zimiz chizadigan medalyon (doira ichida arabcha raqam).
 * Matnga `appendInlineContent("ayah:N")` orqali qo'shiladi.
 */
const val AYAH_PREFIX = "ayah:"
val AyahPlaceholder = Placeholder(1.35.em, 1.35.em, PlaceholderVerticalAlign.TextCenter)
private val MARK = Regex("۝([٠-٩0-9]+)")

fun AnnotatedString.Builder.appendAyahMark(n: Int) = appendInlineContent("$AYAH_PREFIX$n", "﴿$n﴾")

/** Matndagi "۝N" belgilarini medalyonga almashtiradi. [ranges] ga placeholder o'rinlari yoziladi (o'lchash uchun). */
fun withAyahMarks(text: String, ranges: MutableList<AnnotatedString.Range<Placeholder>>? = null): AnnotatedString = buildAnnotatedString {
    var last = 0
    for (m in MARK.findAll(text)) {
        append(text.substring(last, m.range.first))
        val digits = m.groupValues[1].map { c -> if (c in '٠'..'٩') '0' + (c - '٠') else c }.joinToString("")
        val start = length
        appendAyahMark(digits.toIntOrNull() ?: 0)
        ranges?.add(AnnotatedString.Range(AyahPlaceholder, start, length))
        last = m.range.last + 1
    }
    append(text.substring(last))
}

@Composable
fun AyahMedallion(n: Int, color: Color) {
    val tm = rememberTextMeasurer()
    val digits = remember(n) { Uz.arabicNumber(n) }
    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension / 2f
        drawCircle(color.copy(alpha = 0.10f), r * 0.86f)
        drawCircle(color, r * 0.86f, style = Stroke(r * 0.09f))
        drawCircle(color.copy(alpha = 0.55f), r * 0.68f, style = Stroke(r * 0.04f))
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            drawCircle(color, r * 0.08f, Offset(center.x + r * 0.93f * cos(a).toFloat(), center.y + r * 0.93f * sin(a).toFloat()))
        }
        val k = when (digits.length) { 1 -> 0.62f; 2 -> 0.50f; else -> 0.40f }
        val fontPx = size.height * k
        val layout = tm.measure(digits, TextStyle(color = color, fontFamily = QuranFont, fontSize = fontPx.toSp()))
        // Raqamlar asosiy chiziqdan ~0.35 em yuqorida markazlanadi
        val top = center.y - (layout.firstBaseline - fontPx * 0.36f)
        drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, top))
    }
}

/** Text(inlineContent = ...) uchun medalyonlar xaritasi (1..286). */
@Composable
fun rememberAyahInline(color: Color): Map<String, InlineTextContent> = remember(color) {
    (0..286).associate { n -> "$AYAH_PREFIX$n" to InlineTextContent(AyahPlaceholder) { AyahMedallion(n, color) } }
}
