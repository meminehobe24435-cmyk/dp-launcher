package com.dp.launcher.ui.home

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dp.launcher.LauncherApp
import com.dp.launcher.R
import com.dp.launcher.data.AppRepository
import com.dp.launcher.data.AppsSnapshot
import com.dp.launcher.data.model.AppEntry
import com.dp.launcher.databinding.FragmentHomeBinding
import com.dp.launcher.system.DockAction
import com.dp.launcher.system.InputDeviceMonitor
import com.dp.launcher.system.NetworkStatusMonitor
import com.dp.launcher.system.StatusClock
import com.dp.launcher.system.SystemActionRouter
import com.dp.launcher.ui.DesignSpec
import com.dp.launcher.ui.LauncherNavigator
import com.dp.launcher.ui.UiScale
import com.dp.launcher.ui.widget.AppCardView
import kotlinx.coroutines.launch

/**
 * Home screen of the launcher: status bar, featured app row (with reflections) and the dock.
 *
 * Navigation follows the two rules of the reference design:
 *  - the dock is reachable from the app row with DOWN, the app row with UP;
 *  - **every** focus position can jump into the full installed-app list - with DOWN on the dock,
 *    UP on the app row, the MENU key, a long press on a card, or the "My Apps" dock button.
 */
class HomeFragment : Fragment() {

    private var binding: FragmentHomeBinding? = null
    private lateinit var ui: UiScale
    private lateinit var cardAdapter: AppCardAdapter
    private lateinit var dockAdapter: DockAdapter

    private var clock: StatusClock? = null
    private var networkMonitor: NetworkStatusMonitor? = null
    private var inputMonitor: InputDeviceMonitor? = null

    private var lastCardIndex = 0
    private var lastDockIndex = DockItem.SETTINGS.ordinal
    private var entrancePlayed = false

    private val repository: AppRepository
        get() = (requireActivity().application as LauncherApp).appRepository

    private val navigator: LauncherNavigator?
        get() = activity as? LauncherNavigator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val viewBinding = FragmentHomeBinding.inflate(inflater, container, false)
        binding = viewBinding
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ui = UiScale(requireContext())
        setupStatusBar()
        setupAppRow()
        setupDockRow()
        setupEdgeNavigation()
        observeApps()
    }

    override fun onStart() {
        super.onStart()
        clock = StatusClock { time, date ->
            binding?.let {
                it.statusTime.text = time
                it.statusDate.text = date
            }
        }.also { it.start(requireContext()) }

        networkMonitor = NetworkStatusMonitor(requireContext()) { connected, level, _ ->
            binding?.statusWifi?.setImageResource(wifiIconFor(connected, level))
        }.also { it.start() }

        inputMonitor = InputDeviceMonitor(requireContext()) { attached ->
            binding?.statusPointer?.isVisible = attached
        }.also { it.start() }

        repository.start()
    }

    override fun onStop() {
        clock?.stop(requireContext())
        clock = null
        networkMonitor?.stop()
        networkMonitor = null
        inputMonitor?.stop()
        inputMonitor = null
        super.onStop()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    // ------------------------------------------------------------------ setup

    private fun setupStatusBar() {
        val viewBinding = binding ?: return
        val barHeight = ui.pxInt(STATUS_BAR_HEIGHT)
        viewBinding.statusBar.layoutParams = (viewBinding.statusBar.layoutParams as ViewGroup.MarginLayoutParams).apply {
            height = barHeight
            topMargin = ui.pxInt(DesignSpec.STATUS_CENTER_Y) - barHeight / 2
            marginEnd = ui.pxInt(DesignSpec.STATUS_END_MARGIN)
        }
        viewBinding.statusPointer.layoutParams = ViewGroup.LayoutParams(
            ui.pxInt(DesignSpec.STATUS_POINTER_WIDTH),
            ui.pxInt(DesignSpec.STATUS_POINTER_HEIGHT),
        )
        viewBinding.statusWifi.layoutParams = ViewGroup.LayoutParams(
            ui.pxInt(DesignSpec.STATUS_WIFI_WIDTH),
            ui.pxInt(DesignSpec.STATUS_WIFI_HEIGHT),
        )
        ui.applyTextSize(viewBinding.statusTime, DesignSpec.STATUS_TEXT_SIZE)
        ui.applyTextSize(viewBinding.statusDate, DesignSpec.STATUS_TEXT_SIZE)
        // See DesignSpec.STATUS_TEXT_NUDGE: the reference font metrics sit 3 ref-px lower.
        val nudge = ui.pxInt(DesignSpec.STATUS_TEXT_NUDGE)
        viewBinding.statusTime.setPadding(0, nudge, 0, 0)
        viewBinding.statusDate.setPadding(0, nudge, 0, 0)

        val gap = ui.pxInt(DesignSpec.STATUS_ITEM_GAP)
        listOf(viewBinding.statusPointer, viewBinding.statusWifi, viewBinding.statusTime).forEach { item ->
            (item.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = gap
        }
    }

    private fun setupAppRow() {
        val viewBinding = binding ?: return
        cardAdapter = AppCardAdapter(
            ui = ui,
            onCardClick = ::launchApp,
            onCardLongClick = { openAppDrawer(viewBinding.appRow) },
        )
        val focusOverflow = ui.pxInt(DesignSpec.CARD_HEIGHT * (DesignSpec.CARD_FOCUS_SCALE - 1f) / 2f)
        viewBinding.appRow.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = cardAdapter
            itemAnimator = null
            setHasFixedSize(true)
            clipToPadding = false
            clipChildren = false
            setPadding(
                horizontalContentPadding(DesignSpec.HOME_ROW_WIDTH),
                focusOverflow,
                horizontalContentPadding(DesignSpec.HOME_ROW_WIDTH),
                focusOverflow,
            )
        }
        viewBinding.appRow.layoutParams = (viewBinding.appRow.layoutParams as ViewGroup.MarginLayoutParams).apply {
            topMargin = ui.pxInt(DesignSpec.CARD_ROW_TOP) - focusOverflow
        }
        viewBinding.appRow.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) lastCardIndex = focusedPosition(viewBinding.appRow, lastCardIndex)
        }
    }

    private fun setupDockRow() {
        val viewBinding = binding ?: return
        dockAdapter = DockAdapter(ui) { item -> onDockItemSelected(item) }
        val focusOverflow = ui.pxInt(DesignSpec.DOCK_ITEM_HEIGHT * (DesignSpec.DOCK_FOCUS_SCALE - 1f) / 2f)
        viewBinding.dockRow.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = dockAdapter
            itemAnimator = null
            setHasFixedSize(true)
            clipToPadding = false
            clipChildren = false
            setPadding(
                horizontalContentPadding(DesignSpec.DOCK_ROW_WIDTH),
                focusOverflow,
                horizontalContentPadding(DesignSpec.DOCK_ROW_WIDTH),
                focusOverflow,
            )
        }
        viewBinding.dockRow.layoutParams = (viewBinding.dockRow.layoutParams as ViewGroup.MarginLayoutParams).apply {
            topMargin = ui.pxInt(DesignSpec.DOCK_ROW_TOP) - focusOverflow
        }
        viewBinding.dockRow.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) lastDockIndex = focusedPosition(viewBinding.dockRow, lastDockIndex)
        }
    }

    /**
     * Edge navigation. The MENU key is handled on the root so it works from *any* focus
     * position, which is exactly what the reference asks for ("任意焦点可以跳转到全部已安装
     * APPLIST 里面").
     */
    private fun setupEdgeNavigation() {
        val viewBinding = binding ?: return
        viewBinding.appRow.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    moveFocusFromCardsToDock()
                    true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    openAppDrawer(viewBinding.appRow)
                    true
                }

                else -> false
            }
        }
        viewBinding.dockRow.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    openAppDrawer(viewBinding.dockRow)
                    true
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    moveFocusFromDockToCards()
                    true
                }

                else -> false
            }
        }
        viewBinding.root.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_INFO) {
                openAppDrawer(viewBinding.root)
                true
            } else {
                false
            }
        }
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
        cardAdapter.submit(snapshot.home)
        if (!snapshot.loaded) return
        if (!entrancePlayed) {
            entrancePlayed = true
            playEntranceAnimation()
        }
        if (viewBinding.root.findFocus() == null) {
            focusDefault()
        }
    }

    // ------------------------------------------------------------------ actions

    private fun onDockItemSelected(item: DockItem) {
        val viewBinding = binding ?: return
        if (item.action == DockAction.MY_APPS) {
            openAppDrawer(viewBinding.dockRow)
            return
        }
        if (!SystemActionRouter.launch(requireContext(), item.action)) {
            Toast.makeText(requireContext(), R.string.action_unavailable, Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchApp(app: AppEntry) {
        binding?.appRow?.findFocus()?.let { (it as? AppCardView)?.playPressFeedback() }
        val started = runCatching { startActivity(app.launchIntent()) }.isSuccess
        if (!started) {
            Toast.makeText(requireContext(), R.string.app_launch_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAppDrawer(source: View) {
        navigator?.openAppDrawer(source)
    }

    // ------------------------------------------------------------------ focus

    /** Focuses the card that was focused last, or the first card on a cold start. */
    fun focusDefault() {
        binding?.appRow?.let { row ->
            if (row.adapter?.itemCount == 0) return
            row.post {
                if (!row.hasFocus() && row.findFocus() == null) {
                    cardAdapter.focusCard(lastCardIndex)
                }
            }
        }
    }

    /** Restores the focus that was active before the drawer was opened. */
    fun restoreFocus(source: View?) {
        val viewBinding = binding ?: return
        val target = when {
            source == null -> null
            source.id == R.id.dock_row -> viewBinding.dockRow
            else -> viewBinding.appRow
        }
        if (target == null) {
            focusDefault()
            return
        }
        target.post {
            if (target === viewBinding.dockRow) {
                dockAdapter.focusItem(lastDockIndex)
            } else {
                cardAdapter.focusCard(lastCardIndex)
            }
        }
    }

    private fun moveFocusFromCardsToDock() {
        val viewBinding = binding ?: return
        lastCardIndex = focusedPosition(viewBinding.appRow, lastCardIndex)
        dockAdapter.focusItem(lastCardIndex)
    }

    private fun moveFocusFromDockToCards() {
        val viewBinding = binding ?: return
        lastDockIndex = focusedPosition(viewBinding.dockRow, lastDockIndex)
        cardAdapter.focusCard(lastDockIndex)
    }

    private fun focusedPosition(row: RecyclerView, fallback: Int): Int {
        val focused = row.findFocus() ?: return fallback
        val holder = row.findContainingViewHolder(focused)
        return holder?.bindingAdapterPosition?.takeIf { it != RecyclerView.NO_POSITION } ?: fallback
    }

    // ------------------------------------------------------------------ helpers

    private fun horizontalContentPadding(contentWidth: Float): Int =
        ((ui.px(DesignSpec.REFERENCE_WIDTH) - ui.px(contentWidth)) / 2f).toInt().coerceAtLeast(0)

    private fun playEntranceAnimation() {
        val viewBinding = binding ?: return
        val offset = ui.px(ENTRANCE_OFFSET)
        listOf(viewBinding.appRow to 0L, viewBinding.dockRow to ENTRANCE_DELAY_MS).forEach { (view, delay) ->
            view.alpha = 0f
            view.translationY = offset
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(ENTRANCE_DURATION_MS)
                .start()
        }
    }

    private fun wifiIconFor(connected: Boolean, level: Int): Int = when {
        !connected -> R.drawable.ic_wifi_off
        level <= 1 -> R.drawable.ic_wifi_level_1
        level == 2 -> R.drawable.ic_wifi_level_2
        level == 3 -> R.drawable.ic_wifi_level_3
        else -> R.drawable.ic_wifi_level_4
    }

    private companion object {
        const val STATUS_BAR_HEIGHT = 40f
        const val ENTRANCE_OFFSET = 28f
        const val ENTRANCE_DURATION_MS = 320L
        const val ENTRANCE_DELAY_MS = 90L
    }
}
