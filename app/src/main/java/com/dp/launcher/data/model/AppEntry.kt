package com.dp.launcher.data.model

import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Drawable

/**
 * One launchable activity, ready to be rendered by the home row or the app drawer.
 */
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: CharSequence,
    val icon: Drawable,
    /** Background colour of the home card, resolved once when the list is loaded. */
    val cardColor: Int,
    val isSystemApp: Boolean,
) {

    /** Stable identity used for view recycling and list diffing. */
    val key: String get() = "$packageName/$activityName"

    /**
     * Intent that starts the app. `FLAG_ACTIVITY_NEW_TASK` is required because a launcher
     * starts activities outside of its own task.
     */
    fun launchIntent(): Intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        component = ComponentName(packageName, activityName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
    }

    override fun equals(other: Any?): Boolean = other is AppEntry && other.key == key

    override fun hashCode(): Int = key.hashCode()
}
