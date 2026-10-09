package uz.hijriy.app.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import uz.hijriy.app.MainActivity
import uz.hijriy.app.R
import uz.hijriy.app.core.Prayer
import uz.hijriy.app.core.fmtMin
import uz.hijriy.app.data.SettingsStore
import uz.hijriy.app.widget.PrayerWidget
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Bildirishnoma kanallari. Android kanal ovozini keyin o'zgartirishga ruxsat bermaydi — shuning uchun ovoz o'zgarsa yangi kanal ochiladi. */
object Channels {
    private const val PREFIX = "prayer_"

    fun ensure(context: Context, sound: String): String {
        val id = PREFIX + if (sound.isBlank()) "default" else Integer.toHexString(sound.hashCode())
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(id) == null) {
            nm.notificationChannels.filter { it.id.startsWith(PREFIX) || it.id == "prayer_times" }.forEach { nm.deleteNotificationChannel(it.id) }
            val uri = if (sound.isBlank()) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else Uri.parse(sound)
            nm.createNotificationChannel(
                NotificationChannel(id, "Namoz vaqtlari", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Namoz vaqti kirganda eslatma"
                    enableVibration(true)
                    setSound(uri, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                }
            )
        }
        return id
    }
}

object PrayerScheduler {
    private const val REQ = 4201
    private const val REQ_FRIDAY = 4202
    private const val REQ_WIDGET = 4203
    const val ACTION_PRAYER = "uz.hijriy.app.PRAYER_ALARM"
    const val ACTION_FRIDAY = "uz.hijriy.app.FRIDAY"
    const val ACTION_WIDGET = "uz.hijriy.app.WIDGET_TICK"

    data class Next(val prayer: Prayer, val atMillis: Long, val minuteOfDay: Int)

    /** Keyingi eslatma vaqtini topadi (bugun yoki ertaga). */
    fun findNext(context: Context, nowMillis: Long = System.currentTimeMillis(), ignoreSettings: Boolean = false): Next? {
        val s = SettingsStore.read(context)
        if (!ignoreSettings && (!s.notifyEnabled || s.notifyPrayers.isEmpty())) return null
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        for (dayOffset in 0..2) {
            val date = today.plusDays(dayOffset.toLong())
            val t = s.times(date)
            for (p in Prayer.entries) {
                if (!ignoreSettings && (!p.notifiable || p !in s.notifyPrayers)) continue
                val minute = t[p] - if (ignoreSettings) 0 else s.notifyBefore
                val at = date.atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant().toEpochMilli()
                if (at > nowMillis + 1000) return Next(p, at, t[p])
            }
        }
        return null
    }

    fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pending(context, ACTION_PRAYER, REQ, null, 0))
        findNext(context)?.let { set(am, it.atMillis, pending(context, ACTION_PRAYER, REQ, it.prayer, it.minuteOfDay), exact = true) }
        scheduleFriday(context, am)
        // Vidjet: keyingi namoz vaqtida yangilanadi
        findNext(context, ignoreSettings = true)?.let {
            set(am, it.atMillis + 1500, pending(context, ACTION_WIDGET, REQ_WIDGET, null, 0), exact = false)
        }
        runCatching { PrayerWidget.updateAll(context) }
    }

    private fun scheduleFriday(context: Context, am: AlarmManager) {
        val pi = pending(context, ACTION_FRIDAY, REQ_FRIDAY, null, 0)
        am.cancel(pi)
        if (!SettingsStore.read(context).fridayReminder) return
        val zone = ZoneId.systemDefault()
        var d = LocalDate.now(zone)
        val now = LocalDateTime.now(zone)
        while (d.dayOfWeek != DayOfWeek.FRIDAY || (d == now.toLocalDate() && now.toLocalTime().isAfter(java.time.LocalTime.of(8, 30)))) d = d.plusDays(1)
        val at = d.atTime(8, 30).atZone(zone).toInstant().toEpochMilli()
        set(am, at, pi, exact = false)
    }

    private fun set(am: AlarmManager, at: Long, pi: PendingIntent, exact: Boolean) {
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
        try {
            if (exact && canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun pending(context: Context, action: String, req: Int, prayer: Prayer?, minute: Int): PendingIntent {
        val i = Intent(context, PrayerAlarmReceiver::class.java).apply {
            this.action = action
            if (prayer != null) {
                putExtra("prayer", prayer.name)
                putExtra("minute", minute)
            }
        }
        return PendingIntent.getBroadcast(context, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Ilovani ma'lum sahifada ochuvchi intent (bildirishnoma va vidjet uchun). */
    fun openApp(context: Context, route: String?, req: Int = 0): PendingIntent {
        val i = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (route != null) i.putExtra(MainActivity.EXTRA_ROUTE, route)
        return PendingIntent.getActivity(context, req, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            PrayerScheduler.ACTION_FRIDAY -> showFriday(context)
            PrayerScheduler.ACTION_WIDGET -> Unit
            else -> {
                val p = intent.getStringExtra("prayer")?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
                val minute = intent.getIntExtra("minute", -1)
                if (p != null) show(context, p, minute)
            }
        }
        PrayerScheduler.reschedule(context)
    }

    private fun allowed(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun show(context: Context, p: Prayer, minute: Int) {
        if (!allowed(context)) return
        val s = SettingsStore.read(context)
        val now = LocalDateTime.now()
        val left = minute - (now.hour * 60 + now.minute)
        val title = if (s.notifyBefore > 0 && left > 0) "${p.uz} namoziga $left daqiqa qoldi" else "${p.uz} vaqti kirdi"
        val n = NotificationCompat.Builder(context, Channels.ensure(context, s.notifySound))
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(uz.hijriy.app.ui.tr(title, s.script == 1))
            .setContentText(uz.hijriy.app.ui.tr("${p.uz}: ${fmtMin(minute)} • ${s.locName}", s.script == 1))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(PrayerScheduler.openApp(context, "prayer", 1))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(1000 + p.ordinal, n)
    }

    private fun showFriday(context: Context) {
        if (!allowed(context) || !SettingsStore.read(context).fridayReminder) return
        val s = SettingsStore.read(context)
        val jumaTime = fmtMin(s.times(LocalDate.now())[Prayer.DHUHR])
        val n = NotificationCompat.Builder(context, Channels.ensure(context, s.notifySound))
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(uz.hijriy.app.ui.tr("Juma muborak! 🕌", s.script == 1))
            .setContentText(uz.hijriy.app.ui.tr("Juma namozi (peshin $jumaTime). Kahf surasini o'qishni unutmang.", s.script == 1))
            .setStyle(NotificationCompat.BigTextStyle().bigText(uz.hijriy.app.ui.tr(
                "Juma namozi (peshin vaqti $jumaTime). Juma kuni Kahf surasini o'qish, g'usl qilish va Rasulullohga ko'p salovot aytish sunnatdir. Kahf surasini ochish uchun bosing.", s.script == 1
            )))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(PrayerScheduler.openApp(context, "reader/18", 2))
            .build()
        context.getSystemService(NotificationManager::class.java).notify(2000, n)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerScheduler.reschedule(context)
    }
}
