package com.dp.launcher

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dp.launcher.databinding.ActivityLauncherBinding
import com.dp.launcher.ui.LauncherNavigator
import com.dp.launcher.ui.drawer.AppDrawerFragment
import com.dp.launcher.ui.home.HomeFragment

/**
 * The only activity of the launcher: it declares the HOME intent filter, owns the two screens
 * (home + app drawer) and keeps the projector display in a full-screen, always-on state.
 */
class LauncherActivity : AppCompatActivity(), LauncherNavigator {

    private lateinit var binding: ActivityLauncherBinding
    private var drawerSource: View? = null

    private val drawerFragment: AppDrawerFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_DRAWER) as? AppDrawerFragment

    private val homeFragment: HomeFragment?
        get() = supportFragmentManager.findFragmentByTag(TAG_HOME) as? HomeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // A launcher is on screen for hours: never let the panel sleep while it is visible.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.launcher_content, HomeFragment(), TAG_HOME)
                .commit()
        }

        // BACK never leaves the launcher: it only closes the app drawer.
        onBackPressedDispatcher.addCallback(this) {
            if (drawerFragment != null) {
                closeAppDrawer()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing HOME while the drawer is open returns to the home screen.
        if (drawerFragment != null) closeAppDrawer()
    }

    /**
     * Receives every key the view hierarchy did not consume.
     *
     * This is where "any focus position can jump into the installed-app list" is implemented:
     * a container's `setOnKeyListener` is not called while one of its children holds the focus,
     * so MENU, UP above the cards and DOWN below the dock all end up here instead.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val drawer = drawerFragment
        if (drawer != null) {
            if (drawer.handleUnconsumedKey(keyCode)) return true
        } else {
            val home = homeFragment
            if (home != null && home.handleUnconsumedKey(keyCode)) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun openAppDrawer(source: View) {
        if (drawerFragment != null) return
        drawerSource = source
        supportFragmentManager.beginTransaction()
            .add(R.id.launcher_content, AppDrawerFragment(), TAG_DRAWER)
            .commit()
    }

    override fun closeAppDrawer() {
        val drawer = drawerFragment ?: return
        drawer.animateOut {
            if (drawer.isAdded) {
                supportFragmentManager.beginTransaction().remove(drawer).commitNow()
            }
            homeFragment?.restoreFocus(drawerSource)
            drawerSource = null
        }
    }

    /** Hides the system bars; a projector UI must own every pixel of the panel. */
    private fun applyImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.root).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private companion object {
        const val TAG_HOME = "home"
        const val TAG_DRAWER = "app_drawer"
    }
}
