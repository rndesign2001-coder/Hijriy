package uz.hijriy.app

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.click
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
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class ScreensSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun settle() {
        // Navigatsiya animatsiyalari (≈700 ms) to'liq tugashi uchun
        repeat(3) { rule.mainClock.advanceTimeBy(500) }
        rule.waitForIdle()
    }

    private fun clickText(t: String) {
        println("STEP click: $t")
        val n = rule.onAllNodesWithText(t).onFirst()
        // Aylantirish animatsiyasi soat yurishini talab qiladi
        rule.mainClock.autoAdvance = true
        runCatching { n.performScrollTo() }
        rule.mainClock.autoAdvance = false
        n.performClick()
        settle()
    }

    private fun waitFor(what: String, cond: () -> Boolean) {
        println("STEP wait: $what")
        repeat(150) {
            settle()
            if (cond()) return
            Thread.sleep(100)
        }
        throw AssertionError("Kutilgan holat bo'lmadi: $what")
    }

    private fun skipIntro() {
        settle()
        val intro = rule.onAllNodesWithTag("intro").fetchSemanticsNodes()
        if (intro.isNotEmpty()) rule.onAllNodesWithTag("intro").onFirst().performClick()
        settle()
    }

    private fun clickSub(t: String) {
        println("STEP clickSub: $t")
        val n = rule.onAllNodesWithText(t, substring = true).onFirst()
        rule.mainClock.autoAdvance = true
        runCatching { n.performScrollTo() }
        rule.mainClock.autoAdvance = false
        n.performClick()
        settle()
    }

    private fun exists(t: String, substring: Boolean = false): Boolean =
        rule.onAllNodesWithText(t, substring = substring).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun allScreensOpen() {
        rule.mainClock.autoAdvance = false
        skipIntro()
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
        assertTrue(exists("1-juz"))

        // Taqvim
        clickText("Taqvim")
        assertTrue(exists("Milodiy"))
        clickText("Hijriy")
        clickText("Konvertor")
        assertTrue(exists("Milodiy → Hijriy"))
        // Regress: yilni raqamma-raqam yozish ilovani qulatmasligi kerak (21-mart 2001)
        val fields = { rule.onAllNodes(hasSetTextAction()) }
        fields()[0].performTextClearance(); fields()[0].performTextInput("21"); settle()
        clickText(uz.hijriy.app.core.Uz.MONTHS[java.time.LocalDate.now().monthValue - 1])
        clickText("Mart")
        fields()[1].performTextClearance(); settle()
        for (ch in "2001") { fields()[1].performTextInput(ch.toString()); settle() }
        assertTrue(exists("26 Zulhijja 1421 h."))
        fields()[3].performTextClearance(); settle()
        for (ch in "1421") { fields()[3].performTextInput(ch.toString()); settle() }
        assertTrue(exists("Yil", substring = true))

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
    fun mushafTasbehAndNames() {
        rule.mainClock.autoAdvance = false
        skipIntro()
        settle()
        clickText("Mushaf")
        waitFor("mushaf 1-sahifa") { exists("١") }
        rule.onRoot().performTouchInput { swipeRight() }
        settle()
        waitFor("mushaf 2-sahifa") { exists("٢") }
        rule.onNodeWithTag("mushaf_page_2").performClick()
        settle()
        assertTrue("Mushaf paneli bosishda ochilishi kerak", exists("2-sahifa", substring = true))
        rule.activity.onBackPressedDispatcher.onBackPressed(); settle()

        clickText("Tasbeh")
        assertTrue(exists("Subhanalloh"))
        repeat(3) { clickText("/ 33") }
        assertTrue(exists("3"))
        rule.activity.onBackPressedDispatcher.onBackPressed(); settle()

        clickText("99 ism")
        assertTrue(exists("1. Ar-Rohman") || exists("Ar-Rohman"))
    }

    @Test
    fun extrasScreens() {
        rule.mainClock.autoAdvance = false
        val app = rule.activity.application as HijriyApp
        rule.waitUntil(30_000) { app.quran.value != null }
        skipIntro()

        // Rasm tayyorlagich
        clickText("Rasm")
        assertTrue(exists("Joylash uslubi"))
        waitFor("namuna rasmi") { rule.onAllNodesWithContentDescription("Namuna").fetchSemanticsNodes().isNotEmpty() }
        for (l in uz.hijriy.app.ui.WpLayouts) clickText(l)
        clickText("Oltin shafaq")
        rule.activity.onBackPressedDispatcher.onBackPressed(); settle()

        // Duolar
        clickText("Duolar")
        assertTrue(exists("Oyatul Kursiy"))
        clickSub("Kechki zikrlar")
        rule.activity.onBackPressedDispatcher.onBackPressed(); settle()

        // Qazo
        clickText("Qazo")
        rule.onAllNodesWithContentDescription("Qo'shish").onFirst().performClick(); settle()
        assertEquals(1, app.qazo.counts.value[0])
        rule.activity.onBackPressedDispatcher.onBackPressed(); settle()

        // Qidiruv
        clickText("Qur'on")
        rule.onAllNodesWithContentDescription("Oyatlardan qidirish").onFirst().performClick(); settle()
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("الرحمن الرحيم")
        settle()
        clickText("Qidirish")
        waitFor("qidiruv natijasi") { exists("ta oyat topildi", substring = true) }
    }

    @Test
    fun rendersAndWidget() {
        rule.mainClock.autoAdvance = false
        skipIntro()
        val app = rule.activity.application as HijriyApp
        val tm = androidx.compose.ui.text.TextMeasurer(
            androidx.compose.ui.text.font.createFontFamilyResolver(rule.activity),
            androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, 0
        )
        for (bg in 0..3) for (layout in uz.hijriy.app.ui.WpLayouts.indices) {
            val img = uz.hijriy.app.ui.renderToBitmap(
                tm, uz.hijriy.app.ui.WpOptions(bg = bg, layout = layout, sunrise = layout % 2 == 0),
                app.settings.value, java.time.LocalDate.now(), null, 0.25f
            )
            assertEquals(270, img.width)
        }
        uz.hijriy.app.widget.PrayerWidget.build(rule.activity)
        uz.hijriy.app.notify.PrayerScheduler.reschedule(rule.activity)
    }

    @Test
    fun readerOpensLongSuraInFlowModeAndDark() {
        rule.mainClock.autoAdvance = false
        skipIntro()
        val app = rule.activity.application as HijriyApp
        app.settings.update { it.copy(quranFlow = true, themeMode = uz.hijriy.app.data.ThemeMode.DARK, lastSura = 2, lastAyah = 255) }
        settle()
        rule.waitUntil(30_000) { app.quran.value != null }
        settle()
        clickSub("Davom ettirish")
        assertTrue(exists("2. Baqara"))
    }

    @Test
    fun dataAssetsAreComplete() {
        val ctx = rule.activity
        val q = QuranRepo.load(ctx)
        assertEquals(114, q.suras.size)
        assertEquals(6236, q.suras.sumOf { it.count })
        assertEquals(30, q.juz.size)
        assertTrue("Fotiha — Makkiy", q.suras[0].meccan)
        assertTrue("Baqara — Madaniy", !q.suras[1].meccan)
        assertEquals(28, q.suras.count { !it.meccan })
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
