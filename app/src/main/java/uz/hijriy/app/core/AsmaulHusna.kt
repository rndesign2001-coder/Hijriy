package uz.hijriy.app.core

/** Allohning go'zal ismlari (Asmaul Husna) — Termiziy rivoyatidagi tartib. */
data class AllahName(val n: Int, val ar: String, val uz: String, val meaning: String)

object AsmaulHusna {
    private val raw = """
الرَّحْمٰنُ|Ar-Rohman|Mehribon — dunyoda barcha bandalariga rahm qiluvchi
الرَّحِيمُ|Ar-Rohiym|Rahmli — oxiratda mo'minlarga rahm qiluvchi
الْمَلِكُ|Al-Malik|Podshoh — butun borliqning haqiqiy hukmdori
الْقُدُّوسُ|Al-Quddus|Pok — har qanday nuqson va aybdan xoli
السَّلَامُ|As-Salom|Salomat — har qanday ofatdan omon, tinchlik beruvchi
الْمُؤْمِنُ|Al-Mu'min|Omonlik beruvchi — bandalariga xotirjamlik ato etuvchi
الْمُهَيْمِنُ|Al-Muhaymin|Kuzatib, muhofaza qilib turuvchi
الْعَزِيزُ|Al-Aziz|Qudratli — hech kim yenga olmaydigan g'olib
الْجَبَّارُ|Al-Jabbor|Irodasini o'tkazuvchi, singanni tuzatuvchi
الْمُتَكَبِّرُ|Al-Mutakabbir|Buyuklik va kibriyo egasi
الْخَالِقُ|Al-Xoliq|Yaratuvchi
الْبَارِئُ|Al-Bori'|Yo'qdan bor qiluvchi
الْمُصَوِّرُ|Al-Musavvir|Har narsaga shakl va surat beruvchi
الْغَفَّارُ|Al-G'offor|Gunohlarni qayta-qayta kechiruvchi
الْقَهَّارُ|Al-Qahhor|Hamma narsa ustidan hukmron
الْوَهَّابُ|Al-Vahhob|Beminnat, beg'araz hadya qiluvchi
الرَّزَّاقُ|Ar-Razzoq|Barchaga rizq beruvchi
الْفَتَّاحُ|Al-Fattoh|Rahmat eshiklarini ochuvchi, adolat bilan hukm qiluvchi
الْعَلِيمُ|Al-Aliym|Hamma narsani biluvchi
الْقَابِضُ|Al-Qobiz|Rizqni toraytiruvchi
الْبَاسِطُ|Al-Bosit|Rizqni kengaytiruvchi
الْخَافِضُ|Al-Xofiz|Pastlatuvchi
الرَّافِعُ|Ar-Rofi'|Yuksaltiruvchi
الْمُعِزُّ|Al-Mu'izz|Aziz qiluvchi, izzat beruvchi
الْمُذِلُّ|Al-Muzill|Xor qiluvchi
السَّمِيعُ|As-Samiy'|Hamma narsani eshituvchi
الْبَصِيرُ|Al-Basiyr|Hamma narsani ko'ruvchi
الْحَكَمُ|Al-Hakam|Hakam — hukm chiqaruvchi
الْعَدْلُ|Al-Adl|Mutlaq adolatli
اللَّطِيفُ|Al-Latiyf|Lutfli, nozik ishlarni ham biluvchi
الْخَبِيرُ|Al-Xobiyr|Har narsadan xabardor
الْحَلِيمُ|Al-Haliym|Halim — jazolashga shoshilmaydigan
الْعَظِيمُ|Al-Aziym|Ulug'
الْغَفُورُ|Al-G'afur|Kechirimli
الشَّكُورُ|Ash-Shakur|Oz amalga ko'p savob beruvchi
الْعَلِيُّ|Al-Aliy|Oliy — eng yuksak
الْكَبِيرُ|Al-Kabiyr|Buyuk
الْحَفِيظُ|Al-Hafiyz|Saqlovchi, muhofaza qiluvchi
الْمُقِيتُ|Al-Muqiyt|Har jonga quvvat va rizq yetkazuvchi
الْحَسِيبُ|Al-Hasiyb|Hisob oluvchi, bandalariga kifoya qiluvchi
الْجَلِيلُ|Al-Jaliyl|Ulug'vor
الْكَرِيمُ|Al-Kariym|Karamli, saxiy
الرَّقِيبُ|Ar-Roqiyb|Kuzatuvchi
الْمُجِيبُ|Al-Mujiyb|Duolarni ijobat qiluvchi
الْوَاسِعُ|Al-Vosi'|Fazli va ilmi keng
الْحَكِيمُ|Al-Hakiym|Hikmat egasi
الْوَدُودُ|Al-Vadud|Solih bandalarini sevuvchi
الْمَجِيدُ|Al-Majiyd|Sharafi ulug'
الْبَاعِثُ|Al-Bois|O'liklarni qayta tiriltiruvchi
الشَّهِيدُ|Ash-Shahiyd|Har narsaga guvoh
الْحَقُّ|Al-Haqq|Haq — mavjudligi shubhasiz
الْوَكِيلُ|Al-Vakiyl|Vakil — tavakkul qilinadigan zot
الْقَوِيُّ|Al-Qaviy|Kuchli
الْمَتِينُ|Al-Matiyn|Mustahkam, quvvati tugamaydigan
الْوَلِيُّ|Al-Valiy|Do'st, mo'minlarga yordamchi
الْحَمِيدُ|Al-Hamiyd|Maqtovga loyiq
الْمُحْصِي|Al-Muhsiy|Har narsani sanab, hisoblab turuvchi
الْمُبْدِئُ|Al-Mubdi'|Ilk bor yaratuvchi
الْمُعِيدُ|Al-Mu'iyd|Qayta yaratuvchi
الْمُحْيِي|Al-Muhyiy|Hayot beruvchi
الْمُمِيتُ|Al-Mumiyt|O'lim beruvchi
الْحَيُّ|Al-Hayy|Abadiy tirik
الْقَيُّومُ|Al-Qayyum|O'zi bilan qoim, hamma narsani tutib turuvchi
الْوَاجِدُ|Al-Vojid|Istaganini topuvchi, hech narsaga muhtoj emas
الْمَاجِدُ|Al-Mojid|Sharaf va karami ulug'
الْوَاحِدُ|Al-Vohid|Yagona
الْأَحَدُ|Al-Ahad|Yakka, sherigi yo'q
الصَّمَدُ|As-Somad|Behojat — hamma Unga muhtoj
الْقَادِرُ|Al-Qodir|Qodir — har narsaga qodir
الْمُقْتَدِرُ|Al-Muqtadir|Qudrati komil
الْمُقَدِّمُ|Al-Muqaddim|Istaganini oldinga suruvchi
الْمُؤَخِّرُ|Al-Muaxxir|Istaganini ortga suruvchi
الْأَوَّلُ|Al-Avval|Avval — boshlanishi yo'q
الْآخِرُ|Al-Oxir|Oxir — nihoyasi yo'q
الظَّاهِرُ|Az-Zohir|Zohir — borligi dalillari bilan ochiq
الْبَاطِنُ|Al-Botin|Botin — zoti idrokdan yashirin
الْوَالِي|Al-Voliy|Barcha ishlarni boshqaruvchi
الْمُتَعَالِي|Al-Muta'oliy|Har narsadan oliy
الْبَرُّ|Al-Barr|Yaxshilik va ehson qiluvchi
التَّوَّابُ|At-Tavvob|Tavbalarni qabul qiluvchi
الْمُنْتَقِمُ|Al-Muntaqim|Zolimlardan intiqom oluvchi
الْعَفُوُّ|Al-Afuvv|Gunohlarni o'chirib, afv etuvchi
الرَّءُوفُ|Ar-Rouf|O'ta shafqatli
مَالِكُ الْمُلْكِ|Molikul-mulk|Butun mulkning egasi
ذُو الْجَلَالِ وَالْإِكْرَامِ|Zul-jaloli val-ikrom|Ulug'vorlik va karam egasi
الْمُقْسِطُ|Al-Muqsit|Adolat o'rnatuvchi
الْجَامِعُ|Al-Jomi'|Qiyomatda barchani jamlovchi
الْغَنِيُّ|Al-G'aniy|Behojat, boy
الْمُغْنِي|Al-Mug'niy|Boy qiluvchi
الْمَانِعُ|Al-Mone'|Istamaganini to'suvchi
الضَّارُّ|Az-Zorr|Hikmat bilan zarar yetkazuvchi (sinovchi)
النَّافِعُ|An-Nofi'|Foyda beruvchi
النُّورُ|An-Nur|Nur — osmonlar va yerni nurlantiruvchi
الْهَادِي|Al-Hodiy|Hidoyat qiluvchi
الْبَدِيعُ|Al-Badiy'|Namunasiz, go'zal yaratuvchi
الْبَاقِي|Al-Boqiy|Boqiy — abadiy qoluvchi
الْوَارِثُ|Al-Voris|Hamma narsa oxirida Unga qoluvchi
الرَّشِيدُ|Ar-Roshid|To'g'ri yo'lga boshlovchi
الصَّبُورُ|As-Sobur|Sabrli — jazoga shoshilmaydigan
""".trimIndent()

    val names: List<AllahName> = raw.lines().filter { it.isNotBlank() }.mapIndexed { i, l ->
        val p = l.split('|')
        AllahName(i + 1, p[0].trim(), p[1].trim(), p[2].trim())
    }
}
