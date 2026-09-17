package com.dp.launcher.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.dp.launcher.R
import com.dp.launcher.ui.DesignSpec
import com.dp.launcher.ui.UiScale
import com.dp.launcher.ui.home.DockItem

/**
 * One button of the bottom dock: coloured plate, caption strip with the label, and a light
 * border - all drawn here instead of with a static drawable, because the caption strip is a
 * fraction of the button height and therefore has to follow the runtime scale factor.
 *
 * Focus reproduces the reference behaviour: the button grows by 14%, the border turns white and
 * the plate lifts with a shadow.
 */
class DockItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var iconView: ImageView
    private lateinit var labelView: TextView

    private var ui: UiScale = UiScale(context)
    private var model: DockItem? = null

    private val platePath = Path()
    private val borderPath = Path()
    private val bounds = RectF()

    /** Colours sampled from the reference screenshot. */
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PLATE_COLOR }
    private val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CAPTION_COLOR }
    private val captionEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CAPTION_EDGE_COLOR }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = BORDER_COLOR
    }

    /** Invoked with the dock action when the button is activated. */
    var onDockAction: ((DockItem) -> Unit)? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        clipChildren = false
        setWillNotDraw(false)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        iconView = findViewById(R.id.dock_icon)
        labelView = findViewById(R.id.dock_label)
    }

    /** Renders [item] on this button. */
    fun bind(item: DockItem, uiScale: UiScale) {
        ui = uiScale
        model = item

        iconView.setImageResource(item.iconRes)
        ui.applyTextSize(labelView, DesignSpec.DOCK_LABEL_SIZE)
        labelView.setText(item.labelRes)

        setOnClickListener { onDockAction?.invoke(item) }
        invalidate()
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        val target = if (gainFocus) DesignSpec.DOCK_FOCUS_SCALE else 1f
        elevation = if (gainFocus) ui.px(FOCUSED_ELEVATION) else ui.px(RESTING_ELEVATION)
        animate()
            .scaleX(target)
            .scaleY(target)
            .setDuration(FOCUS_ANIMATION_MS)
            .setInterpolator(if (gainFocus) OvershootInterpolator(1.1f) else DecelerateInterpolator())
            .start()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        if (::iconView.isInitialized) {
            val iconHeight = ui.pxInt(DesignSpec.DOCK_ICON_HEIGHT)
            val drawableRatio = iconView.drawable?.let { drawable ->
                val intrinsicWidth = drawable.intrinsicWidth
                val intrinsicHeight = drawable.intrinsicHeight
                if (intrinsicHeight > 0 && intrinsicWidth > 0) {
                    intrinsicWidth.toFloat() / intrinsicHeight
                } else {
                    ICON_FALLBACK_RATIO
                }
            } ?: ICON_FALLBACK_RATIO
            val iconWidth = (iconHeight * drawableRatio).toInt()
            iconView.measure(
                MeasureSpec.makeMeasureSpec(iconWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(iconHeight, MeasureSpec.EXACTLY),
            )
            labelView.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
        }
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (!::iconView.isInitialized) return
        val width = right - left
        val height = bottom - top
        val captionTop = (height * (1f - DesignSpec.DOCK_CAPTION_RATIO)).toInt()

        val iconWidth = iconView.measuredWidth
        val iconHeight = iconView.measuredHeight
        val iconLeft = (width - iconWidth) / 2
        val iconTop = (captionTop - iconHeight) / 2
        iconView.layout(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight)

        val labelWidth = labelView.measuredWidth
        val labelHeight = labelView.measuredHeight
        val labelLeft = (width - labelWidth) / 2
        val labelTop = captionTop + (height - captionTop - labelHeight) / 2
        labelView.layout(labelLeft, labelTop, labelLeft + labelWidth, labelTop + labelHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()
        if (width <= 0f || height <= 0f) return

        val radius = ui.px(DesignSpec.DOCK_ITEM_RADIUS)
        bounds.set(0f, 0f, width, height)
        platePath.reset()
        platePath.addRoundRect(bounds, radius, radius, Path.Direction.CW)

        canvas.drawPath(platePath, fillPaint)

        val captionTop = height * (1f - DesignSpec.DOCK_CAPTION_RATIO)
        val checkpoint = canvas.save()
        canvas.clipPath(platePath)
        canvas.drawRect(0f, captionTop, width, height, captionPaint)
        canvas.drawRect(0f, captionTop, width, captionTop + ui.px(CAPTION_EDGE_HEIGHT), captionEdgePaint)
        canvas.restoreToCount(checkpoint)

        val borderWidth = ui.px(if (isFocused) DesignSpec.FOCUS_RING_WIDTH else DesignSpec.DOCK_BORDER_WIDTH)
        borderPaint.strokeWidth = borderWidth
        borderPaint.color = if (isFocused) Color.WHITE else BORDER_COLOR
        val inset = borderWidth / 2f
        borderPath.reset()
        borderPath.addRoundRect(
            inset,
            inset,
            width - inset,
            height - inset,
            radius - inset,
            radius - inset,
            Path.Direction.CW,
        )
        canvas.drawPath(borderPath, borderPaint)
    }

    override fun setPressed(pressed: Boolean) {
        super.setPressed(pressed)
        // Touch/pointer feedback: a dock button sinks slightly instead of scaling up.
        val target = when {
            pressed -> PRESSED_SCALE
            isFocused -> DesignSpec.DOCK_FOCUS_SCALE
            else -> 1f
        }
        animate().scaleX(target).scaleY(target).setDuration(90L).start()
    }

    /** Applies the palette of the reference design. */
    fun applyPalette(uiScale: UiScale) {
        ui = uiScale
        invalidate()
    }

    val dockModel: DockItem? get() = model

    private companion object {
        const val FOCUS_ANIMATION_MS = 170L
        const val FOCUSED_ELEVATION = 14f
        const val RESTING_ELEVATION = 0f
        const val PRESSED_SCALE = 0.98f
        const val CAPTION_EDGE_HEIGHT = 1f
        const val ICON_FALLBACK_RATIO = 1.1f

        /** Sampled from the reference screenshot. */
        const val PLATE_COLOR = 0xFF1380E7.toInt()
        const val CAPTION_COLOR = 0xFF3496E7.toInt()
        const val CAPTION_EDGE_COLOR = 0xFF2C9BF4.toInt()
        const val BORDER_COLOR = 0xFF3A76F4.toInt()
    }
}
