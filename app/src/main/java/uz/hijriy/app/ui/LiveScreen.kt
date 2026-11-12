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
import androidx.compose.foundation.layout.aspectRatio
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
private data class LiveChannel(val photo: Int, val title: String, val sub: String, val arabic: String, val url: String, val channelId: String)

private val CHANNELS = listOf(
    LiveChannel(
        R.drawable.bg_haram_night, "Makka — Masjidul Harom", "Qur'on kanali • 24 soat jonli", "قناة القرآن الكريم",
        "https://www.youtube.com/@saudiqurantv/live", "UCos52azQNBgW63_9uDJoPDA"
    ),
    LiveChannel(
        R.drawable.bg_nabawi_night, "Madina — Masjidi Nabaviy", "Ar Rahman kanali • 24 soat jonli", "المسجد النبوي",
        "https://www.youtube.com/@arrahmanislamic/live", "UCWainKMJPyXikjekccFf3NA"
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
            CHANNELS.forEachIndexed { idx, ch ->
                PhotoCard(
                    ch.photo,
                    Modifier.height(210.dp).clip(RoundedCornerShape(24.dp)).clickable { nav.go("liveplay/$idx") },
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
                "Efir YouTube'ning rasmiy pleyeri orqali ilova ichida ko'rsatiladi (Makka — Saudiya Qur'on kanali, Madina — Ar Rahman kanali). " +
                    "Ilova videoni o'zida saqlamaydi. Ko'rish internet trafigini sarflaydi.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}


private fun openYoutube(ctx: android.content.Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun playerHtml(channelId: String) = """
<!DOCTYPE html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<style>html,body{margin:0;height:100%;background:#000;overflow:hidden}#p{position:absolute;inset:0;width:100%;height:100%;border:0}</style>
</head><body>
<iframe id="p" allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen
 src="https://www.youtube.com/embed/live_stream?channel=$channelId&enablejsapi=1&autoplay=1&playsinline=1&rel=0&origin=https%3A%2F%2Fuz.hijriy.app"></iframe>
<script src="https://www.youtube.com/iframe_api"></script>
<script>
function onYouTubeIframeAPIReady(){
  new YT.Player('p',{events:{
    onReady:function(e){ try{e.target.playVideo();}catch(x){} Live.onState(-2); },
    onStateChange:function(e){ Live.onState(e.data); },
    onError:function(e){ Live.onError(e.data); }
  }});
}
</script></body></html>
"""

/** YouTube'ning rasmiy IFrame pleyeri bilan ilova ichida jonli efir. */
@android.annotation.SuppressLint("SetJavaScriptEnabled")
@Composable
fun LivePlayerScreen(nav: NavHostController, index: Int) {
    val ch = CHANNELS.getOrElse(index) { CHANNELS[0] }
    val ctx = LocalContext.current
    val activity = ctx as? android.app.Activity
    var error by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    var playing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var fullView by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<android.view.View?>(null) }
    var fullCallback by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }
    val main = androidx.compose.runtime.remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    fun setFull(on: Boolean) {
        val a = activity ?: return
        val ctl = androidx.core.view.WindowCompat.getInsetsController(a.window, a.window.decorView)
        if (on) {
            a.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            ctl.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctl.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        } else {
            a.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            ctl.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    val web = androidx.compose.runtime.remember {
        android.webkit.WebView(ctx).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface fun onError(code: Int) { main.post { error = code } }
                @android.webkit.JavascriptInterface fun onState(st: Int) { main.post { if (st == 1) { playing = true; error = null } } }
            }, "Live")
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onShowCustomView(view: android.view.View, callback: CustomViewCallback) {
                    fullView = view; fullCallback = callback; setFull(true)
                }
                override fun onHideCustomView() {
                    fullView = null; fullCallback = null; setFull(false)
                }
            }
            webViewClient = object : android.webkit.WebViewClient() {
                override fun shouldOverrideUrlLoading(view: android.webkit.WebView, req: android.webkit.WebResourceRequest): Boolean {
                    // pleyerdagi "YouTube" logotipi va boshqa havolalar YouTube ilovasida ochiladi
                    if (req.isForMainFrame) { openYoutube(ctx, req.url.toString()); return true }
                    return false
                }
            }
            loadDataWithBaseURL("https://uz.hijriy.app/", playerHtml(ch.channelId), "text/html", "utf-8", null)
        }
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            fullCallback?.onCustomViewHidden()
            setFull(false)
            runCatching { web.loadUrl("about:blank"); web.destroy() }
        }
    }
    androidx.activity.compose.BackHandler(enabled = fullView != null) { fullCallback?.onCustomViewHidden(); fullView = null; setFull(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(ch.title, ch.sub, onBack = { nav.popBackStack() })
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
            ) {
                androidx.compose.ui.viewinterop.AndroidView({ web }, Modifier.fillMaxSize())
                if (!playing && error == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = Color.White)
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error?.let { code ->
                    Text(
                        when (code) {
                            101, 150, 152, 153 -> "Kanal efirni boshqa ilovalarda ko'rsatishga ruxsat bermagan. YouTube ilovasida ko'ring."
                            2, 5, 100 -> "Hozir efir topilmadi — birozdan keyin qayta urinib ko'ring yoki YouTube'da oching."
                            else -> "Efirni yuklab bo'lmadi (xato $code). Internetni tekshiring yoki YouTube'da oching."
                        },
                        color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.OutlinedButton(onClick = {
                        error = null; playing = false
                        web.loadDataWithBaseURL("https://uz.hijriy.app/", playerHtml(ch.channelId), "text/html", "utf-8", null)
                    }) { Text("Qayta yuklash") }
                    androidx.compose.material3.OutlinedButton(onClick = { openYoutube(ctx, ch.url) }) { Text("YouTube'da ochish") }
                }
                Text(ch.arabic, color = Color(0xFFC8962E), fontSize = 20.sp)
                Text(
                    "To'liq ekran uchun pleyerdagi ⛶ tugmasini bosing. Ovozni telefon tugmalari bilan boshqaring.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        fullView?.let { v ->
            androidx.compose.ui.viewinterop.AndroidView({ v }, Modifier.fillMaxSize().background(Color.Black))
        }
    }
}
