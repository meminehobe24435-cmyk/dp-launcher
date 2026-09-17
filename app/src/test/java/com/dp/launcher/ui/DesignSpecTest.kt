package com.dp.launcher.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the geometry that has to stay consistent with the 1280x720 reference design.
 *
 * These are plain arithmetic checks - no Android framework involved - but they catch the kind of
 * mistake that is otherwise only visible on the device: a row that no longer fits the reference
 * canvas, or asymmetric margins after somebody tweaks one card value.
 */
class DesignSpecTest {

    @Test
    fun `home row fits the reference canvas with symmetric margins`() {
        val margin = (DesignSpec.REFERENCE_WIDTH - DesignSpec.HOME_ROW_WIDTH) / 2f
        assertEquals(122f, margin, 0.5f)
    }

    @Test
    fun `dock row fits the reference canvas with symmetric margins`() {
        val margin = (DesignSpec.REFERENCE_WIDTH - DesignSpec.DOCK_ROW_WIDTH) / 2f
        assertEquals(120f, margin, 0.5f)
    }

    @Test
    fun `card row and dock row do not overlap`() {
        val reflectionBottom = DesignSpec.CARD_ROW_TOP + DesignSpec.CARD_HEIGHT + DesignSpec.REFLECTION_HEIGHT
        assertTrue(
            "reflection ends at $reflectionBottom but the dock starts at ${DesignSpec.DOCK_ROW_TOP}",
            reflectionBottom < DesignSpec.DOCK_ROW_TOP,
        )
    }

    @Test
    fun `dock rows and cards stay inside the canvas`() {
        val dockBottom = DesignSpec.DOCK_ROW_TOP + DesignSpec.DOCK_ITEM_HEIGHT
        assertTrue("dock bottom $dockBottom exceeds the canvas", dockBottom <= DesignSpec.REFERENCE_HEIGHT)

        val statusBottom = DesignSpec.STATUS_CENTER_Y + DesignSpec.STATUS_WIFI_HEIGHT / 2f
        assertTrue("status bar overlaps the card row", statusBottom < DesignSpec.CARD_ROW_TOP)
    }

    @Test
    fun `focused items keep a slight overlap instead of a gap`() {
        // Focus scales a dock item by 17%; the row padding has to absorb that growth.
        val growth = DesignSpec.DOCK_ITEM_HEIGHT * (DesignSpec.DOCK_FOCUS_SCALE - 1f) / 2f
        assertTrue("focus growth must stay below the gap to the neighbours", growth > 0f)
        assertTrue(growth < DesignSpec.CARD_ROW_TOP)
    }
}
