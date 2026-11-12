package uz.hijriy.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import uz.hijriy.app.R
import uz.hijriy.app.ui.theme.TitleFont

/**
 * Makka va Madinadan jonli efir. Video ilova ichida saqlanmaydi va qayta tarqatilmaydi —
 * Saudiya Arabistoni teleradio boshqarmasining rasmiy YouTube kanallari ochiladi.
 */
private data class LiveChannel(val photo: Int, val title: String, val sub: String, val arabic: String, val url: String)

private val CHANNELS = listOf(
    LiveChannel(
        R.drawable.bg_haram_night, "Makka — Masjidul Harom", "Qur'on kanali • 24 soat jonli", "قناة القرآن الكريم",
        "https://www.youtube.com/@saudiqurantv/live"
    ),
    LiveChannel(
        R.drawable.bg_nabawi_night, "Madina — Masjidi Nabaviy", "Sunnat kanali • 24 soat jonli", "قناة السنة النبوية",
        "https://www.youtube.com/@saudisunnahtv/live"
    ),
)

@Composable
fun LiveScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    val pulse by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p"
    )
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Jonli efir", "Makka va Madina", onBack = { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CHANNELS.forEach { ch ->
                PhotoCard(
                    ch.photo,
                    Modifier.height(210.dp).clip(RoundedCornerShape(24.dp)).clickable {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ch.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    },
                    alignY = 0.2f
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFD32F2F)).padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(8.dp).alpha(pulse).clip(CircleShape).background(Color.White))
                            Spacer(Modifier.size(6.dp))
                            Text("JONLI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(ch.arabic, color = Color(0xFFE9C46A), fontSize = 16.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(ch.title, color = Color.White, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(ch.sub, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("▶  Ko'rish", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
            Text(
                "Efir Saudiya Arabistoni teleradio boshqarmasining rasmiy YouTube kanallaridan (Qur'on va Sunnat kanallari) ochiladi. " +
                    "Ilova videoni o'zida saqlamaydi. Ko'rish internet trafigini sarflaydi.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
