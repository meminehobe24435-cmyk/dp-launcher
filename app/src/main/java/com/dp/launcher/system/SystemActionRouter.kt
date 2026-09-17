package com.dp.launcher.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.dp.launcher.R

/** Actions that can be triggered from the bottom dock. */
enum class DockAction {
    KEYSTONE,
    MIRACAST,
    SIGNAL_SOURCE,
    MY_APPS,
    SETTINGS,
}

/**
 * Routes a dock button to the activity that actually implements the feature.
 *
 * Keystone correction, screen mirroring and input-source selection are vendor features, so
 * there is no single AOSP intent for them. Instead of hard-coding one vendor package, every
 * action owns a **candidate list** in `res/values/arrays.xml`. The OEM (or anyone porting this
 * launcher to another projector) edits that XML - no Kotlin change required.
 *
 * A candidate is one of:
 *  - `component:com.vendor.settings/.KeystoneActivity`
 *  - `action:android.settings.CAST_SETTINGS`
 *  - `action:com.vendor.action.INPUT_SOURCE|component:com.vendor.tv/.SourceActivity`
 *    (several alternatives separated by `|`, first resolvable one wins)
 *
 * When nothing resolves, [fallback] keeps the button useful instead of doing nothing.
 */
object SystemActionRouter {

    private const val COMPONENT_PREFIX = "component:"
    private const val ACTION_PREFIX = "action:"
    private const val ALTERNATIVE_SEPARATOR = '|'

    /**
     * Starts the activity bound to [action].
     *
     * @return `true` when an activity was started, `false` when the caller has to handle the
     *   action itself (the dock uses this for [DockAction.MY_APPS]).
     */
    fun launch(context: Context, action: DockAction): Boolean {
        if (action == DockAction.MY_APPS) return false

        val candidates = context.resources.getStringArray(action.candidateArrayRes())
        for (candidate in candidates) {
            for (alternative in candidate.split(ALTERNATIVE_SEPARATOR)) {
                val intent = alternative.toIntent() ?: continue
                if (startIfResolvable(context, intent)) return true
            }
        }
        return fallback(context, action)
    }

    private fun DockAction.candidateArrayRes(): Int = when (this) {
        DockAction.KEYSTONE -> R.array.keystone_intents
        DockAction.MIRACAST -> R.array.miracast_intents
        DockAction.SIGNAL_SOURCE -> R.array.signal_source_intents
        DockAction.SETTINGS -> R.array.settings_intents
        DockAction.MY_APPS -> R.array.settings_intents
    }

    private fun String.toIntent(): Intent? {
        val value = trim()
        if (value.isEmpty()) return null
        return when {
            value.startsWith(COMPONENT_PREFIX) -> componentIntent(value.removePrefix(COMPONENT_PREFIX))
            value.startsWith(ACTION_PREFIX) -> Intent(value.removePrefix(ACTION_PREFIX))
            value.startsWith("http") -> Intent(Intent.ACTION_VIEW, Uri.parse(value))
            else -> null
        }?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    }

    private fun componentIntent(flat: String): Intent? {
        val component = ComponentName.unflattenFromString(flat) ?: return null
        return Intent(Intent.ACTION_MAIN).setComponent(component)
    }

    /** True when some activity handles [intent] - explicit components are trusted as-is. */
    private fun startIfResolvable(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    /**
     * Last resort: open the closest generic AOSP page and tell the user what happened, so a
     * missing vendor implementation never looks like a broken button.
     */
    private fun fallback(context: Context, action: DockAction): Boolean {
        val generic = when (action) {
            DockAction.KEYSTONE, DockAction.MIRACAST -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }
        generic.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val started = runCatching { context.startActivity(generic) }.isSuccess
        if (!started) {
            Toast.makeText(context, R.string.action_unavailable, Toast.LENGTH_SHORT).show()
        }
        return started
    }
}
