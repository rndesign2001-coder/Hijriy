package uz.hijriy.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.hijriy.app.R

/** Bosh sahifa fon fotosuratlari (Wikimedia Commons, ochiq litsenziyalar — "Mualliflar" bo'limida). */
data class HeroBg(val res: Int, val title: String)

object HeroBgs {
    val all = listOf(
        HeroBg(R.drawable.bg_kaaba_sunset, "Ka'ba — shom"),
        HeroBg(R.drawable.bg_nabawi_dusk, "Masjidi Nabaviy — oqshom"),
        HeroBg(R.drawable.bg_haram_night, "Masjidul Harom — tun"),
        HeroBg(R.drawable.bg_nabawi_sunset, "Masjidi Nabaviy — quyosh botishi"),
        HeroBg(R.drawable.bg_haram_evening, "Masjidul Harom — kechqurun"),
        HeroBg(R.drawable.bg_nabawi_night, "Masjidi Nabaviy — tun"),
    )

    /** 0 — jonli osmon, 1..N — fotosurat. */
    fun of(index: Int): HeroBg? = all.getOrNull(index - 1)

    private val cache = HashMap<Pair<Int, Int>, ImageBitmap>()

    fun decode(ctx: Context, res: Int, sample: Int): ImageBitmap = synchronized(cache) {
        cache.getOrPut(res to sample) {
            val o = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeResource(ctx.resources, res, o).asImageBitmap()
        }
    }
}

/** Rasmni fon oqimida kichraytirib o'qiydi (xotira tejaladi, interfeys qotmaydi). */
@Composable
fun rememberBgBitmap(res: Int, sample: Int = 1): State<ImageBitmap?> {
    val ctx = LocalContext.current
    return produceState<ImageBitmap?>(null, res, sample) {
        value = withContext(Dispatchers.IO) { runCatching { HeroBgs.decode(ctx, res, sample) }.getOrNull() }
    }
}

/** Karta orqasidagi fotosurat: kesib to'ldiriladi, ustidan matn o'qilishi uchun qorong'i parda tortiladi. */
@Composable
fun PhotoBackdrop(
    res: Int,
    scrim: androidx.compose.ui.graphics.Color,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    alignY: Float = 0f,
    strength: Float = 1f,
) {
    val bmp by rememberBgBitmap(res, 1)
    androidx.compose.foundation.layout.Box(modifier) {
        bmp?.let {
            androidx.compose.foundation.Image(
                it, null, androidx.compose.ui.Modifier.matchParentSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                alignment = androidx.compose.ui.BiasAlignment(0f, alignY),
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High
            )
        }
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier.matchParentSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(
                        scrim.copy(alpha = (0.30f * strength).coerceAtMost(1f)),
                        scrim.copy(alpha = (0.55f * strength).coerceAtMost(1f)),
                        scrim.copy(alpha = (0.86f * strength).coerceAtMost(1f)),
                    )
                )
            )
        )
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier.matchParentSize().background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(scrim.copy(alpha = 0.35f * strength.coerceAtMost(1f)), androidx.compose.ui.graphics.Color.Transparent)
                )
            )
        )
    }
}

/** Fotosuratli bezakli karta: oltin hoshiya, nozik girih naqsh, oq matn. */
@Composable
fun PhotoCard(
    res: Int,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    scrim: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF07231C),
    alignY: Float = 0f,
    strength: Float = 1f,
    padding: androidx.compose.ui.unit.Dp = 18.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
    androidx.compose.foundation.layout.Box(
        modifier.fillMaxWidth().clip(shape)
            .background(scrim)
            .border(1.dp, androidx.compose.ui.graphics.Color(0xFFE9C46A).copy(alpha = 0.45f), shape)
    ) {
        PhotoBackdrop(res, scrim, androidx.compose.ui.Modifier.matchParentSize(), alignY, strength)
        androidx.compose.foundation.Canvas(androidx.compose.ui.Modifier.matchParentSize()) {
            drawGirih(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f), 54.dp.toPx(), 0.8.dp.toPx())
        }
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides androidx.compose.ui.graphics.Color.White
        ) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.padding(padding), content = content)
        }
    }
}

/** Kartalar uchun ochiq litsenziyali fotosuratlar (manbalar — "Mualliflar" bo'limida). */
object CardPhotos {
    val registanNight = R.drawable.card_registan_night
    val registanGold = R.drawable.card_registan_gold
    val hazratiImam = R.drawable.card_hazrati_imam
    val quranFolio = R.drawable.card_quran_folio
    val crescentDusk = R.drawable.card_crescent_dusk
    val crescentNight = R.drawable.card_crescent_night
    val arafat = R.drawable.card_arafat
    val chimganSky = R.drawable.card_chimgan_sky
    val storm = R.drawable.card_storm
    val sunriseClouds = R.drawable.card_sunrise_clouds
}
