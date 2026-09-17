package com.dp.launcher

import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.View.MeasureSpec
import com.dp.launcher.ui.drawer.AppDrawerFragment
import com.dp.launcher.ui.home.HomeFragment
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * End-to-end smoke test of the launcher on the JVM.
 *
 * It launches the real activity, inflates the real layouts and forces a measure/layout pass.
 * That is the level at which layout-parameter mistakes surface - a plain `ViewGroup.LayoutParams`
 * handed to a `LinearLayout` child, for example, only blows up when the parent measures, which a
 * compile-time check can never see.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], qualifiers = "w1280dp-h720dp-land-xhdpi")
class LauncherSmokeTest {

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun layoutPass(activity: LauncherActivity) {
        val root = activity.findViewById<View>(R.id.launcher_content)
        assertNotNull("launcher_content is missing", root)
        root.measure(
            MeasureSpec.makeMeasureSpec(2560, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(1440, MeasureSpec.EXACTLY),
        )
        root.layout(0, 0, 2560, 1440)
    }

    @Test
    fun `home screen inflates, measures and lays out`() {
        val controller = Robolectric.buildActivity(LauncherActivity::class.java).setup()
        idle()
        val activity = controller.get()

        val home = activity.supportFragmentManager.findFragmentByTag("home")
        assertNotNull("HomeFragment was not added", home)
        assertTrue(home is HomeFragment)

        // The failing case this test exists for: measuring the status bar with wrong params.
        layoutPass(activity)
    }

    @Test
    fun `app drawer opens on top of the home screen and closes again`() {
        val controller = Robolectric.buildActivity(LauncherActivity::class.java).setup()
        idle()
        val activity = controller.get()

        activity.openAppDrawer(activity.findViewById(R.id.launcher_content))
        idle()
        assertNotNull(
            "AppDrawerFragment was not added",
            activity.supportFragmentManager.findFragmentByTag("app_drawer"),
        )
        layoutPass(activity)

        activity.closeAppDrawer()
        idle()
        assertNull(
            "AppDrawerFragment is still attached",
            activity.supportFragmentManager.findFragmentByTag("app_drawer"),
        )
        layoutPass(activity)
    }

    @Test
    fun `back press never finishes the launcher`() {
        val controller = Robolectric.buildActivity(LauncherActivity::class.java).setup()
        idle()
        val activity = controller.get()

        activity.onBackPressedDispatcher.onBackPressed()
        idle()
        assertTrue("the launcher activity must survive BACK", !activity.isFinishing)
    }

    @Test
    fun `menu key opens the app drawer from any focus position`() {
        val controller = Robolectric.buildActivity(LauncherActivity::class.java).setup()
        idle()
        val activity = controller.get()

        // Exactly what the device delivers: the key was not consumed by any view.
        val handled = activity.onKeyDown(
            KeyEvent.KEYCODE_MENU,
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU),
        )
        idle()

        assertTrue("MENU must be consumed by the launcher", handled)
        assertNotNull(
            "MENU did not open the app drawer",
            activity.supportFragmentManager.findFragmentByTag("app_drawer"),
        )
    }

    @Test
    fun `up on the first grid row closes the drawer`() {
        val controller = Robolectric.buildActivity(LauncherActivity::class.java).setup()
        idle()
        val activity = controller.get()

        activity.openAppDrawer(activity.findViewById(R.id.launcher_content))
        idle()
        val drawer = activity.supportFragmentManager.findFragmentByTag("app_drawer")
        assertNotNull(drawer)

        activity.onKeyDown(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP),
        )
        idle()
        assertNull(
            "UP on the first row did not close the drawer",
            activity.supportFragmentManager.findFragmentByTag("app_drawer"),
        )
    }
}
