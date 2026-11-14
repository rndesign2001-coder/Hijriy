package uz.hijriy.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.R
import uz.hijriy.app.core.Hijri
import uz.hijriy.app.core.IslamicDays
import uz.hijriy.app.core.Prayer
import uz.hijriy.app.core.Uz
import uz.hijriy.app.core.fmtMin
import uz.hijriy.app.data.PlayState
import uz.hijriy.app.data.QuranAudio
import uz.hijriy.app.data.SettingsStore
import uz.hijriy.app.notify.PrayerScheduler
import uz.hijriy.app.ui.tr
import java.time.LocalDate
import java.time.LocalDateTime

private fun AppWidgetManager.idsOf(c: Context, cls: Class<*>) = getAppWidgetIds(ComponentName(c, cls))

/** Barcha vidjetlarni yangilaydi (namoz vaqti o'tganda, sozlamalar o'zgarganda). */
object Widgets {
    fun updateAll(c: Context) {
        runCatching { PrayerWidget.updateAll(c) }
        runCatching { NextPrayerWidget.updateAll(c) }
        runCatching { HijriWidget.updateAll(c) }
        runCatching { QuranWidget.update(c, QuranAudio.state.value) }
    }
}

// ------------------------------ Keyingi namoz (2×2) ------------------------------

class NextPrayerWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, build(context, PrayerWidget.sizeOf(manager, id)))
        PrayerScheduler.reschedule(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: android.os.Bundle?) {
        manager.updateAppWidget(id, build(context, PrayerWidget.sizeOf(manager, id)))
    }

    companion object {
        private val prayers = listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)

        fun updateAll(c: Context) {
            val m = AppWidgetManager.getInstance(c) ?: return
            for (id in m.idsOf(c, NextPrayerWidget::class.java)) m.updateAppWidget(id, build(c, PrayerWidget.sizeOf(m, id)))
        }

        fun build(c: Context, size: Pair<Int, Int>): RemoteViews {
            val s = SettingsStore.read(c)
            val zone = s.zone
            val now = LocalDateTime.now(zone)
            val today = now.toLocalDate()
            val t = s.times(today)
            val minuteNow = now.hour * 60 + now.minute
            val cyr = s.script == 1
            var idx = prayers.indexOfFirst { t[it] > minuteNow }
            val date: LocalDate
            val minute: Int
            if (idx < 0) { idx = 0; date = today.plusDays(1); minute = s.times(date)[Prayer.FAJR] } else { date = today; minute = t[prayers[idx]] }
            val p = prayers[idx]
            val v = RemoteViews(c.packageName, R.layout.widget_next)
            runCatching { v.setImageViewBitmap(R.id.art, WidgetArt.overlay(c, size.first, size.second, night = false)) }
            v.setTextViewText(R.id.label, tr("Keyingi namoz", cyr))
            v.setImageViewResource(R.id.icon, uz.hijriy.app.ui.prayerHI.getValue(p).res)
            v.setTextViewText(R.id.name, tr(p.uz, cyr))
            v.setTextViewText(R.id.time, fmtMin(minute))
            v.setTextViewText(R.id.location, tr(s.locName, cyr))
            val at = date.atStartOfDay(zone).plusMinutes(minute.toLong()).toInstant().toEpochMilli()
            v.setChronometer(R.id.countdown, SystemClock.elapsedRealtime() + (at - System.currentTimeMillis()), null, true)
            v.setChronometerCountDown(R.id.countdown, true)
            v.setOnClickPendingIntent(R.id.root, PrayerScheduler.openApp(c, "prayer", 31))
            return v
        }
    }
}

// ------------------------------ Hijriy sana (2×2) ------------------------------

class HijriWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, build(context, PrayerWidget.sizeOf(manager, id)))
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, newOptions: android.os.Bundle?) {
        manager.updateAppWidget(id, build(context, PrayerWidget.sizeOf(manager, id)))
    }

    companion object {
        fun updateAll(c: Context) {
            val m = AppWidgetManager.getInstance(c) ?: return
            for (id in m.idsOf(c, HijriWidget::class.java)) m.updateAppWidget(id, build(c, PrayerWidget.sizeOf(m, id)))
        }

        fun build(c: Context, size: Pair<Int, Int>): RemoteViews {
            val s = SettingsStore.read(c)
            val today = LocalDate.now(s.zone)
            val h = Hijri.fromGregorian(today, s.hijriAdjust)
            val cyr = s.script == 1
            val v = RemoteViews(c.packageName, R.layout.widget_hijri)
            runCatching { v.setImageViewBitmap(R.id.art, WidgetArt.overlay(c, size.first, size.second, night = true)) }
            v.setTextViewText(R.id.weekday, tr(Uz.weekday(today.dayOfWeek), cyr))
            v.setTextViewText(R.id.day, h.day.toString())
            v.setTextViewText(R.id.month, tr("${h.monthName} ${h.year}", cyr))
            v.setTextViewText(R.id.arabic, "${Uz.arabicNumber(h.day)} ${h.monthNameAr} ${Uz.arabicNumber(h.year)}")
            v.setTextViewText(R.id.greg, tr(Uz.gregorian(today), cyr))
            val special = IslamicDays.find(h)?.name
            v.setViewVisibility(R.id.special, if (special != null) View.VISIBLE else View.GONE)
            if (special != null) v.setTextViewText(R.id.special, "✦ " + tr(special, cyr))
            v.setOnClickPendingIntent(R.id.root, PrayerScheduler.openApp(c, "calendar", 32))
            return v
        }
    }
}

// ------------------------------ Qur'on pleyeri (4×1) ------------------------------

/** Pleyer ko'rinishini to'ldiradi — bildirishnoma ham, vidjet ham shu bir xil dizayndan foydalanadi. */
object PlayerViews {
    data class Info(val title: String, val sub: String, val count: String, val progress: Int)

    fun info(c: Context, st: PlayState): Info {
        val app = HijriyApp.instance
        val s = app.settings.value
        val cyr = s.script == 1
        fun t(x: String) = tr(x, cyr)
        val q = app.quran.value
        if (!st.active) {
            val last = q?.suras?.getOrNull(s.lastSura - 1)
            return Info(
                t("Qur'oni Karim"),
                t(if (last != null) "Davom etish: ${last.uzName}, ${s.lastAyah}-oyat" else "Tilovatni tinglang"),
                "", 0
            )
        }
        val sura = q?.suras?.getOrNull(st.sura - 1)
        val name = sura?.uzName ?: "${st.sura}-sura"
        val count = sura?.count ?: 0
        return Info(
            title = t(if (st.ayah == 0) "$name — Bismillah" else "$name — ${st.ayah}-oyat"),
            sub = t(
                when {
                    st.error != null -> st.error
                    st.loading -> "Yuklanmoqda…"
                    else -> QuranAudio.reciterName
                }
            ),
            count = if (count > 0) t("${st.sura}-sura · ${st.ayah} / $count oyat") else "",
            progress = if (count > 0) (st.ayah * 100 / count).coerceIn(0, 100) else 0,
        )
    }

    fun fill(v: RemoteViews, i: Info, st: PlayState, hasProgress: Boolean, hasCount: Boolean) {
        v.setTextViewText(R.id.pl_title, i.title)
        v.setTextViewText(R.id.pl_sub, i.sub)
        if (hasCount) {
            v.setTextViewText(R.id.pl_count, i.count)
            v.setViewVisibility(R.id.pl_count, if (i.count.isEmpty()) View.GONE else View.VISIBLE)
        }
        if (hasProgress) v.setProgressBar(R.id.pl_progress, 100, i.progress, st.loading)
        v.setImageViewResource(R.id.pl_play, if (st.playing && !st.loading) R.drawable.ic_pl_pause else R.drawable.ic_pl_play)
    }
}

class QuranWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        update(context, QuranAudio.state.value)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE, ACTION_NEXT, ACTION_PREV -> {
                val st = QuranAudio.state.value
                if (st.active) when (intent.action) {
                    ACTION_TOGGLE -> QuranAudio.togglePause()
                    ACTION_NEXT -> QuranAudio.next()
                    ACTION_PREV -> QuranAudio.prev()
                } else update(context, st)
            }
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "uz.hijriy.app.WIDGET_QURAN_TOGGLE"
        const val ACTION_NEXT = "uz.hijriy.app.WIDGET_QURAN_NEXT"
        const val ACTION_PREV = "uz.hijriy.app.WIDGET_QURAN_PREV"

        private fun bc(c: Context, action: String, req: Int): PendingIntent = PendingIntent.getBroadcast(
            c, req, Intent(c, QuranWidget::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        fun update(c: Context, st: PlayState) {
            val m = AppWidgetManager.getInstance(c) ?: return
            val ids = m.idsOf(c, QuranWidget::class.java)
            if (ids.isEmpty()) return
            val v = RemoteViews(c.packageName, R.layout.widget_quran)
            PlayerViews.fill(v, PlayerViews.info(c, st), st, hasProgress = true, hasCount = false)
            val s = SettingsStore.read(c)
            if (st.active) {
                v.setOnClickPendingIntent(R.id.pl_play, bc(c, ACTION_TOGGLE, 41))
                v.setOnClickPendingIntent(R.id.pl_next, bc(c, ACTION_NEXT, 42))
                v.setOnClickPendingIntent(R.id.pl_prev, bc(c, ACTION_PREV, 43))
                v.setOnClickPendingIntent(R.id.pl_root, PrayerScheduler.openApp(c, "reader/${st.sura}?ayah=${st.ayah.coerceAtLeast(1)}", 44))
            } else {
                // Tilovat yo'q: tugmalar ilovani oxirgi o'qilgan surada ochadi
                val route = if (s.lastSura > 0) "reader/${s.lastSura}?ayah=${s.lastAyah}" else "quran"
                val open = PrayerScheduler.openApp(c, route, 44)
                v.setOnClickPendingIntent(R.id.pl_play, open)
                v.setOnClickPendingIntent(R.id.pl_next, open)
                v.setOnClickPendingIntent(R.id.pl_prev, open)
                v.setOnClickPendingIntent(R.id.pl_root, open)
            }
            for (id in ids) m.updateAppWidget(id, v)
        }
    }
}
