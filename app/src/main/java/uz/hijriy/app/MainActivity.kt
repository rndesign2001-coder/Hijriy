package uz.hijriy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.hijriy.app.ui.AppRoot
import uz.hijriy.app.ui.theme.HijriyTheme
import uz.hijriy.app.ui.theme.LocalExtra

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as HijriyApp
        setContent {
            val s by app.settings.state.collectAsStateWithLifecycle()
            HijriyTheme(s.themeMode, s.palette) {
                val dark = LocalExtra.current.dark
                val view = LocalView.current
                SideEffect {
                    val c = WindowCompat.getInsetsController(window, view)
                    c.isAppearanceLightStatusBars = !dark
                    c.isAppearanceLightNavigationBars = !dark
                }
                AppRoot(app)
            }
        }
    }
}
