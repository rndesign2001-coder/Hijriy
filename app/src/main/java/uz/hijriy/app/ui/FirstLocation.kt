package uz.hijriy.app.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.data.LocationHelper
import uz.hijriy.app.R
import uz.hijriy.app.ui.theme.TitleFont

/** Yangi o'rnatilganda: namoz vaqtlari to'g'ri bo'lishi uchun joylashuvni so'raydi (GPS yoki ro'yxat). */
@Composable
fun FirstLocationScreen(app: HijriyApp, onPickFromList: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val cyr = app.settings.value.script == 1
    fun t(lat: String, cy: String) = if (cyr) cy else lat

    var needSettings by remember { mutableStateOf(false) }
    var needAppSettings by remember { mutableStateOf(false) }

    fun gps() {
        needSettings = false; needAppSettings = false
        if (!LocationHelper.servicesOn(ctx)) {
            // GPS o'chiq — tizimning joylashuv sozlamalarini darhol ochamiz
            msg = t("GPS o'chiq. Yoqing, so'ng qaytib keling — joylashuv avtomatik aniqlanadi.",
                "GPS ўчиқ. Ёқинг, сўнг қайтиб келинг — жойлашув автоматик аниқланади.")
            needSettings = true
            runCatching { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            return
        }
        busy = true; msg = null
        scope.launch {
            val r = locateAndApply(app, ctx)
            busy = false
            if (r == null) msg = t(
                "Joylashuv topilmadi. Ochiq joyda qayta urinib ko'ring yoki shaharni ro'yxatdan tanlang.",
                "Жойлашув топилмади. Очиқ жойда қайта уриниб кўринг ёки шаҳарни рўйхатдан танланг."
            )
        }
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.any { it }) gps() else {
            needAppSettings = true
            msg = t(
                "Joylashuvga ruxsat berilmadi. Ruxsatni sozlamalardan yoqing yoki shaharni ro'yxatdan tanlang.",
                "Жойлашувга рухсат берилмади. Рухсатни созламалардан ёқинг ёки шаҳарни рўйхатдан танланг."
            )
        }
    }
    fun openAppSettings() {
        runCatching {
            ctx.startActivity(android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                android.net.Uri.parse("package:" + ctx.packageName)
            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
    BackHandler { /* joy tanlanmaguncha yopilmaydi */ }

    Box(
        Modifier.fillMaxSize().background(Color(0xFF06201A))
            .clickable(remember { MutableInteractionSource() }, null) { }
    ) {
        PhotoBackdrop(R.drawable.bg_nabawi_sunset, Color(0xFF06201A), Modifier.fillMaxSize(), alignY = -0.2f, strength = 1.1f)
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Column(Modifier.widthIn(max = 520.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                HIcon(HI.LOCATION, Color.White, size = 72.dp)
                Spacer(Modifier.height(14.dp))
                Text(
                    t("Joylashuvingizni belgilang", "Жойлашувингизни белгиланг"),
                    color = Color.White, fontFamily = TitleFont, fontWeight = FontWeight.Bold, fontSize = 26.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    t(
                        "Namoz vaqtlari, qibla va ob-havo siz turgan joyga qarab hisoblanadi. Joylashuv faqat telefoningizda saqlanadi.",
                        "Намоз вақтлари, қибла ва об-ҳаво сиз турган жойга қараб ҳисобланади. Жойлашув фақат телефонингизда сақланади."
                    ),
                    color = Color.White.copy(alpha = 0.88f), fontSize = 15.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        perm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE9C46A), contentColor = Color(0xFF1B1405))
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color(0xFF1B1405), strokeWidth = 2.5.dp)
                    else Text(t("📍  GPS orqali aniqlash", "📍  GPS орқали аниқлаш"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onPickFromList, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) { Text(t("Shahar yoki tumanni tanlash", "Шаҳар ёки туманни танлаш"), fontSize = 16.sp) }
                msg?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = Color(0xFFFFD7A0), fontSize = 15.sp, textAlign = TextAlign.Center)
                }
                if (needAppSettings) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { openAppSettings() }, modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9C46A)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE9C46A))
                    ) { Text(t("Ruxsatni sozlamalarda yoqish", "Рухсатни созламаларда ёқиш")) }
                }
                if (needSettings) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { gps() }, modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9C46A)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE9C46A))
                    ) { Text(t("GPS yoqildi — qayta aniqlash", "GPS ёқилди — қайта аниқлаш")) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
