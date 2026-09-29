package uz.hijriy.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Dizaynni ko'rib chiqish uchun asosiy ekranlarning rasmlari (build/shots). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()
    private val dir = File("build/shots").apply { mkdirs() }

    private fun settle(ms: Long = 1500) {
        var left = ms
        while (left > 0) { rule.mainClock.advanceTimeBy(minOf(500, left)); left -= 500 }
        rule.waitForIdle()
    }

    private fun shot(name: String) {
        val img = rule.onRoot().captureToImage().asAndroidBitmap()
        File(dir, "$name.png").outputStream().use { img.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("SHOT $name")
    }

    private fun click(t: String, sub: Boolean = false) {
        val n = rule.onAllNodesWithText(t, substring = sub).onFirst()
        rule.mainClock.autoAdvance = true
        runCatching { n.performScrollTo() }
        rule.mainClock.autoAdvance = false
        n.performClick(); settle()
    }

    private fun back() { rule.activity.onBackPressedDispatcher.onBackPressed(); settle() }

    @Test
    fun screens() {
        rule.mainClock.autoAdvance = false
        val app = rule.activity.application as HijriyApp
        settle(1200); shot("01_intro")
        settle(2400)
        rule.onAllNodesWithTag("intro").fetchSemanticsNodes().takeIf { it.isNotEmpty() }?.let {
            rule.onAllNodesWithTag("intro").onFirst().performClick(); settle()
        }
        rule.waitUntil(30_000) { app.quran.value != null }
        settle(); shot("02_home")
        click("Namoz"); shot("03_prayer")
        click("Yana"); click("Qibla kompasi")
        shot("04_qibla_islomiy")
        click("Klassik"); shot("05_qibla_klassik")
        click("Zamonaviy"); shot("06_qibla_zamonaviy")
        back()
        click("Tasbeh"); shot("07_tasbeh"); back()
        click("Duolar va zikrlar"); shot("08_duas"); back()
        click("Rasm tayyorlash"); settle(2000); shot("09_wallpaper"); back()
        app.settings.update { it.copy(themeMode = uz.hijriy.app.data.ThemeMode.DARK) }
        settle()
        click("Asosiy"); settle(); shot("10_home_dark")
        click("Qur'on"); shot("11_quran_list")
        click("Fotiha"); settle(); shot("12_reader")
        back()
        app.settings.update { it.copy(themeMode = uz.hijriy.app.data.ThemeMode.LIGHT) }
        click("Asosiy"); click("Mushaf"); settle(3000); shot("13_mushaf")
    }

    @Test
    fun wallpapers() {
        rule.mainClock.autoAdvance = false
        settle(4000)
        val app = rule.activity.application as HijriyApp
        val tm = androidx.compose.ui.text.TextMeasurer(
            androidx.compose.ui.text.font.createFontFamilyResolver(rule.activity),
            androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, 0
        )
        rule.runOnUiThread {
            for (layout in 0..3) {
                val img = uz.hijriy.app.ui.renderToBitmap(
                    tm, uz.hijriy.app.ui.WpOptions(bg = layout, layout = layout, font = layout, color = if (layout == 3) 1 else 0),
                    app.settings.value, java.time.LocalDate.now(), null, 0.5f
                ).asAndroidBitmap()
                File(dir, "wp_$layout.png").outputStream().use { img.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }
}
