package uz.hijriy.app

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.text.drawText
import uz.hijriy.app.ui.drawHIcon
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
        val img = runCatching {
            var b: Bitmap? = null
            rule.runOnUiThread {
                val v = rule.activity.window.decorView
                b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
                v.draw(android.graphics.Canvas(b!!))
            }
            b!!
        }.onFailure { println("SHOT-ERR $name: $it") }.getOrNull() ?: return
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
        rule.onAllNodesWithText("Konvertor").onFirst().let { rule.mainClock.autoAdvance = true; runCatching { it.performScrollTo() }; rule.mainClock.autoAdvance = false }
        settle(); shot("02b_home_pastki")
        rule.onAllNodesWithText("Quyosh va ob-havo").onFirst().let { rule.mainClock.autoAdvance = true; runCatching { it.performScrollTo() }; rule.mainClock.autoAdvance = false }
        settle(); shot("02c_home_quyosh")
        for ((r, n) in listOf("hatm" to "20_hatm", "journal" to "21_journal", "ramadan" to "22_ramadan", "mosques" to "23_mosques", "credits" to "24_credits", "more" to "25_more", "hajj" to "30_hajj", "zakat" to "31_zakat")) {
            app.pendingRoute.value = r; settle(2500); shot(n); back()
        }
        app.pendingRoute.value = "location"; settle(); click("Dunyo shaharlari"); shot("26_location_world"); back()
        app.pendingRoute.value = "focus/1?ayah=5"; settle(2500); shot("27_focus"); back()
        app.settings.update { it.copy(bgIndex = 2) }; settle(2500); shot("28_home_bg2")
        app.settings.update { it.copy(bgIndex = 0) }; settle(1500); shot("29_home_sky")
        app.settings.update { it.copy(bgIndex = 1) }; settle()
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
        app.settings.update { it.copy(script = 1) }; settle(); shot("10b_home_kirill")
        click("Намоз"); shot("10c_prayer_kirill")
        app.settings.update { it.copy(script = 0) }; settle()
        app.settings.update { it.copy(quranReading = true) }
        click("Qur'on"); shot("11_quran_list")
        click("Fotiha"); settle(); shot("12_reader")
        back()
        app.settings.update { it.copy(themeMode = uz.hijriy.app.data.ThemeMode.LIGHT) }
        app.pendingRoute.value = "mushaf/50"; settle(3000); shot("13_mushaf")
        back()
        app.pendingRoute.value = "mushaf/4"; settle(3000); shot("13b_mushaf_tajvid")
        back()
        app.pendingRoute.value = "duas"; settle(); shot("14_duas_dark")
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

    /** Ilovadagi barcha ikonkalar bitta varaqda (dizaynerga yuborish uchun). */
    @Test
    fun iconSheet() {
        rule.mainClock.autoAdvance = false
        settle(1000)
        val labels = mapOf(
            "QURAN" to "Qur'on", "MUSHAF" to "Mushaf", "QIBLA" to "Qibla", "TASBEH" to "Tasbeh", "NAMES" to "99 ism",
            "DUA" to "Duolar", "WALLPAPER" to "Rasm", "QAZO" to "Qazo", "CONVERTER" to "Konvertor", "CALENDAR" to "Taqvim",
            "WEATHER" to "Ob-havo", "SETTINGS" to "Sozlamalar", "MOSQUE" to "Masjidlar", "HATM" to "Hatm", "RAMADAN" to "Ramazon",
            "JOURNAL" to "Kundalik", "SUNRISE" to "Quyosh chiqishi", "SUNSET" to "Shom / botishi", "FAJR" to "Bomdod", "DHUHR" to "Peshin",
            "ASR" to "Asr", "ISHA" to "Xufton", "HOME" to "Asosiy (panel)", "PRAYER" to "Namoz (panel)", "MORE" to "Yana (panel)",
            "FOCUS" to "Fokus", "PHOTO" to "Fon", "LOCATION" to "Joylashuv", "ZAKAT" to "Zakot", "KAABA" to "Haj va Umra",
        )
        val tm = androidx.compose.ui.text.TextMeasurer(
            androidx.compose.ui.text.font.createFontFamilyResolver(rule.activity),
            androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, 0
        )
        rule.runOnUiThread {
            val all = uz.hijriy.app.ui.HI.entries
            val cols = 6; val cw = 260f; val ch = 300f; val top = 120f
            val rows = (all.size + cols - 1) / cols
            val img = androidx.compose.ui.graphics.ImageBitmap((cols * cw).toInt(), (top + rows * ch + 40).toInt())
            androidx.compose.ui.graphics.drawscope.CanvasDrawScope().draw(
                androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr,
                androidx.compose.ui.graphics.Canvas(img), androidx.compose.ui.geometry.Size(img.width.toFloat(), img.height.toFloat())
            ) {
                drawRect(androidx.compose.ui.graphics.Color(0xFFFBF6E9))
                val title = tm.measure("Hijriy Taqvim — ikonkalar (${all.size} ta)", androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(44f, androidx.compose.ui.unit.TextUnitType.Sp), color = androidx.compose.ui.graphics.Color(0xFF0B5D48)))
                drawText(title, topLeft = androidx.compose.ui.geometry.Offset(40f, 36f))
                all.forEachIndexed { i, ic ->
                    val x = (i % cols) * cw; val y = top + (i / cols) * ch
                    drawRoundRect(androidx.compose.ui.graphics.Color.White, androidx.compose.ui.geometry.Offset(x + 14, y + 10), androidx.compose.ui.geometry.Size(cw - 28, ch - 20), androidx.compose.ui.geometry.CornerRadius(28f))
                    drawRoundRect(androidx.compose.ui.graphics.Color(0x33C8962E), androidx.compose.ui.geometry.Offset(x + 14, y + 10), androidx.compose.ui.geometry.Size(cw - 28, ch - 20), androidx.compose.ui.geometry.CornerRadius(28f), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                    androidx.compose.ui.graphics.drawscope.translate(x + (cw - 160f) / 2, y + 34f) {
                        androidx.compose.ui.graphics.drawscope.inset(0f, 0f, size.width - 160f, size.height - 160f) {
                            run { drawHIcon(ic, androidx.compose.ui.graphics.Color(0xFF0B5D48), androidx.compose.ui.graphics.Color(0xFFC8962E)) }
                        }
                    }
                    val l1 = tm.measure(labels[ic.name] ?: ic.name, androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(24f, androidx.compose.ui.unit.TextUnitType.Sp), color = androidx.compose.ui.graphics.Color(0xFF1B1B1B)))
                    drawText(l1, topLeft = androidx.compose.ui.geometry.Offset(x + (cw - l1.size.width) / 2, y + 206f))
                    val l2 = tm.measure("HI.${ic.name}", androidx.compose.ui.text.TextStyle(fontSize = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp), color = androidx.compose.ui.graphics.Color(0xFF8A8A8A)))
                    drawText(l2, topLeft = androidx.compose.ui.geometry.Offset(x + (cw - l2.size.width) / 2, y + 242f))
                }
            }
            File(dir, "icons_sheet.png").outputStream().use { img.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
