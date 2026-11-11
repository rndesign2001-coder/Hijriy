package uz.hijriy.app.widget

import uz.hijriy.app.ui.tr
import uz.hijriy.app.ui.res

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

/** Bosh ekran vidjeti: joy, hijriy sana, keyingi namoz va teskari sanoq, 5 vaqt namoz. */
class PrayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, build(context, sizeOf(manager, id)))
        PrayerScheduler.reschedule(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: android.os.Bundle?) {
        manager.updateAppWidget(id, build(context, sizeOf(manager, id)))
    }

    companion object {
        private val prayers = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
        private val cells = listOf(R.id.cell0, R.id.cell1, R.id.cell2, R.id.cell3, R.id.cell4)
        private val times = listOf(R.id.time0, R.id.time1, R.id.time2, R.id.time3, R.id.time4)
        private val names = listOf(R.id.name0, R.id.name1, R.id.name2, R.id.name3, R.id.name4)
        private val icons = listOf(R.id.icon0, R.id.icon1, R.id.icon2, R.id.icon3, R.id.icon4)

        /** Vidjet o'lchami (dp): kenglik — portret rejimdagi eng kichik, balandlik — eng katta qiymat. */
        fun sizeOf(m: AppWidgetManager, id: Int): Pair<Int, Int> {
            val o = m.getAppWidgetOptions(id)
            val w = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0).takeIf { it > 0 } ?: 320
            val h = o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0).takeIf { it > 0 } ?: 170
            return w to h
        }

        fun updateAll(context: Context) {
            val m = AppWidgetManager.getInstance(context) ?: return
            val ids = m.getAppWidgetIds(ComponentName(context, PrayerWidget::class.java))
            for (id in ids) m.updateAppWidget(id, build(context, sizeOf(m, id)))
        }

        fun build(context: Context, sizeDp: Pair<Int, Int> = 320 to 170): RemoteViews {
            val s = SettingsStore.read(context)
            val zone = SettingsStore.read(context).zone
            val now = LocalDateTime.now(zone)
            val today = now.toLocalDate()
            val t = s.times(today)
            val minuteNow = now.hour * 60 + now.minute
            val v = RemoteViews(context.packageName, R.layout.widget_prayer)
            val h = Hijri.fromGregorian(today, s.hijriAdjust)
            val cyr = s.script == 1
            v.setTextViewText(R.id.location, tr(s.locName, cyr))
            v.setTextViewText(R.id.hijri, tr("${h.day} ${h.monthName} ${h.year}", cyr))
            var nextIdx = prayers.indexOfFirst { t[it] > minuteNow }
            val nextDate: LocalDate
            val nextMinute: Int
            if (nextIdx < 0) {
                nextIdx = 0; nextDate = today.plusDays(1); nextMinute = s.times(nextDate)[Prayer.FAJR]
            } else { nextDate = today; nextMinute = t[prayers[nextIdx]] }
            val currentIdx = prayers.indexOfLast { t[it] <= minuteNow }
            // Naqshli fon, quyosh yo'li va ikonkalar
            val sunrise = t.calculated.getValue(Prayer.SUNRISE)
            val sunset = Math.round(t.sunsetExact * 60).toInt()
            runCatching { v.setImageViewBitmap(R.id.art, WidgetArt.background(context, sizeDp.first, sizeDp.second, sunrise, sunset, minuteNow + now.second / 60f)) }
            v.setTextViewText(R.id.sunrise, tr("↑ ${fmtMin(sunrise)}  chiqishi", cyr))
            v.setTextViewText(R.id.sunset, tr("botishi  ${fmtMin(sunset)} ↓", cyr))
            val day = sunset - sunrise
            v.setTextViewText(R.id.daylen, tr("kun ${day / 60} s ${day % 60} d", cyr))
            prayers.forEachIndexed { i, p ->
                v.setImageViewResource(icons[i], uz.hijriy.app.ui.prayerHI.getValue(p).res)
                v.setTextViewText(times[i], fmtMin(t[p]))
                v.setTextViewText(names[i], tr(p.uz, cyr))
                val active = i == currentIdx
                v.setInt(cells[i], "setBackgroundResource", if (active) R.drawable.widget_cell_active else R.drawable.widget_cell)
                v.setTextColor(times[i], if (active) 0xFF0B3D2E.toInt() else 0xFFFFFFFF.toInt())
                v.setTextColor(names[i], if (active) 0xFF0B3D2E.toInt() else 0xE6FFFFFF.toInt())
            }
            v.setTextViewText(R.id.next, tr("Keyingi: ${prayers[nextIdx].uz} ${fmtMin(nextMinute)}", cyr))
            val nextAt = nextDate.atStartOfDay(zone).plusMinutes(nextMinute.toLong()).toInstant().toEpochMilli()
            val base = SystemClock.elapsedRealtime() + (nextAt - System.currentTimeMillis())
            v.setChronometer(R.id.countdown, base, null, true)
            v.setChronometerCountDown(R.id.countdown, true)
            v.setOnClickPendingIntent(R.id.root, PrayerScheduler.openApp(context, "prayer", 3))
            return v
        }
    }
}
