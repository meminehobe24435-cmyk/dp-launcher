package com.dp.launcher.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.graphics.createBitmap

/**
 * Container that renders its single child and adds the mirrored "倒影" (reflection) below it,
 * exactly like the reference design.
 *
 * How it works:
 *  - the child keeps its own measured height, the container reports `child + reflectionHeight`;
 *  - the child is rasterised once into a cache bitmap, which is reused until the card content
 *    or size changes ([invalidateReflection] marks the cache dirty);
 *  - the bitmap is drawn again through a mirrored canvas transform and masked with a vertical
 *    alpha gradient, so the reflection fades out towards the bottom.
 *
 * See `docs/design-spec.md#reflection` for the measured geometry (46 ref-px tall, alpha 0.42).
 */
class ReflectionContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val fadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    private var cached: Bitmap? = null
    private var cacheDirty = true
    private var reflectionHeight = 0
    private var reflectionAlpha = DEFAULT_ALPHA

    /** Configures the reflection using device pixels. */
    fun setReflection(reflectionHeightPx: Int, alpha: Float = DEFAULT_ALPHA) {
        if (reflectionHeight == reflectionHeightPx && reflectionAlpha == alpha) return
        reflectionHeight = reflectionHeightPx
        reflectionAlpha = alpha
        requestLayout()
        invalidate()
    }

    /** Call whenever the child content changes (new icon, new label, ...). */
    fun invalidateReflection() {
        cacheDirty = true
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // The reflection lives outside the child's bounds, so the container grows by its height.
        setMeasuredDimension(measuredWidth, measuredHeight + reflectionHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) cacheDirty = true
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val child = getChildAt(0) ?: return
        // The child is pinned to the top; the space below it is reserved for the reflection.
        child.layout(0, 0, measuredWidth, (measuredHeight - reflectionHeight).coerceAtLeast(0))
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (reflectionHeight <= 0) return
        val source = captureChild() ?: return
        drawReflection(canvas, source)
    }

    private fun captureChild(): Bitmap? {
        val child = getChildAt(0) ?: return null
        if (child.width <= 0 || child.height <= 0) return null
        cached?.let { if (!cacheDirty && it.width == child.width && it.height == child.height) return it }

        val bitmap = createBitmap(child.width, child.height)
        val canvas = Canvas(bitmap)
        // Capture at rest: a card that is currently scaling up for focus would otherwise be
        // rasterised at the wrong size.
        val scaleX = child.scaleX
        val scaleY = child.scaleY
        child.scaleX = 1f
        child.scaleY = 1f
        child.draw(canvas)
        child.scaleX = scaleX
        child.scaleY = scaleY

        cached = bitmap
        cacheDirty = false
        return bitmap
    }

    private fun drawReflection(canvas: Canvas, source: Bitmap) {
        val childBottom = (measuredHeight - reflectionHeight).coerceAtLeast(0).toFloat()
        val reflectionBottom = childBottom + reflectionHeight

        val checkpoint = canvas.save()
        canvas.clipRect(0f, childBottom, measuredWidth.toFloat(), reflectionBottom)
        canvas.saveLayer(0f, childBottom, measuredWidth.toFloat(), reflectionBottom, null)

        // Mirror the card around its bottom edge: (x, y) -> (x, 2 * childBottom - y).
        canvas.translate(0f, 2f * childBottom)
        canvas.scale(1f, -1f)
        canvas.drawBitmap(source, 0f, 0f, bitmapPaint)
        canvas.restore()

        // Fade the mirrored copy out towards the bottom of the reflection area.
        fadePaint.shader = LinearGradient(
            0f,
            childBottom,
            0f,
            reflectionBottom,
            (reflectionAlpha * 255f).toInt().coerceIn(0, 255) shl 24,
            0x00000000,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, childBottom, measuredWidth.toFloat(), reflectionBottom, fadePaint)

        canvas.restoreToCount(checkpoint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cached?.recycle()
        cached = null
    }

    private companion object {
        const val DEFAULT_ALPHA = 0.42f
    }
}
