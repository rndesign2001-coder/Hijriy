package uz.hijriy.app.ui

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import uz.hijriy.app.HijriyApp
import java.io.File

/** Katta xarita: istalgan hududni surib, ko'rinib turgan joydagi barcha masjidlarni topish (OpenStreetMap). */
@Composable
fun MosqueMapScreen(app: HijriyApp, nav: NavHostController) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    mosqueCacheDir = ctx.cacheDir
    val map = remember {
        Configuration.getInstance().apply {
            userAgentValue = "HijriyTaqvim/1.0 (${ctx.packageName})"
            osmdroidBasePath = File(ctx.filesDir, "osmdroid")
            osmdroidTileCache = File(ctx.cacheDir, "osmdroid/tiles")
        }
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 3.0; maxZoomLevel = 19.0
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(s.lat, s.lon))
            isTilesScaledToDpi = true
        }
    }
    val mosqueIcon = remember {
        val px = (40 * ctx.resources.displayMetrics.density).toInt()
        val src = android.graphics.BitmapFactory.decodeResource(ctx.resources, HI.MOSQUE.res)
        BitmapDrawable(ctx.resources, android.graphics.Bitmap.createScaledBitmap(src, px, px, true))
    }

    fun search() {
        val z = map.zoomLevelDouble
        if (z < 9.0) { status = "Qidirish uchun xaritani biroz kattalashtiring"; return }
        val b = map.boundingBox
        loading = true; status = null
        scope.launch {
            runCatching { fetchMosquesBox(b.latSouth, b.lonWest, b.latNorth, b.lonEast, s.lat, s.lon) }
                .onSuccess { list ->
                    map.overlays.removeAll { it is Marker && it.id == "m" }
                    list.forEach { m ->
                        map.overlays.add(Marker(map).apply {
                            id = "m"; position = GeoPoint(m.lat, m.lon); title = m.name
                            snippet = if (m.km < 1) "${(m.km * 1000).toInt()} m" else "%.1f km".format(m.km)
                            icon = mosqueIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            setOnMarkerClickListener { mk, _ ->
                                if (mk.isInfoWindowShown) {
                                    val uri = Uri.parse("geo:${m.lat},${m.lon}?q=${m.lat},${m.lon}(${Uri.encode(m.name)})")
                                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                } else mk.showInfoWindow()
                                true
                            }
                        })
                    }
                    map.invalidate()
                    status = if (list.isEmpty()) "Bu hududda masjid topilmadi" else "${list.size} ta masjid topildi"
                }
                .onFailure { e -> status = (e as? MosqueFetchError)?.message ?: "Internetga ulanib bo'lmadi" }
            loading = false
        }
    }

    // O'zimning joyim
    LaunchedEffect(Unit) {
        map.overlays.add(Marker(map).apply {
            id = "me"; position = GeoPoint(s.lat, s.lon); title = s.locName
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        })
        kotlinx.coroutines.delay(600)
        search()
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) { Lifecycle.Event.ON_RESUME -> map.onResume(); Lifecycle.Event.ON_PAUSE -> map.onPause(); else -> Unit }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs); map.onDetach() }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView({ map }, Modifier.fillMaxSize())
        Surface(
            Modifier.statusBarsPadding().padding(12.dp).fillMaxWidth(), shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), shadowElevation = 6.dp
        ) {
            Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Orqaga") }
                Column(Modifier.weight(1f)) {
                    Text("Masjidlar xaritasi", style = MaterialTheme.typography.titleMedium)
                    Text(status ?: "Xaritani suring va qidiring", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = { map.controller.animateTo(GeoPoint(s.lat, s.lon), 14.0, 600L) }) { Icon(Icons.Filled.MyLocation, "Mening joyim") }
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { search() }, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else HIcon(HI.MOSQUE, MaterialTheme.colorScheme.onPrimary, size = 22.dp)
                HSpace(8.dp)
                Text(if (loading) "Qidirilmoqda…" else "Shu hududdan masjid qidirish")
            }
            Box(Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                androidx.compose.material3.Text("© OpenStreetMap hissadorlari", color = Color(0xFF333333), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

