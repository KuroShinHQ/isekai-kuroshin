package com.example.isekaikuroshin.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.isekaikuroshin.data.PersistentDataManager

/**
 * Animasyon ayarlarını kontrol ederek animasyon spec döndürür
 * Animasyonlar kapalıysa 0ms duration döner (animasyon yok)
 */
@Composable
fun <T> rememberAnimationSpec(
    durationMillis: Int = 300,
    delayMillis: Int = 0
): AnimationSpec<T> {
    val gameData by PersistentDataManager.gameData.collectAsState()
    val animationsEnabled = gameData.settingsData.uiSettings.enableAnimations
    val animationSpeed = gameData.settingsData.uiSettings.animationSpeed

    return if (animationsEnabled) {
        tween(
            durationMillis = (durationMillis / animationSpeed).toInt(),
            delayMillis = delayMillis
        )
    } else {
        tween(durationMillis = 0) // Animasyon yok
    }
}

/**
 * Haptic feedback tetikler (ayar kontrolü ile)
 */
@Composable
fun rememberHapticPerformer(): HapticPerformer {
    val context = LocalContext.current
    val gameData by PersistentDataManager.gameData.collectAsState()
    val hapticEnabled = gameData.settingsData.uiSettings.enableHapticFeedback

    return HapticPerformer(context, hapticEnabled)
}

class HapticPerformer(
    private val context: Context,
    private val enabled: Boolean
) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Kısa titreşim (buton tıklamaları için)
     */
    fun performClick() {
        if (!enabled) return
        vibrate(50)
    }

    /**
     * Orta titreşim (önemli aksiyonlar için)
     */
    fun performHeavyClick() {
        if (!enabled) return
        vibrate(100)
    }

    /**
     * Uzun titreşim (uyarılar için)
     */
    fun performLongPress() {
        if (!enabled) return
        vibrate(200)
    }

    /**
     * Başarı titreşimi (2 kısa titreşim)
     */
    fun performSuccess() {
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 50, 100, 50)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 100, 50), -1)
        }
    }

    /**
     * Hata titreşimi (3 kısa titreşim)
     */
    fun performError() {
        if (!enabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 50, 50, 50, 50, 50)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 50, 50, 50, 50), -1)
        }
    }

    private fun vibrate(duration: Long) {
        // TODO-FIX-03: İzin kontrolü - uygulamanın çökmesini önler
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            // İzin verilmemişse sessizce geri dön
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(duration)
        }
    }
}
