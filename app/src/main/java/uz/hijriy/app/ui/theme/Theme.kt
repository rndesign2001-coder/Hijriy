package uz.hijriy.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import uz.hijriy.app.R
import uz.hijriy.app.data.ThemeMode

val QuranFont = FontFamily(Font(R.font.amiri_quran, FontWeight.Normal))

data class Palette(
    val title: String,
    val primary: Color,
    val primaryDark: Color,
    val accent: Color,
    val gradLight: List<Color>,
    val gradDark: List<Color>,
    val bgLight: Color,
    val bgDark: Color,
)

val Palettes = listOf(
    Palette(
        "Zumrad", Color(0xFF0F7B5F), Color(0xFF5FD4AE), Color(0xFFC9A227),
        listOf(Color(0xFF0F7B5F), Color(0xFF0A4F3E)), listOf(Color(0xFF12483B), Color(0xFF0A241E)),
        Color(0xFFF3F7F5), Color(0xFF0D1412)
    ),
    Palette(
        "Firuza", Color(0xFF0E6BA8), Color(0xFF7CC4F2), Color(0xFFE0A93B),
        listOf(Color(0xFF1178B8), Color(0xFF0A3E66)), listOf(Color(0xFF123A57), Color(0xFF08182A)),
        Color(0xFFF2F6FA), Color(0xFF0B1219)
    ),
    Palette(
        "Oltin", Color(0xFF8A5A12), Color(0xFFE8B866), Color(0xFF0F7B5F),
        listOf(Color(0xFFA56B16), Color(0xFF5E3A08)), listOf(Color(0xFF4A3212), Color(0xFF1E1407)),
        Color(0xFFFAF6EE), Color(0xFF15110B)
    ),
    Palette(
        "Binafsha", Color(0xFF5B3FA8), Color(0xFFB9A3F5), Color(0xFFD4A63A),
        listOf(Color(0xFF6A4BC0), Color(0xFF34206E)), listOf(Color(0xFF32245E), Color(0xFF140E26)),
        Color(0xFFF6F4FB), Color(0xFF110E18)
    ),
)

data class Extra(val dark: Boolean, val palette: Palette) {
    val headerGradient: List<Color> get() = if (dark) palette.gradDark else palette.gradLight
}

val LocalExtra = staticCompositionLocalOf { Extra(false, Palettes[0]) }

private fun scheme(p: Palette, dark: Boolean): ColorScheme = if (!dark) lightColorScheme(
    primary = p.primary,
    onPrimary = Color.White,
    primaryContainer = p.primary.copy(alpha = 0.14f).compositeOver(p.bgLight),
    onPrimaryContainer = p.primary.darken(0.45f),
    secondary = p.accent,
    onSecondary = Color.White,
    secondaryContainer = p.accent.copy(alpha = 0.18f).compositeOver(p.bgLight),
    onSecondaryContainer = p.accent.darken(0.5f),
    tertiary = p.accent,
    background = p.bgLight,
    onBackground = Color(0xFF15201C),
    surface = Color.White,
    onSurface = Color(0xFF15201C),
    surfaceVariant = p.primary.copy(alpha = 0.06f).compositeOver(Color.White),
    onSurfaceVariant = Color(0xFF55615C),
    surfaceContainer = p.primary.copy(alpha = 0.05f).compositeOver(Color.White),
    surfaceContainerHigh = p.primary.copy(alpha = 0.08f).compositeOver(Color.White),
    surfaceContainerLow = Color.White,
    outline = Color(0xFFB9C4BF),
    outlineVariant = Color(0xFFDDE5E1),
) else darkColorScheme(
    primary = p.primaryDark,
    onPrimary = Color(0xFF06140F),
    primaryContainer = p.primaryDark.copy(alpha = 0.18f).compositeOver(p.bgDark),
    onPrimaryContainer = p.primaryDark,
    secondary = p.accent.lighten(0.25f),
    onSecondary = Color(0xFF1A1405),
    secondaryContainer = p.accent.copy(alpha = 0.2f).compositeOver(p.bgDark),
    onSecondaryContainer = p.accent.lighten(0.4f),
    tertiary = p.accent.lighten(0.25f),
    background = p.bgDark,
    onBackground = Color(0xFFE4ECE8),
    surface = p.bgDark.lighten(0.04f),
    onSurface = Color(0xFFE4ECE8),
    surfaceVariant = p.primaryDark.copy(alpha = 0.08f).compositeOver(p.bgDark),
    onSurfaceVariant = Color(0xFFA7B3AE),
    surfaceContainer = p.primaryDark.copy(alpha = 0.07f).compositeOver(p.bgDark.lighten(0.03f)),
    surfaceContainerHigh = p.primaryDark.copy(alpha = 0.11f).compositeOver(p.bgDark.lighten(0.05f)),
    surfaceContainerLow = p.bgDark.lighten(0.03f),
    outline = Color(0xFF45524D),
    outlineVariant = Color(0xFF2B3531),
)

fun Color.compositeOver(bg: Color): Color {
    val a = alpha
    return Color(
        red * a + bg.red * (1 - a),
        green * a + bg.green * (1 - a),
        blue * a + bg.blue * (1 - a),
        1f
    )
}

fun Color.darken(f: Float) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)
fun Color.lighten(f: Float) = Color(red + (1 - red) * f, green + (1 - green) * f, blue + (1 - blue) * f, alpha)

private val AppTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

val ArabicTitle = TextStyle(fontFamily = QuranFont, fontSize = 22.sp)

@Composable
fun HijriyTheme(mode: ThemeMode, paletteIndex: Int, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val p = Palettes[paletteIndex.coerceIn(0, Palettes.lastIndex)]
    androidx.compose.runtime.CompositionLocalProvider(LocalExtra provides Extra(dark, p)) {
        MaterialTheme(colorScheme = scheme(p, dark), typography = AppTypography, content = content)
    }
}
