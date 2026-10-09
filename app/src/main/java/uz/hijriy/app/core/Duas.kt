package uz.hijriy.app.core

/**
 * Duo va zikrlar. [quran] berilgan bo'lsa, arabcha matn ilovadagi Qur'on matnidan olinadi
 * (sura, boshlang'ich oyat, oxirgi oyat) — shu bilan matn xatosiz bo'ladi.
 */
data class Dua(
    val title: String,
    val ar: String = "",
    val meaning: String,
    val count: Int = 1,
    val source: String,
    val quran: List<Triple<Int, Int, Int>> = emptyList(),
)

data class DuaSection(val title: String, val emoji: String, val items: List<Dua>)

object Duas {
    private val kursi = Dua(
        "Oyatul Kursiy", meaning = "Alloh — Undan o'zga iloh yo'q, U tirik va abadiy turuvchidir. Uni mudroq ham, uyqu ham tutmaydi. " +
            "Osmonlar va yerdagi narsalar Unikidir… Uning Kursiysi osmonlar va yerni qamrab olgan. U Oliy va Ulug'dir.",
        source = "Baqara surasi, 255-oyat", quran = listOf(Triple(2, 255, 255))
    )

    val sections = listOf(
        DuaSection(
            "Tong zikrlari", "🌅", listOf(
                kursi,
                Dua("Ixlos, Falaq, Nos suralari", meaning = "Uch marta o'qiladi — har narsadan kifoya qiladi.", count = 3,
                    source = "Abu Dovud, Termiziy", quran = listOf(Triple(112, 1, 4), Triple(113, 1, 5), Triple(114, 1, 6))),
                Dua(
                    "Tong duosi",
                    "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ",
                    "Allohim, Sening izning bilan tongga yetdik, Sening izning bilan kechga yetdik. Sen bilan tirilamiz, Sen bilan o'lamiz va qayta tirilib Senga qaytamiz.",
                    1, "Termiziy"
                ),
                Dua(
                    "Sayyidul istig'for",
                    "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
                    "Allohim, Sen mening Robbimsan, Sendan o'zga iloh yo'q. Meni Sen yaratding, men Sening bandangman. Qo'limdan kelgancha Senga bergan ahdim va va'dam ustidaman. " +
                        "Qilgan ishlarimning yomonligidan Sendan panoh so'rayman. Menga bergan ne'matingni tan olaman, gunohimni ham tan olaman. Meni kechir, chunki gunohlarni Sendan boshqa hech kim kechirmaydi.",
                    1, "Buxoriy"
                ),
                Dua(
                    "Himoya duosi",
                    "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
                    "Ismi bilan birga yerda ham, osmonda ham hech narsa zarar bera olmaydigan Allohning ismi bilan. U eshituvchi va biluvchidir.",
                    3, "Abu Dovud, Termiziy"
                ),
                Dua(
                    "Rozilik duosi",
                    "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا",
                    "Allohni Robb deb, Islomni din deb va Muhammad sollallohu alayhi vasallamni payg'ambar deb rozi bo'ldim.",
                    3, "Abu Dovud"
                ),
                Dua(
                    "Tavakkul",
                    "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
                    "Menga Alloh kifoya. Undan o'zga iloh yo'q. Unga tavakkul qildim. U ulug' Arshning Robbidir.",
                    7, "Abu Dovud"
                ),
                Dua(
                    "Tahlil",
                    "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
                    "Yolg'iz Allohdan o'zga iloh yo'q, Uning sherigi yo'q. Mulk ham, hamd ham Unikidir. U har narsaga qodirdir.",
                    10, "Buxoriy, Muslim"
                ),
                Dua(
                    "Tasbih",
                    "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
                    "Allohni hamdi bilan poklab yod etaman.",
                    100, "Muslim"
                ),
            )
        ),
        DuaSection(
            "Kechki zikrlar", "🌙", listOf(
                kursi,
                Dua("Ixlos, Falaq, Nos suralari", meaning = "Uch marta o'qiladi.", count = 3, source = "Abu Dovud, Termiziy", quran = listOf(Triple(112, 1, 4), Triple(113, 1, 5), Triple(114, 1, 6))),
                Dua(
                    "Kech duosi",
                    "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ",
                    "Allohim, Sening izning bilan kechga yetdik, Sening izning bilan tongga yetdik. Sen bilan tirilamiz, Sen bilan o'lamiz va qaytish Sengadir.",
                    1, "Termiziy"
                ),
                Dua(
                    "Panoh so'rash",
                    "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
                    "Allohning mukammal kalimalari bilan U yaratgan narsalarning yomonligidan panoh so'rayman.",
                    3, "Muslim"
                ),
                Dua(
                    "Baqara surasining oxirgi ikki oyati",
                    meaning = "Kim kechasi bu ikki oyatni o'qisa, unga kifoya qiladi.",
                    source = "Buxoriy, Muslim", quran = listOf(Triple(2, 285, 286))
                ),
            )
        ),
        DuaSection(
            "Namozdan keyin", "🕌", listOf(
                Dua("Istig'for", "أَسْتَغْفِرُ اللَّهَ", "Allohdan mag'firat so'rayman.", 3, "Muslim"),
                Dua(
                    "Salom duosi",
                    "اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالْإِكْرَامِ",
                    "Allohim, Sen Salomsan, tinchlik Sendandir. Ey ulug'lik va ikrom egasi, Sen barakotlisan.",
                    1, "Muslim"
                ),
                kursi,
                Dua("Subhanalloh", "سُبْحَانَ اللَّهِ", "Alloh barcha nuqsonlardan pokdir.", 33, "Muslim"),
                Dua("Alhamdulillah", "الْحَمْدُ لِلَّهِ", "Barcha hamdlar Allohgadir.", 33, "Muslim"),
                Dua("Allohu akbar", "اللَّهُ أَكْبَرُ", "Alloh eng buyukdir.", 33, "Muslim"),
                Dua(
                    "Yuzinchi zikr",
                    "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
                    "Yolg'iz Allohdan o'zga iloh yo'q, Uning sherigi yo'q. Mulk ham, hamd ham Unikidir. U har narsaga qodirdir.",
                    1, "Muslim"
                ),
                Dua(
                    "Yordam so'rash",
                    "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ وَشُكْرِكَ وَحُسْنِ عِبَادَتِكَ",
                    "Allohim, Seni zikr qilishimga, Senga shukr qilishimga va Senga go'zal ibodat qilishimga yordam ber.",
                    1, "Abu Dovud, Nasoiy"
                ),
            )
        ),
    )
}
