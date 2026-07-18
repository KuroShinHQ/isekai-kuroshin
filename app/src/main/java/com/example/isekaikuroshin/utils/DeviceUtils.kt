package com.example.isekaikuroshin.utils

import android.os.Build
import android.util.Log

/**
 * Device utility functions for detecting device characteristics
 */
object DeviceUtils {

    private const val TAG = "DeviceUtils"

    /**
     * Detects if the app is running on an Android emulator
     * @return true if running on emulator, false if running on real device
     */
    fun isEmulator(): Boolean {
        val result = (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.DEVICE.contains("sdk_gphone")
                || Build.DEVICE.contains("goldfish")
                || Build.DEVICE.contains("ranchu")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))

        if (result) {
            Log.w(TAG, "🤖 Running on EMULATOR - Device: ${Build.MODEL}, Hardware: ${Build.HARDWARE}, Fingerprint: ${Build.FINGERPRINT}")
        } else {
            Log.d(TAG, "📱 Running on REAL DEVICE - Device: ${Build.MODEL}, Manufacturer: ${Build.MANUFACTURER}")
        }

        return result
    }
}
