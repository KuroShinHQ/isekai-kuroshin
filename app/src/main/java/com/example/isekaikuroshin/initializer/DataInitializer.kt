package com.example.isekaikuroshin.initializer

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.example.isekaikuroshin.data.PersistentDataManager

/**
 * RACE CONDITION FIX - Strateji 2: Hilt Eager Initialization + Startup Library
 *
 * Bu Initializer, uygulama başlamadan ÖNCE PersistentDataManager'ı başlatır.
 * Bu sayede AIClientProvider her zaman hazır veriyle çalışır, race condition ortadan kalkar.
 *
 * AndroidX Startup Library kullanarak, ContentProvider pattern ile
 * Application.onCreate()'den ÖNCE çalışır.
 */
class DataInitializer : Initializer<PersistentDataManager> {

    companion object {
        private const val TAG = "DataInitializer"
    }

    /**
     * Bu fonksiyon Application.onCreate()'den ÖNCE çalışır
     * Blocking/senkron çalışır, tamamlanana kadar uygulama başlamaz
     */
    override fun create(context: Context): PersistentDataManager {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "========================================")
        Log.d(TAG, "RACE CONDITION FIX: DataInitializer starting")
        Log.d(TAG, "Thread: ${Thread.currentThread().name}")
        Log.d(TAG, "========================================")

        // PersistentDataManager'ı senkron olarak başlat
        // Bu işlem tamamlanana kadar uygulama başlamaz
        PersistentDataManager.initializeSynchronously(context)

        val duration = System.currentTimeMillis() - startTime
        Log.d(TAG, "========================================")
        Log.d(TAG, "RACE CONDITION FIX: DataInitializer completed")
        Log.d(TAG, "Duration: ${duration}ms")
        Log.d(TAG, "PersistentDataManager is now ready for use")
        Log.d(TAG, "========================================")

        return PersistentDataManager
    }

    /**
     * Dependencies - bu initializer başka initializer'lara bağlı değil
     */
    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
