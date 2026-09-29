package uz.hijriy.app.ui

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.core.Qibla
import uz.hijriy.app.ui.theme.LocalExtra
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun QiblaScreen(app: HijriyApp, nav: NavHostController) {
    val s by app.settings.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val qibla = remember(s.lat, s.lon) { Qibla.bearing(s.lat, s.lon) }
    val distance = remember(s.lat, s.lon) { Qibla.distanceKm(s.lat, s.lon) }
    var heading by remember { mutableFloatStateOf(0f) }
    var accuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var hasSensor by remember { mutableStateOf(true) }
    val declination = remember(s.lat, s.lon) {
        GeomagneticField(s.lat.toFloat(), s.lon.toFloat(), 500f, System.currentTimeMillis()).declination
    }

    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rv = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val grav = FloatArray(3)
        val geo = FloatArray(3)
        var hasG = false
        var hasM = false
        val rot = FloatArray(9)
        val orient = FloatArray(3)
        var smooth = Float.NaN
        fun push(azDeg: Float) {
            val trueHeading = (azDeg + declination + 360f) % 360f
            smooth = if (smooth.isNaN()) trueHeading else {
                var d = trueHeading - smooth
                if (d > 180) d -= 360f
                if (d < -180) d += 360f
                (smooth + d * 0.15f + 360f) % 360f
            }
            heading = smooth
        }
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                when (e.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rot, e.values)
                        SensorManager.getOrientation(rot, orient)
                        push(Math.toDegrees(orient[0].toDouble()).toFloat())
                    }
                    Sensor.TYPE_ACCELEROMETER -> { System.arraycopy(e.values, 0, grav, 0, 3); hasG = true }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(e.values, 0, geo, 0, 3); hasM = true
                        if (rv == null && hasG && SensorManager.getRotationMatrix(rot, null, grav, geo)) {
                            SensorManager.getOrientation(rot, orient)
                            push(Math.toDegrees(orient[0].toDouble()).toFloat())
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, a: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD || sensor.type == Sensor.TYPE_ROTATION_VECTOR) accuracy = a
            }
        }
        if (rv != null) sm.registerListener(l, rv, SensorManager.SENSOR_DELAY_GAME)
        if (mag != null) sm.registerListener(l, mag, SensorManager.SENSOR_DELAY_GAME)
        if (rv == null && acc != null) sm.registerListener(l, acc, SensorManager.SENSOR_DELAY_GAME)
        hasSensor = rv != null || (acc != null && mag != null)
        onDispose { sm.unregisterListener(l) }
    }

    val diff = run {
        var d = qibla.toFloat() - heading
        while (d > 180) d -= 360f
        while (d < -180) d += 360f
        d
    }
    val aligned = abs(diff) < 3f
    LaunchedEffect(aligned) {
        if (aligned) runCatching {
            val v = ctx.getSystemService(Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= 26) v?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    val dialRot = -heading

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Qibla", s.locName, onBack = { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                listOf("Klassik", "Islomiy", "Zamonaviy").forEachIndexed { i, t ->
                    androidx.compose.material3.FilterChip(
                        selected = s.qiblaStyle == i,
                        onClick = { app.settings.update { it.copy(qiblaStyle = i) } },
                        label = { Text(t) }
                    )
                }
            }
            Text(
                if (!hasSensor) "Telefoningizda kompas sensori topilmadi" else if (aligned) "✅ Qibla tomonga qaradingiz" else
                    if (diff > 0) "O'ngga ${diff.roundToInt()}° buriling" else "Chapga ${(-diff).roundToInt()}° buriling",
                style = MaterialTheme.typography.titleLarge,
                color = if (aligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            VSpace(16.dp)
            Box(Modifier.widthIn(max = 380.dp).fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                androidx.compose.animation.Crossfade(s.qiblaStyle, label = "style") { st ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        when (st) {
                            1 -> CompassIslamic(heading, qibla.toFloat(), aligned)
                            2 -> CompassModern(heading, qibla.toFloat(), aligned)
                            else -> Compass(dialRot, qibla.toFloat(), aligned)
                        }
                    }
                }
            }
            VSpace(16.dp)
            SectionCard(Modifier.fillMaxWidth().widthIn(max = 480.dp)) {
                Text("Qibla yo'nalishi: ${"%.1f".format(qibla)}° (shimoldan)", style = MaterialTheme.typography.titleMedium)
                Text("Telefon yo'nalishi: ${heading.roundToInt()}°", style = MaterialTheme.typography.bodyMedium)
                Text("Makkagacha: ${"%,.0f".format(distance).replace(',', ' ')} km", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Magnit og'ishi hisobga olingan: ${"%.1f".format(declination)}°",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                    VSpace(6.dp)
                    Text(
                        "⚠️ Kompas aniqligi past: telefonni havoda 8 raqami shaklida bir necha marta aylantiring.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error
                    )
                }
                VSpace(6.dp)
                Text(
                    "Telefonni tekis ushlang, metall buyumlar va magnitlardan uzoqroq turing.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Compass(dialRotation: Float, qibla: Float, aligned: Boolean) {
    val cs = MaterialTheme.colorScheme
    val accent = LocalExtra.current.palette.accent
    val ring = cs.surfaceContainerHigh
    val tick = cs.onSurfaceVariant
    val primary = cs.primary
    val north = Color(0xFFE53935)
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = center
            drawCircle(ring, r)
            drawCircle(if (aligned) primary else accent, r, style = Stroke(width = r * 0.03f))
            rotate(dialRotation, c) {
                for (i in 0 until 72) {
                    val a = Math.toRadians(i * 5.0 - 90)
                    val len = if (i % 18 == 0) r * 0.12f else if (i % 2 == 0) r * 0.07f else r * 0.04f
                    val o = Offset(c.x + (r * 0.93f) * cos(a).toFloat(), c.y + (r * 0.93f) * sin(a).toFloat())
                    val i2 = Offset(c.x + (r * 0.93f - len) * cos(a).toFloat(), c.y + (r * 0.93f - len) * sin(a).toFloat())
                    drawLine(if (i == 0) north else tick, i2, o, strokeWidth = if (i % 18 == 0) 5f else 2.5f, cap = StrokeCap.Round)
                }
                // Qibla ko'rsatkichi
                rotate(qibla, c) {
                    val p = Path().apply {
                        moveTo(c.x, c.y - r * 0.78f)
                        lineTo(c.x - r * 0.09f, c.y - r * 0.5f)
                        lineTo(c.x + r * 0.09f, c.y - r * 0.5f)
                        close()
                    }
                    drawPath(p, if (aligned) primary else accent)
                    drawLine(if (aligned) primary else accent, c, Offset(c.x, c.y - r * 0.52f), strokeWidth = r * 0.03f, cap = StrokeCap.Round)
                }
            }
            // Telefonning yuqori tomoni
            val top = Path().apply {
                moveTo(c.x, c.y - r * 1.0f + 2f)
                lineTo(c.x - r * 0.05f, c.y - r * 0.88f)
                lineTo(c.x + r * 0.05f, c.y - r * 0.88f)
                close()
            }
            drawPath(top, cs.onSurface)
            drawCircle(cs.onSurface, r * 0.035f, c)
        }
        // Harflar (N/E/S/W) va Ka'ba — aylanuvchi qatlam
        Box(Modifier.fillMaxSize().padding(8.dp)) {
            CompassLabels(dialRotation, qibla)
        }
    }
}

@Composable
private fun CompassLabels(rotation: Float, qibla: Float) {
    val labels = listOf(0f to "Sh", 90f to "Sq", 180f to "J", 270f to "G'")
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val r = minOf(maxWidth, maxHeight) / 2
        labels.forEach { (deg, t) ->
            val a = Math.toRadians((deg + rotation - 90).toDouble())
            val rr = r * 0.66f
            Text(
                t,
                modifier = Modifier.align(Alignment.Center).padding(0.dp).offsetBy(rr * cos(a).toFloat(), rr * sin(a).toFloat()),
                style = MaterialTheme.typography.titleMedium,
                color = if (deg == 0f) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val qa = Math.toRadians((qibla + rotation - 90).toDouble())
        Text(
            "🕋", fontSize = 30.sp,
            modifier = Modifier.align(Alignment.Center).offsetBy(r * 0.84f * cos(qa).toFloat(), r * 0.84f * sin(qa).toFloat())
        )
    }
}

private fun Modifier.offsetBy(x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp) = this.offset(x = x, y = y)
