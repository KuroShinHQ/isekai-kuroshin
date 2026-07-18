package com.example.isekaikuroshin.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object (DAO) for Settlement (Yerleşim Yeri) veritabanı işlemleri.
 * CRUD operasyonlarını sağlar ve settlement verilerinin veritabanında yönetimini kolaylaştırır.
 *
 * TODO-NEM-05 kapsamında oluşturulmuş, Settlement sisteminin veritabanı erişimini sağlar.
 */
@Dao
interface SettlementDao {

    /**
     * Tüm settlement'ları getirir
     */
    @Query("SELECT * FROM settlements")
    suspend fun getAllSettlements(): List<SettlementEntity>

    /**
     * Belirli bir ID'ye sahip settlement'ı getirir
     */
    @Query("SELECT * FROM settlements WHERE id = :settlementId")
    suspend fun getSettlementById(settlementId: String): SettlementEntity?

    /**
     * Yeni bir settlement ekler, çakışma durumunda günceller
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementEntity)

    /**
     * Birden fazla settlement'ı aynı anda ekler
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlements(settlements: List<SettlementEntity>)

    /**
     * Var olan bir settlement'ı günceller
     */
    @Update
    suspend fun updateSettlement(settlement: SettlementEntity)

    /**
     * Birden fazla settlement'ı aynı anda günceller
     */
    @Update
    suspend fun updateSettlements(settlements: List<SettlementEntity>)

    /**
     * Belirli bir settlement'ı siler
     */
    @Query("DELETE FROM settlements WHERE id = :settlementId")
    suspend fun deleteSettlement(settlementId: String)

    /**
     * Tüm settlement'ları siler (test amaçlı)
     */
    @Query("DELETE FROM settlements")
    suspend fun deleteAllSettlements()

    /**
     * Settlement sayısını döndürür
     */
    @Query("SELECT COUNT(*) FROM settlements")
    suspend fun getSettlementCount(): Int

    /**
     * Belirli bir günden sonra güncellenen settlement'ları getirir
     */
    @Query("SELECT * FROM settlements WHERE lastUpdated > :timestamp")
    suspend fun getSettlementsUpdatedAfter(timestamp: Long): List<SettlementEntity>
}