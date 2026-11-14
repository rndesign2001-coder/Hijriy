package uz.hijriy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.hijriy.app.ui.AppRoot
import uz.hijriy.app.ui.theme.HijriyTheme
import uz.hijriy.app.ui.theme.LocalExtra

class MainActivity : ComponentActivity() {
    private var updateManager: AppUpdateManager? = null
    private val updateReady = mutableStateOf(false)
    private val notifLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (!ok) android.widget.Toast.makeText(
            this, "Bildirishnoma ruxsati yo'q — pleyer bildirishnomada ko'rinmaydi. Telefon sozlamalari → Ilovalar → Hijriy → Bildirishnomalar.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    /** Tilovat boshlanganda bildirishnomalar yoqilganini tekshiradi; yoqilmagan bo'lsa ruxsat so'raydi. */
    private fun ensureNotifications() {
        if (androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        if (android.os.Build.VERSION.SDK_INT >= 33) notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        else android.widget.Toast.makeText(
            this, "Bildirishnomalar o'chirilgan — pleyer bildirishnomada ko'rinmaydi. Telefon sozlamalaridan yoqing.", android.widget.Toast.LENGTH_LONG
        ).show()
    }

    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    private val installListener = InstallStateUpdatedListener { st ->
        if (st.installStatus() == InstallStatus.DOWNLOADED) updateReady.value = true
    }

    /** Play Market'da yangi versiya chiqqanini tekshiradi va yangilashni taklif qiladi. */
    private fun checkForUpdate() {
        try {
            val um = updateManager ?: AppUpdateManagerFactory.create(this).also {
                updateManager = it
                it.registerListener(installListener)
            }
            um.appUpdateInfo.addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) { updateReady.value = true; return@addOnSuccessListener }
                val avail = info.updateAvailability()
                if (avail == UpdateAvailability.UPDATE_AVAILABLE ||
                    avail == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    val type = when {
                        info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                        info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                        else -> return@addOnSuccessListener
                    }
                    try { um.startUpdateFlowForResult(info, updateLauncher, AppUpdateOptions.newBuilder(type).build()) } catch (_: Exception) {}
                }
            }
        } catch (_: Throwable) { /* Play xizmatlari yo'q — jim */ }
    }

    override fun onResume() {
        uz.hijriy.app.data.QuranAudio.beforePlay = { ensureNotifications() }
        super.onResume()
        checkForUpdate()
    }

    override fun onDestroy() {
        uz.hijriy.app.data.QuranAudio.beforePlay = null
        try { updateManager?.unregisterListener(installListener) } catch (_: Throwable) {}
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as HijriyApp
        intent?.getStringExtra(EXTRA_ROUTE)?.let { app.pendingRoute.value = it }
        setContent {
            val s by app.settings.state.collectAsStateWithLifecycle()
            HijriyTheme(s.themeMode, s.palette, s.script == 1, s.readingScript == 1) {
                val dark = LocalExtra.current.dark
                val view = LocalView.current
                SideEffect {
                    val c = WindowCompat.getInsetsController(window, view)
                    c.isAppearanceLightStatusBars = !dark
                    c.isAppearanceLightNavigationBars = !dark
                }
                AppRoot(app)
                val ready by updateReady
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    AnimatedVisibility(
                        visible = ready,
                        enter = slideInVertically { it }, exit = slideOutVertically { it }
                    ) {
                        Surface(
                            Modifier.navigationBarsPadding().padding(16.dp, 16.dp, 16.dp, 96.dp).fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.inverseSurface, shadowElevation = 8.dp
                        ) {
                            Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (s.script == 1) "Янги версия юкланди" else "Yangi versiya yuklandi",
                                    color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.weight(1f))
                                TextButton(onClick = { updateManager?.completeUpdate() }) {
                                    Text(if (s.script == 1) "Янгилаш" else "Yangilash", color = MaterialTheme.colorScheme.inversePrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_ROUTE)?.let { (application as HijriyApp).pendingRoute.value = it }
    }

    companion object {
        const val EXTRA_ROUTE = "open_route"
    }
}
