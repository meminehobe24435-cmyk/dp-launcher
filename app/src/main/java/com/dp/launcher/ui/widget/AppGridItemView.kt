package com.dp.launcher.ui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.dp.launcher.R
import com.dp.launcher.data.model.AppEntry
import com.dp.launcher.ui.DesignSpec
import com.dp.launcher.ui.UiScale

/**
 * One cell of the all-apps grid shown by the drawer.
 *
 * Visually it is a quieter version of [AppCardView]: a translucent plate that only becomes
 * visible when the cell is focused, so a wall of icons stays readable on the projector.
 */
class AppGridItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val plate = GradientDrawable()
    private lateinit var iconView: ImageView
    private lateinit var labelView: TextView
    private var ui: UiScale = UiScale(context)

    var onAppClick: ((AppEntry) -> Unit)? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        clipChildren = false
        background = plate
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        iconView = findViewById(R.id.grid_icon)
        labelView = findViewById(R.id.grid_label)
    }

    fun bind(app: AppEntry, uiScale: UiScale) {
        ui = uiScale
        plate.cornerRadius = ui.px(GRID_CORNER_RADIUS)
        plate.setColor(Color.TRANSPARENT)
        plate.setStroke(0, Color.TRANSPARENT)

        val iconSize = ui.pxInt(DesignSpec.DRAWER_GRID_ICON_SIZE)
        iconView.layoutParams = LayoutParams(iconSize, iconSize).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = ui.pxInt(GRID_ICON_TOP_MARGIN)
        }
        iconView.setImageDrawable(app.icon)
        iconView.contentDescription = app.label

        ui.applyTextSize(labelView, DesignSpec.DRAWER_GRID_LABEL_SIZE)
        labelView.text = app.label
        labelView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = ui.pxInt(GRID_LABEL_BOTTOM_MARGIN)
        }
        labelView.maxLines = 1
        labelView.ellipsize = android.text.TextUtils.TruncateAt.END

        setOnClickListener { onAppClick?.invoke(app) }
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        plate.setColor(if (gainFocus) FOCUSED_PLATE_COLOR else Color.TRANSPARENT)
        plate.setStroke(if (gainFocus) ui.pxInt(DesignSpec.FOCUS_RING_WIDTH) else 0, Color.WHITE)
        animate()
            .scaleX(if (gainFocus) GRID_FOCUS_SCALE else 1f)
            .scaleY(if (gainFocus) GRID_FOCUS_SCALE else 1f)
            .setDuration(FOCUS_ANIMATION_MS)
            .setInterpolator(if (gainFocus) OvershootInterpolator(1.1f) else DecelerateInterpolator())
            .start()
    }

    private companion object {
        const val GRID_CORNER_RADIUS = 14f
        const val GRID_ICON_TOP_MARGIN = 16f
        const val GRID_LABEL_BOTTOM_MARGIN = 12f
        const val GRID_FOCUS_SCALE = 1.10f
        const val FOCUS_ANIMATION_MS = 150L
        const val FOCUSED_PLATE_COLOR = 0x59FFFFFF
    }
}
