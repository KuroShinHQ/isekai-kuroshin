package com.example.isekaikuroshin.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.isekaikuroshin.data.world.FactionState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Room Entity for Faction (Fraksiyon) verilerini veritabanında saklamak için.
 * FactionState nesnesini JSON formatında serialize ederek tek bir sütunda tutar.
 *
 * TODO-NEM-06 kapsamında oluşturulmuş, Faction sisteminin veritabanı katmanını sağlar.
 */
@Entity(tableName = "factions")
data class FactionEntity(
    @PrimaryKey val id: String,
    val stateJson: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * FactionState'i FactionEntity'ye dönüştürür
 */
fun FactionState.toEntity(): FactionEntity {
    return FactionEntity(
        id = this.id,
        stateJson = Json.encodeToString(this),
        lastUpdated = System.currentTimeMillis()
    )
}

/**
 * FactionEntity'yi FactionState'e dönüştürür
 */
fun FactionEntity.toFactionState(): FactionState {
    return Json.decodeFromString(this.stateJson)
}