package uz.hijriy.app

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import uz.hijriy.app.data.QuranRepo
import uz.hijriy.app.data.Regions
import uz.hijriy.app.data.TajweedRule

/**
 * Barcha ekranlarni ochib chiqadigan "tutun" testi — ilova qulamasligini tekshiradi.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreensSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun settle() {
        repeat(8) { rule.mainClock.advanceTimeByFrame() }
        rule.waitForIdle()
    }

    private fun clickText(t: String) {
        val n = rule.onAllNodesWithText(t).onFirst()
        runCatching { n.performScrollTo() }
        n.performClick()
        settle()
    }

    private fun exists(t: String, substring: Boolean = false): Boolean =
        rule.onAllNodesWithText(t, substring = substring).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun allScreensOpen() {
        rule.mainClock.autoAdvance = false
        settle()
        val app = rule.activity.application as HijriyApp
        rule.waitUntil(30_000) { app.quran.value != null }
        settle()

        // Bosh sahifa
        assertTrue(exists("Keyingi", substring = true))
        assertTrue(exists("Bomdod"))

        // Namoz
        clickText("Namoz")
        assertTrue(exists("Namoz vaqtlari"))
        clickText("Peshin")          // sozlash oynasi
        assertTrue(exists("Peshin vaqtini sozlash"))
        clickText("+5")
        clickText("Saqlash")
        assertEquals(5, app.settings.value.adjust[uz.hijriy.app.core.Prayer.DHUHR]?.offset)
        clickText("Oylik jadval")
        assertTrue(exists("Sana"))

        // Qur'on
        clickText("Qur'on")
        assertTrue(exists("Qur'oni Karim"))
        clickText("Fotiha")
        assertTrue(exists("1. Fotiha"))
        clickText("Tajvid")
        rule.activity.onBackPressedDispatcher.onBackPressed()
        settle()
        clickText("Juzlar")
        assertTrue(exists("30-juz"))

        // Taqvim
        clickText("Taqvim")
        assertTrue(exists("Milodiy"))
        clickText("Hijriy")
        clickText("Konvertor")
        assertTrue(exists("Milodiy → Hijriy"))

        // Yana → Qibla, Ob-havo, Sozlamalar, Joylashuv
        clickText("Yana")
        clickText("Qibla kompasi")
        assertTrue(exists("Qibla yo'nalishi", substring = true))
        rule.activity.onBackPressedDispatcher.onBackPressed(); settle()

        clickText("Ob-havo")
        assertTrue(exists("Ob-havo"))
        rule.activity.onBackPressedDispatcher.onBackPressed(); settle()

        clickText("Sozlamalar")
        assertTrue(exists("Hisoblash usuli"))
        clickText("Tungi")
        assertEquals(uz.hijriy.app.data.ThemeMode.DARK, app.settings.value.themeMode)
        clickText("Firuza")
        assertEquals(1, app.settings.value.palette)
        rule.activity.onBackPressedDispatcher.onBackPressed(); settle()

        clickText("Joylashuv")
        clickText("Samarqand viloyati")
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("Urgut")
        settle()
        clickText("Urgut tumani")
        assertEquals("Urgut tumani", app.settings.value.locName)
    }

    @Test
    fun readerOpensLongSuraInFlowModeAndDark() {
        rule.mainClock.autoAdvance = false
        val app = rule.activity.application as HijriyApp
        app.settings.update { it.copy(quranFlow = true, themeMode = uz.hijriy.app.data.ThemeMode.DARK, lastSura = 2, lastAyah = 255) }
        settle()
        rule.waitUntil(30_000) { app.quran.value != null }
        settle()
        clickText("Davom ettirish")
        assertTrue(exists("2. Baqara"))
    }

    @Test
    fun dataAssetsAreComplete() {
        val ctx = rule.activity
        val q = QuranRepo.load(ctx)
        assertEquals(114, q.suras.size)
        assertEquals(6236, q.suras.sumOf { it.count })
        assertEquals(30, q.juz.size)
        var ann = 0
        for (s in q.suras) for (a in s.ayahs) {
            assertTrue(a.ann.size % 3 == 0)
            var i = 0
            while (i < a.ann.size) {
                assertTrue("${s.number}:${a.number}", a.ann[i] < a.ann[i + 1] && a.ann[i + 1] <= a.text.length)
                assertTrue(a.ann[i + 2] in TajweedRule.entries.indices)
                i += 3; ann++
            }
        }
        assertTrue(ann > 55_000)
        val regions = Regions.all(ctx)
        assertEquals(14, regions.size)
        assertEquals(209, regions.sumOf { it.districts.size })
        for (d in regions.flatMap { it.districts }) {
            assertTrue(d.name, d.lat in 37.0..45.7 && d.lon in 55.9..73.2)
        }
    }
}
