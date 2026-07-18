package com.example.isekaikuroshin.game

import com.example.isekaikuroshin.data.PersistentDataManager
import kotlinx.serialization.Serializable

/**
 * G62: Combat Result Persistence
 *
 * Savaş sonuçlarını kaydeder ve UI'da gösterir.
 * Combat sisteminden gelen sonuçları persistent storage'a yazar.
 */

@Serializable
data class CombatResult(
    val timestamp: Long = System.currentTimeMillis(),
    val enemyName: String,
    val victory: Boolean,
    val xpGained: Int = 0,
    val itemsGained: List<String> = emptyList(), // Item ID'leri
    val statChanges: Map<String, Int> = emptyMap()
)

object CombatResultManager {

    private const val COMBAT_HISTORY_KEY = "combat_history"
    private const val MAX_HISTORY_SIZE = 50 // Son 50 savaşı tut

    /**
     * G62: Savaş sonucunu kaydet
     *
     * @param result Combat sonucu
     */
    fun recordCombat(result: CombatResult) {
        val currentHistory = getCombatHistory().toMutableList()
        currentHistory.add(result)

        // Maksimum boyutu aş if, eski kayıtları sil
        if (currentHistory.size > MAX_HISTORY_SIZE) {
            currentHistory.removeAt(0)
        }

        // PersistentDataManager'a kaydet
        PersistentDataManager.saveString(
            key = COMBAT_HISTORY_KEY,
            value = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(CombatResult.serializer()),
                currentHistory
            )
        )

        android.util.Log.d("CombatResultManager", "G62: Combat kaydedildi - ${result.enemyName}, Victory: ${result.victory}, XP: ${result.xpGained}")
    }

    /**
     * G62: Son N savaşı getir
     *
     * @param limit Kaç savaş getirileceği (varsayılan: 10)
     * @return Son combat sonuçları
     */
    fun getRecentCombats(limit: Int = 10): List<CombatResult> {
        return getCombatHistory().takeLast(limit)
    }

    /**
     * G62: Tüm savaş geçmişini getir
     */
    fun getCombatHistory(): List<CombatResult> {
        val jsonString = PersistentDataManager.getString(COMBAT_HISTORY_KEY, "[]")
        return try {
            kotlinx.serialization.json.Json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(CombatResult.serializer()),
                jsonString
            )
        } catch (e: Exception) {
            android.util.Log.e("CombatResultManager", "Failed to load combat history", e)
            emptyList()
        }
    }

    /**
     * G62: Toplam kazanılan savaş sayısı
     */
    fun getTotalVictories(): Int {
        return getCombatHistory().count { it.victory }
    }

    /**
     * G62: Toplam kaybedilen savaş sayısı
     */
    fun getTotalDefeats(): Int {
        return getCombatHistory().count { !it.victory }
    }

    /**
     * G62: Toplam kazanılan XP (tüm savaşlardan)
     */
    fun getTotalXPFromCombat(): Int {
        return getCombatHistory().sumOf { it.xpGained }
    }

    /**
     * G62: En çok savaşılan düşman
     */
    fun getMostFoughtEnemy(): String? {
        return getCombatHistory()
            .groupBy { it.enemyName }
            .maxByOrNull { it.value.size }
            ?.key
    }
}
