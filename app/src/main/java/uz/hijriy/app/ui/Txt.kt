package uz.hijriy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import uz.hijriy.app.data.UzTranslit

/** Interfeys kirill yozuvida ko'rsatilsinmi. */
val LocalCyr = staticCompositionLocalOf { false }

/** Joriy yozuvga moslangan matn (lotin → kirill). */
@Composable
fun tr(s: String): String {
    val cyr = LocalCyr.current
    return if (cyr) remember(s) { UzTranslit.toCyrillic(s) } else s
}

fun tr(s: String, cyr: Boolean): String = if (cyr) UzTranslit.toCyrillic(s) else s

/**
 * Ilovadagi barcha matnlar shu Text orqali chiqadi: lotin/kirill tanloviga qarab avtomatik o'giriladi.
 * (material3.Text bilan bir xil parametrlar.)
 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current,
) = androidx.compose.material3.Text(
    tr(text), modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration,
    textAlign, lineHeight, overflow, softWrap, maxLines, minLines, onTextLayout, style
)

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val cyr = LocalCyr.current
    // Bezaksiz matnni o'giramiz; bezakli (Qur'on, tajvid) matnlar arabcha — o'zgarmaydi
    val t = if (cyr && text.spanStyles.isEmpty() && text.paragraphStyles.isEmpty() && inlineContent.isEmpty())
        remember(text) { AnnotatedString(UzTranslit.toCyrillic(text.text)) } else text
    androidx.compose.material3.Text(
        t, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, letterSpacing, textDecoration,
        textAlign, lineHeight, overflow, softWrap, maxLines, minLines, inlineContent, onTextLayout, style
    )
}

/** Toast va boshqa View matnlari uchun: sozlamaga qarab kirillga o'giradi. */
fun uiText(ctx: android.content.Context, s: String): String {
    val app = ctx.applicationContext as? uz.hijriy.app.HijriyApp ?: return s
    return tr(s, app.settings.value.script == 1)
}

/** Arabcha matnning o'qilishi — joriy yozuvda (lotin yoki kirill), yumshoq fon va chap chiziq bilan. */
@Composable
fun ReadingText(r: uz.hijriy.app.core.Translit.Result, modifier: Modifier = Modifier, prefix: String = "") {
    val cyr = LocalCyr.current
    val cs = androidx.compose.material3.MaterialTheme.colorScheme
    androidx.compose.foundation.layout.Row(
        modifier
            .fillMaxWidthCompat()
            .then(Modifier.background(cs.primary.copy(alpha = 0.06f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp)))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            Modifier.padding(top = 3.dp).width(3.dp).height(16.dp)
                .background(cs.primary.copy(alpha = 0.6f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
        )
        androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
        androidx.compose.material3.Text(
            prefix + if (cyr) r.cyrillic else r.latin,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                fontStyle = FontStyle.Italic, lineHeight = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp)
            ),
            color = cs.onSurface.copy(alpha = 0.85f)
        )
    }
}

private fun Modifier.fillMaxWidthCompat() = this.fillMaxWidth()
