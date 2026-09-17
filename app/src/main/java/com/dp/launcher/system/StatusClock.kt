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
 */
class StatusClock(private val onTick: (time: String, date: String) -> Unit) : BroadcastReceiver() {

    private val timeFormat = SimpleDateFormat(TIME_PATTERN, Locale.US)
    private val dateFormat = SimpleDateFormat(DATE_PATTERN, Locale.US)

    override fun onReceive(context: Context?, intent: Intent?) = emit()

    fun start(context: Context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
        }
        context.registerReceiver(this, filter)
        emit()
    }

    fun stop(context: Context) {
        runCatching { context.unregisterReceiver(this) }
    }

    /** Pushes the current time to the listener. */
    fun emit() {
        val now = Date()
        val use24Hour = DateFormat.is24HourFormat(null)
        val timePattern = if (use24Hour) TIME_PATTERN_24H else TIME_PATTERN
        onTick(
            SimpleDateFormat(timePattern, Locale.US).format(now),
            dateFormat.format(now),
        )
    }

    private companion object {
        const val TIME_PATTERN = "h:mm a"
        const val TIME_PATTERN_24H = "H:mm"
        const val DATE_PATTERN = "EEEE,MMMM d"
    }
}
