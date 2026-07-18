package com.example.isekaikuroshin.ui.components

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.isekaikuroshin.data.rememberLocalizedText
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.example.isekaikuroshin.utils.DeviceUtils
import java.util.concurrent.Executors

/**
 * Shared Camera Preview Component
 *
 * Generic camera preview component that can work with different ML detectors
 * (PoseDetectorHelper, HandDetectorHelper, etc.)
 *
 * Usage:
 * ```
 * CameraPreview(
 *     lifecycleOwner = lifecycleOwner,
 *     imageAnalyzer = { imageProxy ->
 *         // Process image with your detector
 *         val result = detector.detect(imageProxy)
 *         imageProxy.close()
 *     },
 *     onViewSizeChanged = { width, height ->
 *         // Update overlay dimensions
 *     }
 * )
 * ```
 */
@OptIn(ExperimentalPermissionsApi::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun CameraPreview(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    imageAnalyzer: (androidx.camera.core.ImageProxy) -> Unit,
    onViewSizeChanged: (Float, Float) -> Unit = { _, _ -> },
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA,  // Changed to prioritize front camera
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    // Check permission status
    when {
        cameraPermissionState.status.isGranted -> {
            // Permission granted - show camera
            CameraPreviewInternal(
                lifecycleOwner = lifecycleOwner,
                imageAnalyzer = imageAnalyzer,
                onViewSizeChanged = onViewSizeChanged,
                cameraSelector = cameraSelector,
                modifier = modifier
            )
        }
        else -> {
            // Permission not granted - show request screen
            CameraPermissionRequest(
                onRequestPermission = {
                    Log.d(TAG, "📸 Requesting camera permission...")
                    cameraPermissionState.launchPermissionRequest()
                }
            )
        }
    }
}

/**
 * Internal Camera Preview Implementation
 *
 * Separated from permission handling for cleaner code
 */
@androidx.camera.core.ExperimentalGetImage
@Composable
private fun CameraPreviewInternal(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    imageAnalyzer: (androidx.camera.core.ImageProxy) -> Unit,
    onViewSizeChanged: (Float, Float) -> Unit,
    cameraSelector: CameraSelector,
    modifier: Modifier
) {
    val context = LocalContext.current

    // Dedicated executor for image analysis
    val imageAnalysisExecutor = remember {
        Executors.newSingleThreadExecutor().also {
            Log.d(TAG, "🔧 ImageAnalysis executor created")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                Log.d(TAG, "🧹 Shutting down ImageAnalysis executor...")
                imageAnalysisExecutor.shutdown()
                Log.d(TAG, "✅ ImageAnalysis executor shut down")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error shutting down executor: ${e.message}", e)
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            Log.d(TAG, "🏗️ AndroidView factory started")
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FIT_CENTER  // Aspect ratio korunarak sığdır
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            try {
                Log.d(TAG, "📷 Getting CameraProvider...")
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    try {
                        Log.d(TAG, "🎥 CameraProvider listener triggered")
                        val cameraProvider = cameraProviderFuture.get()
                        Log.d(TAG, "✅ CameraProvider obtained successfully")

                        // Preview use case
                        val preview = Preview.Builder()
                            .build()
                            .also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                                Log.d(TAG, "✅ Preview use case created")
                            }

                        // ImageAnalysis use case
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                Log.d(TAG, "✅ ImageAnalysis use case created")

                                analysis.setAnalyzer(imageAnalysisExecutor) { imageProxy ->
                                    try {
                                        // Call the provided analyzer on executor thread
                                        imageAnalyzer(imageProxy)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "❌ Image analysis error: ${e.message}", e)
                                        // Always close the proxy even on error
                                        imageProxy.close()
                                    }
                                }
                            }

                        // Unbind previous bindings
                        cameraProvider.unbindAll()
                        Log.d(TAG, "🧹 Previous camera bindings cleared")

                        // Bind use cases to lifecycle
                        Log.d(TAG, "🔗 Binding use cases to lifecycleOwner...")

                        // Emulator detection for logging
                        val isEmulator = DeviceUtils.isEmulator()
                        Log.d(TAG, "📱 Device Type: ${if (isEmulator) "EMULATOR" else "REAL DEVICE"}")

                        var cameraBindSuccess = false

                        // --- PRIORITY 1: FRONT CAMERA (PRIMARY GOAL) ---
                        // Always try front camera first on both real devices and emulator
                        try {
                            Log.d(TAG, "📷 [1/3] Attempting FRONT CAMERA (Priority 1)...")
                            val frontCameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                            // Verify this selector can find at least one camera
                            if (cameraProvider.hasCamera(frontCameraSelector)) {
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    frontCameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                                Log.d(TAG, "✅ [1/3] FRONT CAMERA bound successfully! (Laptop webcam or device front camera)")
                                cameraBindSuccess = true
                            } else {
                                Log.w(TAG, "⚠️ [1/3] FRONT CAMERA not available on this device")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "⚠️ [1/3] FRONT CAMERA failed: ${e.message}. Trying back camera...")
                        }

                        // --- PRIORITY 2: BACK CAMERA (FALLBACK 1) ---
                        if (!cameraBindSuccess) {
                            try {
                                Log.d(TAG, "📷 [2/3] Attempting BACK CAMERA (Fallback 1)...")
                                val backCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                if (cameraProvider.hasCamera(backCameraSelector)) {
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        backCameraSelector,
                                        preview,
                                        imageAnalysis
                                    )
                                    Log.d(TAG, "✅ [2/3] BACK CAMERA bound successfully!")
                                    cameraBindSuccess = true
                                } else {
                                    Log.w(TAG, "⚠️ [2/3] BACK CAMERA not available on this device")
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "⚠️ [2/3] BACK CAMERA also failed: ${e.message}. Trying generic selector...")
                            }
                        }

                        // --- PRIORITY 3: ANY AVAILABLE CAMERA (FALLBACK 2 - EMULATOR RESCUE) ---
                        // This will bypass LensFacing issues on emulators
                        if (!cameraBindSuccess) {
                            try {
                                Log.d(TAG, "📷 [3/3] Attempting ANY AVAILABLE CAMERA (Fallback 2 - Emulator Rescue)...")
                                val genericCameraSelector = CameraSelector.Builder().build()

                                if (cameraProvider.hasCamera(genericCameraSelector)) {
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        genericCameraSelector,
                                        preview,
                                        imageAnalysis
                                    )
                                    Log.d(TAG, "✅ [3/3] GENERIC CAMERA bound successfully! (Emulator Rescue Mode)")
                                    cameraBindSuccess = true
                                } else {
                                    Log.e(TAG, "❌ [3/3] No cameras found on this device at all!")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ [3/3] CRITICAL: All camera selection methods failed: ${e.message}")
                                throw IllegalStateException("No cameras available on this device", e)
                            }
                        }

                        // --- FINAL VERIFICATION ---
                        if (!cameraBindSuccess) {
                            Log.e(TAG, "❌❌❌ CRITICAL: Camera initialization completely failed!")
                            throw IllegalStateException("Failed to bind any camera after all fallback attempts")
                        }

                        Log.d(TAG, "✅✅✅ Camera started successfully!")

                    } catch (e: Exception) {
                        Log.e(TAG, "❌❌❌ Camera initialization error: ${e.message}", e)
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

            } catch (e: Exception) {
                Log.e(TAG, "❌ CameraProvider getInstance error: ${e.message}", e)
                e.printStackTrace()
            }

            Log.d(TAG, "✅ PreviewView returned")
            previewView
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        update = { previewView ->
            try {
                // Update view size callback when dimensions change
                previewView.post {
                    val width = previewView.width.toFloat()
                    val height = previewView.height.toFloat()
                    if (width > 0 && height > 0) {
                        Log.d(TAG, "📐 View dimensions updated: ${width}x${height}")
                        onViewSizeChanged(width, height)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ View size update error: ${e.message}", e)
            }
        }
    )
}

/**
 * Camera Permission Request Screen
 */
@Composable
fun CameraPermissionRequest(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📷",
                fontSize = 64.sp
            )
            Text(
                text = rememberLocalizedText("camera_permission_required"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = rememberLocalizedText("camera_permission_desc"),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text("İzin Ver")
            }
        }
    }
}

private const val TAG = "CameraPreview"
