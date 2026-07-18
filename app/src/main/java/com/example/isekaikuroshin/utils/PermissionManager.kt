package com.example.isekaikuroshin.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.isekaikuroshin.data.PersistentDataManager
import android.util.Log

/**
 * G57: Runtime Permissions Implementation
 *
 * Yönetilen İzinler:
 * - CAMERA: Pose detection, hand gesture recognition
 * - RECORD_AUDIO: Voice commands for jutsu
 * - POST_NOTIFICATIONS: Quest updates, reminders (Android 13+)
 */
object PermissionManager {

    /**
     * Gerekli izinler listesi
     */
    val REQUIRED_PERMISSIONS = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)

        // Android 13+ için bildirim izni
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Tüm izinlerin verilip verilmediğini kontrol et
     */
    fun hasAllPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Hangi izinlerin verilmediğini bul
     */
    fun getMissingPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * İzin durumunu PersistentDataManager'a kaydet
     */
    fun savePermissionStatus(
        cameraGranted: Boolean,
        audioGranted: Boolean,
        notificationsGranted: Boolean
    ) {
        PersistentDataManager.updateSettingsData { settings ->
            settings.copy(
                permissionsGranted = settings.permissionsGranted.copy(
                    cameraPermission = cameraGranted,
                    audioPermission = audioGranted,
                    notificationPermission = notificationsGranted
                )
            )
        }

        Log.d("PermissionManager", "✅ G57: İzin durumları kaydedildi - Camera: $cameraGranted, Audio: $audioGranted, Notifications: $notificationsGranted")
    }

    /**
     * İzin istek sonuçlarını işle
     */
    fun handlePermissionResults(
        context: Context,
        permissions: Map<String, Boolean>
    ) {
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            true // Android 12 ve altında bildirim izni otomatik
        }

        savePermissionStatus(cameraGranted, audioGranted, notificationsGranted)

        // Kullanıcıya feedback
        if (hasAllPermissions(context)) {
            Log.d("PermissionManager", "✅ G57: Tüm izinler verildi!")
        } else {
            val missing = getMissingPermissions(context)
            Log.w("PermissionManager", "⚠️ G57: Eksik izinler: ${missing.joinToString()}")
        }
    }
}

/**
 * Compose composable: İzin durumunu takip et ve iste
 *
 * Kullanım:
 * ```kotlin
 * PermissionRequester(
 *     onAllPermissionsGranted = { navController.navigate("next_screen") },
 *     onPermissionsDenied = { missingPermissions -> /* Handle */ }
 * )
 * ```
 */
@Composable
fun PermissionRequester(
    onAllPermissionsGranted: () -> Unit,
    onPermissionsDenied: (List<String>) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // İzin isteği launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        PermissionManager.handlePermissionResults(context, permissions)

        if (PermissionManager.hasAllPermissions(context)) {
            onAllPermissionsGranted()
        } else {
            onPermissionsDenied(PermissionManager.getMissingPermissions(context))
        }
    }

    // İlk yüklendiğinde izinleri kontrol et
    LaunchedEffect(Unit) {
        if (PermissionManager.hasAllPermissions(context)) {
            Log.d("PermissionRequester", "✅ G57: İzinler zaten verilmiş, devam ediliyor")
            onAllPermissionsGranted()
        } else {
            Log.d("PermissionRequester", "📋 G57: İzinler isteniyor...")
            permissionLauncher.launch(PermissionManager.REQUIRED_PERMISSIONS.toTypedArray())
        }
    }
}

/**
 * Compose composable: Manuel izin isteme butonu
 *
 * Kullanım: Settings ekranında veya tutorial'da
 */
@Composable
fun RequestPermissionsButton(
    onPermissionsResult: (Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        PermissionManager.handlePermissionResults(context, permissions)
        onPermissionsResult(PermissionManager.hasAllPermissions(context))
    }

    androidx.compose.material3.Button(
        onClick = {
            permissionLauncher.launch(PermissionManager.REQUIRED_PERMISSIONS.toTypedArray())
        }
    ) {
        androidx.compose.material3.Text("İzinleri Talep Et")
    }
}
