package com.dp.launcher.ui.drawer

import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dp.launcher.LauncherApp
import com.dp.launcher.R
import com.dp.launcher.data.AppRepository
import com.dp.launcher.data.AppsSnapshot
import com.dp.launcher.data.model.AppEntry
import com.dp.launcher.databinding.FragmentAppDrawerBinding
import com.dp.launcher.ui.DesignSpec
import com.dp.launcher.ui.LauncherNavigator
import com.dp.launcher.ui.UiScale
import kotlinx.coroutines.launch

/**
 * The "全部已安装应用列表" (all installed apps) screen.
 *
 * It is an overlay fragment on top of [com.dp.launcher.ui.home.HomeFragment]: the home screen
 * stays visible behind the scrim, and closing the drawer restores the focus that opened it.
 *
 * Focus rules:
 *  - opening the drawer moves the focus to the first app, so a D-pad never lands on nothing;
 *  - BACK and UP from the first row close it again;
 *  - the list follows package install/uninstall events live, like the home row.
 */
class AppDrawerFragment : Fragment() {

    private var binding: FragmentAppDrawerBinding? = null
    private lateinit var ui: UiScale
    private lateinit var adapter: AllAppsAdapter
    private var entered = false

    private val navigator: LauncherNavigator?
        get() = activity as? LauncherNavigator

    private val repository: AppRepository
        get() = (requireActivity().application as LauncherApp).appRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val viewBinding = FragmentAppDrawerBinding.inflate(inflater, container, false)
        binding = viewBinding
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui = UiScale(requireContext())
        setupGrid()
        setupKeys()
        observeApps()
        playEntranceAnimation()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun setupGrid() {
        val viewBinding = binding ?: return
        adapter = AllAppsAdapter(ui, ::launchApp)
        val focusOverflow = ui.pxInt(
            DesignSpec.DRAWER_GRID_ITEM_HEIGHT * (GRID_FOCUS_SCALE - 1f) / 2f,
        )
        viewBinding.drawerGrid.apply {
            layoutManager = GridLayoutManager(context, DesignSpec.DRAWER_GRID_COLUMNS)
            adapter = this@AppDrawerFragment.adapter
            itemAnimator = null
            clipToPadding = false
            clipChildren = false
            setPadding(
                ui.pxInt(DesignSpec.DRAWER_HORIZONTAL_PADDING),
                focusOverflow,
                ui.pxInt(DesignSpec.DRAWER_HORIZONTAL_PADDING),
                focusOverflow,
            )
        }
        viewBinding.drawerGrid.layoutParams = (viewBinding.drawerGrid.layoutParams as ViewGroup.MarginLayoutParams).apply {
            topMargin = ui.pxInt(DesignSpec.DRAWER_TOP) - focusOverflow
        }
        ui.applyTextSize(viewBinding.drawerTitle, DesignSpec.DRAWER_TITLE_SIZE)
        ui.applyTextSize(viewBinding.drawerCount, DesignSpec.DRAWER_HINT_SIZE)
        ui.applyTextSize(viewBinding.drawerHint, DesignSpec.DRAWER_HINT_SIZE)

        viewBinding.drawerHeader.layoutParams =
            (viewBinding.drawerHeader.layoutParams as FrameLayout.LayoutParams).apply {
                topMargin = ui.pxInt(DesignSpec.DRAWER_TITLE_TOP)
                marginStart = ui.pxInt(DesignSpec.DRAWER_HORIZONTAL_PADDING)
            }
        viewBinding.drawerHint.layoutParams =
            (viewBinding.drawerHint.layoutParams as FrameLayout.LayoutParams).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                bottomMargin = ui.pxInt(DesignSpec.DRAWER_HINT_BOTTOM)
                marginStart = ui.pxInt(DesignSpec.DRAWER_HORIZONTAL_PADDING)
            }
    }

    private fun setupKeys() {
        val viewBinding = binding ?: return
        viewBinding.root.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && isOnFirstRow()) {
                close()
                true
            } else {
                false
            }
        }
    }

    /** Fades the drawer out, then reports completion so the host can remove the fragment. */
    fun animateOut(onFinished: () -> Unit) {
        val viewBinding = binding
        if (viewBinding == null) {
            onFinished()
            return
        }
        viewBinding.drawerGrid.animate()
            .alpha(0f)
            .translationY(ui.px(ENTRANCE_OFFSET))
            .setDuration(EXIT_DURATION_MS)
            .start()
        viewBinding.drawerScrim.animate()
            .alpha(0f)
            .setDuration(EXIT_DURATION_MS)
            .withEndAction(onFinished)
            .start()
    }

    private fun observeApps() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.snapshot.collect { snapshot -> render(snapshot) }
            }
        }
    }

    private fun render(snapshot: AppsSnapshot) {
        val viewBinding = binding ?: return
        adapter.submit(snapshot.all)
        viewBinding.drawerCount.text = resources.getQuantityString(
            R.plurals.drawer_app_count,
            snapshot.all.size,
            snapshot.all.size,
        )
        if (!entered && snapshot.loaded && snapshot.all.isNotEmpty()) {
            entered = true
            viewBinding.drawerGrid.post {
                viewBinding.drawerGrid.layoutManager?.findViewByPosition(0)?.requestFocus()
            }
        }
    }

    private fun playEntranceAnimation() {
        val viewBinding = binding ?: return
        viewBinding.drawerScrim.alpha = 0f
        viewBinding.drawerGrid.alpha = 0f
        viewBinding.drawerGrid.translationY = ui.px(ENTRANCE_OFFSET)
        viewBinding.drawerScrim.animate().alpha(1f).setDuration(ENTRANCE_DURATION_MS).start()
        viewBinding.drawerGrid.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(ENTRANCE_DURATION_MS)
            .start()
    }

    private fun isOnFirstRow(): Boolean {
        val viewBinding = binding ?: return true
        val position = viewBinding.drawerGrid.findFocus()
            ?.let { viewBinding.drawerGrid.findContainingViewHolder(it)?.bindingAdapterPosition }
            ?: return true
        return position < DesignSpec.DRAWER_GRID_COLUMNS
    }

    private fun launchApp(app: AppEntry) {
        val started = runCatching { startActivity(app.launchIntent()) }.isSuccess
        if (!started) {
            Toast.makeText(requireContext(), R.string.app_launch_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun close() {
        navigator?.closeAppDrawer()
    }

    private companion object {
        const val GRID_FOCUS_SCALE = 1.10f
        const val ENTRANCE_OFFSET = 24f
        const val ENTRANCE_DURATION_MS = 220L
        const val EXIT_DURATION_MS = 150L
    }
}
