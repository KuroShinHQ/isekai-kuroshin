package com.example.isekaikuroshin.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.isekaikuroshin.data.world.SettlementState
import com.example.isekaikuroshin.data.database.SettlementEntity
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Room Entity for Settlement (Yerleşim Yeri) verilerini veritabanında saklamak için.
 * SettlementState nesnesini JSON formatında serialize ederek tek bir sütunda tutar.
 *
 * TODO-NEM-05 kapsamında oluşturulmuş, Settlement sisteminin veritabanı katmanını sağlar.
 */
@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val id: String,
    val stateJson: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * SettlementState'i SettlementEntity'ye dönüştürür
 */
fun SettlementState.toEntity(): SettlementEntity {
    return SettlementEntity(
        id = this.id,
        stateJson = Json.encodeToString(this),
        lastUpdated = System.currentTimeMillis()
    )
}

/**
 * SettlementEntity'yi SettlementState'e dönüştürür
 */
fun SettlementEntity.toSettlementState(): SettlementState {
    return Json.decodeFromString(this.stateJson)
}