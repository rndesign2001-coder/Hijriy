package uz.hijriy.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.data.District
import uz.hijriy.app.data.LocationHelper
import uz.hijriy.app.data.Regions
import uz.hijriy.app.notify.PrayerScheduler

@Composable
fun LocationScreen(app: HijriyApp, nav: NavHostController) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val regions = remember { Regions.all(ctx) }
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(s.locRegion) }
    var locating by remember { mutableStateOf(false) }
    var gpsMsg by remember { mutableStateOf<String?>(null) }

    var world by remember { mutableStateOf(s.country != "UZ") }
    var wq by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<List<uz.hijriy.app.data.City>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchErr by remember { mutableStateOf<String?>(null) }
    androidx.compose.runtime.LaunchedEffect(wq) {
        val q = wq.trim()
        if (q.length < 2) { found = emptyList(); searchErr = null; return@LaunchedEffect }
        kotlinx.coroutines.delay(450)
        searching = true
        runCatching { uz.hijriy.app.data.Geo.search(q, if (s.script == 1) "ru" else "en") }
            .onSuccess { found = it; searchErr = if (it.isEmpty()) "Hech narsa topilmadi" else null }
            .onFailure { searchErr = "Internetga ulanib bo'lmadi — quyidagi ro'yxatdan tanlang" }
        searching = false
    }

    fun chooseCity(c: uz.hijriy.app.data.City) {
        app.settings.update {
            val m = if (it.autoMethod) uz.hijriy.app.core.methodForCountry(c.cc) else it.method
            it.copy(
                locName = c.name, locRegion = listOf(c.admin, c.country).filter { x -> x.isNotBlank() }.distinct().joinToString(", "),
                lat = c.lat, lon = c.lon, fromGps = false, tz = if (c.cc == "UZ") "" else c.tz, country = c.cc.ifBlank { "UZ" },
                method = m, hanafi = if (it.autoMethod) c.cc !in setOf("SA", "AE", "QA", "KW", "BH", "OM", "YE", "EG", "MA", "DZ", "TN", "LY", "SD", "MY", "ID", "SG", "BN") else it.hanafi,
            )
        }
        app.settings.markLocChosen()
        PrayerScheduler.reschedule(app)
        app.refreshWeather(force = true)
        nav.popBackStack()
    }

    fun choose(d: District) {
        app.settings.update {
            it.copy(
                locName = d.name, locRegion = d.region, lat = d.lat, lon = d.lon, fromGps = false, tz = "", country = "UZ",
                method = if (it.autoMethod) uz.hijriy.app.core.CalcMethod.UZBEKISTAN else it.method, hanafi = if (it.autoMethod) true else it.hanafi,
            )
        }
        app.settings.markLocChosen()
        PrayerScheduler.reschedule(app)
        app.refreshWeather(force = true)
        nav.popBackStack()
    }

    fun runGps() {
        if (!uz.hijriy.app.data.LocationHelper.servicesOn(ctx)) {
            gpsMsg = "GPS o'chiq. Yoqing — tizim sozlamalari ochildi."
            runCatching { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            return
        }
        locating = true
        gpsMsg = null
        scope.launch {
            val r = locateAndApply(app, ctx)
            locating = false
            gpsMsg = r ?: "Joylashuv aniqlanmadi. GPS yoqilganini tekshiring yoki ro'yxatdan tanlang."
        }
    }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.any { it }) runGps() else gpsMsg = "Joylashuvga ruxsat berilmadi"
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Joylashuv", s.locName, onBack = { nav.popBackStack() })
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            item {
                SectionCard(Modifier.fillMaxWidth()) {
                    Text("GPS orqali aniqlash", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Eng aniq namoz vaqti uchun turgan joyingiz koordinatasi olinadi.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    VSpace(10.dp)
                    Button(onClick = {
                        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (granted) runGps()
                        else perm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }, enabled = !locating) {
                        if (locating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Icon(Icons.Filled.MyLocation, null)
                        HSpace(8.dp)
                        Text(if (locating) "Aniqlanmoqda…" else "Joylashuvni aniqlash")
                    }
                    gpsMsg?.let {
                        VSpace(6.dp)
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                VSpace(12.dp)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(4.dp),
                ) {
                    listOf(false to "O'zbekiston", true to "Dunyo shaharlari").forEach { (w, label) ->
                        val sel = world == w
                        Box(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { world = w }.padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(label, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelLarge) }
                    }
                }
                VSpace(12.dp)
            }
            if (world) {
                item {
                    OutlinedTextField(
                        value = wq, onValueChange = { wq = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Shahar nomi (masalan: Istanbul, Seoul)") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = {
                            if (searching) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else if (wq.isNotEmpty()) IconButton(onClick = { wq = "" }) { Icon(Icons.Filled.Close, null) }
                        },
                        singleLine = true, shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        )
                    )
                    searchErr?.let { VSpace(6.dp); Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    VSpace(6.dp)
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                            app.settings.update { it.copy(autoMethod = !it.autoMethod) }
                        }.padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Usulni davlatga qarab tanlash", style = MaterialTheme.typography.titleSmall)
                            Text("Hozir: ${s.method.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        androidx.compose.material3.Switch(checked = s.autoMethod, onCheckedChange = { v -> app.settings.update { it.copy(autoMethod = v) } })
                    }
                    VSpace(4.dp)
                }
                val list = if (wq.trim().length >= 2) found else uz.hijriy.app.data.Geo.popular
                if (wq.trim().length < 2) item { SectionTitle("Mashhur shaharlar") }
                items(list, key = { "${it.name}/${it.lat}/${it.lon}" }) { c ->
                    CityRow(c, s.locName == c.name && kotlin.math.abs(s.lat - c.lat) < 0.01) { chooseCity(c) }
                }
                item {
                    VSpace(10.dp)
                    Text(
                        "Shaharlar: Open-Meteo Geocoding (GeoNames, CC BY 4.0). Namoz vaqtlari qurilmada astronomik hisoblanadi.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@LazyColumn
            }
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Tuman yoki shahar nomi") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, null) } },
                    singleLine = true, shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    )
                )
                VSpace(8.dp)
            }
            val k = query.trim().lowercase().replace("'", "").replace("‘", "").replace("’", "")
            if (k.isNotEmpty()) {
                val found = regions.flatMap { it.districts }.filter {
                    it.name.lowercase().replace("'", "").contains(k) || it.cyr.lowercase().contains(k)
                }
                items(found) { d -> DistrictRow(d, s.locName == d.name && !s.fromGps, showRegion = true) { choose(d) } }
            } else {
                regions.forEach { r ->
                    item(key = r.name) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { expanded = if (expanded == r.name) "" else r.name }
                                .padding(horizontal = 8.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(r.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${r.districts.size} ta tuman/shahar", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(if (expanded == r.name) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                        }
                    }
                    if (expanded == r.name) {
                        items(r.districts, key = { r.name + "/" + it.name }) { d ->
                            DistrictRow(d, s.locName == d.name && !s.fromGps, showRegion = false) { choose(d) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityRow(c: uz.hijriy.app.data.City, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { androidx.compose.material3.Text(c.cc.ifBlank { "?" }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer) }
        HSpace(12.dp)
        Column(Modifier.weight(1f)) {
            Text(c.name, style = MaterialTheme.typography.bodyLarge)
            Text(listOf(c.admin, c.country).filter { it.isNotBlank() }.distinct().joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        androidx.compose.material3.Text(
            uz.hijriy.app.core.methodForCountry(c.cc).title.substringBefore(" ("), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary, maxLines = 1
        )
        if (selected) { HSpace(6.dp); Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun DistrictRow(d: District, selected: Boolean, showRegion: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(d.name, style = MaterialTheme.typography.bodyLarge)
            if (showRegion) Text(d.region, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}


/** GPS orqali joylashuvni aniqlab sozlamalarga yozadi. Natija matni yoki null (aniqlanmadi). */
suspend fun locateAndApply(app: uz.hijriy.app.HijriyApp, ctx: android.content.Context): String? {
    val loc = LocationHelper.locate(ctx) ?: return null
    val near = Regions.nearest(ctx, loc.latitude, loc.longitude)
    val inUz = near != null && near.second < 60
    var name = near?.let { (d, km) -> if (km < 40) d.name else "Mening joylashuvim" } ?: "Mening joylashuvim"
    var region = near?.let { (d, km) -> if (km < 40) d.region else "" } ?: ""
    var cc = if (inUz) "UZ" else ""
    if (!inUz) {
        // Davlat va shahar nomi — qurilmaning Geocoder xizmati (bo'lmasa vaqt mintaqasidan)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                android.location.Geocoder(ctx).getFromLocation(loc.latitude, loc.longitude, 1)?.firstOrNull()
            }.getOrNull()
        }?.let { a ->
            cc = a.countryCode ?: ""
            name = a.locality ?: a.subAdminArea ?: a.adminArea ?: name
            region = listOfNotNull(a.adminArea, a.countryName).distinct().joinToString(", ")
        }
    }
    app.settings.update {
        val m = if (it.autoMethod && cc.isNotBlank()) uz.hijriy.app.core.methodForCountry(cc) else it.method
        it.copy(
            locName = name, locRegion = region, lat = loc.latitude, lon = loc.longitude, fromGps = true,
            tz = if (inUz) "" else java.time.ZoneId.systemDefault().id, country = cc.ifBlank { it.country }, method = m,
        )
    }
    app.settings.markLocChosen()
    PrayerScheduler.reschedule(app)
    app.refreshWeather(force = true)
    return "Aniqlandi: $name (${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)})"
}
