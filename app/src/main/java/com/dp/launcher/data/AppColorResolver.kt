package com.dp.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import com.dp.launcher.data.model.AppEntry

/**
 * Decides the background colour of a home card.
 *
 * Resolution order:
 * 1. [BRAND_COLOURS] - colours measured from the reference design for the demo apps, so the
 *    replica matches the target screenshot exactly.
 * 2. The icon's own dominant colour, extracted with [Palette] and normalised for contrast.
 * 3. A deterministic colour derived from the package name, so an app never changes colour
 *    between two launches.
 */
object AppColorResolver {

    /** Colors sampled pixel by pixel from the reference screenshot (see docs/design-spec.md). */
    private val BRAND_COLOURS: Map<String, Int> = mapOf(
        "com.netflix.ninja" to 0xFF010005.toInt(),
        "com.netflix.mediaclient" to 0xFF010005.toInt(),
        "com.netflix.mediaclient.tv" to 0xFF010005.toInt(),
        "com.google.android.youtube" to 0xFF47426A.toInt(),
        "com.google.android.youtube.tv" to 0xFF47426A.toInt(),
        "com.android.vending" to 0xFF82DD7E.toInt(),
        "com.android.chrome" to 0xFF8A48D0.toInt(),
    )

    /** Fallback palette, chosen so that white labels stay readable. */
    private val FALLBACK_COLOURS: IntArray = intArrayOf(
        0xFF243B7A.toInt(),
        0xFF1D6FA5.toInt(),
        0xFF5B2E7E.toInt(),
        0xFF1F7A5A.toInt(),
        0xFF8A3A3A.toInt(),
        0xFF2F4858.toInt(),
        0xFF6B4A1F.toInt(),
        0xFF24306B.toInt(),
    )

    fun resolve(context: Context, entry: AppEntry, icon: Drawable, iconSizePx: Int = 96): Int {
        BRAND_COLOURS[entry.packageName]?.let { return it }
        extractDominantColour(icon, iconSizePx)?.let { return it }
        return fallbackColour(entry.packageName)
    }

    private fun extractDominantColour(icon: Drawable, sizePx: Int): Int? {
        val bitmap = icon.toBitmap(sizePx) ?: return null
        val palette = Palette.from(bitmap).clearFilters().generate()
        val swatch = palette.vibrantSwatch
            ?: palette.darkVibrantSwatch
            ?: palette.mutedSwatch
            ?: palette.dominantSwatch
            ?: return null
        // A card is a large surface: desaturate slightly and force a usable luminance so the
        // white label keeps enough contrast.
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(swatch.rgb, hsl)
        hsl[1] = (hsl[1] * 0.85f).coerceIn(0f, 1f)
        hsl[2] = hsl[2].coerceIn(0.12f, 0.72f)
        return ColorUtils.HSLToColor(hsl)
    }

    private fun fallbackColour(packageName: String): Int =
        FALLBACK_COLOURS[(packageName.hashCode() and Int.MAX_VALUE) % FALLBACK_COLOURS.size]

    @Suppress("DEPRECATION")
    private fun Drawable.toBitmap(sizePx: Int): Bitmap? = runCatching {
        val width = intrinsicWidth.takeIf { it > 0 } ?: sizePx
        val height = intrinsicHeight.takeIf { it > 0 } ?: sizePx
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, sizePx, sizePx)
        draw(canvas)
        setBounds(0, 0, width, height)
        bitmap
    }.getOrNull()
}
