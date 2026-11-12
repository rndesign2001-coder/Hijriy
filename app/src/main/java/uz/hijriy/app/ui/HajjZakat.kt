package uz.hijriy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.json.JSONObject
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.ui.theme.TitleFont

// ------------------------------ Haj va Umra ------------------------------

private class Dua(val ar: String, val meaning: String, val title: String = "Duo")
private class Step(val title: String, val text: String, val duas: List<Dua> = emptyList())

private val TALBIYA = Dua(
    "لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ، إِنَّ الْحَمْدَ وَالنِّعْمَةَ لَكَ وَالْمُلْكَ، لَا شَرِيكَ لَكَ",
    "Labbay, Allohim, labbay! Labbay, Sening sheriging yo'q, labbay! Albatta, hamd ham, ne'mat ham, mulk ham Seniki. Sening sheriging yo'q.",
    "Talbiya"
)
private val RABBANA = Dua(
    "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
    "Robbimiz, bizga bu dunyoda ham yaxshilik ber, oxiratda ham yaxshilik ber va bizni do'zax azobidan saqla.",
    "Rukni Yamoniy va Hajarul asvad orasida"
)
private val TAHLIL = Dua(
    "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
    "Allohdan o'zga iloh yo'q, U yolg'izdir, sherigi yo'q. Mulk Uniki, hamd Unga xosdir va U har narsaga qodirdir.",
    "Tahlil"
)

private val UMRA = listOf(
    Step(
        "1. Ihromga kirish (miqotda)",
        "Miqotga yetmasdan g'usl qilinadi. Erkaklar ikki bo'lak tikilmagan oq mato (izor va rido) kiyadi; ayollar odatiy yopiq kiyimda bo'ladi, yuz va kaftlar ochiq qoladi. Makruh vaqt bo'lmasa, ikki rakat ihrom namozi o'qiladi, so'ng umraga niyat qilinib talbiya aytiladi. Talbiya aytilishi bilan ihrom boshlanadi.",
        listOf(
            Dua("اللَّهُمَّ إِنِّي أُرِيدُ الْعُمْرَةَ فَيَسِّرْهَا لِي وَتَقَبَّلْهَا مِنِّي", "Allohim, men umrani niyat qildim, uni menga oson qil va mendan qabul et.", "Umra niyati"),
            TALBIYA,
        )
    ),
    Step(
        "2. Ihrom taqiqlari",
        "Ihromdagi kishi atir sepmaydi, soch-tirnoq olmaydi, ov qilmaydi, janjal va behuda so'zdan saqlanadi, er-xotin yaqinligi man etiladi. Erkaklar tikilgan kiyim va bosh kiyim kiymaydi, oyoqning ust suyagini yopadigan poyabzal kiymaydi. Talbiyani tez-tez aytib turish sunnat."
    ),
    Step(
        "3. Masjidul Haromga kirish",
        "Masjidga o'ng oyoq bilan, kirish duosi bilan kiriladi. Ka'bani birinchi ko'rganda qo'l ko'tarib duo qilish mustahab — bu duo ijobat bo'ladigan paytlardan.",
        listOf(Dua("بِسْمِ اللَّهِ وَالصَّلَاةُ وَالسَّلَامُ عَلَى رَسُولِ اللَّهِ، اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ", "Alloh nomi bilan. Allohning Rasuliga salovot va salom bo'lsin. Allohim, menga rahmating eshiklarini och.", "Masjidga kirish duosi"))
    ),
    Step(
        "4. Tavof — 7 aylana",
        "Tahorat bilan, Hajarul asvad ro'parasidan boshlanadi; Ka'ba chap tomonda qoladi. Erkaklar tavof davomida o'ng yelkani ochib (iztibo'), dastlabki uch aylanada qisqa qadamlar bilan tez yuradi (ramal). Har aylana boshida Hajarul asvadga qarab qo'l ko'tarib istilom qilinadi. Tavof davomida o'zingiz bilgan duo va zikrlarni o'qishingiz mumkin.",
        listOf(Dua("بِسْمِ اللَّهِ وَاللَّهُ أَكْبَرُ", "Alloh nomi bilan, Alloh buyukdir.", "Har aylana boshida (istilom)"), RABBANA)
    ),
    Step(
        "5. Maqomi Ibrohim ortida 2 rakat",
        "Tavofdan so'ng, iloji bo'lsa Maqomi Ibrohim ortida (bo'lmasa masjidning istalgan joyida) ikki rakat tavof namozi o'qiladi — Hanafiy mazhabida vojib. Birinchi rakatda \"Kofirun\", ikkinchisida \"Ixlos\" surasini o'qish sunnat.",
        listOf(Dua("وَاتَّخِذُوا مِنْ مَقَامِ إِبْرَاهِيمَ مُصَلًّى", "Ibrohim maqomidan namozgoh tutinglar. (Baqara, 125)", "Oyat"))
    ),
    Step(
        "6. Zamzam suvi",
        "Qiblaga yuzlanib, Bismilloh bilan, uch nafasda to'yib ichiladi va duo qilinadi.",
        listOf(Dua("اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا وَرِزْقًا وَاسِعًا وَشِفَاءً مِنْ كُلِّ دَاءٍ", "Allohim, Sendan foydali ilm, keng rizq va har qanday dardga shifo so'rayman.", "Zamzam ichganda"))
    ),
    Step(
        "7. Sa'y — Safo va Marva orasida 7 marta",
        "Safodan boshlanib Marvada tugaydi: Safodan Marvaga bir marta, Marvadan Safoga ikkinchi marta hisoblanadi. Safoga chiqqach Ka'baga yuzlanib, qo'l ko'tarib takbir, tahlil aytiladi va duo qilinadi. Yashil chiroqlar orasida erkaklar yengil yugurib o'tadi.",
        listOf(
            Dua("إِنَّ الصَّفَا وَالْمَرْوَةَ مِنْ شَعَائِرِ اللَّهِ", "Albatta, Safo va Marva Allohning nishonalaridandir. (Baqara, 158)", "Safoga yaqinlashganda"),
            Dua(
                "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ، لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ، أَنْجَزَ وَعْدَهُ، وَنَصَرَ عَبْدَهُ، وَهَزَمَ الْأَحْزَابَ وَحْدَهُ",
                "Allohdan o'zga iloh yo'q, U yolg'iz, sherigi yo'q. Mulk Uniki, hamd Unga xos va U har narsaga qodir. Allohdan o'zga iloh yo'q, yolg'iz O'zi; va'dasini bajardi, bandasiga nusrat berdi va guruhlarni yolg'iz O'zi yengdi.",
                "Safo va Marva ustida"
            ),
        )
    ),
    Step(
        "8. Soch oldirish yoki qisqartirish",
        "Erkaklar sochni butunlay oldirishi afzal yoki sochning kamida to'rtdan bir qismidan barmoq bo'g'imi uzunligida qisqartiradi. Ayollar soch uchidan barmoq bo'g'imi uzunligida kesadi. Shu bilan umra tugaydi va ihromdan chiqiladi."
    ),
)

private val HAJJ = listOf(
    Step(
        "Tamattu' haji haqida",
        "O'zbekistonlik ziyoratchilar odatda tamattu' hajini ado etadi: avval umra qilinib ihromdan chiqiladi, so'ng 8-Zulhijjada Makkadan haj uchun qayta ihromga kiriladi. Umra amallari \"Umra\" bo'limida."
    ),
    Step(
        "8-Zulhijja — Tarviya kuni",
        "Makkadagi turar joydan g'usl qilib ihromga kiriladi, haj niyat qilinib talbiya aytiladi. Minoga borib, peshindan to 9-kuni bomdodgacha besh vaqt namozni shu yerda o'qish sunnat.",
        listOf(Dua("اللَّهُمَّ إِنِّي أُرِيدُ الْحَجَّ فَيَسِّرْهُ لِي وَتَقَبَّلْهُ مِنِّي", "Allohim, men hajni niyat qildim, uni menga oson qil va mendan qabul et.", "Haj niyati"), TALBIYA)
    ),
    Step(
        "9-Zulhijja — Arafa kuni (hajning asosiy rukni)",
        "Quyosh chiqqach Arafotga boriladi. Zavoldan to quyosh botguncha Arafotda turish (vuquf) — hajning eng muhim rukni; usiz haj bo'lmaydi. Kun duo, istig'for, zikr va talbiya bilan o'tkaziladi. Quyosh botgach, shomni o'qimasdan Muzdalifaga yo'l olinadi.",
        listOf(TAHLIL.let { Dua(it.ar, it.meaning, "Arafa kunining eng afzal duosi") })
    ),
    Step(
        "Muzdalifa kechasi",
        "Muzdalifada shom va xufton xufton vaqtida birga o'qiladi. Kecha shu yerda o'tkaziladi, jamratlar uchun mayda toshlar teriladi (49 yoki 70 dona). Bomdoddan so'ng quyosh chiqishiga yaqin qadar Muzdalifada turish (vuquf) Hanafiy mazhabida vojib."
    ),
    Step(
        "10-Zulhijja — Qurbon hayiti",
        "Minoga qaytib, Jamratul Aqabaga 7 ta tosh otiladi — har biri bilan takbir aytiladi; birinchi tosh bilan talbiya to'xtatiladi. So'ng qurbonlik so'yiladi (tamattu' va qiron uchun vojib), keyin soch oldiriladi yoki qisqartiriladi. Hanafiy mazhabida bu tartib (tosh — qurbonlik — soch) vojib. Shundan so'ng er-xotin yaqinligidan boshqa ihrom taqiqlari yechiladi.",
        listOf(Dua("بِسْمِ اللَّهِ، اللَّهُ أَكْبَرُ", "Alloh nomi bilan, Alloh buyukdir.", "Har bir tosh otilganda"))
    ),
    Step(
        "Ziyorat (ifoza) tavofi va sa'y",
        "Hajning rukni bo'lgan ziyorat tavofi 10-Zulhijjadan 12-Zulhijja quyosh botishigacha ado etilishi vojib. Tamattu' qiluvchi undan so'ng sa'y ham qiladi. Shu bilan ihromning barcha taqiqlari yechiladi.",
        listOf(RABBANA)
    ),
    Step(
        "11–12 (13)-Zulhijja — Mino kunlari",
        "Kechalari Minoda qolinadi. Har kuni zavoldan keyin uchala jamratga — kichik, o'rta va katta (Aqaba) — 7 tadan tosh otiladi. Kichik va o'rta jamratdan keyin chetga o'tib, qiblaga yuzlanib uzoq duo qilish sunnat. 12-kuni quyosh botishidan oldin Minodan chiqib ketish mumkin; qolganlar 13-kuni ham tosh otadi.",
        listOf(Dua("بِسْمِ اللَّهِ، اللَّهُ أَكْبَرُ", "Alloh nomi bilan, Alloh buyukdir.", "Har bir tosh otilganda"))
    ),
    Step(
        "Vido' (xayrlashuv) tavofi",
        "Makkadan qaytishdan oldin vido' tavofi qilinadi — tashqaridan kelgan ziyoratchilar uchun vojib (hayz yoki nifosdagi ayollardan soqit). Bu tavofdan keyin sa'y yo'q. Zamzam ichilib, Ka'baga qarab duo qilinadi."
    ),
)

@Composable
fun HajjScreen(app: HijriyApp, nav: NavHostController) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val steps = if (tab == 0) UMRA else HAJJ
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Haj va Umra", "Bosqichma-bosqich amallar va duolar", onBack = { nav.popBackStack() })
        Row(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer).padding(4.dp)
        ) {
            listOf("Umra", "Haj").forEachIndexed { i, l ->
                val sel = i == tab
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { tab = i }.padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) { Text(l, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge) }
            }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                OrnateCard(listOf(Color(0xFF3A2A08), Color(0xFFA67C26), Color(0xFFD6AE52)), photo = if (tab == 0) uz.hijriy.app.R.drawable.bg_kaaba_sunset else CardPhotos.arafat, alignY = if (tab == 0) 0.2f else 0f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HIcon(HI.KAABA, Color(0xFF1B1B1B), size = 48.dp, accent = Color(0xFFE9C46A))
                        HSpace(12.dp)
                        Column {
                            Text(if (tab == 0) "Umra" else "Haj (tamattu')", color = Color.White, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            Text(
                                "Hanafiy mazhabi bo'yicha qisqa qo'llanma. Batafsil masalalarni guruhingiz imomi yoki yo'lboshchisidan so'rang.",
                                color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            itemsIndexed(steps, key = { i, st -> "$tab/$i/${st.title}" }) { _, st ->
                var open by remember(tab, st.title) { mutableStateOf(false) }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, Color(0xFFE9C46A).copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .clickable { open = !open }.padding(16.dp)
                ) {
                    Text(st.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    VSpace(6.dp)
                    Text(st.text, style = MaterialTheme.typography.bodyMedium)
                    if (st.duas.isNotEmpty()) {
                        VSpace(8.dp)
                        if (!open) Text("Duolarni ko'rish (${st.duas.size}) ›", style = MaterialTheme.typography.labelLarge, color = Color(0xFFC8962E))
                        else st.duas.forEach { d -> VSpace(8.dp); DuaBlock(d.title, d.ar, d.meaning, s.readingScript == 1) }
                    }
                }
            }
        }
    }
}

// ------------------------------ Zakot kalkulyatori ------------------------------

private val ZFields = listOf(
    "cash" to "Naqd pul", "bank" to "Bank hisobi va kartalardagi pul", "trade" to "Savdo mollari (sotish uchun)",
    "recv" to "Qaytarilishi aniq bo'lgan qarzlar (sizga)", "gold_g" to "Oltin (gramm)", "silver_g" to "Kumush (gramm)",
    "debt" to "To'lanishi kerak bo'lgan qarzlaringiz", "gold_p" to "1 gramm oltin narxi", "silver_p" to "1 gramm kumush narxi",
)

private fun fmtSum(v: Double): String = String.format(java.util.Locale.US, "%,.0f", v).replace(',', ' ')

@Composable
fun ZakatScreen(app: HijriyApp, nav: NavHostController) {
    val saved = remember {
        runCatching { JSONObject(app.settings.getString("zakat") ?: "{}") }.getOrDefault(JSONObject())
    }
    val vals = remember { mutableStateMapOf<String, String>().apply { ZFields.forEach { (k, _) -> put(k, saved.optString(k, "")) } } }
    var basis by remember { mutableIntStateOf(saved.optInt("basis", 0)) } // 0 — kumush, 1 — oltin
    fun num(k: String) = vals[k]?.replace(" ", "")?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
    fun persist() {
        val j = JSONObject(); ZFields.forEach { (k, _) -> j.put(k, vals[k] ?: "") }; j.put("basis", basis)
        app.settings.putString("zakat", j.toString())
    }
    val goldP = num("gold_p"); val silverP = num("silver_p")
    val assets = num("cash") + num("bank") + num("trade") + num("recv") + num("gold_g") * goldP + num("silver_g") * silverP
    val net = assets - num("debt")
    val nisab = if (basis == 0) 595 * silverP else 85 * goldP
    val due = nisab > 0 && net >= nisab
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Zakot kalkulyatori", "Hanafiy mazhabi bo'yicha", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            OrnateCard(if (due) listOf(Color(0xFF0A2A20), Color(0xFF1E6B4F), Color(0xFF2E8B67)) else listOf(Color(0xFF1C1C26), Color(0xFF3B3B4F), Color(0xFF55556E)), photo = CardPhotos.registanGold) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (due) "To'lanadigan zakot" else if (nisab <= 0) "Narxlarni kiriting" else "Zakot farz emas", color = Color.White.copy(alpha = 0.85f))
                    Text(
                        if (due) "${fmtSum(net * 0.025)} so'm" else "—", color = Color(0xFFE9C46A),
                        fontWeight = FontWeight.ExtraBold, fontSize = 34.sp
                    )
                    VSpace(6.dp)
                    Text("Sof boylik: ${fmtSum(net)} so'm", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text("Nisob: ${if (nisab > 0) fmtSum(nisab) + " so'm" else "—"} (${if (basis == 0) "595 g kumush" else "85 g oltin"})", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                }
            }
            VSpace(12.dp)
            Text("Nisob asosi", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                listOf("Kumush (595 g)", "Oltin (85 g)").forEachIndexed { i, l ->
                    androidx.compose.material3.FilterChip(basis == i, { basis = i; persist() }, { Text(l) })
                }
            }
            Text(
                "Hanafiy ulamolari kambag'allar manfaati uchun kumush nisobini afzal ko'radi. Narxlarni bozordagi joriy narx yoki O'zbekiston musulmonlari idorasi e'lon qilgan nisob bo'yicha kiriting.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            VSpace(10.dp)
            listOf(
                "Narxlar" to listOf("gold_p", "silver_p"),
                "Mol-mulk" to listOf("cash", "bank", "trade", "recv", "gold_g", "silver_g"),
                "Ayiriladi" to listOf("debt"),
            ).forEach { (title, keys) ->
                SectionTitle(title)
                keys.forEach { k ->
                    val label = ZFields.first { it.first == k }.second
                    OutlinedTextField(
                        value = vals[k] ?: "", onValueChange = { v -> vals[k] = v.filter { it.isDigit() || it == '.' || it == ',' }; persist() },
                        label = { Text(label) }, singleLine = true,
                        suffix = { androidx.compose.material3.Text(if (k.endsWith("_g")) "g" else "so'm") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(14.dp)
                    )
                }
            }
            VSpace(10.dp)
            SectionCard(Modifier.fillMaxWidth()) {
                Text("Eslatma", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                listOf(
                    "Zakot boylik nisobga yetgan kundan boshlab bir hijriy yil o'tgach farz bo'ladi.",
                    "Shaxsiy ehtiyoj buyumlari — uy, mashina, kiyim, ro'zg'or jihozlari — zakotga kirmaydi.",
                    "Zakot boylikning 2,5 foizi (qirqdan biri).",
                    "Murakkab holatlarda (shirkat, ijara daromadi, chorva, ekin) imom yoki muftidan so'rang.",
                ).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
            }
            VSpace(24.dp)
        }
    }
}
