package com.example.isekaikuroshin.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * 🔄 Pending Sync Manager - Çevrimdışıyken yapılan işlemleri saklar ve senkronize eder
 *
 * Akıllı Özellikler:
 * - Aynı türdeki işlemleri birleştirir (Toplu işlem)
 * - API maliyetini düşürür (20 istek → 1 istek)
 * - Kullanıcıya anlamlı bildirimler gösterir
 */
class PendingSyncManager(private val context: Context) {

    private val gson = Gson()
    private val syncFile = File(context.filesDir, "pending_sync.json")

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    init {
        updatePendingCount()
    }

    /**
     * Yeni bir pending işlem ekle
     *
     * @param type İşlem tipi ("journal", "quest", etc.)
     * @param data İşlem verisi
     */
    fun addPendingItem(type: String, data: String) {
        val items = loadPendingItems().toMutableList()

        val newItem = PendingSyncItem(
            type = type,
            data = data,
            timestamp = System.currentTimeMillis()
        )

        items.add(newItem)
        savePendingItems(items)

        _pendingCount.value = items.size

        android.util.Log.d("PendingSyncManager", "➕ Added pending item: $type")
    }

    /**
     * Tüm pending işlemleri getir
     */
    fun getAllPendingItems(): List<PendingSyncItem> {
        return loadPendingItems()
    }

    /**
     * Belirli türdeki pending işlemleri getir ve birleştir
     *
     * ⚡ AKILLI ÖZELLİK: Aynı türdeki işlemleri birleştirir
     *
     * Örnek:
     * - 20 günlük girişi → Tek bir "haftalık özet" metni
     * - 5 görev güncellemesi → Tek bir "toplu güncelleme" metni
     */
    fun getPendingItemsByType(type: String): BatchedPendingData {
        val items = loadPendingItems().filter { it.type == type }

        if (items.isEmpty()) {
            return BatchedPendingData(
                type = type,
                combinedData = "",
                count = 0,
                oldestTimestamp = 0L,
                newestTimestamp = 0L
            )
        }

        // Tüm verileri birleştir
        val combinedData = items.joinToString(separator = "\n---\n") { item ->
            val date = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("tr"))
                .format(java.util.Date(item.timestamp))
            "[$date]\n${item.data}"
        }

        return BatchedPendingData(
            type = type,
            combinedData = combinedData,
            count = items.size,
            oldestTimestamp = items.minOf { it.timestamp },
            newestTimestamp = items.maxOf { it.timestamp }
        )
    }

    /**
     * Belirli türdeki tüm pending işlemleri sil
     */
    fun clearPendingItemsByType(type: String) {
        val items = loadPendingItems().filterNot { it.type == type }
        savePendingItems(items)
        _pendingCount.value = items.size

        android.util.Log.d("PendingSyncManager", "🗑️ Cleared pending items: $type")
    }

    /**
     * Tüm pending işlemleri sil
     */
    fun clearAllPending() {
        savePendingItems(emptyList())
        _pendingCount.value = 0

        android.util.Log.d("PendingSyncManager", "🗑️ Cleared all pending items")
    }

    /**
     * Pending işlem sayısını güncelle
     */
    private fun updatePendingCount() {
        _pendingCount.value = loadPendingItems().size
    }

    // Private helpers
    private fun loadPendingItems(): List<PendingSyncItem> {
        if (!syncFile.exists()) return emptyList()

        return try {
            val json = syncFile.readText()
            val type = object : TypeToken<List<PendingSyncItem>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("PendingSyncManager", "Error loading pending items: ${e.message}")
            emptyList()
        }
    }

    private fun savePendingItems(items: List<PendingSyncItem>) {
        try {
            val json = gson.toJson(items)
            syncFile.writeText(json)
        } catch (e: Exception) {
            android.util.Log.e("PendingSyncManager", "Error saving pending items: ${e.message}")
        }
    }
}

/**
 * Pending senkronizasyon öğesi
 */
data class PendingSyncItem(
    val type: String,       // "journal", "quest", "exercise"
    val data: String,       // İşlem verisi
    val timestamp: Long     // Ne zaman eklendi (ms)
)

/**
 * Toplu işlem için birleştirilmiş veri
 */
data class BatchedPendingData(
    val type: String,           // İşlem tipi
    val combinedData: String,   // Birleştirilmiş tüm veriler
    val count: Int,             // Kaç işlem birleştirildi
    val oldestTimestamp: Long,  // En eski işlem
    val newestTimestamp: Long   // En yeni işlem
) {
    /**
     * Tarih aralığı - Kullanıcıya gösterilecek
     * Örn: "18-25 Ekim 2025"
     */
    fun getDateRange(): String {
        if (count == 0) return ""
        if (count == 1) {
            val date = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("tr"))
                .format(java.util.Date(oldestTimestamp))
            return date
        }

        val startDate = java.text.SimpleDateFormat("dd", java.util.Locale("tr"))
            .format(java.util.Date(oldestTimestamp))
        val endDate = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale("tr"))
            .format(java.util.Date(newestTimestamp))

        return "$startDate-$endDate"
    }

    /**
     * Kullanıcıya gösterilecek özet mesaj
     */
    fun getSummaryMessage(): String {
        return when (type) {
            "journal" -> "$count günlük girişi (${getDateRange()})"
            "quest" -> "$count görev güncellemesi"
            "exercise" -> "$count egzersiz kaydı"
            else -> "$count işlem"
        }
    }
}
