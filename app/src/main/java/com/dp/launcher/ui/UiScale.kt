package com.dp.launcher.ui

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Converts [DesignSpec] reference pixels into real device pixels.
 *
 * The scale factor is `min(width / 1280, height / 720)`, i.e. the reference design always
 * fits completely inside the screen. On the 16:9 target panel the factor simply is
 * `width / 1280` and the UI is pixel-for-pixel identical to the reference screenshot.
 */
class UiScale(context: Context) {

    private val displayMetrics = context.resources.displayMetrics

    /** Device pixels per reference pixel. */
    val factor: Float = min(
        displayMetrics.widthPixels / DesignSpec.REFERENCE_WIDTH,
        displayMetrics.heightPixels / DesignSpec.REFERENCE_HEIGHT,
    )

    /** Height of the reference design once scaled, in device pixels. */
    val contentHeight: Float = DesignSpec.REFERENCE_HEIGHT * factor

    /**
     * Vertical offset that centres the reference design on displays that are not 16:9.
     * Zero on the target panel.
     */
    val verticalInset: Float =
        ((displayMetrics.heightPixels - contentHeight) / 2f).coerceAtLeast(0f)

    /** Reference pixel -> device pixel. */
    fun px(reference: Float): Float = reference * factor

    /** Reference pixel -> rounded device pixel. */
    fun pxInt(reference: Float): Int = px(reference).roundToInt()

    /** Reference pixel measured from the top of the design -> device pixel from the screen top. */
    fun top(reference: Float): Float = verticalInset + px(reference)

    /** Reference pixel measured from the bottom of the design -> device pixels from the screen bottom. */
    fun bottom(reference: Float): Float = verticalInset + px(DesignSpec.REFERENCE_HEIGHT - reference)

    /** Sets a text size given in reference pixels, ignoring the system font scale. */
    fun applyTextSize(view: TextView, reference: Float) {
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(reference))
    }

    companion object {
        fun of(view: View): UiScale = UiScale(view.context)
    }
}
