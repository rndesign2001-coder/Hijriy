package uz.hijriy.app.ui

import android.Manifest
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.hijriy.app.HijriyApp
import uz.hijriy.app.core.Hijri
import uz.hijriy.app.core.Prayer
import uz.hijriy.app.core.Uz
import uz.hijriy.app.core.fmtMin
import uz.hijriy.app.data.Settings
import uz.hijriy.app.ui.theme.QuranFont
import uz.hijriy.app.ui.theme.TitleFont
import java.io.File
import java.time.LocalDate

// ------------------------------ Parametrlar ------------------------------

val WpBackgrounds = listOf("Tungi masjid", "Zumrad naqsh", "Oltin shafaq", "Tong nuri")
val WpLayouts = listOf("Klassik", "Shisha karta", "Pastki panel", "Arabcha sarlavha")
val WpFonts = listOf("Serif" to TitleFont, "Mashinka" to FontFamily.Monospace, "Zamonaviy" to FontFamily.SansSerif, "Nafis" to FontFamily.Cursive)
val WpColors = listOf(
    "Oq" to Color.White, "Oltin" to Color(0xFFF3D57E), "Krem" to Color(0xFFFBF1D9),
    "Zumrad" to Color(0xFF7CE0B8), "Qora" to Color(0xFF111111)
)

data class WpOptions(
    val bg: Int = 0,             // 0..3 ichki fonlar, -1 — galereya rasmi
    val layout: Int = 0,
    val font: Int = 0,
    val color: Int = 0,
    val sunrise: Boolean = false,
    val hijri: Boolean = true,
    val location: Boolean = true,
    val dim: Float = 0.35f,      // rasmni qoraytirish (o'qilishi uchun)
    val tomorrow: Boolean = false,
)

const val WP_W = 1080f
const val WP_H = 1920f

// ------------------------------ Chizish ------------------------------

private fun DrawScope.drawBackground(kind: Int, photo: ImageBitmap?, dim: Float) {
    if (photo != null) {
        // markazdan kesib (center-crop) joylash
        val s = maxOf(size.width / photo.width, size.height / photo.height)
        val sw = (size.width / s).toInt(); val sh = (size.height / s).toInt()
        val sx = (photo.width - sw) / 2; val sy = (photo.height - sh) / 2
        drawImage(photo, IntOffset(sx, sy), IntSize(sw, sh), IntOffset.Zero, IntSize(size.width.toInt(), size.height.toInt()))
        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = dim * 0.6f), Color.Black.copy(alpha = dim), Color.Black.copy(alpha = dim * 1.2f))))
        return
    }
    val w = size.width; val h = size.height
    when (kind) {
        0 -> { // Tungi masjid
            drawRect(Brush.verticalGradient(listOf(Color(0xFF050817), Color(0xFF151A4F), Color(0xFF3B2C7A), Color(0xFF6E3C8F))))
            with(StarField(140, 3)) { draw(Color.White, 0.25f, 0.7f, 3.2f) }
            val mc = Offset(w * 0.78f, h * 0.31f)
            drawCircle(Brush.radialGradient(listOf(Color(0x66FFF1C1), Color.Transparent), mc, w * 0.3f), w * 0.3f, mc)
            drawPath(crescentPath(mc.x, mc.y, w * 0.085f), Color(0xFFFFF1C1))
            drawPath(mosquePath(w * 1.05f, h + 4f, h * 0.2f, -w * 0.025f), Color(0xFF07091C))
        }
        1 -> { // Zumrad naqsh
            drawRect(Brush.radialGradient(listOf(Color(0xFF1AA37A), Color(0xFF0B5D48), Color(0xFF042219)), Offset(w / 2, h * 0.4f), h * 0.75f))
            drawStarLattice(Color(0x40F3D57E), w / 4.5f, 3f, 0f, Offset(w / 2, h / 2))
            drawRect(Brush.radialGradient(listOf(Color.Transparent, Color(0xAA000000)), Offset(w / 2, h / 2), h * 0.7f))
            drawRect(Color(0xFFD4A72C), Offset(40f, 40f), Size(w - 80f, h - 80f), style = Stroke(6f))
            drawRect(Color(0x99F3D57E), Offset(58f, 58f), Size(w - 116f, h - 116f), style = Stroke(2f))
        }
        2 -> { // Oltin shafaq
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1E0B3F), Color(0xFF6B1F63), Color(0xFFD9485F), Color(0xFFF7A440), Color(0xFFFFD27F))))
            val sc = Offset(w / 2, h * 0.8f)
            drawCircle(Brush.radialGradient(listOf(Color(0xCCFFE8A3), Color.Transparent), sc, w * 0.6f), w * 0.6f, sc)
            drawCircle(Color(0xFFFFE6A0), w * 0.16f, sc)
            drawPath(mosquePath(w * 0.95f, h + 4f, h * 0.24f, w * 0.025f), Color(0xFF2A0E2E))
        }
        else -> { // Tong nuri
            drawRect(Brush.verticalGradient(listOf(Color(0xFF0D1B3E), Color(0xFF274C8C), Color(0xFF8E7CC3), Color(0xFFF6B7A0))))
            with(StarField(60, 9)) { draw(Color.White.copy(alpha = 0.7f), 0.6f, 0.35f, 2.5f) }
            val c = Offset(w / 2, h * 1.02f)
            for (i in 0 until 14) {
                val a = Math.toRadians((-160 + i * 10).toDouble())
                drawLine(Color(0x22FFFFFF), c, Offset(c.x + h * kotlin.math.cos(a).toFloat(), c.y + h * kotlin.math.sin(a).toFloat()), 40f)
            }
            drawPath(mosquePath(w * 0.8f, h + 4f, h * 0.22f, w * 0.1f), Color(0xFF14183A))
        }
    }
}

fun DrawScope.renderPrayerWallpaper(
    tm: TextMeasurer, o: WpOptions, s: Settings, date: LocalDate, photo: ImageBitmap?,
) {
    drawBackground(o.bg, photo, o.dim)
    val t = s.times(date)
    val prayers = Prayer.entries.filter { it != Prayer.SUNRISE || o.sunrise }
    val color = WpColors[o.color.coerceIn(0, WpColors.lastIndex)].second
    val font = WpFonts[o.font.coerceIn(0, WpFonts.lastIndex)].second
    val dark = color.luminance() < 0.3f
    val shadow = Shadow(if (dark) Color(0x66FFFFFF) else Color(0xAA000000), Offset(0f, 4f), 12f)
    val w = size.width; val h = size.height
    val hj = Hijri.fromGregorian(date, s.hijriAdjust)
    fun style(sz: Float, weight: FontWeight = FontWeight.Bold, fam: FontFamily = font, col: Color = color, align: TextAlign = TextAlign.Start) =
        TextStyle(color = col, fontSize = sz.sp, fontFamily = fam, fontWeight = weight, shadow = shadow, textAlign = align)
    fun text(raw: String, x: Float, y: Float, st: TextStyle, width: Float? = null) {
        val str = tr(raw, s.script == 1)
        if (width != null) drawText(tm, str, Offset(x, y), st, size = Size(width, h))
        else drawText(tm, str, Offset(x, y), st)
    }
    val title = if (o.location) "${s.locName.removeSuffix(" tumani").removeSuffix(" shahri")} namoz vaqtlari" else "Namoz vaqtlari"
    val dateLine = Uz.gregorianFull(date) + if (o.hijri) "\n${hj.day} ${hj.monthName} ${hj.year} h." else ""

    when (o.layout) {
        0 -> { // Klassik — rasmdagi namunaga o'xshash, ikki ustun
            text(title, 0f, h * 0.08f, style(58f, align = TextAlign.Center), w)
            text(dateLine, 0f, h * 0.08f + 90f, style(40f, FontWeight.Medium, align = TextAlign.Center), w)
            val top = h * 0.5f; val step = if (prayers.size > 5) 118f else 132f
            prayers.forEachIndexed { i, p ->
                val y = top + i * step
                text(p.uz.uppercase().replace("XUFTON", "XUFTON"), w * 0.13f, y, style(84f))
                text(fmtMin(t[p]), w * 0.60f, y, style(84f))
            }
        }
        1 -> { // Shisha karta
            val l = w * 0.08f; val r = w * 0.92f
            val cardTop = h * 0.26f
            val rowH = 128f
            val cardH = 370f + prayers.size * rowH
            drawRoundRect(Color.Black.copy(alpha = 0.38f), Offset(l, cardTop), Size(r - l, cardH), CornerRadius(56f))
            drawRoundRect(Color(0xFFF3D57E).copy(alpha = 0.8f), Offset(l, cardTop), Size(r - l, cardH), CornerRadius(56f), style = Stroke(4f))
            text("مواقيت الصلاة", l, cardTop + 34f, style(64f, FontWeight.Normal, QuranFont, Color(0xFFF3D57E), TextAlign.Center), r - l)
            text(title, l, cardTop + 150f, style(46f, align = TextAlign.Center), r - l)
            text(dateLine, l, cardTop + 214f, style(32f, FontWeight.Medium, align = TextAlign.Center), r - l)
            prayers.forEachIndexed { i, p ->
                val y = cardTop + 340f + i * rowH
                drawLine(Color.White.copy(alpha = 0.18f), Offset(l + 50f, y), Offset(r - 50f, y), 2f)
                text(p.uz, l + 70f, y + 24f, style(66f, FontWeight.Medium))
                val tt = fmtMin(t[p])
                val m = tm.measure(tt, style(66f))
                text(tt, r - 70f - m.size.width, y + 24f, style(66f))
            }
        }
        2 -> { // Pastki panel
            text(title, 0f, h * 0.12f, style(66f, align = TextAlign.Center), w)
            text(dateLine, 0f, h * 0.12f + 100f, style(42f, FontWeight.Medium, align = TextAlign.Center), w)
            val bandTop = h * 0.64f
            drawRoundRect(Color.Black.copy(alpha = 0.42f), Offset(36f, bandTop), Size(w - 72f, 330f), CornerRadius(48f))
            val colW = (w - 72f) / prayers.size
            prayers.forEachIndexed { i, p ->
                val x = 36f + i * colW
                text(p.uz, x, bandTop + 60f, style(if (prayers.size > 5) 30f else 36f, FontWeight.Medium, align = TextAlign.Center), colW)
                text(fmtMin(t[p]), x, bandTop + 140f, style(if (prayers.size > 5) 52f else 60f, align = TextAlign.Center), colW)
                if (i > 0) drawLine(Color.White.copy(alpha = 0.2f), Offset(x, bandTop + 50f), Offset(x, bandTop + 280f), 2f)
            }
        }
        else -> { // Arabcha sarlavha + kataklar
            text("مواقيت الصلاة", 0f, h * 0.08f, style(110f, FontWeight.Normal, QuranFont, Color(0xFFF3D57E), TextAlign.Center), w)
            text(title, 0f, h * 0.08f + 200f, style(50f, align = TextAlign.Center), w)
            text(dateLine, 0f, h * 0.08f + 270f, style(36f, FontWeight.Medium, align = TextAlign.Center), w)
            val cols = 2; val gap = 36f; val cw = (w - 120f - gap) / cols; val ch = 230f
            val top = h * 0.42f
            prayers.forEachIndexed { i, p ->
                val x = 60f + (i % cols) * (cw + gap); val y = top + (i / cols) * (ch + gap)
                drawRoundRect(Color.White.copy(alpha = 0.14f), Offset(x, y), Size(cw, ch), CornerRadius(40f))
                drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(x, y), Size(cw, ch), CornerRadius(40f), style = Stroke(3f))
                text(p.uz, x, y + 36f, style(44f, FontWeight.Medium, align = TextAlign.Center), cw)
                text(fmtMin(t[p]), x, y + 100f, style(84f, align = TextAlign.Center), cw)
            }
        }
    }
    text("Hijriy Taqvim", 0f, h - 90f, style(28f, FontWeight.Medium, TitleFont, color.copy(alpha = 0.7f), TextAlign.Center), w)
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

fun renderToBitmap(tm: TextMeasurer, o: WpOptions, s: Settings, date: LocalDate, photo: ImageBitmap?, scale: Float = 1f): ImageBitmap {
    val img = ImageBitmap((WP_W * scale).toInt(), (WP_H * scale).toInt())
    val canvas = androidx.compose.ui.graphics.Canvas(img)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(WP_W, WP_H)) {
        if (scale != 1f) drawContext.transform.scale(scale, scale, Offset.Zero)
        renderPrayerWallpaper(tm, o, s, date, photo)
    }
    return img
}

private fun loadPhoto(ctx: Context, uri: Uri): ImageBitmap? = runCatching {
    val bmp: Bitmap = if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { dec, info, _ ->
            val max = maxOf(info.size.width, info.size.height)
            if (max > 2400) dec.setTargetSampleSize(max / 2000)
            dec.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } else {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        val max = maxOf(opts.outWidth, opts.outHeight)
        val o2 = BitmapFactory.Options().apply { inSampleSize = if (max > 2400) max / 2000 else 1 }
        ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, o2)!! }
    }
    bmp.asImageBitmap()
}.getOrNull()

private fun saveToGallery(ctx: Context, bmp: Bitmap): Boolean = runCatching {
    val name = "namoz_vaqtlari_${System.currentTimeMillis()}.png"
    if (Build.VERSION.SDK_INT >= 29) {
        val v = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Hijriy Taqvim")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v) ?: return false
        ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.insertImage(ctx.contentResolver, bmp, name, "Namoz vaqtlari") ?: return false
    }
    true
}.getOrDefault(false)

private fun shareBitmap(ctx: Context, bmp: Bitmap) {
    val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
    val f = File(dir, "namoz_vaqtlari.png")
    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.files", f)
    val i = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(i, "Ulashish").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

// ------------------------------ Ekran ------------------------------

@Composable
fun WallpaperScreen(app: HijriyApp, nav: NavHostController) {
    val ctx = LocalContext.current
    val s by app.settings.state.collectAsStateWithLifecycle()
    // Fon oqimida ishlatiladigan alohida matn o'lchagich (UI bilan to'qnashmaydi)
    val resolver = androidx.compose.ui.platform.LocalFontFamilyResolver.current
    val tm = remember { TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr, 0) }
    val scope = rememberCoroutineScope()
    var o by remember { mutableStateOf(WpOptions()) }
    var photo by remember { mutableStateOf<ImageBitmap?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    val date = if (o.tomorrow) LocalDate.now().plusDays(1) else LocalDate.now()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val p = withContext(Dispatchers.IO) { loadPhoto(ctx, uri) }
            if (p != null) { photo = p; o = o.copy(bg = -1) }
            else android.widget.Toast.makeText(ctx, uiText(ctx, "Rasmni ochib bo'lmadi"), Toast.LENGTH_SHORT).show()
        }
    }
    fun full(): Bitmap = renderToBitmap(tm, o, s, date, if (o.bg == -1) photo else null).asAndroidBitmap()
    fun doSave() {
        busy = true
        scope.launch {
            val bmp = full()
            val ok = withContext(Dispatchers.IO) { saveToGallery(ctx, bmp) }
            busy = false
            android.widget.Toast.makeText(ctx, uiText(ctx, if (ok) "Galereyaga saqlandi (Pictures/Hijriy Taqvim)" else "Saqlab bo'lmadi"), Toast.LENGTH_SHORT).show()
        }
    }
    val storagePerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) doSave() }

    LaunchedEffect(o, s, photo) {
        // Asosiy oqimda chiziladi (tez, ~50 ms) — matn keshini boshqa oqim bilan bo'lishmaslik uchun
        kotlinx.coroutines.delay(60)
        preview = runCatching { renderToBitmap(tm, o, s, date, if (o.bg == -1) photo else null, 0.5f) }.getOrNull()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Rasm tayyorlash", "Namoz vaqtlari bilan chiroyli rasm", onBack = { nav.popBackStack() })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(0.72f)
                    .aspectRatio(WP_W / WP_H)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .testTag("wp_preview"),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(preview, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "pv") { img ->
                    if (img != null) Image(img, "Namuna", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else CircularProgressIndicator()
                }
            }
            VSpace(12.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (Build.VERSION.SDK_INT < 29 &&
                        ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    ) storagePerm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else doSave()
                }, enabled = !busy) { Icon(Icons.Filled.Download, null); HSpace(6.dp); Text("Saqlash") }
                FilledTonalButton(onClick = { runCatching { shareBitmap(ctx, full()) } }) {
                    Icon(Icons.Filled.Share, null); HSpace(6.dp); Text("Ulashish")
                }
            }
            FilledTonalButton(onClick = {
                busy = true
                scope.launch {
                    val bmp = full()
                    val ok = withContext(Dispatchers.IO) { runCatching { WallpaperManager.getInstance(ctx).setBitmap(bmp) }.isSuccess }
                    busy = false
                    android.widget.Toast.makeText(ctx, uiText(ctx, if (ok) "Fon rasmi o'rnatildi" else "O'rnatib bo'lmadi"), Toast.LENGTH_SHORT).show()
                }
            }, enabled = !busy, modifier = Modifier.padding(top = 6.dp)) { Icon(Icons.Filled.Wallpaper, null); HSpace(6.dp); Text("Telefon fon rasmi qilish") }

            SectionTitleLeft("Fon")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BgThumb("Galereya", o.bg == -1, photo) {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                WpBackgrounds.forEachIndexed { i, name -> BgThumb(name, o.bg == i, null, i) { o = o.copy(bg = i) } }
            }
            SectionTitleLeft("Joylash uslubi")
            ChipRow(WpLayouts, o.layout) { o = o.copy(layout = it) }
            SectionTitleLeft("Shrift")
            ChipRow(WpFonts.map { it.first }, o.font) { o = o.copy(font = it) }
            SectionTitleLeft("Yozuv rangi")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WpColors.forEachIndexed { i, (n, c) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { o = o.copy(color = i) }) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(c)
                                .border(3.dp, if (o.color == i) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Text(n, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            SectionTitleLeft("Qo'shimcha")
            SectionCard(Modifier.fillMaxWidth(), padding = 6.dp) {
                SettingRow(null, "Quyosh chiqishini qo'shish") { Switch(o.sunrise, { o = o.copy(sunrise = it) }) }
                SettingRow(null, "Hijriy sana") { Switch(o.hijri, { o = o.copy(hijri = it) }) }
                SettingRow(null, "Joy nomi (${s.locName})") { Switch(o.location, { o = o.copy(location = it) }) }
                SettingRow(null, "Ertangi kun vaqtlari") { Switch(o.tomorrow, { o = o.copy(tomorrow = it) }) }
                if (o.bg == -1) {
                    Text("Rasmni qoraytirish: ${(o.dim * 100).toInt()}%", modifier = Modifier.padding(start = 8.dp, top = 4.dp))
                    Slider(o.dim, { o = o.copy(dim = it) }, valueRange = 0f..0.8f)
                }
            }
            VSpace(28.dp)
        }
    }
}

@Composable
private fun SectionTitleLeft(t: String) {
    Text(t, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp))
}

@Composable
private fun ChipRow(items: List<String>, sel: Int, onSel: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, n -> FilterChip(selected = sel == i, onClick = { onSel(i) }, label = { Text(n) }) }
    }
}

@Composable
private fun BgThumb(name: String, selected: Boolean, photo: ImageBitmap?, kind: Int = -1, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(76.dp)) {
        Box(
            Modifier
                .size(width = 72.dp, height = 112.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(3.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (kind >= 0) {
                val thumb = remember(kind) { renderBackground(kind, 0.1f) }
                Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else if (photo != null) {
                Image(photo, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Faqat fonni kichik o'lchamda chizadi (tanlash uchun namunalar). */
fun renderBackground(kind: Int, scale: Float): ImageBitmap {
    val img = ImageBitmap((WP_W * scale).toInt(), (WP_H * scale).toInt())
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(img), Size(WP_W, WP_H)) {
        drawContext.transform.scale(scale, scale, Offset.Zero)
        drawBackground(kind, null, 0f)
    }
    return img
}
