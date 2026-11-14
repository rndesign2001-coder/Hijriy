package uz.hijriy.app.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.MainActivity
import uz.hijriy.app.R
import uz.hijriy.app.data.PlayState
import uz.hijriy.app.data.QuranAudio
import uz.hijriy.app.data.UzTranslit

/**
 * Qur'on tilovati uchun oldingi fon xizmati: bildirishnomada (va qulflangan ekranda) pleyer —
 * oldingi / ijro-pauza / keyingi / yopish tugmalari. Ilovaga qaytmasdan boshqarish mumkin.
 */
class QuranAudioService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var session: MediaSession
    private var foreground = false

    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { QuranAudio.pause() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        ensureChannel(this)
        session = MediaSession(this, "HijriyQuran").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { QuranAudio.resume() }
                override fun onPause() { QuranAudio.pause() }
                override fun onSkipToNext() { QuranAudio.next() }
                override fun onSkipToPrevious() { QuranAudio.prev() }
                override fun onStop() { QuranAudio.stop() }
            })
            isActive = true
        }
        runCatching { registerReceiver(noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) }
        scope.launch {
            QuranAudio.state.collectLatest { st ->
                if (!st.active) { shutdown(); return@collectLatest }
                render(st)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService() chaqirilganda 5 soniya ichida startForeground shart
        render(QuranAudio.state.value)
        when (intent?.action) {
            ACTION_TOGGLE -> QuranAudio.togglePause()
            ACTION_NEXT -> QuranAudio.next()
            ACTION_PREV -> QuranAudio.prev()
            ACTION_STOP -> QuranAudio.stop()
        }
        return START_NOT_STICKY
    }

    private fun shutdown() {
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        foreground = false
        stopSelf()
    }

    private fun render(st: PlayState) {
        val app = HijriyApp.instance
        val cyr = app.settings.value.script == 1
        fun t(x: String) = if (cyr) UzTranslit.toCyrillic(x) else x
        val suraName = app.quran.value?.suras?.getOrNull(st.sura - 1)?.uzName ?: "${st.sura}-sura"
        val title = when {
            !st.active -> t("Qur'oni Karim")
            st.ayah == 0 -> t("$suraName — Bismillah")
            else -> t("$suraName — ${st.ayah}-oyat")
        }
        val sub = when {
            st.error != null -> t(st.error)
            st.loading -> t("Yuklanmoqda…")
            else -> t(QuranAudio.reciterName)
        }
        session.setMetadata(
            MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, sub).build()
        )
        session.setPlaybackState(
            PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or
                        PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_STOP
                )
                .setState(
                    when {
                        st.loading -> PlaybackState.STATE_BUFFERING
                        st.playing -> PlaybackState.STATE_PLAYING
                        else -> PlaybackState.STATE_PAUSED
                    }, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f
                ).build()
        )
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        fun action(res: Int, label: String, act: String, req: Int) = Notification.Action.Builder(
            Icon.createWithResource(this, res), t(label),
            PendingIntent.getService(
                this, req, Intent(this, QuranAudioService::class.java).setAction(act),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        ).build()
        val playing = st.playing || st.loading
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(sub)
            .setContentIntent(open)
            .setDeleteIntent(
                PendingIntent.getService(
                    this, 5, Intent(this, QuranAudioService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(playing)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .addAction(action(android.R.drawable.ic_media_previous, "Oldingi", ACTION_PREV, 1))
            .addAction(
                if (st.playing) action(android.R.drawable.ic_media_pause, "To'xtatish", ACTION_TOGGLE, 2)
                else action(android.R.drawable.ic_media_play, "Davom etish", ACTION_TOGGLE, 2)
            )
            .addAction(action(android.R.drawable.ic_media_next, "Keyingi", ACTION_NEXT, 3))
            .addAction(action(android.R.drawable.ic_menu_close_clear_cancel, "Yopish", ACTION_STOP, 4))
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .build()
        if (playing) {
            ServiceCompat.startForeground(this, ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            foreground = true
        } else {
            // pauzada bildirishnomani suzib o'chirib tashlash mumkin (u pleyerni yopadi)
            if (foreground) ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
            foreground = false
            getSystemService(NotificationManager::class.java).notify(ID, n)
        }
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        runCatching { unregisterReceiver(noisy) }
        runCatching { session.isActive = false; session.release() }
        runCatching { getSystemService(NotificationManager::class.java).cancel(ID) }
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "quran_audio"
        private const val ID = 5301
        const val ACTION_TOGGLE = "uz.hijriy.app.QURAN_TOGGLE"
        const val ACTION_NEXT = "uz.hijriy.app.QURAN_NEXT"
        const val ACTION_PREV = "uz.hijriy.app.QURAN_PREV"
        const val ACTION_STOP = "uz.hijriy.app.QURAN_STOP"
        @Volatile var running = false

        private fun ensureChannel(c: Context) {
            val nm = c.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL, "Qur'on tilovati", NotificationManager.IMPORTANCE_LOW).apply {
                        description = "Tilovat pleyeri: to'xtatish, keyingi oyat, yopish"
                        setShowBadge(false)
                    }
                )
            }
        }

        fun start(c: Context) {
            runCatching { c.startForegroundService(Intent(c, QuranAudioService::class.java)) }
        }
    }
}
