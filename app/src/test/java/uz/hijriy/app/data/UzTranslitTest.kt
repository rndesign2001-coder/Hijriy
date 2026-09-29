package uz.hijriy.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UzTranslitTest {
    @Test
    fun cyrillicToLatin() {
        assertEquals("Barcha maqtov, shukrlar olamlarning tarbiyachisi Allohga bo'lsin.",
            UzTranslit.toLatin("Барча мақтов, шукрлар оламларнинг тарбиячиси Аллоҳга бўлсин."))
        assertEquals("Qur'on", UzTranslit.toLatin("Қуръон"))
        assertEquals("QUR'ON", UzTranslit.toLatin("ҚУРЪОН"))
        assertEquals("Yer yuzida", UzTranslit.toLatin("Ер юзида"))
        assertEquals("Yordam, g'ayrat", UzTranslit.toLatin("Ёрдам, ғайрат"))
        assertEquals("Ya'qub (a.s.) 12:4", UzTranslit.toLatin("Яъқуб (а.с.) 12:4"))
    }
}
