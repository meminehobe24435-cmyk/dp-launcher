package com.dp.launcher.ui

/**
 * Single source of truth for every measurement of the launcher UI.
 *
 * All values are expressed in **reference pixels** of the 1280x720 design that was
 * measured from the target projector UI (see `docs/design-spec.md` for how each number
 * was extracted from the reference screenshot). [UiScale] converts them to device pixels
 * at runtime, so the layout is identical on a 720p projector and proportionally identical
 * on 1080p/4K panels.
 *
 * Keep this file and `preview/` in sync - `tools/export_tokens.py` generates the CSS
 * custom properties of the HTML preview directly from these constants.
 */
object DesignSpec {

    /** Width of the reference design, in reference pixels. */
    const val REFERENCE_WIDTH = 1280f

    /** Height of the reference design, in reference pixels. */
    const val REFERENCE_HEIGHT = 720f

    // ---------------------------------------------------------------- status bar

    /** Vertical centre of the status bar row. */
    const val STATUS_CENTER_Y = 71f

    /** Distance from the right screen edge to the end of the date label. */
    const val STATUS_END_MARGIN = 116f

    /** Gap between two status bar items. */
    const val STATUS_ITEM_GAP = 16f

    /** Height of the status bar icons (wifi / input device). */
    const val STATUS_ICON_HEIGHT = 20f

    /** Size of the pointer-device (mouse/air-mouse) indicator. */
    const val STATUS_POINTER_WIDTH = 36f
    const val STATUS_POINTER_HEIGHT = 18f

    /** Size of the wifi indicator. */
    const val STATUS_WIFI_WIDTH = 34f
    const val STATUS_WIFI_HEIGHT = 18f

    /** Font size of the clock and the date. */
    const val STATUS_TEXT_SIZE = 27f

    /**
     * Downward nudge of the status text only.
     *
     * The reference was rendered with a font whose vertical metrics differ slightly from
     * Roboto; without this nudge the labels sit 3 ref-px higher than in the reference, while
     * the icons above them already line up. Fitted with tools/calibrate.py.
     */
    const val STATUS_TEXT_NUDGE = 3f

    // --------------------------------------------------------------- app card row

    /** Top edge of the app card row. */
    const val CARD_ROW_TOP = 134f

    /**
     * Card size and gap. Fitted with `tools/calibrate.py` (stage1) against the reference
     * screenshot: 250/12 minimises the pixel difference over the card row, giving the 122 ref-px
     * side margin measured on the reference.
     */
    const val CARD_WIDTH = 250f

    /** Height of the card itself, without its reflection. */
    const val CARD_HEIGHT = 296f

    const val CARD_GAP = 12f

    /**
     * Total width of the home row: 4 cards plus 3 gaps. Screen minus this width, split in two,
     * is exactly the 120 ref-px margin measured in the reference.
     */
    const val HOME_ROW_WIDTH = 4 * CARD_WIDTH + 3 * CARD_GAP

    const val CARD_CORNER_RADIUS = 18f

    /** Edge length of the square box the app icon is centred in. */
    const val CARD_ICON_SIZE = 140f

    /** Downward offset of the icon box centre from the card centre (optical centring). */
    const val CARD_ICON_OFFSET_Y = 0f

    const val CARD_LABEL_SIZE = 35f

    /** Distance from the card bottom to the baseline area of the label. */
    const val CARD_LABEL_BOTTOM_MARGIN = 18f

    /** Scale applied to a focused app card. */
    const val CARD_FOCUS_SCALE = 1.06f

    /** Stroke width of the white focus ring. */
    const val FOCUS_RING_WIDTH = 3f

    // ------------------------------------------------------------------ reflection

    /** Height of the mirrored, fading reflection below every card. */
    const val REFLECTION_HEIGHT = 50f

    /**
     * Alpha of the reflection directly below the card; it fades to 0 at the bottom.
     * Fitted from the reference: solving `colour = a * card + (1 - a) * wallpaper` on the
     * reflection gradient gives a = 0.60 at the card edge and a linear fade over 50 ref px.
     */
    const val REFLECTION_ALPHA = 0.60f

    // ------------------------------------------------------------------- dock row

    /** Top edge of the dock (bottom function bar). */
    const val DOCK_ROW_TOP = 516f

    const val DOCK_ITEM_WIDTH = 200f

    const val DOCK_ITEM_HEIGHT = 123f

    const val DOCK_ITEM_GAP = 10f

    /** Total width of the dock row: 5 items plus 4 gaps (same 120 ref-px margins as above). */
    const val DOCK_ROW_WIDTH = 5 * DOCK_ITEM_WIDTH + 4 * DOCK_ITEM_GAP

    const val DOCK_ITEM_RADIUS = 16f

    /** Height of the caption strip that carries the label, as a fraction of the item. */
    const val DOCK_CAPTION_RATIO = 0.30f

    const val DOCK_ICON_HEIGHT = 64f

    const val DOCK_LABEL_SIZE = 25f

    /** Scale applied to the focused dock item (fitted with tools/calibrate.py). */
    const val DOCK_FOCUS_SCALE = 1.17f

    /** Stroke width of the light border drawn around every dock item. */
    const val DOCK_BORDER_WIDTH = 2f

    // ----------------------------------------------------------------- app drawer

    /**
     * App drawer background: the same wallpaper blue as the home screen, fully opaque, so the
     * grid keeps maximum contrast on a projector. The drawer fades in over the home screen, which
     * still reads as "on top of" the launcher.
     */
    const val DRAWER_SCRIM_COLOR = 0xFF0200FB.toInt()

    const val DRAWER_TITLE_SIZE = 32f

    const val DRAWER_TOP = 96f

    /** Top edge of the drawer header. */
    const val DRAWER_TITLE_TOP = 34f

    /** Distance from the bottom edge to the keyboard hint. */
    const val DRAWER_HINT_BOTTOM = 26f

    /** 6 columns of 186 ref-px fit the 1120 ref-px content width left by [DRAWER_HORIZONTAL_PADDING]. */
    const val DRAWER_GRID_COLUMNS = 6

    const val DRAWER_GRID_ITEM_WIDTH = 186f

    const val DRAWER_GRID_ITEM_HEIGHT = 168f

    const val DRAWER_GRID_ICON_SIZE = 88f

    const val DRAWER_GRID_LABEL_SIZE = 24f

    const val DRAWER_HINT_SIZE = 20f

    const val DRAWER_HORIZONTAL_PADDING = 80f
}
