package com.dp.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Live clock for the status bar.
 *
 * Uses the reference design's formats - `8:08 AM` and `Saturday,November 11` - and refreshes
 * on `ACTION_TIME_TICK` (emitted every minute) plus on manual time/timezone changes.
 * `ACTION_TIME_TICK` cannot be declared in the manifest, so the receiver lives here and is
 * registered by the hosting view.
 *
 * The context is kept (application context only) because the time format depends on the user's
 * 12/24-hour setting: `DateFormat.is24HourFormat` needs a real context and crashes on null.
 */
class StatusClock(
    context: Context,
    private val onTick: (time: String, date: String) -> Unit,
) : BroadcastReceiver() {

    private val appContext: Context = context.applicationContext

    private val dateFormat = SimpleDateFormat(DATE_PATTERN, Locale.US)

    override fun onReceive(context: Context?, intent: Intent?) = emit()

    /** Registers the tick receiver and pushes the current time once. */
    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        appContext.registerReceiver(this, filter)
        emit()
    }

    /** Unregisters the receiver. Safe to call when [start] was never called. */
    fun stop() {
        runCatching { appContext.unregisterReceiver(this) }
    }

    /** Pushes the current time to the listener. */
    fun emit() {
        val now = Date()
        val pattern = if (DateFormat.is24HourFormat(appContext)) TIME_PATTERN_24H else TIME_PATTERN
        onTick(
            SimpleDateFormat(pattern, Locale.US).format(now),
            dateFormat.format(now),
        )
    }

    private companion object {
        const val TIME_PATTERN = "h:mm a"
        const val TIME_PATTERN_24H = "H:mm"
        const val DATE_PATTERN = "EEEE,MMMM d"
    }
}
