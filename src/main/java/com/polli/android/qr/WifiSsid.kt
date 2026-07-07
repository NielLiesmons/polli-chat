package com.polli.android.qr

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log

/** Best-effort current Wi-Fi SSID, used to hint that both devices are on the same network. */
object WifiSsid {
    private const val TAG = "WifiSsid"

    /** Returns the SSID or null when unavailable / insufficient permissions. Call off the main thread. */
    fun current(context: Context): String? {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return null
            if (!wifiManager.isWifiEnabled) return null
            @Suppress("DEPRECATION")
            val ssid = wifiManager.connectionInfo?.ssid
            // "<unknown ssid>" is returned on insufficient rights.
            if (ssid.isNullOrEmpty() || ssid == "<unknown ssid>") null else ssid
        } catch (e: Exception) {
            Log.w(TAG, "failed to read wifi ssid", e)
            null
        }
    }
}
