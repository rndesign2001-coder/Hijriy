package uz.hijriy.app.ui

import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VolunteerActivism
import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.core.CalcMethod
import uz.hijriy.app.core.Hijri
import uz.hijriy.app.core.Prayer
import uz.hijriy.app.core.PrayerAdjust
import uz.hijriy.app.core.fmtMin
import uz.hijriy.app.data.ThemeMode
import uz.hijriy.app.notify.PrayerScheduler
import uz.hijriy.app.ui.theme.Palettes
import java.time.LocalDate

@Composable
fun SettingsScreen(app: HijriyApp, nav: NavHostController) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var methodDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Prayer?>(null) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        app.settings.update { it.copy(notifyEnabled = ok) }
        PrayerScheduler.reschedule(app)
    }
    val ringPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val uri: Uri? = if (Build.VERSION.SDK_INT >= 33)
            res.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        else @Suppress("DEPRECATION") res.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (res.resultCode == android.app.Activity.RESULT_OK) {
            app.settings.update { it.copy(notifySound = uri?.toString() ?: "") }
            uz.hijriy.app.notify.Channels.ensure(app, uri?.toString() ?: "")
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val saved = importNotificationSound(ctx, uri)
            if (saved != null) {
                app.settings.update { it.copy(notifySound = saved.toString()) }
                uz.hijriy.app.notify.Channels.ensure(app, saved.toString())
                android.widget.Toast.makeText(ctx, "Azon ovozi o'rnatildi", android.widget.Toast.LENGTH_SHORT).show()
            } else android.widget.Toast.makeText(ctx, "Faylni qo'shib bo'lmadi", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    fun update(block: (uz.hijriy.app.data.Settings) -> uz.hijriy.app.data.Settings) {
        app.settings.update(block)
        PrayerScheduler.reschedule(app)
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Sozlamalar", onBack = { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 640.dp)) {
                // ---------- Ko'rinish ----------
                SectionTitle("Ko'rinish")
                SectionCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                    SettingRow(Icons.Filled.Brightness4, "Rejim", s.themeMode.title)
                    Row(Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { m ->
                            FilterChip(selected = s.themeMode == m, onClick = { update { it.copy(themeMode = m) } }, label = { Text(m.title) })
                        }
                    }
                    VSpace(8.dp)
                    SettingRow(Icons.Filled.Palette, "Mavzu rangi", Palettes[s.palette.coerceIn(0, Palettes.lastIndex)].title)
                    Row(
                        Modifier.padding(start = 8.dp, bottom = 6.dp).horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Palettes.forEachIndexed { i, p ->
                            Column(
                                Modifier.clip(RoundedCornerShape(12.dp)).clickable { update { it.copy(palette = i) } }.padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(p.gradLight))
                                        .border(
                                            3.dp, if (s.palette == i) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) { if (s.palette == i) Icon(Icons.Filled.Check, null, tint = Color.White) }
                                Text(p.title, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // ---------- Joylashuv ----------
                SectionTitle("Joylashuv")
                SectionCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                    SettingRow(
                        Icons.Filled.LocationOn, s.locName,
                        (if (s.fromGps) "GPS • " else "") + "%.4f, %.4f".format(s.lat, s.lon),
                        onClick = { nav.go("location") }
                    )
                }

                // ---------- Namoz vaqtlari ----------
                SectionTitle("Namoz vaqtlari hisobi")
                SectionCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                    SettingRow(Icons.Filled.AccessTime, "Hisoblash usuli", s.method.title, onClick = { methodDialog = true })
                    SettingRow(null, "Asr vaqti", if (s.hanafi) "Hanafiy (soya 2 barobar)" else "Shofe'iy / Molikiy / Hanbaliy (1 barobar)",
                        onClick = { update { it.copy(hanafi = !it.hanafi) } }) {
                        Switch(checked = s.hanafi, onCheckedChange = { v -> update { it.copy(hanafi = v) } })
                    }
                    if (s.method == CalcMethod.CUSTOM) {
                        Text("Bomdod burchagi: ${"%.1f".format(s.customFajr)}°", modifier = Modifier.padding(start = 8.dp))
                        Slider(s.customFajr.toFloat(), { v -> update { it.copy(customFajr = (v * 2).toInt() / 2.0) } }, valueRange = 10f..20f)
                        Text("Xufton burchagi: ${"%.1f".format(s.customIsha)}°", modifier = Modifier.padding(start = 8.dp))
                        Slider(s.customIsha.toFloat(), { v -> update { it.copy(customIsha = (v * 2).toInt() / 2.0) } }, valueRange = 10f..20f)
                    }
                }
                SectionTitle("Vaqtlarni qo'lda sozlash")
                SectionCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                    val t = s.times(LocalDate.now())
                    Prayer.entries.forEach { p ->
                        val a = s.adjust[p] ?: PrayerAdjust()
                        SettingRow(
                            null, "${p.uz}: ${fmtMin(t[p])}",
                            when {
                                a.fixed >= 0 -> "Doimiy vaqt"
                                a.offset != 0 -> "Hisobiy ${fmtMin(t.calculated.getValue(p))} ${if (a.offset > 0) "+" else ""}${a.offset} daqiqa"
                                else -> "Hisobiy vaqt"
                            },
                            onClick = { editing = p }
                        )
                    }
                    if (s.adjust.values.any { !it.isDefault }) {
                        OutlinedButton(onClick = { update { it.copy(adjust = emptyMap()) } }, modifier = Modifier.padding(start = 8.dp)) {
                            Icon(Icons.Filled.Restore, null); HSpace(6.dp); Text("Hammasini asliga qaytarish")
                        }
                    }
                }

                // ---------- Hijriy ----------
                SectionTitle("Hijriy taqvim")
                SectionCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                    val h = Hijri.fromGregorian(LocalDate.now(), s.hijriAdjust)
                    SettingRow(Icons.Filled.CalendarMonth, "Bugun: $h", "Mahalliy e'longa moslash uchun kunni suring")
                    Row(Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (-2..2).forEach { d ->
                            FilterChip(
                                selected = s.hijriAdjust == d, onClick = { update { it.copy(hijriAdjust = d) } },
                                label = { Text(if (d > 0) "+$d" else "$d") }
                            )
                        }
                    }
                }

                // ---------- Bildirishnoma ----------
                SectionTitle("Eslatmalar")
                SectionCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                    SettingRow(Icons.Filled.Notifications, "Namoz vaqti eslatmasi", if (s.notifyEnabled) "Yoqilgan" else "O'chirilgan") {
                        Switch(checked = s.notifyEnabled, onCheckedChange = { v ->
                            if (v && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else update { it.copy(notifyEnabled = v) }
                        })
                    }
                    if (s.notifyEnabled) {
                        Row(Modifier.padding(start = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Prayer.entries.filter { it.notifiable }.forEach { p ->
                                FilterChip(
                                    selected = p in s.notifyPrayers,
                                    onClick = {
                                        update { st ->
                                            st.copy(notifyPrayers = if (p in st.notifyPrayers) st.notifyPrayers - p else st.notifyPrayers + p)
                                        }
                                    },
                                    label = { Text(p.uz) }
                                )
                            }
                        }
                        VSpace(6.dp)
                        Text("Oldindan eslatish: ${if (s.notifyBefore == 0) "vaqt kirganda" else "${s.notifyBefore} daqiqa oldin"}",
                            modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                        Row(Modifier.padding(start = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0, 5, 10, 15, 30).forEach { m ->
                                FilterChip(selected = s.notifyBefore == m, onClick = { update { it.copy(notifyBefore = m) } },
                                    label = { Text(if (m == 0) "0" else "$m") })
                            }
                        }
                        if (Build.VERSION.SDK_INT >= 31) {
                            val am = ctx.getSystemService(AlarmManager::class.java)
                            if (am != null && !am.canScheduleExactAlarms()) {
                                TextButton(onClick = {
                                    runCatching {
                                        ctx.startActivity(
                                            Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}"))
                                        )
                                    }
                                }) { Text("Aniq vaqtda eslatish uchun ruxsat bering") }
                            }
                        }
                    }
                }

                SectionCard(Modifier.fillMaxWidth().padding(top = 10.dp), padding = 12.dp) {
                    val soundTitle = remember(s.notifySound) {
                        if (s.notifySound.isBlank()) "Standart ovoz"
                        else runCatching { android.media.RingtoneManager.getRingtone(ctx, Uri.parse(s.notifySound))?.getTitle(ctx) }.getOrNull() ?: "Tanlangan ovoz"
                    }
                    SettingRow(Icons.Filled.MusicNote, "Eslatma ovozi", soundTitle, onClick = {
                        ringPicker.launch(Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALL)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Eslatma ovozi")
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            if (s.notifySound.isNotBlank()) putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(s.notifySound))
                        })
                    })
                    if (Build.VERSION.SDK_INT >= 29) SettingRow(
                        Icons.Filled.LibraryMusic, "Azon faylini tanlash",
                        "Telefondagi azon (mp3) faylini eslatma ovozi qilish",
                        onClick = { audioPicker.launch(arrayOf("audio/*")) }
                    )
                    SettingRow(Icons.Filled.EventAvailable, "Juma kuni eslatma", "Juma ertalab 8:30 da — Juma namozi va Kahf surasi") {
                        Switch(checked = s.fridayReminder, onCheckedChange = { v ->
                            if (v && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                            update { it.copy(fridayReminder = v) }
                        })
                    }
                    SettingRow(
                        Icons.Filled.Widgets, "Bosh ekran vidjeti",
                        "Telefon bosh ekranini uzoq bosing → Vidjetlar → Hijriy Taqvim"
                    )
                }

                SectionTitle("Ilova haqida")
                SectionCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                    SettingRow(
                        Icons.Filled.Info, "Hijriy Taqvim",
                        "Namoz vaqtlari O'zbekiston musulmonlari idorasi e'lon qiladigan vaqtlarga moslab hisoblanadi. " +
                            "Qur'on matni: Tanzil (Usmoniy), tajvid: cpfair/quran-tajweed, shrift: Amiri Quran. Ob-havo: Open-Meteo."
                    )
                }
                VSpace(24.dp)
            }
        }
    }

    if (methodDialog) {
        AlertDialog(
            onDismissRequest = { methodDialog = false },
            title = { Text("Hisoblash usuli") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    CalcMethod.entries.forEach { m ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .clickable { update { it.copy(method = m) }; methodDialog = false }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = s.method == m, onClick = { update { it.copy(method = m) }; methodDialog = false })
                            Column {
                                Text(m.title)
                                Text(
                                    if (m == CalcMethod.CUSTOM) "Burchaklarni o'zingiz belgilaysiz"
                                    else "Bomdod ${m.fajr}° • Xufton ${if (m.ishaMinutes > 0) "${m.ishaMinutes} daq." else "${m.isha}°"}" +
                                        (if (m.maghribPlus > 0) " • Shom +${m.maghribPlus} daq." else ""),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { methodDialog = false }) { Text("Yopish") } }
        )
    }
    editing?.let { p ->
        AdjustDialog(
            prayer = p,
            calculatedToday = s.times(LocalDate.now()).calculated.getValue(p),
            current = s.adjust[p] ?: PrayerAdjust(),
            onDismiss = { editing = null },
            onSave = { a -> update { st -> st.copy(adjust = st.adjust + (p to a)) }; editing = null }
        )
    }
}

@Composable
fun MoreScreen(nav: NavHostController) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Yana")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionCard(Modifier.fillMaxWidth(), padding = 8.dp) {
                SettingRow(Icons.Filled.Explore, "Qibla kompasi", "Ka'ba yo'nalishini aniqlash", onClick = { nav.go("qibla") })
                SettingRow(Icons.Filled.Fingerprint, "Tasbeh", "Zikr sanagich, namozdan keyingi tasbeh", onClick = { nav.go("tasbeh") })
                SettingRow(Icons.Filled.VolunteerActivism, "Duolar va zikrlar", "Tong, kech va namozdan keyingi zikrlar", onClick = { nav.go("duas") })
                SettingRow(Icons.Filled.Image, "Rasm tayyorlash", "Namoz vaqtlari bilan chiroyli rasm yoki fon", onClick = { nav.go("wallpaper") })
                SettingRow(Icons.Filled.Checklist, "Qazo namozlar", "Qazo namozlarni hisoblab borish", onClick = { nav.go("qazo") })
                SettingRow(Icons.Filled.Search, "Qur'ondan qidirish", "Arabcha matn yoki o'zbekcha tarjima", onClick = { nav.go("search") })
                SettingRow(Icons.Filled.AutoAwesome, "Allohning 99 ismi", "Arabcha, o'qilishi va o'zbekcha ma'nosi", onClick = { nav.go("names") })
                SettingRow(Icons.Filled.WbSunny, "Ob-havo", "Hozirgi holat va 7 kunlik prognoz", onClick = { nav.go("weather") })
                SettingRow(Icons.Filled.SwapHoriz, "Sana konvertori", "Milodiy ⇄ Hijriy", onClick = { nav.go("converter") })
                SettingRow(Icons.AutoMirrored.Filled.MenuBook, "Qur'oni Karim", "Tajvidli va oddiy o'qish", onClick = {
                    nav.navigate("quran") { launchSingleTop = true; restoreState = true }
                })
                SettingRow(Icons.Filled.LocationOn, "Joylashuv", "Viloyat, tuman yoki GPS", onClick = { nav.go("location") })
                SettingRow(Icons.Filled.Settings, "Sozlamalar", "Mavzu, hisob usuli, eslatmalar", onClick = { nav.go("settings") })
            }
        }
    }
}


/** Tanlangan audio faylni telefonning "Notifications" papkasiga nusxalaydi — shunda tizim uni eslatma ovozi sifatida chala oladi. */
private fun importNotificationSound(ctx: android.content.Context, src: Uri): Uri? = runCatching {
    if (Build.VERSION.SDK_INT < 29) return null
    val resolver = ctx.contentResolver
    val mime = resolver.getType(src) ?: "audio/mpeg"
    val ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "mp3"
    val values = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Audio.Media.DISPLAY_NAME, "Azon_HijriyTaqvim_${System.currentTimeMillis()}.$ext")
        put(android.provider.MediaStore.Audio.Media.MIME_TYPE, mime)
        put(android.provider.MediaStore.Audio.Media.RELATIVE_PATH, "Notifications/")
        put(android.provider.MediaStore.Audio.Media.IS_NOTIFICATION, 1)
        put(android.provider.MediaStore.Audio.Media.TITLE, "Azon (Hijriy Taqvim)")
    }
    val dst = resolver.insert(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    resolver.openInputStream(src)!!.use { input -> resolver.openOutputStream(dst)!!.use { input.copyTo(it) } }
    dst
}.getOrNull()
