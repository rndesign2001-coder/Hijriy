package uz.hijriy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.core.Hijri
import uz.hijriy.app.core.HijriDate
import uz.hijriy.app.core.IslamicDays
import uz.hijriy.app.core.Uz
import uz.hijriy.app.ui.theme.QuranFont
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun CalendarScreen(app: HijriyApp, nav: NavHostController, initialTab: Int, standalone: Boolean = false) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(initialTab) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            if (standalone) "Sana konvertori" else "Taqvim",
            if (s.hijriAdjust != 0) "Hijriy tuzatish: ${if (s.hijriAdjust > 0) "+" else ""}${s.hijriAdjust} kun" else "Ummul-Quro hisobi",
            onBack = if (standalone) ({ nav.popBackStack(); Unit }) else null
        )
        if (!standalone) {
            TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
                listOf("Milodiy", "Hijriy", "Konvertor").forEachIndexed { i, t ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) })
                }
            }
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 640.dp)) {
                when (tab) {
                    0 -> GregorianMonth(s.hijriAdjust)
                    1 -> HijriMonth(s.hijriAdjust)
                    else -> Converter(s.hijriAdjust)
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(title: String, subtitle: String, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Oldingi oy") }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        IconButton(onClick = onToday) { Icon(Icons.Filled.Today, "Bugun") }
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Keyingi oy") }
    }
}

private data class Cell(val big: String, val small: String, val isToday: Boolean, val friday: Boolean, val event: String?)

@Composable
private fun MonthGrid(firstWeekdayIndex: Int, cells: List<Cell>) {
    Row(Modifier.fillMaxWidth()) {
        Uz.WEEKDAYS_SHORT.forEachIndexed { i, d ->
            Text(
                d, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium,
                color = if (i == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    VSpace(6.dp)
    val all = List(firstWeekdayIndex) { null } + cells
    all.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            for (i in 0 until 7) {
                val c = week.getOrNull(i)
                Box(Modifier.weight(1f).aspectRatio(0.86f).padding(2.dp)) {
                    if (c != null) {
                        val bg = when {
                            c.isToday -> MaterialTheme.colorScheme.primary
                            c.event != null -> MaterialTheme.colorScheme.secondaryContainer
                            c.friday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
                            else -> MaterialTheme.colorScheme.surfaceContainer
                        }
                        val fg = if (c.isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        Column(
                            Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(bg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(c.big, style = MaterialTheme.typography.titleMedium, color = fg)
                            Text(c.small, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.7f), maxLines = 1)
                            if (c.event != null && !c.isToday) Box(
                                Modifier.padding(top = 2.dp).size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventsList(events: List<Pair<String, String>>) {
    if (events.isEmpty()) return
    VSpace(14.dp)
    SectionCard(Modifier.fillMaxWidth()) {
        Text("Muhim kunlar", style = MaterialTheme.typography.titleMedium)
        VSpace(6.dp)
        events.forEach { (d, n) ->
            Row(Modifier.padding(vertical = 4.dp)) {
                Text(d, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 110.dp))
                Text(n, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun GregorianMonth(adj: Int) {
    val today = LocalDate.now()
    var month by remember { mutableStateOf(today.withDayOfMonth(1)) }
    val hStart = Hijri.fromGregorian(month, adj)
    val hEnd = Hijri.fromGregorian(month.plusDays(month.lengthOfMonth() - 1L), adj)
    MonthHeader(
        "${Uz.MONTHS[month.monthValue - 1]} ${month.year}",
        if (hStart.month == hEnd.month) "${hStart.monthName} ${hStart.year}" else "${hStart.monthName} – ${hEnd.monthName} ${hEnd.year}",
        { month = month.minusMonths(1) }, { month = month.plusMonths(1) }, { month = today.withDayOfMonth(1) }
    )
    VSpace(8.dp)
    val events = mutableListOf<Pair<String, String>>()
    val cells = (0 until month.lengthOfMonth()).map { i ->
        val d = month.plusDays(i.toLong())
        val h = Hijri.fromGregorian(d, adj)
        val ev = IslamicDays.find(h)?.name
        if (ev != null) events += "${d.dayOfMonth} ${Uz.MONTHS[d.monthValue - 1]}" to ev
        Cell(
            "${d.dayOfMonth}",
            if (h.day == 1) h.monthName.take(6) else "${h.day}",
            d == today, d.dayOfWeek.value == 5, ev
        )
    }
    MonthGrid(month.dayOfWeek.value - 1, cells)
    EventsList(events)
}

@Composable
private fun HijriMonth(adj: Int) {
    val todayG = LocalDate.now()
    val todayH = Hijri.fromGregorian(todayG, adj)
    var ym by remember { mutableStateOf(todayH.year to todayH.month) }
    val (y, m) = ym
    val len = Hijri.monthLength(y, m)
    val first = Hijri.toGregorian(HijriDate(y, m, 1), adj) ?: todayG
    val last = first.plusDays(len - 1L)
    fun shift(k: Int) {
        val idx = y * 12 + (m - 1) + k
        ym = (idx / 12) to (idx % 12 + 1)
    }
    MonthHeader(
        "${Hijri.MONTHS_UZ[m - 1]} $y",
        "${first.dayOfMonth} ${Uz.MONTHS[first.monthValue - 1]} – ${last.dayOfMonth} ${Uz.MONTHS[last.monthValue - 1]} ${last.year} • $len kun",
        { shift(-1) }, { shift(1) }, { ym = todayH.year to todayH.month }
    )
    Text(
        Hijri.MONTHS_AR[m - 1], fontFamily = QuranFont, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
    )
    VSpace(8.dp)
    val events = mutableListOf<Pair<String, String>>()
    val cells = (0 until len).map { i ->
        val g = first.plusDays(i.toLong())
        val h = HijriDate(y, m, i + 1)
        val ev = IslamicDays.find(h)?.name
        if (ev != null) events += "${h.day} ${h.monthName}" to "$ev (${g.dayOfMonth} ${Uz.MONTHS[g.monthValue - 1]})"
        Cell(
            "${i + 1}",
            if (g.dayOfMonth == 1) Uz.MONTHS[g.monthValue - 1].take(3) else "${g.dayOfMonth}",
            g == todayG, g.dayOfWeek.value == 5, ev
        )
    }
    MonthGrid(first.dayOfWeek.value - 1, cells)
    EventsList(events)
}

@Composable
private fun <T> Picker(label: String, items: List<T>, selected: Int, text: (T) -> String, onSelect: (Int) -> Unit, modifier: Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text(items[selected]), style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            }
            Icon(Icons.Filled.ArrowDropDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            items.forEachIndexed { i, it ->
                DropdownMenuItem(text = { Text(text(it)) }, onClick = { onSelect(i); open = false })
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value, onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(4)) },
        label = { Text(label) }, singleLine = true, modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun Converter(adj: Int) {
    val today = LocalDate.now()
    // Milodiy → Hijriy
    var gd by remember { mutableStateOf(today.dayOfMonth.toString()) }
    var gm by remember { mutableIntStateOf(today.monthValue - 1) }
    var gy by remember { mutableStateOf(today.year.toString()) }
    // Hijriy → Milodiy
    val th = Hijri.fromGregorian(today, adj)
    var hd by remember { mutableStateOf(th.day.toString()) }
    var hm by remember { mutableIntStateOf(th.month - 1) }
    var hy by remember { mutableStateOf(th.year.toString()) }

    SectionCard(Modifier.fillMaxWidth()) {
        Text("Milodiy → Hijriy", style = MaterialTheme.typography.titleMedium)
        VSpace(10.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberField("Kun", gd, { gd = it }, Modifier.weight(0.8f))
            Picker("Oy", Uz.MONTHS, gm, { it }, { gm = it }, Modifier.weight(1.5f))
            NumberField("Yil", gy, { gy = it }, Modifier.weight(1f))
        }
        VSpace(12.dp)
        val g = runCatching { LocalDate.of(gy.toInt(), gm + 1, gd.toInt()) }.getOrNull()
        ResultBox(
            if (g == null) null else {
                val h = Hijri.fromGregorian(g, adj)
                Triple(
                    "${h.day} ${h.monthName} ${h.year} h.",
                    "${Uz.arabicNumber(h.day)} ${h.monthNameAr} ${Uz.arabicNumber(h.year)}",
                    Uz.weekday(g.dayOfWeek) + (if (!Hijri.isUmmAlQuraRange(g)) " • arifmetik hisob" else "") +
                        (IslamicDays.find(h)?.let { " • ${it.name}" } ?: "") + relative(g, today)
                )
            }
        )
        TextButton(onClick = { gd = today.dayOfMonth.toString(); gm = today.monthValue - 1; gy = today.year.toString() }) { Text("Bugungi sana") }
    }
    VSpace(14.dp)
    SectionCard(Modifier.fillMaxWidth()) {
        Text("Hijriy → Milodiy", style = MaterialTheme.typography.titleMedium)
        VSpace(10.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberField("Kun", hd, { hd = it }, Modifier.weight(0.8f))
            Picker("Oy", Hijri.MONTHS_UZ, hm, { it }, { hm = it }, Modifier.weight(1.5f))
            NumberField("Yil", hy, { hy = it }, Modifier.weight(1f))
        }
        VSpace(12.dp)
        val hDate = runCatching { HijriDate(hy.toInt(), hm + 1, hd.toInt()) }.getOrNull()
        val g2 = hDate?.let { Hijri.toGregorian(it, adj) }
        ResultBox(
            if (g2 == null) null else Triple(
                Uz.gregorian(g2),
                Uz.weekday(g2.dayOfWeek),
                "Bu oy ${Hijri.monthLength(hDate.year, hDate.month)} kun" + relative(g2, today)
            ),
            errorText = if (hDate != null && hDate.day > 0) "Bu oyda ${Hijri.monthLength(hDate.year, hDate.month)} kun bor" else "Sanani kiriting"
        )
    }
    VSpace(10.dp)
    Text(
        "Hisob Ummul-Quro rasmiy jadvali asosida (1924–2077). Mahalliy e'lon 1 kun farq qilsa, Sozlamalarda hijriy tuzatishni o'zgartiring.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun relative(d: LocalDate, today: LocalDate): String {
    val n = ChronoUnit.DAYS.between(today, d)
    return when {
        n == 0L -> " • bugun"
        n > 0 -> " • $n kundan keyin"
        else -> " • ${-n} kun oldin"
    }
}

@Composable
private fun ResultBox(r: Triple<String, String, String>?, errorText: String = "Sana noto'g'ri") {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        if (r == null) Text(errorText, color = MaterialTheme.colorScheme.error)
        else Column {
            Text(r.first, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            if (r.second.any { it in '؀'..'ۿ' }) Text(r.second, fontFamily = QuranFont, fontSize = 20.sp)
            else Text(r.second, style = MaterialTheme.typography.titleMedium)
            Text(r.third, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
