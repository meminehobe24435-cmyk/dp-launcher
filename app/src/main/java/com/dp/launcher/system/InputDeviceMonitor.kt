package com.dp.launcher.system

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import androidx.core.content.getSystemService

/**
 * Tracks whether a pointer device (mouse, touchpad, trackball, air-mouse remote) is attached.
 *
 * The reference design shows that indicator in the status bar, right before the wifi icon.
 * Some TV sticks only report hot-plug events, so the state is additionally re-checked on a
 * slow heartbeat while the home screen is visible.
 */
class InputDeviceMonitor(
    private val context: Context,
    private val onChanged: (pointerAttached: Boolean) -> Unit,
) : InputManager.InputDeviceListener {

    private val inputManager: InputManager? = context.getSystemService()
    private val handler = Handler(Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() {
            publish()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    fun start() {
        inputManager?.registerInputDeviceListener(this, handler)
        handler.post(poll)
    }

    fun stop() {
        handler.removeCallbacks(poll)
        inputManager?.unregisterInputDeviceListener(this)
    }

    override fun onInputDeviceAdded(deviceId: Int) = publish()

    override fun onInputDeviceRemoved(deviceId: Int) = publish()

    override fun onInputDeviceChanged(deviceId: Int) = publish()

    private fun publish() {
        onChanged(hasPointerDevice())
    }

    private fun hasPointerDevice(): Boolean {
        val manager = inputManager ?: return false
        return manager.inputDeviceIds.any { id ->
            val device = manager.getInputDevice(id) ?: return@any false
            if (!device.isEnabled) return@any false
            val sources = device.sources
            POINTER_SOURCES.any { source -> (sources and source) == source }
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 10_000L

        val POINTER_SOURCES = intArrayOf(
            InputDevice.SOURCE_MOUSE,
            InputDevice.SOURCE_TOUCHPAD,
            InputDevice.SOURCE_TRACKBALL,
            InputDevice.SOURCE_STYLUS,
        )
    }
}
