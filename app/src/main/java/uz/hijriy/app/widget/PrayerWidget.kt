package uz.hijriy.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.widget.RemoteViews
import uz.hijriy.app.R
import uz.hijriy.app.core.Hijri
import uz.hijriy.app.core.Prayer
import uz.hijriy.app.core.fmtMin
import uz.hijriy.app.data.SettingsStore
import uz.hijriy.app.notify.PrayerScheduler
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Bosh ekran vidjeti: joy, hijriy sana, keyingi namoz va teskari sanoq, 5 vaqt namoz. */
class PrayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, build(context))
        PrayerScheduler.reschedule(context)
    }

    companion object {
        private val prayers = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
        private val cells = listOf(R.id.cell0, R.id.cell1, R.id.cell2, R.id.cell3, R.id.cell4)
        private val times = listOf(R.id.time0, R.id.time1, R.id.time2, R.id.time3, R.id.time4)
        private val names = listOf(R.id.name0, R.id.name1, R.id.name2, R.id.name3, R.id.name4)

        fun updateAll(context: Context) {
            val m = AppWidgetManager.getInstance(context) ?: return
            val ids = m.getAppWidgetIds(ComponentName(context, PrayerWidget::class.java))
            if (ids.isEmpty()) return
            val v = build(context)
            for (id in ids) m.updateAppWidget(id, v)
        }

        fun build(context: Context): RemoteViews {
            val s = SettingsStore.read(context)
            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.now(zone)
            val today = now.toLocalDate()
            val t = s.times(today)
            val minuteNow = now.hour * 60 + now.minute
            val v = RemoteViews(context.packageName, R.layout.widget_prayer)
            val h = Hijri.fromGregorian(today, s.hijriAdjust)
            v.setTextViewText(R.id.location, s.locName)
            v.setTextViewText(R.id.hijri, "${h.day} ${h.monthName} ${h.year}")
            var nextIdx = prayers.indexOfFirst { t[it] > minuteNow }
            val nextDate: LocalDate
            val nextMinute: Int
            if (nextIdx < 0) {
                nextIdx = 0; nextDate = today.plusDays(1); nextMinute = s.times(nextDate)[Prayer.FAJR]
            } else { nextDate = today; nextMinute = t[prayers[nextIdx]] }
            val currentIdx = prayers.indexOfLast { t[it] <= minuteNow }
            prayers.forEachIndexed { i, p ->
                v.setTextViewText(times[i], fmtMin(t[p]))
                val active = i == currentIdx
                v.setInt(cells[i], "setBackgroundResource", if (active) R.drawable.widget_cell_active else R.drawable.widget_cell)
                v.setTextColor(times[i], if (active) 0xFF0B3D2E.toInt() else 0xFFFFFFFF.toInt())
                v.setTextColor(names[i], if (active) 0xFF0B3D2E.toInt() else 0xE6FFFFFF.toInt())
            }
            v.setTextViewText(R.id.next, "Keyingi: ${prayers[nextIdx].uz} ${fmtMin(nextMinute)}")
            val nextAt = nextDate.atStartOfDay(zone).plusMinutes(nextMinute.toLong()).toInstant().toEpochMilli()
            val base = SystemClock.elapsedRealtime() + (nextAt - System.currentTimeMillis())
            v.setChronometer(R.id.countdown, base, null, true)
            v.setChronometerCountDown(R.id.countdown, true)
            v.setOnClickPendingIntent(R.id.root, PrayerScheduler.openApp(context, "prayer", 3))
            return v
        }
    }
}
