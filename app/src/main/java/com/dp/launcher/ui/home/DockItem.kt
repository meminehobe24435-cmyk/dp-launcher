package com.dp.launcher.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.dp.launcher.R
import com.dp.launcher.system.DockAction

/**
 * The five fixed buttons of the bottom bar, in the order of the reference design.
 */
enum class DockItem(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val action: DockAction,
) {
    KEYSTONE(R.string.dock_keystone, R.drawable.ic_dock_keystone, DockAction.KEYSTONE),
    MIRACAST(R.string.dock_miracast, R.drawable.ic_dock_miracast, DockAction.MIRACAST),
    SIGNAL_SOURCE(R.string.dock_signal_source, R.drawable.ic_dock_signal_source, DockAction.SIGNAL_SOURCE),
    MY_APPS(R.string.dock_my_apps, R.drawable.ic_dock_my_apps, DockAction.MY_APPS),
    SETTINGS(R.string.dock_settings, R.drawable.ic_dock_settings, DockAction.SETTINGS),
}
