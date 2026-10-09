package uz.hijriy.app.ui

import androidx.compose.foundation.text.InlineTextContent
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
