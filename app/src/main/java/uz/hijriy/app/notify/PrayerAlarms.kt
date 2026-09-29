package uz.hijriy.app.notify

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.MainActivity
import uz.hijriy.app.R
import uz.hijriy.app.core.Prayer
import uz.hijriy.app.core.fmtMin
import uz.hijriy.app.data.SettingsStore
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object PrayerScheduler {
    private const val REQ = 4201

    data class Next(val prayer: Prayer, val atMillis: Long, val minuteOfDay: Int)

    /** Keyingi eslatma vaqtini topadi (bugun yoki ertaga). */
    fun findNext(context: Context, nowMillis: Long = System.currentTimeMillis()): Next? {
        val s = SettingsStore.read(context)
        if (!s.notifyEnabled || s.notifyPrayers.isEmpty()) return null
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        for (dayOffset in 0..2) {
            val date = today.plusDays(dayOffset.toLong())
            val t = s.times(date)
            for (p in Prayer.entries) {
                if (!p.notifiable || p !in s.notifyPrayers) continue
                val minute = t[p] - s.notifyBefore
                val at = date.atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant().toEpochMilli()
                if (at > nowMillis + 1000) return Next(p, at, t[p])
            }
        }
        return null
    }

    fun reschedule(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(context, null, 0)
        am.cancel(pi)
        val next = findNext(context) ?: return
        val intent = pending(context, next.prayer, next.minuteOfDay)
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) am.canScheduleExactAlarms() else true
        try {
            if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.atMillis, intent)
        }
    }

    private fun pending(context: Context, prayer: Prayer?, minute: Int): PendingIntent {
        val i = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = "uz.hijriy.app.PRAYER_ALARM"
            if (prayer != null) {
                putExtra("prayer", prayer.name)
                putExtra("minute", minute)
            }
        }
        return PendingIntent.getBroadcast(
            context, REQ, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val p = intent.getStringExtra("prayer")?.let { runCatching { Prayer.valueOf(it) }.getOrNull() }
        val minute = intent.getIntExtra("minute", -1)
        if (p != null) show(context, p, minute)
        PrayerScheduler.reschedule(context)
    }

    private fun show(context: Context, p: Prayer, minute: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val s = SettingsStore.read(context)
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val now = LocalDateTime.now()
        val left = minute - (now.hour * 60 + now.minute)
        val title = if (s.notifyBefore > 0 && left > 0) "${p.uz} namoziga $left daqiqa qoldi" else "${p.uz} vaqti kirdi"
        val n = NotificationCompat.Builder(context, HijriyApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText("${p.uz}: ${fmtMin(minute)} • ${s.locName}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(1000 + p.ordinal, n)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerScheduler.reschedule(context)
    }
}
