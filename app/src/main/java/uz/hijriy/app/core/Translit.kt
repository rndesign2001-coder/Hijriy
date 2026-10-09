package uz.hijriy.app.core

/**
 * Qur'on va duolarning o'qilishi (talaffuz transliteratsiyasi) — o'zbek lotin va kirill yozuvida.
 *
 * To'liq harakatli Usmoniy matndan qoidalar asosida hosil qilinadi (Hafs rivoyati):
 *  - vasl: hamzai vasl tushadi, oldingi so'z oxiridagi uzun unli qisqaradi, "al" oldingi so'zga ulanadi
 *    (robbil ʼaalamiyn, ihdinas sirootol mustaqiym);
 *  - shamsiy harflar va idg'om (shadda orqali): ar-rohmaan, mir robbihim, hudal lilmuttaqiyn;
 *  - iqlob (ۢ / ۭ): min baʼdi → mim baʼdi;
 *  - lafzi jalola: Alloh (yo'g'on) / lillahi (ingichka);
 *  - vaqf (oyat oxirida): oxirgi harakat va tanvin tushadi, fathatan → aa, ة → h;
 *  - yo'g'on harflar (خ ص ض ط ظ غ ق, fathali/zammali ر) dan keyingi fatha → "o";
 *  - uzun unlilar: aa/oo, iy, uu; muqattaʼa harflari nomi bilan o'qiladi.
 *
 * Belgilar: ʼ — ayn va hamza (kirillda ъ), g‘ — غ (kirillda ғ), x — خ, h — ح va ه (kirillda ҳ).
 */
object Translit {
    // ---- harakatlar va Usmoniy belgilar ----
    private const val FATHA = '\u064E'; private const val DAMMA = '\u064F'; private const val KASRA = '\u0650'
    private const val FATHATAN = '\u064B'; private const val DAMMATAN = '\u064C'; private const val KASRATAN = '\u064D'
    private const val SHADDA = '\u0651'; private const val SUKUN = '\u0652'; private const val MADDAH = '\u0653'
    private const val HAMZA_ABOVE = '\u0654'; private const val HAMZA_BELOW = '\u0655'; private const val DAGGER = '\u0670'
    private const val HIGH_SEEN = '\u06DC'; private const val ROUND_ZERO = '\u06DF'; private const val RECT_ZERO = '\u06E0'
    private const val HIGH_MEEM = '\u06E2'; private const val LOW_MEEM = '\u06ED'; private const val IMALA = '\u06EA'
    private const val HIGH_NOON = '\u06E8'
    private const val SMALL_WAW = '\u06E5'; private const val SMALL_YEH = '\u06E6'; private const val TATWEEL = '\u0640'
    private const val WASLA = '\u0671'

    private fun isMark(c: Char) = c in '\u064B'..'\u065F' || c == DAGGER ||
        (c in '\u06D6'..'\u06ED' && c != SMALL_WAW && c != SMALL_YEH)

    private val VOWELS = setOf(FATHA, DAMMA, KASRA, FATHATAN, DAMMATAN, KASRATAN)

    /** Undoshlar: belgi kodi va yo'g'onligi. */
    private data class Cons(val code: String, val heavy: Boolean = false)

    private val consMap = mapOf(
        'ب' to Cons("b"), 'ت' to Cons("t"), 'ث' to Cons("s"), 'ج' to Cons("j"), 'ح' to Cons("h"),
        'خ' to Cons("x", true), 'د' to Cons("d"), 'ذ' to Cons("z"), 'ر' to Cons("r"), 'ز' to Cons("z"),
        'س' to Cons("s"), 'ش' to Cons("sh"), 'ص' to Cons("s", true), 'ض' to Cons("z", true), 'ط' to Cons("t", true),
        'ظ' to Cons("z", true), 'ع' to Cons("'"), 'غ' to Cons("gh", true), 'ف' to Cons("f"), 'ق' to Cons("q", true),
        'ك' to Cons("k"), 'ل' to Cons("l"), 'م' to Cons("m"), 'ن' to Cons("n"), 'ه' to Cons("h"), 'و' to Cons("v"),
        'ي' to Cons("y"), 'ى' to Cons("y"), 'ة' to Cons("t"),
    )
    private val hamzas = setOf('ء', 'أ', 'إ', 'ؤ', 'ئ')

    // ---- fonemalar ----
    private sealed interface Ph
    private class C(
        var code: String, val heavy: Boolean = false, var double: Boolean = false,
        val hamza: Boolean = false, val ayn: Boolean = false, val tanwin: Boolean = false,
        val taMarbuta: Boolean = false, val article: Boolean = false, val unmarked: Boolean = false,
    ) : Ph
    private class V(
        val q: Char, var long: Boolean = false, var heavy: Boolean = false,
        val lafz: Boolean = false, var silah: Boolean = false,
    ) : Ph

    private class Unit(val ch: Char, val marks: String)
    private class Word(val ph: MutableList<Ph>, val wasl: Boolean, val waslArticle: Boolean, val lafzStart: Boolean, val firstShadda: Boolean)

    /** Natija: lotin va kirill yozuvidagi o'qilish. */
    data class Result(val latin: String, val cyrillic: String)

    private val muqattaat = mapOf(
        "الم" to ("Alif laam miim" to "Алиф лаам мийм"),
        "المص" to ("Alif laam miim sood" to "Алиф лаам мийм соод"),
        "الر" to ("Alif laam roo" to "Алиф лаам роо"),
        "المر" to ("Alif laam miim roo" to "Алиф лаам мийм роо"),
        "كهيعص" to ("Kaaf haa yaa ʼayn sood" to "Кааф ҳаа яа айн соод"),
        "طه" to ("Toohaa" to "Тоҳаа"),
        "طسم" to ("Too siin miim" to "Тоо сийн мийм"),
        "طس" to ("Too siin" to "Тоо сийн"),
        "يس" to ("Yaa siin" to "Яа сийн"),
        "ص" to ("Sood" to "Соод"),
        "حم" to ("Haa miim" to "Ҳаа мийм"),
        "عسق" to ("ʼAyn siin qoof" to "Айн сийн қооф"),
        "ق" to ("Qoof" to "Қооф"),
        "ن" to ("Nuun" to "Нуун"),
    )

    /**
     * Imloviy (oddiy) yozuvdagi duo matni: hamzai vasl alifini (so'z boshida yoki وَ/فَ/بِ/لِ/كَ dan keyin,
     * ortidan sukunli harf yoki "al" kelganda) ٱ ga almashtiradi, keyin [ayah] kabi o'qiladi.
     */
    fun dua(text: String): Result = ayah(normalizeImlai(text))

    fun normalizeImlai(text: String): String = text.split(' ').joinToString(" ") { w ->
        val u = parse(w)
        val sb = StringBuilder()
        for ((i, x) in u.withIndex()) {
            val atStart = i <= 2 && (0 until i).all { j -> u[j].ch in "وفبلك" && vowelOf(u[j].marks) != null }
            val nx = u.getOrNull(i + 1)
            val waslNext = nx != null && (SUKUN in nx.marks || nx.ch == 'ل')
            if (x.ch == 'ا' && x.marks.isEmpty() && atStart && waslNext) sb.append(WASLA)
            else sb.append(x.ch)
            if (!(x.ch == 'ا' && x.marks.isEmpty() && atStart && waslNext)) sb.append(x.marks)
        }
        sb.toString()
    }

    /** Bir oyat (yoki pauzagacha bo'lgan duo bo'lagi). Oxirida vaqf qilinadi. */
    fun ayah(text: String): Result {
        // Duolarda vergul — pauza
        val parts = text.split('،', '.', '؛').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size > 1) {
            val rs = parts.map { segment(it) }
            return Result(cap(rs.joinToString(", ") { it.latin }), cap(rs.joinToString(", ") { it.cyrillic }))
        }
        return segment(text).let { Result(cap(it.latin), cap(it.cyrillic)) }
    }

    private fun parse(word: String): List<Unit> {
        val out = ArrayList<Unit>()
        val sb = StringBuilder()
        var cur: Char? = null
        for (c in word) {
            if (isMark(c) && cur != null) { sb.append(c); continue }
            if (cur != null) out.add(Unit(cur, sb.toString()))
            cur = c; sb.clear()
        }
        if (cur != null) out.add(Unit(cur, sb.toString()))
        return out
    }

    private fun vowelOf(m: String): Char? = when {
        FATHA in m || FATHATAN in m -> 'a'
        DAMMA in m || DAMMATAN in m -> 'u'
        KASRA in m || KASRATAN in m -> 'i'
        else -> null
    }
    private fun hasTanwin(m: String) = FATHATAN in m || DAMMATAN in m || KASRATAN in m

    private fun segment(text: String): Result {
        val rawWords = text.split(' ', ' ').filter { it.isNotBlank() }
        val unitsList = rawWords.map { parse(it) }
        val words = ArrayList<Word>()
        val pre = ArrayList<Pair<String, String>?>()  // muqattaʼa so'zlari
        for ((wi, units) in unitsList.withIndex()) {
            val stripped = units.filter { it.ch != TATWEEL }.joinToString("") { it.ch.toString() }
            val noVowels = units.none { u -> u.marks.any { it in VOWELS || it == SUKUN || it == SHADDA } }
            val muq = if (noVowels) muqattaat[stripped] else null
            if (muq != null) {
                pre.add(muq); words.add(Word(mutableListOf(), false, false, false, false)); continue
            }
            pre.add(null)
            val nextUnits = unitsList.getOrNull(wi + 1)
            words.add(buildWord(units, wi == 0, wi == unitsList.lastIndex, nextUnits))
        }
        // ---- so'zlararo qoidalar ----
        for (k in 1 until words.size) {
            val w = words[k]
            // oldingi (bo'sh qolmagan) so'z
            var pk = k - 1
            while (pk > 0 && words[pk].ph.isEmpty() && pre[pk] == null) pk--
            val p = words[pk]
            if (pre[k] != null || pre[pk] != null) continue
            if (w.wasl) {
                val last = p.ph.lastOrNull()
                if (last is V && last.long && !last.lafz) last.long = false
                if (last is C && last.tanwin) p.ph.add(V('i'))
                if (w.lafzStart || !w.waslArticle) {
                    p.ph.addAll(w.ph); w.ph.clear()
                } else {
                    // "al" yoki ikkilangan shamsiy undoshni oldingi so'zga ko'chirish
                    val first = w.ph.firstOrNull()
                    if (first is C) {
                        if (first.double) {
                            first.double = false
                            p.ph.add(C(first.code, first.heavy))
                        } else {
                            p.ph.add(w.ph.removeAt(0))
                        }
                    }
                }
            } else if (w.firstShadda) {
                val last = p.ph.lastOrNull()
                val first = w.ph.firstOrNull()
                if (last is C && (last.unmarked || last.tanwin) && first is C) {
                    p.ph.removeAt(p.ph.lastIndex)
                    first.double = false
                    p.ph.add(C(first.code, first.heavy))
                }
            }
        }
        // Oyat boshidagi shadda (oldingi oyatdan idg'om) — bitta undosh
        (words.firstOrNull { it.ph.isNotEmpty() }?.ph?.firstOrNull() as? C)?.let { if (pre[0] == null) it.double = false }
        // ---- lafzi jalola yo'g'onligi ----
        var lastQ = 'a'
        for (w in words) for (ph in w.ph) if (ph is V) {
            if (ph.lafz) ph.heavy = lastQ != 'i'
            lastQ = ph.q
        }
        // ---- vaqf ----
        words.lastOrNull { it.ph.isNotEmpty() }?.let { applyWaqf(it.ph) }

        val lat = ArrayList<String>(); val cyr = ArrayList<String>()
        for ((i, w) in words.withIndex()) {
            val m = pre[i]
            if (m != null) { lat.add(m.first); cyr.add(m.second); continue }
            if (w.ph.isEmpty()) continue
            lat.add(renderLatin(w.ph)); cyr.add(renderCyr(w.ph))
        }
        return Result(lat.joinToString(" "), cyr.joinToString(" "))
    }

    private fun cap(s: String): String {
        val i = s.indexOfFirst { it.isLetter() && it != 'ʼ' }
        return if (i < 0) s else s.substring(0, i) + s[i].uppercaseChar() + s.substring(i + 1)
    }

    private fun applyWaqf(ph: MutableList<Ph>) {
        val last = ph.lastOrNull() ?: return
        if (last is C && last.tanwin) {
            ph.removeAt(ph.lastIndex)
            val v = ph.lastOrNull() as? V ?: return
            val carrier = ph.getOrNull(ph.lastIndex - 1) as? C
            if (carrier != null && carrier.taMarbuta) {
                ph.removeAt(ph.lastIndex); carrier.code = "h"
            } else if (v.q == 'a') v.long = true
            else ph.removeAt(ph.lastIndex)
            return
        }
        if (last is V && (!last.long || last.silah)) {
            ph.removeAt(ph.lastIndex)
            (ph.lastOrNull() as? C)?.let { if (it.taMarbuta) it.code = "h" }
        }
    }

    private fun buildWord(units: List<Unit>, firstWord: Boolean, lastWord: Boolean, next: List<Unit>?): Word {
        val ph = ArrayList<Ph>()
        var wasl = false; var article = false; var lafzStart = false
        var lastVowel: V? = null
        var prevTanwin = false
        val firstShadda = units.firstOrNull()?.marks?.contains(SHADDA) == true
        var i = 0
        fun nextHasShadda(idx: Int): Boolean {
            val n = units.getOrNull(idx + 1)
            if (n != null) return SHADDA in n.marks
            return next?.firstOrNull()?.marks?.contains(SHADDA) == true
        }
        while (i < units.size) {
            val u = units[i]; val m = u.marks; val ch = u.ch
            val isLast = i == units.lastIndex
            if (ROUND_ZERO in m) { i++; continue }
            if (RECT_ZERO in m) {
                if (isLast && lastWord) lastVowel?.long = true
                i++; continue
            }
            // Hamzai vasl
            if (ch == WASLA) {
                if (i == 0) {
                    if (firstWord) {
                        val n1 = units.getOrNull(1)
                        val q = if (n1?.ch == 'ل') 'a' else {
                            val n2 = units.getOrNull(2)
                            if (n2 != null && DAMMA in n2.marks) 'u' else 'i'
                        }
                        val v = V(q); ph.add(v); lastVowel = v
                    } else wasl = true
                    if (units.getOrNull(1)?.ch == 'ل') article = true
                } else if (units.getOrNull(i + 1)?.ch == 'ل') article = true
                i++; continue
            }
            // So'z boshidagi sukunli lom (لْـَٔيْكَةِ) — vaslsiz "al"
            if (i == 0 && !firstWord && ch == 'ل' && SUKUN in m) {
                wasl = true; article = true
                ph.add(C("l", article = true)); lastVowel = null
                i++; continue
            }
            // Lafzi jalola
            if (ch == 'ل' && SHADDA in m && FATHA in m && units.getOrNull(i - 1)?.ch == 'ل' &&
                units.getOrNull(i + 1)?.ch == 'ه' &&
                (i + 1 == units.lastIndex || (units.getOrNull(i + 2)?.ch == 'م' && SHADDA in units[i + 2].marks))
            ) {
                if (ph.isEmpty() || (ph.size == 1 && ph[0] is V)) lafzStart = true
                ph.add(C("l", double = true))
                val v = V('a', long = true, lafz = true); ph.add(v); lastVowel = v
                i++; continue
            }
            // Kichik vov / yo (silah, uzun unli)
            if (ch == SMALL_WAW || ch == SMALL_YEH) {
                lastVowel?.let { if (!it.long) { it.long = true; it.silah = true } }
                i++; continue
            }
            // Tatvil: dagger alif yoki hamza tashuvchisi
            if (ch == TATWEEL) {
                if (HAMZA_ABOVE in m || HAMZA_BELOW in m) {
                    ph.add(C("'", hamza = true))
                    vowelOf(m)?.let { q -> val v = V(q); ph.add(v); lastVowel = v }
                } else if (DAGGER in m) lastVowel?.long = true
                i++; continue
            }
            val vq = vowelOf(m)
            // Madd harflari
            if (vq == null && SUKUN !in m && SHADDA !in m && HAMZA_ABOVE !in m) {
                if (DAGGER in m && (ch == 'و' || ch == 'ي' || ch == 'ى')) { lastVowel?.long = true; i++; continue }
                when (ch) {
                    'ا' -> {
                        if (!prevTanwin && lastVowel != null && lastVowel.q == 'a') lastVowel.long = true
                        i++; continue
                    }
                    'ى' -> {
                        if (!prevTanwin && lastVowel != null && (lastVowel.q == 'a' || lastVowel.q == 'i')) lastVowel.long = true
                        i++; continue
                    }
                    'و' -> if (lastVowel != null && lastVowel.q == 'u' && ph.lastOrNull() === lastVowel) {
                        lastVowel.long = true; i++; continue
                    }
                    'ي' -> if (lastVowel != null && lastVowel.q == 'i' && ph.lastOrNull() === lastVowel) {
                        lastVowel.long = true; i++; continue
                    }
                }
            }
            prevTanwin = false
            // Undosh
            val isHamza = ch in hamzas || HAMZA_ABOVE in m || HAMZA_BELOW in m
            val base = if (isHamza) Cons("'") else consMap[ch]
            if (base == null) { i++; continue }
            var cons: Cons = base
            if (HIGH_SEEN in m && ch == 'ص') cons = Cons("s")
            val unmarked = vq == null && SUKUN !in m && SHADDA !in m
            // Idg'om: harakatsiz undosh + keyingisi shaddali → o'qilmaydi
            if (unmarked && !isLast && nextHasShadda(i)) { i++; continue }
            if (unmarked && isLast && nextHasShadda(i) && next != null) {
                ph.add(C(cons.code, cons.heavy, unmarked = true)); i++; continue
            }
            var code = cons.code
            if (unmarked && ch == 'ن' && (HIGH_MEEM in m || LOW_MEEM in m)) code = "m"
            val isArticleLam = article && ch == 'ل' && units.getOrNull(i - 1)?.ch == WASLA
            val c = C(
                code, cons.heavy, double = SHADDA in m, hamza = isHamza && ch != 'ع', ayn = ch == 'ع',
                taMarbuta = ch == 'ة', article = isArticleLam, unmarked = unmarked,
            )
            ph.add(c)
            if (vq != null) {
                val heavy = cons.heavy || (ch == 'ر' && (vq == 'a' || vq == 'u'))
                val v = if (IMALA in m) V('e', long = true) else V(vq, heavy = heavy && vq == 'a')
                if (DAGGER in m && vq == 'a') v.long = true
                ph.add(v); lastVowel = v
                if (HIGH_NOON in m) { ph.add(C("n")); lastVowel = null }
                if (hasTanwin(m)) {
                    val nCode = if (HIGH_MEEM in m || LOW_MEEM in m) "m" else "n"
                    ph.add(C(nCode, tanwin = true)); prevTanwin = true
                }
            } else if (IMALA in m) {
                val v = V('e', long = true); ph.add(v); lastVowel = v
            } else {
                lastVowel = null
            }
            i++
        }
        return Word(ph, wasl, article, lafzStart, firstShadda)
    }

    // ---- chiqarish ----
    private fun latCons(code: String) = when (code) {
        "'" -> "ʼ"; "gh" -> "g‘"; else -> code
    }
    private fun cyrCons(code: String) = when (code) {
        "b" -> "б"; "t" -> "т"; "s" -> "с"; "j" -> "ж"; "h" -> "ҳ"; "x" -> "х"; "d" -> "д"; "z" -> "з"; "r" -> "р"
        "sh" -> "ш"; "'" -> "ъ"; "gh" -> "ғ"; "f" -> "ф"; "q" -> "қ"; "k" -> "к"; "l" -> "л"; "m" -> "м"; "n" -> "н"
        "v" -> "в"; "y" -> "й"; else -> code
    }

    private fun latVowel(v: V): String = when (v.q) {
        'a' -> if (v.lafz) (if (v.heavy) "o" else "a") else if (v.heavy) (if (v.long) "oo" else "o") else if (v.long) "aa" else "a"
        'i' -> if (v.long) "iy" else "i"
        'u' -> if (v.long) "uu" else "u"
        else -> "e"
    }
    private fun cyrVowel(v: V): String = when (v.q) {
        'a' -> if (v.lafz) (if (v.heavy) "о" else "а") else if (v.heavy) (if (v.long) "оо" else "о") else if (v.long) "аа" else "а"
        'i' -> if (v.long) "ий" else "и"
        'u' -> if (v.long) "уу" else "у"
        else -> "е"
    }

    private fun renderLatin(ph: List<Ph>): String {
        val sb = StringBuilder()
        for ((i, p) in ph.withIndex()) when (p) {
            is C -> {
                if (p.hamza && (sb.isEmpty() || (i > 0 && (ph[i - 1] as? C)?.article == true))) continue
                val s = latCons(p.code)
                if ((s == "h" && sb.endsWith("s")) || (s == "g‘" && sb.endsWith("n"))) sb.append('-')
                sb.append(s); if (p.double) sb.append(s)
            }
            is V -> sb.append(latVowel(p))
        }
        return sb.toString()
    }

    private fun renderCyr(ph: List<Ph>): String {
        val sb = StringBuilder()
        var i = 0
        while (i < ph.size) {
            val p = ph[i]
            when (p) {
                is C -> {
                    if ((p.hamza || p.ayn) && sb.isEmpty()) { i++; continue }
                    if (p.hamza && i > 0 && (ph[i - 1] as? C)?.article == true) { i++; continue }
                    val nv = ph.getOrNull(i + 1) as? V
                    if (p.code == "y" && nv != null && !nv.lafz && (nv.q == 'a' || nv.q == 'u')) {
                        if (p.double) sb.append("й")
                        val first = when {
                            nv.q == 'u' -> "ю"
                            nv.heavy -> "ё"
                            else -> "я"
                        }
                        sb.append(first)
                        if (nv.long) sb.append(if (nv.q == 'u') "у" else if (nv.heavy) "о" else "а")
                        i += 2; continue
                    }
                    val s = cyrCons(p.code)
                    sb.append(s); if (p.double) sb.append(s)
                }
                is V -> sb.append(cyrVowel(p))
            }
            i++
        }
        return sb.toString()
    }
}
