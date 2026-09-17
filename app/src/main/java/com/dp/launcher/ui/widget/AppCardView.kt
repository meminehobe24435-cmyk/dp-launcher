package com.dp.launcher.ui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
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
 * One app card of the home row: a rounded coloured plate with the app icon centred on it and
 * the app label near the bottom edge.
 *
 * Every dimension is derived from [DesignSpec] through [UiScale], so the card keeps the exact
 * proportions of the reference design on a 720p projector and on a 1080p/4K panel alike.
 * Focus adds a small scale-up plus the white ring used across the whole launcher.
 */
class AppCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val plate = GradientDrawable()
    private lateinit var iconView: ImageView
    private lateinit var labelView: TextView

    private var ui: UiScale = UiScale(context)
    private var entry: AppEntry? = null

    /** Invoked when the user opens the card. */
    var onAppClick: ((AppEntry) -> Unit)? = null

    /** Invoked on a long press; the home screen uses it to open the all-apps list. */
    var onAppLongClick: ((AppEntry) -> Unit)? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
        clipChildren = false
        background = plate
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        iconView = findViewById(R.id.card_icon)
        labelView = findViewById(R.id.card_label)
    }

    /** Renders [app] on this card. */
    fun bind(app: AppEntry, uiScale: UiScale) {
        ui = uiScale
        this.entry = app

        plate.cornerRadius = ui.px(DesignSpec.CARD_CORNER_RADIUS)
        plate.setColor(app.cardColor)
        applyFocusAppearance()

        val iconSize = ui.pxInt(DesignSpec.CARD_ICON_SIZE)
        iconView.layoutParams = LayoutParams(iconSize, iconSize).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            topMargin = (ui.pxInt(DesignSpec.CARD_HEIGHT) - iconSize) / 2 +
                ui.pxInt(DesignSpec.CARD_ICON_OFFSET_Y)
        }
        iconView.setImageDrawable(app.icon)
        iconView.contentDescription = app.label

        ui.applyTextSize(labelView, DesignSpec.CARD_LABEL_SIZE)
        labelView.text = app.label
        labelView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = ui.pxInt(DesignSpec.CARD_LABEL_BOTTOM_MARGIN)
        }

        setOnClickListener { onAppClick?.invoke(app) }
        setOnLongClickListener {
            onAppLongClick?.invoke(app)
            true
        }
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        applyFocusAppearance()
        val target = if (gainFocus) DesignSpec.CARD_FOCUS_SCALE else 1f
        animate()
            .scaleX(target)
            .scaleY(target)
            .setDuration(FOCUS_ANIMATION_MS)
            .setInterpolator(if (gainFocus) OvershootInterpolator(1.2f) else DecelerateInterpolator())
            .start()
    }

    /** Small "press" feedback before the target activity takes over the screen. */
    fun playPressFeedback() {
        animate().scaleX(0.96f).scaleY(0.96f).setDuration(80L).withEndAction {
            animate().scaleX(DesignSpec.CARD_FOCUS_SCALE).scaleY(DesignSpec.CARD_FOCUS_SCALE)
                .setDuration(120L).start()
        }.start()
    }

    private fun applyFocusAppearance() {
        val ringWidth = if (isFocused) ui.pxInt(DesignSpec.FOCUS_RING_WIDTH) else 0
        plate.setStroke(ringWidth, Color.WHITE)
        elevation = if (isFocused) ui.px(FOCUSED_ELEVATION) else 0f
    }

    /** The reflection container that hosts this card, if any. */
    fun reflectionHost(): ReflectionContainer? = parent as? ReflectionContainer

    private companion object {
        const val FOCUS_ANIMATION_MS = 160L
        const val FOCUSED_ELEVATION = 10f
    }
}
