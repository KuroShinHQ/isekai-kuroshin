package com.example.isekaikuroshin.data.database

import androidx.room.*
import com.example.isekaikuroshin.data.DynamicContentType
import kotlinx.coroutines.flow.Flow

/**
 * DynamicContent DAO - GM Tarafından Üretilen Eşsiz İçerikler
 *
 * Bu DAO GM'in ürettiği tüm eşsiz içerikleri yönetir:
 * - Elementler, Yetenekler, Itemler
 * - Export/Import işlemleri
 * - Icon yönetimi
 */
@Dao
interface DynamicContentDao {

    /**
     * Yeni bir dynamic content ekle veya güncelle
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(content: DynamicContentEntity)

    /**
     * Birden fazla content ekle (bulk insert)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contents: List<DynamicContentEntity>)

    /**
     * ID'ye göre content getir
     */
    @Query("SELECT * FROM dynamic_content WHERE id = :contentId")
    suspend fun getById(contentId: String): DynamicContentEntity?

    /**
     * Tüm contentleri getir (Flow)
     */
    @Query("SELECT * FROM dynamic_content ORDER BY createdAtTimestamp DESC")
    fun getAllContents(): Flow<List<DynamicContentEntity>>

    /**
     * Type'a göre contentleri getir
     */
    @Query("SELECT * FROM dynamic_content WHERE type = :type ORDER BY createdAtTimestamp DESC")
    fun getContentsByType(type: DynamicContentType): Flow<List<DynamicContentEntity>>

    /**
     * Export edilmiş contentleri getir (kullanıcı beğendikleri)
     */
    @Query("SELECT * FROM dynamic_content WHERE isExported = 1 ORDER BY createdAtTimestamp DESC")
    fun getExportedContents(): Flow<List<DynamicContentEntity>>

    /**
     * Icon'u olmayan contentleri getir (placeholder gösterilecek)
     */
    @Query("SELECT * FROM dynamic_content WHERE hasIcon = 0 ORDER BY createdAtTimestamp DESC")
    fun getContentsWithoutIcon(): Flow<List<DynamicContentEntity>>

    /**
     * Favorileri getir
     */
    @Query("SELECT * FROM dynamic_content WHERE isFavorite = 1 ORDER BY createdAtTimestamp DESC")
    fun getFavoriteContents(): Flow<List<DynamicContentEntity>>

    /**
     * Content'i sil
     */
    @Query("DELETE FROM dynamic_content WHERE id = :contentId")
    suspend fun deleteById(contentId: String)

    /**
     * Tüm contentleri sil (oyun sıfırlama için)
     */
    @Query("DELETE FROM dynamic_content")
    suspend fun deleteAll()

    /**
     * Icon ekle/güncelle
     */
    @Query("UPDATE dynamic_content SET iconPath = :iconPath, hasIcon = 1 WHERE id = :contentId")
    suspend fun updateIcon(contentId: String, iconPath: String)

    /**
     * Export durumunu güncelle
     */
    @Query("UPDATE dynamic_content SET isExported = :isExported WHERE id = :contentId")
    suspend fun updateExportStatus(contentId: String, isExported: Boolean)

    /**
     * Favori durumunu güncelle
     */
    @Query("UPDATE dynamic_content SET isFavorite = :isFavorite WHERE id = :contentId")
    suspend fun updateFavoriteStatus(contentId: String, isFavorite: Boolean)

    /**
     * İstatistikler: Type'a göre sayı
     */
    @Query("SELECT COUNT(*) FROM dynamic_content WHERE type = :type")
    suspend fun getCountByType(type: DynamicContentType): Int

    /**
     * İstatistikler: Icon'u olanların sayısı
     */
    @Query("SELECT COUNT(*) FROM dynamic_content WHERE hasIcon = 1")
    suspend fun getCountWithIcon(): Int
}
