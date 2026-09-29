package uz.hijriy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.core.Hijri
import uz.hijriy.app.core.Prayer
import uz.hijriy.app.core.PrayerAdjust
import uz.hijriy.app.core.Uz
import uz.hijriy.app.core.fmtMin
import uz.hijriy.app.notify.PrayerScheduler
import java.time.LocalDate

private val prayerEmoji = mapOf(
    Prayer.FAJR to "🌄", Prayer.SUNRISE to "☀️", Prayer.DHUHR to "🌞",
    Prayer.ASR to "🌤️", Prayer.MAGHRIB to "🌇", Prayer.ISHA to "🌙"
)

@Composable
fun PrayerScreen(app: HijriyApp, nav: NavHostController) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    val now by rememberNow(15_000)
    var date by remember { mutableStateOf(LocalDate.now()) }
    var monthly by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Prayer?>(null) }
    val today = now.toLocalDate()
    val t = remember(s, date) { s.times(date) }
    val pn = prayerNow(s, now)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Namoz vaqtlari", s.locName, actions = {
            IconButton(onClick = { nav.go("settings") }) { Icon(Icons.Filled.Tune, "Sozlamalar") }
        })
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !monthly, onClick = { monthly = false }, label = { Text("Kunlik") })
            FilterChip(selected = monthly, onClick = { monthly = true }, label = { Text("Oylik jadval") })
        }
        if (!monthly) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    SectionCard(padding = 8.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { date = date.minusDays(1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Oldingi kun")
                            }
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { date = LocalDate.now() },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(Uz.gregorianFull(date), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                                Text(
                                    Hijri.fromGregorian(date, s.hijriAdjust).toString() + " h." + if (date != today) "  • bugunga qaytish" else "",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { date = date.plusDays(1) }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Keyingi kun")
                            }
                        }
                    }
                }
                items(Prayer.entries) { p ->
                    val active = date == today && p == pn.current
                    val bg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
                    val fg = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .clickable { editing = p }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(prayerEmoji.getValue(p), style = MaterialTheme.typography.headlineSmall)
                        HSpace(14.dp)
                        Column(Modifier.weight(1f)) {
                            Text(p.uz, style = MaterialTheme.typography.titleMedium, color = fg)
                            val a = s.adjust[p]
                            if (a != null && !a.isDefault) {
                                Text(
                                    if (a.fixed >= 0) "Doimiy vaqt" else "Hisobiy ${fmtMin(t.calculated.getValue(p))} ${signed(a.offset)} daq.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (active) fg.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary
                                )
                            } else if (active) {
                                Text("Hozirgi vaqt", style = MaterialTheme.typography.bodySmall, color = fg.copy(alpha = 0.8f))
                            } else if (date == today && p == pn.next) {
                                Text("Keyingi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text(
                            fmtMin(t[p]), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = fg
                        )
                        HSpace(6.dp)
                        Icon(Icons.Filled.Edit, "Sozlash", tint = fg.copy(alpha = 0.45f), modifier = Modifier.width(18.dp))
                    }
                }
                item {
                    Text(
                        "Usul: ${s.method.title} • Asr: ${if (s.hanafi) "Hanafiy" else "Shofe'iy"}\n" +
                            "Vaqtni sozlash uchun ustiga bosing: +N daqiqa qo'shish, o'zingiz bilgan vaqtni kiritish yoki doimiy vaqt belgilash mumkin.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            MonthlyTable(app, date)
        }
    }

    editing?.let { p ->
        AdjustDialog(
            prayer = p,
            calculatedToday = s.times(LocalDate.now()).calculated.getValue(p),
            current = s.adjust[p] ?: PrayerAdjust(),
            onDismiss = { editing = null },
            onSave = { a ->
                app.settings.update { st -> st.copy(adjust = st.adjust + (p to a)) }
                PrayerScheduler.reschedule(app)
                editing = null
            }
        )
    }
}

private fun signed(n: Int) = if (n > 0) "+$n" else n.toString()

@Composable
private fun MonthlyTable(app: HijriyApp, anchor: LocalDate) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    var month by remember { mutableStateOf(anchor.withDayOfMonth(1)) }
    val days = remember(s, month) { (0 until month.lengthOfMonth()).map { month.plusDays(it.toLong()) } }
    val today = LocalDate.now()
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
            IconButton(onClick = { month = month.minusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
            Text(
                "${Uz.MONTHS[month.monthValue - 1]} ${month.year}", Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center
            )
            IconButton(onClick = { month = month.plusMonths(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
        }
        val scroll = rememberScrollState()
        Column(Modifier.horizontalScroll(scroll)) {
            val cols = listOf("Sana", "Hijriy") + Prayer.entries.map { it.uz }
            val widths = listOf(64, 70, 62, 62, 62, 62, 62, 62)
            Row(
                Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary)
                    .padding(vertical = 8.dp)
            ) {
                cols.forEachIndexed { i, c ->
                    Text(
                        c, Modifier.width(widths[i].dp), textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            LazyColumn {
                items(days) { d ->
                    val t = s.times(d)
                    val h = Hijri.fromGregorian(d, s.hijriAdjust)
                    val isToday = d == today
                    val bg = when {
                        isToday -> MaterialTheme.colorScheme.primaryContainer
                        d.dayOfWeek.value == 5 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        else -> Color.Transparent
                    }
                    Row(Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(vertical = 7.dp)) {
                        Text(
                            "${d.dayOfMonth} ${Uz.WEEKDAYS_SHORT[d.dayOfWeek.value - 1]}", Modifier.width(widths[0].dp),
                            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            "${h.day} ${h.monthName.take(5)}", Modifier.width(widths[1].dp),
                            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
                        )
                        Prayer.entries.forEachIndexed { i, p ->
                            Text(
                                fmtMin(t[p]), Modifier.width(widths[i + 2].dp), textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustDialog(
    prayer: Prayer,
    calculatedToday: Int,
    current: PrayerAdjust,
    onDismiss: () -> Unit,
    onSave: (PrayerAdjust) -> Unit,
) {
    // 0 = +N daqiqa, 1 = vaqtni kiritish (farq saqlanadi), 2 = doimiy vaqt
    var mode by remember { mutableIntStateOf(if (current.fixed >= 0) 2 else 0) }
    var offset by remember { mutableIntStateOf(current.offset) }
    val startRaw = if (current.fixed >= 0) current.fixed else calculatedToday + current.offset
    val startMin = ((startRaw % 1440) + 1440) % 1440
    val tp = rememberTimePickerState(startMin / 60, startMin % 60, is24Hour = true)
    val pickedMin = tp.hour * 60 + tp.minute

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${prayer.uz} vaqtini sozlash") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "Bugungi hisobiy vaqt: ${fmtMin(calculatedToday)}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                VSpace(10.dp)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("+N daqiqa") })
                    FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Vaqtni kiritish") })
                    FilterChip(selected = mode == 2, onClick = { mode = 2 }, label = { Text("Doimiy") })
                }
                VSpace(12.dp)
                when (mode) {
                    0 -> {
                        Row(
                            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(onClick = { offset = (offset - 5).coerceAtLeast(-180) }) { Text("−5") }
                            FilledTonalIconButton(onClick = { offset = (offset - 1).coerceAtLeast(-180) }) { Text("−1") }
                            Text(
                                signed(offset), style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            FilledTonalIconButton(onClick = { offset = (offset + 1).coerceAtMost(180) }) { Text("+1") }
                            OutlinedButton(onClick = { offset = (offset + 5).coerceAtMost(180) }) { Text("+5") }
                        }
                        VSpace(8.dp)
                        Text(
                            "Natija: ${fmtMin(calculatedToday + offset)} — har kuni hisobiy vaqtga $offset daqiqa qo'shiladi.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    1 -> {
                        TimeInput(state = tp)
                        val diff = pickedMin - calculatedToday
                        Text(
                            "Farq: ${signed(diff)} daqiqa. Keyingi kunlarda vaqt hisobiy vaqt bilan birga o'zgaradi, shu farq saqlanadi.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    else -> {
                        TimeInput(state = tp)
                        Text(
                            "Har kuni aynan ${fmtMin(pickedMin)} ko'rsatiladi (masalan, masjid jamoat vaqti).",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    when (mode) {
                        0 -> PrayerAdjust(offset = offset)
                        1 -> PrayerAdjust(offset = pickedMin - calculatedToday)
                        else -> PrayerAdjust(fixed = pickedMin)
                    }
                )
            }) { Text("Saqlash") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(PrayerAdjust()) }) { Text("Asliga") }
                TextButton(onClick = onDismiss) { Text("Bekor") }
            }
        }
    )
}
