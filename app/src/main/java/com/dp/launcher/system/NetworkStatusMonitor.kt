package com.dp.launcher.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.getSystemService

/**
 * Reports whether the projector is connected to a Wi-Fi network and how strong the signal is.
 *
 * A [ConnectivityManager.NetworkCallback] restricted to the Wi-Fi transport drives the state;
 * the signal level is read from [WifiManager] and mapped to the five-step icon of the design.
 */
class NetworkStatusMonitor(
    private val context: Context,
    private val onChanged: (connected: Boolean, level: Int, maxLevel: Int) -> Unit,
) {

    private val connectivityManager: ConnectivityManager? = context.getSystemService()

    private var connected = false

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            connected = true
            publish()
        }

        override fun onLost(network: Network) {
            connected = false
            publish()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            connected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            publish()
        }
    }

    fun start() {
        val manager = connectivityManager ?: return
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        runCatching { manager.registerNetworkCallback(request, callback) }
        publish()
    }

    fun stop() {
        runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
        connected = false
    }

    private fun publish() {
        onChanged(connected, if (connected) signalLevel() else 0, MAX_LEVEL)
    }

    @Suppress("DEPRECATION")
    private fun signalLevel(): Int {
        val wifiManager = context.applicationContext.getSystemService<WifiManager>() ?: return MAX_LEVEL
        val rssi = runCatching { wifiManager.connectionInfo?.rssi }.getOrNull() ?: return MAX_LEVEL
        return levelForRssi(rssi)
    }

    /**
     * Maps dBm to the five steps of the design. Thresholds follow the usual Android
     * `WifiManager.calculateSignalLevel` buckets, inlined here to avoid the deprecated API.
     */
    private fun levelForRssi(rssi: Int): Int = when {
        rssi >= -55 -> 4
        rssi >= -67 -> 3
        rssi >= -78 -> 2
        rssi >= -88 -> 1
        else -> 0
    }

    private companion object {
        /** Number of steps of the wifi icon drawn in `ic_wifi_*.xml`. */
        const val MAX_LEVEL = 4
    }
}
