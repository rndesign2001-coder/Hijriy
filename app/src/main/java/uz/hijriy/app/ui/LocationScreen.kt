package uz.hijriy.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
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

    fun choose(d: District) {
        app.settings.update { it.copy(locName = d.name, locRegion = d.region, lat = d.lat, lon = d.lon, fromGps = false) }
        PrayerScheduler.reschedule(app)
        app.refreshWeather(force = true)
        nav.popBackStack()
    }

    fun runGps() {
        locating = true
        gpsMsg = null
        scope.launch {
            val loc = LocationHelper.locate(ctx)
            locating = false
            if (loc == null) {
                gpsMsg = "Joylashuv aniqlanmadi. GPS yoqilganini tekshiring yoki ro'yxatdan tanlang."
            } else {
                val near = Regions.nearest(ctx, loc.latitude, loc.longitude)
                val name = near?.let { (d, km) -> if (km < 40) d.name else "Mening joylashuvim" } ?: "Mening joylashuvim"
                val region = near?.let { (d, km) -> if (km < 40) d.region else "" } ?: ""
                app.settings.update {
                    it.copy(locName = name, locRegion = region, lat = loc.latitude, lon = loc.longitude, fromGps = true)
                }
                PrayerScheduler.reschedule(app)
                app.refreshWeather(force = true)
                gpsMsg = "Aniqlandi: $name (${"%.4f".format(loc.latitude)}, ${"%.4f".format(loc.longitude)})"
            }
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
