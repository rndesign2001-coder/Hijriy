package uz.hijriy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.hijriy.app.core.Translit
import java.io.File

/** Qur'on va duolar o'qilishi (transliteratsiya) — oltin namunalar va butun Qur'on bo'yicha tekshiruv. */
class TranslitTest {
    private val ayahs: List<Pair<String, String>> by lazy {
        val raw = File("src/main/assets/quran.json").readText()
        // {"n":1,...,"a":[["matn",[...]], ...]}
        val out = ArrayList<Pair<String, String>>()
        Regex("\\{\"n\":(\\d+),.*?\"a\":\\[(.*?)\\]\\]\\}").findAll(raw).forEach { s ->
            var i = 0
            Regex("\\[\"([^\"]+)\",\\[").findAll(s.groupValues[2]).forEach { a ->
                i++; out.add("${s.groupValues[1]}:$i" to a.groupValues[1])
            }
        }
        out
    }
    private fun q(key: String) = Translit.ayah(ayahs.first { it.first == key }.second)

    @Test
    fun parsesWholeQuran() {
        assertEquals(6236, ayahs.size)
        for ((k, t) in ayahs) {
            val r = Translit.ayah(t)
            assertTrue(k, r.latin.isNotBlank() && r.cyrillic.isNotBlank())
            assertFalse("$k: arabcha harf qolgan", Regex("[\\u0600-\\u06FF]").containsMatchIn(r.latin + r.cyrillic))
            assertFalse("$k: kirillda lotin harfi", Regex("[A-Za-z]").containsMatchIn(r.cyrillic))
            assertFalse("$k: ortiqcha bo'shliq", "  " in r.latin || r.latin.endsWith(" "))
        }
    }

    @Test
    fun fatiha() {
        val exp = listOf(
            "Bismillahir rohmaanir rohiym",
            "Alhamdu lillahi robbil ʼaalamiyn",
            "Arrohmaanir rohiym",
            "Maaliki yavmid diyn",
            "Iyyaaka naʼbudu vaʼiyyaaka nastaʼiyn",
            "Ihdinas sirootol mustaqiym",
            "Sirootol laziyna anʼamta ʼalayhim g‘oyril mag‘zuubi ʼalayhim valaz zoolliyn",
        )
        exp.forEachIndexed { i, e -> assertEquals(e, q("1:${i + 1}").latin) }
        assertEquals("Бисмиллаҳир роҳмаанир роҳийм", q("1:1").cyrillic)
        assertEquals("Ийяака наъбуду ваъийяака настаъийн", q("1:5").cyrillic)
    }

    @Test
    fun shortSuras() {
        assertEquals("Qul huvallohu ahad", q("112:1").latin)
        assertEquals("Allohus somad", q("112:2").latin)
        assertEquals("Lam yalid valam yuulad", q("112:3").latin)
        assertEquals("Valam yakul lahuu kufuvan ahad", q("112:4").latin)
        assertEquals("Qul aʼuuzu birobbil falaq", q("113:1").latin)
        assertEquals("Vamin sharrin naffaasaati fil ʼuqod", q("113:4").latin)
        assertEquals("Qul aʼuuzu birobbin naas", q("114:1").latin)
        assertEquals("Minal jinnati vannaas", q("114:6").latin)
        assertEquals("Қул ҳуваллоҳу аҳад", q("112:1").cyrillic)
        // muqattaʼa, iqlob, idg'om, imola, sakta
        assertEquals("Alif laam miim", q("2:1").latin)
        assertEquals("Zaalikal kitaabu laa royba fiyhi hudal lilmuttaqiyn", q("2:2").latin)
        assertTrue(q("11:41").latin.contains("majrehaa"))
        assertTrue(q("2:5").latin.contains("hudam mir robbihim"))
        assertTrue(q("26:176").latin.contains("as-haabul aykatil"))
        assertEquals("ʼAllamal qurʼaan", q("55:2").latin)
    }

    @Test
    fun duas() {
        assertEquals("Subhaanallohi vabihamdih", Translit.dua("سُبْحَانَ اللَّهِ وَبِحَمْدِهِ").latin)
        assertEquals("Allohu akbar", Translit.dua("اللَّهُ أَكْبَرُ").latin)
        assertEquals("Alhamdu lillah", Translit.dua("الْحَمْدُ لِلَّهِ").latin)
        assertEquals("Astag‘firulloh", Translit.dua("أَسْتَغْفِرُ اللَّهَ").latin)
        assertEquals(
            "Bismillahil laziy laa yazurru maʼasmihi shayʼun fil arzi valaa fis samaaʼi vahuvas samiyʼul ʼaliym",
            Translit.dua("بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ").latin
        )
        assertEquals(
            "Allohumma antas salaamu vaminkas salaam, tabaarokta yaa zal jalaali valikroom",
            Translit.dua("اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ").latin
        )
        assertEquals("Аллоҳу акбар", Translit.dua("اللَّهُ أَكْبَرُ").cyrillic)
    }
}
