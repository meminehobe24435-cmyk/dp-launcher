package com.dp.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.dp.launcher.R
import com.dp.launcher.data.model.AppEntry
import java.text.Collator
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the UI needs to know about installed apps. */
data class AppsSnapshot(
    val home: List<AppEntry> = emptyList(),
    val all: List<AppEntry> = emptyList(),
    val loaded: Boolean = false,
)

/**
 * Reads the installed application list from [PackageManager] and keeps it up to date while
 * packages are installed, removed or updated.
 *
 * All package-manager work happens on [Dispatchers.IO]; the snapshot is published on
 * [Dispatchers.Main] so collectors can touch views directly.
 */
class AppRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val registered = AtomicBoolean(false)
    private val packageManager: PackageManager = context.packageManager

    private val _snapshot = MutableStateFlow(AppsSnapshot())
    val snapshot: StateFlow<AppsSnapshot> = _snapshot.asStateFlow()

    /** Packages pinned to the home row, in order. Overridable per product via `arrays.xml`. */
    private val featuredPackages: List<String> by lazy {
        context.resources.getStringArray(R.array.featured_packages).toList()
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    /** Registers the package observer and publishes the first snapshot. Safe to call twice. */
    fun start() {
        if (registered.compareAndSet(false, true)) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            context.registerReceiver(packageReceiver, filter)
        }
        refresh()
    }

    /** Stops observing package changes. */
    fun stop() {
        if (registered.compareAndSet(true, false)) {
            runCatching { context.unregisterReceiver(packageReceiver) }
        }
    }

    /** Re-reads the installed apps off the main thread. */
    fun refresh() {
        scope.launch {
            val apps = queryLaunchableApps()
            val home = pickHomeRow(apps)
            withContext(Dispatchers.Main) {
                _snapshot.value = AppsSnapshot(home = home, all = apps, loaded = true)
            }
        }
    }

    private fun queryLaunchableApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        val collator = Collator.getInstance(Locale.getDefault())
        return resolved.asSequence()
            .mapNotNull { info -> info.toEntry() }
            .sortedWith { left, right -> collator.compare(left.label.toString(), right.label.toString()) }
            .toList()
    }

    private fun ResolveInfo.toEntry(): AppEntry? {
        // `ResolveInfo.activity` was removed from the public SDK in API 35; `activityInfo` is the
        // supported field and has been available since API 1.
        val info = activityInfo ?: return null
        if (info.packageName == context.packageName) return null
        val applicationInfo = info.applicationInfo ?: return null
        val icon = runCatching { loadIcon(packageManager) }.getOrNull() ?: return null
        val entry = AppEntry(
            packageName = info.packageName,
            activityName = info.name,
            label = runCatching { loadLabel(packageManager) }.getOrNull() ?: info.packageName,
            icon = icon,
            cardColor = 0,
            isSystemApp = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
        )
        return entry.copy(cardColor = AppColorResolver.resolve(context, entry, icon))
    }

    /**
     * Home row = the featured packages that are actually installed, filled up with the
     * alphabetically first apps of the remaining list.
     */
    private fun pickHomeRow(apps: List<AppEntry>): List<AppEntry> {
        val featured = featuredPackages.mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }
        if (featured.size >= HOME_ROW_SIZE) return featured.take(HOME_ROW_SIZE)
        val remainder = apps.filterNot { app -> featured.any { it.key == app.key } }
        return (featured + remainder).take(HOME_ROW_SIZE)
    }

    companion object {
        /** Number of cards on the home row, measured from the reference design. */
        const val HOME_ROW_SIZE = 4
    }
}
