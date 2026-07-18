package com.example.isekaikuroshin.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object (DAO) for Faction (Fraksiyon) veritabanı işlemleri.
 * CRUD operasyonlarını sağlar ve faction verilerinin veritabanında yönetimini kolaylaştırır.
 *
 * TODO-NEM-06 kapsamında oluşturulmuş, Faction sisteminin veritabanı erişimini sağlar.
 */
@Dao
interface FactionDao {

    /**
     * Tüm faction'ları getirir
     */
    @Query("SELECT * FROM factions")
    suspend fun getAllFactions(): List<FactionEntity>

    /**
     * Belirli bir ID'ye sahip faction'ı getirir
     */
    @Query("SELECT * FROM factions WHERE id = :factionId")
    suspend fun getFactionById(factionId: String): FactionEntity?

    /**
     * Yeni bir faction ekler, çakışma durumunda günceller
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaction(faction: FactionEntity)

    /**
     * Birden fazla faction'ı aynı anda ekler
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFactions(factions: List<FactionEntity>)

    /**
     * Var olan bir faction'ı günceller
     */
    @Update
    suspend fun updateFaction(faction: FactionEntity)

    /**
     * Birden fazla faction'ı aynı anda günceller
     */
    @Update
    suspend fun updateFactions(factions: List<FactionEntity>)

    /**
     * Belirli bir faction'ı siler
     */
    @Query("DELETE FROM factions WHERE id = :factionId")
    suspend fun deleteFaction(factionId: String)

    /**
     * Tüm faction'ları siler (test amaçlı)
     */
    @Query("DELETE FROM factions")
    suspend fun deleteAllFactions()

    /**
     * Faction sayısını döndürür
     */
    @Query("SELECT COUNT(*) FROM factions")
    suspend fun getFactionCount(): Int

    /**
     * Belirli bir günden sonra güncellenen faction'ları getirir
     */
    @Query("SELECT * FROM factions WHERE lastUpdated > :timestamp")
    suspend fun getFactionsUpdatedAfter(timestamp: Long): List<FactionEntity>

    /**
     * Belirli bir güç seviyesinin üstündeki faction'ları getirir
     */
    @Query("SELECT * FROM factions WHERE json_extract(stateJson, '$.powerLevel') > :minPowerLevel")
    suspend fun getFactionsWithMinPower(minPowerLevel: Int): List<FactionEntity>

    /**
     * Belirli bir fraksiyon türündeki tüm faction'ları getirir
     */
    @Query("SELECT * FROM factions WHERE json_extract(stateJson, '$.type') = :factionType")
    suspend fun getFactionsByType(factionType: String): List<FactionEntity>
}