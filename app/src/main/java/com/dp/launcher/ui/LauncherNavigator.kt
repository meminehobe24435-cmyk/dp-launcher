package com.dp.launcher.ui

import android.view.View

/**
 * Navigation contract between the home screen, the app drawer and the activity that hosts both.
 * Keeping it here avoids the fragments having to know about each other.
 */
interface LauncherNavigator {

    /**
     * Shows the list of every installed app.
     *
     * @param source the view that had the focus when the drawer was requested, so the focus can
     *   be restored exactly where the user left it.
     */
    fun openAppDrawer(source: View)

    /** Hides the drawer and restores the focus on [openAppDrawer]'s source. */
    fun closeAppDrawer()
}
