package uz.hijriy.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
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
