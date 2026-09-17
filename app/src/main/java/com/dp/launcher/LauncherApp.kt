package com.dp.launcher

import android.app.Application
import com.dp.launcher.data.AppRepository

/**
 * Process-wide holder for the app list. A launcher is started constantly by the system, so
 * the installed-application index is kept warm in the application scope instead of being
 * rebuilt on every resume.
 */
class LauncherApp : Application() {

    val appRepository: AppRepository by lazy { AppRepository(this) }
}
