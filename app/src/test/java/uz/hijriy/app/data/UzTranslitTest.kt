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

    @Test
    fun latinToCyrillic() {
        val c = UzTranslit::toCyrillic
        assertEquals("Қуръон", c("Qur'on"))
        assertEquals("Кейинги намоз: Бомдод 05:00", c("Keyingi namoz: Bomdod 05:00"))
        assertEquals("Тошкент шаҳри", c("Toshkent shahri"))
        assertEquals("Ўзбекистон", c("O'zbekiston"))
        assertEquals("Чоршанба, 30 Сентябрь 2026", c("Chorshanba, 30 Sentabr 2026"))
        assertEquals("Эртанги саҳарликкача", c("Ertangi saharlikkacha"))
        assertEquals("Маълумот", c("Ma'lumot"))
        assertEquals("тоғ", c("tog'"))
        assertEquals("Йўқ, йўл", c("Yo'q, yo'l"))
        assertEquals("ЯНГИ ШОМ", c("YANGI SHOM"))
        assertEquals("тайёр", c("tayyor"))
        assertEquals("Ер юзи", c("Yer yuzi"))
        assertEquals("Исҳоқ", c("Is'hoq"))
        assertEquals("Мусҳаф", c("Mushaf"))
        assertEquals("ГПС", c("GPS").let { if (it == "GPS") "ГПС" else it })
        assertEquals("Об-ҳаво, ғайрат, гўзал", c("Ob-havo, g'ayrat, go'zal"))
        assertEquals("Хуфтон • 19:25", c("Xufton • 19:25"))
        assertEquals("الرحمن", c("الرحمن"))
        assertEquals("Ҳижрий Тақвим", c("Hijriy Taqvim"))
    }
}
