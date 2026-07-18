# ÖLÜM YANKISI (Death Echo) - IntelligentContentEngine Genişletme Planı

## 📋 GEREKSİNİMLER

### 1. Yeni Fonksiyon: `generateDeathEchoPlaylist()`

Bu fonksiyon, ölen oyuncular için "Ruh Parçası" (Soul Fragment) verilerini kullanarak kişiselleştirilmiş içerik üretecek.

---

## 🧠 PSEUDO-KOD

```kotlin
/**
 * ÖLÜM YANKISI - Ölüm sonrası kişiselleştirilmiş içerik üretimi
 *
 * @param deathCount Oyuncunun toplam ölüm sayısı (DeathArchive.size)
 * @param lastDeathRecord Son ölüm kaydı (en son ölümün detayları)
 * @param maxVideos Maksimum video sayısı
 * @param maxPhotos Maksimum fotoğraf sayısı
 * @return PersonalizedPlaylist (ölüm temalı içerikler)
 */
fun generateDeathEchoPlaylist(
    deathCount: Int,
    lastDeathRecord: DeathRecord?,
    maxVideos: Int = 8,
    maxPhotos: Int = 8
): PersonalizedPlaylist {

    Log.d(TAG, "💀 ÖLÜM YANKISI başlatılıyor...")
    Log.d(TAG, "📊 Ölüm sayısı: $deathCount")

    // ========================================
    // ADIM 1: Duygusal Derinlik Hesapla
    // ========================================
    // Ölüm sayısı arttıkça içerik derinliği artar
    val emotionalDepth = when {
        deathCount >= 6 -> "D3"  // 6+ ölüm: En derin, karanlık içerikler
        deathCount >= 3 -> "D2"  // 3-5 ölüm: Orta derinlik
        else -> "D1"             // 1-2 ölüm: Hafif içerikler
    }

    Log.d(TAG, "🌊 Duygusal derinlik: $emotionalDepth")

    // ========================================
    // ADIM 2: Ölüm Sebebine Göre Baskın Duygu Belirle
    // ========================================
    val dominantEmotion = when {
        lastDeathRecord == null -> "SADNESS"

        // Ölüm sebebini analiz et
        lastDeathRecord.deathCause.contains("COMBAT", ignoreCase = true) -> "ANGER"
        lastDeathRecord.deathCause.contains("UMBROS", ignoreCase = true) -> "FEAR"
        lastDeathRecord.deathCause.contains("STARVE", ignoreCase = true) -> "SADNESS"
        lastDeathRecord.deathCause.contains("POISON", ignoreCase = true) -> "CONFUSION"

        else -> "SADNESS" // Varsayılan duygu
    }

    Log.d(TAG, "😢 Baskın duygu: $dominantEmotion (Sebep: ${lastDeathRecord?.deathCause})")

    // ========================================
    // ADIM 3: Veritabanından POSTDEATH İçeriklerini Filtrele
    // ========================================
    val database = loadOrBuildDatabase()

    // POSTDEATH ekran türüne sahip tüm medyaları al
    val postDeathVideos = database.videos.filter { video ->
        video.screenType.uppercase().contains("POSTDEATH") &&
        isDepthCompatible(video.depth, emotionalDepth)
    }

    val postDeathPhotos = database.photos.filter { photo ->
        photo.screenType.uppercase().contains("POSTDEATH") &&
        isDepthCompatible(photo.depth, emotionalDepth)
    }

    Log.d(TAG, "🎬 POSTDEATH video sayısı: ${postDeathVideos.size}")
    Log.d(TAG, "📸 POSTDEATH fotoğraf sayısı: ${postDeathPhotos.size}")

    // ========================================
    // ADIM 4: Duygu Eşleşmesiyle Puanla
    // ========================================
    val scoredVideos = postDeathVideos.map { video ->
        val emotionScore = if (video.emotion.equals(dominantEmotion, ignoreCase = true)) {
            0.9f // Tam eşleşme
        } else if (isEmotionCompatible(video.emotion, dominantEmotion)) {
            0.5f // Uyumlu duygu (örn: FEAR + CONFUSION)
        } else {
            0.3f // Uyumsuz ama gösterilebilir
        }

        val depthScore = getDepthScore(video.depth, emotionalDepth)
        val totalScore = (emotionScore * 0.7f) + (depthScore * 0.3f)

        ScoredMedia(video, totalScore)
    }

    val scoredPhotos = postDeathPhotos.map { photo ->
        val emotionScore = if (photo.emotion.equals(dominantEmotion, ignoreCase = true)) {
            0.9f
        } else if (isEmotionCompatible(photo.emotion, dominantEmotion)) {
            0.5f
        } else {
            0.3f
        }

        val depthScore = getDepthScore(photo.depth, emotionalDepth)
        val totalScore = (emotionScore * 0.7f) + (depthScore * 0.3f)

        ScoredMedia(photo, totalScore)
    }

    // ========================================
    // ADIM 5: En Yüksek Puanlı İçerikleri Seç
    // ========================================
    val selectedVideos = scoredVideos
        .sortedByDescending { it.score }
        .take(maxVideos)
        .map { it.media.resourceId }

    val selectedPhotos = scoredPhotos
        .sortedByDescending { it.score }
        .take(maxPhotos)
        .map { it.media.resourceId }

    Log.d(TAG, "✅ Seçilen video sayısı: ${selectedVideos.size}")
    Log.d(TAG, "✅ Seçilen fotoğraf sayısı: ${selectedPhotos.size}")

    // ========================================
    // ADIM 6: Fallback Mekanizması
    // ========================================
    // Eğer hiç POSTDEATH içeriği yoksa, genel içeriklerden seç
    val finalVideos = if (selectedVideos.isEmpty()) {
        Log.w(TAG, "⚠️ POSTDEATH videosu bulunamadı, RETURNINGUSER içerikleri kullanılıyor")
        generateFallbackVideos(emotionalDepth, dominantEmotion, maxVideos)
    } else {
        selectedVideos
    }

    val finalPhotos = if (selectedPhotos.isEmpty()) {
        Log.w(TAG, "⚠️ POSTDEATH fotoğrafı bulunamadı, RETURNINGUSER içerikleri kullanılıyor")
        generateFallbackPhotos(emotionalDepth, dominantEmotion, maxPhotos)
    } else {
        selectedPhotos
    }

    // ========================================
    // ADIM 7: Karakter Profili Oluştur
    // ========================================
    val characterProfile = CharacterProfile(
        mainArchetype = "UMBROS", // Ölüm sonrası özel arketip
        dominantEmotion = dominantEmotion,
        emotionalDepth = emotionalDepth,
        deathCount = deathCount,
        lastDeathCause = lastDeathRecord?.deathCause ?: "UNKNOWN"
    )

    Log.d(TAG, "💀 Karakter Profili: ${characterProfile.mainArchetype} / ${characterProfile.dominantEmotion} / ${characterProfile.emotionalDepth}")

    // ========================================
    // ADIM 8: Playlist Döndür
    // ========================================
    return PersonalizedPlaylist(
        videos = finalVideos,
        photos = finalPhotos,
        characterProfile = characterProfile
    )
}

// ========================================
// YARDIMCI FONKSİYONLAR
// ========================================

/**
 * Derinlik uyumluluğunu kontrol eder
 * Kural: Oyuncu derinliğinden daha derin içerikler gösterilmez
 */
private fun isDepthCompatible(contentDepth: String, playerDepth: String): Boolean {
    val contentLevel = when (contentDepth.uppercase()) {
        "D1" -> 1
        "D2" -> 2
        "D3" -> 3
        else -> 1
    }

    val playerLevel = when (playerDepth.uppercase()) {
        "D1" -> 1
        "D2" -> 2
        "D3" -> 3
        else -> 1
    }

    return contentLevel <= playerLevel
}

/**
 * Derinlik puanı hesaplar
 * Tam eşleşme: 1.0, bir seviye fark: 0.7, iki seviye fark: 0.4
 */
private fun getDepthScore(contentDepth: String, playerDepth: String): Float {
    val contentLevel = when (contentDepth.uppercase()) {
        "D1" -> 1
        "D2" -> 2
        "D3" -> 3
        else -> 1
    }

    val playerLevel = when (playerDepth.uppercase()) {
        "D1" -> 1
        "D2" -> 2
        "D3" -> 3
        else -> 1
    }

    return when (kotlin.math.abs(contentLevel - playerLevel)) {
        0 -> 1.0f    // Tam eşleşme
        1 -> 0.7f    // 1 seviye fark
        else -> 0.4f // 2+ seviye fark
    }
}

/**
 * Duygu uyumluluğunu kontrol eder
 * Bazı duygular birbiriyle uyumludur (örn: FEAR + CONFUSION)
 */
private fun isEmotionCompatible(emotion1: String, emotion2: String): Boolean {
    val compatibilityMap = mapOf(
        "FEAR" to listOf("CONFUSION", "SADNESS"),
        "ANGER" to listOf("SADNESS", "FEAR"),
        "SADNESS" to listOf("FEAR", "ANGER", "CALM"),
        "CALM" to listOf("SADNESS", "JOY"),
        "JOY" to listOf("CALM"),
        "CONFUSION" to listOf("FEAR", "SADNESS")
    )

    val compatible = compatibilityMap[emotion1.uppercase()] ?: emptyList()
    return compatible.contains(emotion2.uppercase())
}

/**
 * Fallback videoları seçer (POSTDEATH içeriği yoksa)
 */
private fun generateFallbackVideos(depth: String, emotion: String, maxCount: Int): List<Int> {
    val database = loadOrBuildDatabase()

    return database.videos
        .filter { video ->
            video.screenType.uppercase().contains("RETURNINGUSER") &&
            isDepthCompatible(video.depth, depth)
        }
        .sortedByDescending { video ->
            if (video.emotion.equals(emotion, ignoreCase = true)) 0.8f else 0.4f
        }
        .take(maxCount)
        .map { it.resourceId }
}

/**
 * Fallback fotoğrafları seçer (POSTDEATH içeriği yoksa)
 */
private fun generateFallbackPhotos(depth: String, emotion: String, maxCount: Int): List<Int> {
    val database = loadOrBuildDatabase()

    return database.photos
        .filter { photo ->
            photo.screenType.uppercase().contains("RETURNINGUSER") &&
            isDepthCompatible(photo.depth, depth)
        }
        .sortedByDescending { photo ->
            if (photo.emotion.equals(emotion, ignoreCase = true)) 0.8f else 0.4f
        }
        .take(maxCount)
        .map { it.resourceId }
}

/**
 * Geçici veri sınıfı - puanlama için
 */
private data class ScoredMedia(
    val media: MediaMetadata,
    val score: Float
)

/**
 * Genişletilmiş CharacterProfile (ölüm verileri dahil)
 */
data class CharacterProfile(
    val mainArchetype: String,
    val dominantEmotion: String,
    val emotionalDepth: String,
    val deathCount: Int = 0,              // YENİ
    val lastDeathCause: String = "NONE"   // YENİ
)
```

---

## 🔧 ENTEGRASYON NOKTALARI

### 1. UserEntryViewModel.kt - Ölüm Durumu Kontrolü

```kotlin
private fun determineUserFlow() {
    val isFirst = PersistentDataManager.isFirstLaunch()
    val playerData = PersistentDataManager.gameData.value.playerData
    val deathArchive = PersistentDataManager.gameData.value.deathArchive

    val flowState = when {
        isFirst -> UserFlowState.YENI_KULLANICI
        !playerData.isAlive -> UserFlowState.OLUM_SONRASI  // ← ÖLÜM DURUMU!
        playerData.name.isBlank() -> UserFlowState.YENI_KULLANICI
        else -> UserFlowState.GERI_DONEN_KULLANICI
    }

    // ÖLÜM SONRASI için "Ölüm Yankısı" motorunu çalıştır
    if (flowState == UserFlowState.OLUM_SONRASI) {
        viewModelScope.launch {
            generateDeathEchoContent(deathArchive)
        }
    }

    _uiState.value = UserEntryUiState(
        flowState = flowState,
        playerName = playerData.name,
        deathCount = deathArchive.size,  // ← YENİ ALAN
        // ...
    )
}

private fun generateDeathEchoContent(deathArchive: List<DeathRecord>) {
    try {
        GameLogger.logSystem("💀 Ölüm Yankısı motoru başlatılıyor...")

        val deathCount = deathArchive.size
        val lastDeathRecord = deathArchive.lastOrNull()

        // IntelligentContentEngine ile ölüm temalı playlist oluştur
        val playlist = intelligentContentEngine.generateDeathEchoPlaylist(
            deathCount = deathCount,
            lastDeathRecord = lastDeathRecord,
            maxVideos = 8,
            maxPhotos = 8
        )

        // UI State'i güncelle
        _uiState.value = _uiState.value.copy(
            dynamicVideoIds = playlist.videos,
            dynamicPhotoIds = playlist.photos
        )

        GameLogger.logSystem("💀 Ölüm Yankısı tamamlandı: ${playlist.videos.size} video, ${playlist.photos.size} fotoğraf")

    } catch (e: Exception) {
        GameLogger.logError("DeathEchoEngine", "Ölüm Yankısı hatası", e)
    }
}
```

### 2. UserEntryUiState.kt - Yeni Alan

```kotlin
data class UserEntryUiState(
    val flowState: UserFlowState = UserFlowState.LOADING,
    val playerName: String = "",
    val moralityScore: Float = 0.0f,
    val deathCount: Int = 0,                           // ← YENİ
    val lastDeathCause: String = "",                   // ← YENİ
    val dynamicPlaylist: List<String> = emptyList(),
    val dynamicVideoIds: List<Int> = emptyList(),
    val dynamicPhotoIds: List<Int> = emptyList(),
    val isLoading: Boolean = false
)
```

### 3. PersistentDataManager.kt - CRITICAL BUG FIX

```kotlin
/**
 * CRITICAL FIX: DeathArchive'i koruyarak oyunu sıfırla
 */
fun resetAllData(keepDeathArchive: Boolean = true) {
    android.util.Log.d("PersistentDataManager", "🔄 Oyun sıfırlanıyor...")

    // Mevcut ölüm arşivini kaydet
    val existingDeathArchive = if (keepDeathArchive) {
        _gameData.value.deathArchive
    } else {
        emptyList()
    }

    // Tüm oyun verilerini sıfırla AMA ölüm arşivini koru
    _gameData.value = PersistentGameData(
        deathArchive = existingDeathArchive  // ← RUH PARÇASI KORUNDU!
    )
    saveGameData()

    android.util.Log.d("PersistentDataManager", "💀 Ölüm arşivi korundu: ${existingDeathArchive.size} kayıt")
}
```

---

## 📊 TEST SENARYOLARI

### Senaryo 1: İlk Ölüm (1 death)
- **Beklenen Derinlik**: D1
- **Beklenen Duygu**: SADNESS (veya ölüm sebebine göre)
- **Beklenen Sonuç**: Hafif, melankolik içerikler

### Senaryo 2: Beşinci Ölüm (5 deaths)
- **Beklenen Derinlik**: D2
- **Beklenen Duygu**: ANGER (eğer COMBAT ölümüyse)
- **Beklenen Sonuç**: Orta derinlik, öfke temalı içerikler

### Senaryo 3: Onuncu Ölüm (10 deaths)
- **Beklenen Derinlik**: D3
- **Beklenen Duygu**: FEAR (eğer UMBROS ölümüyse)
- **Beklenen Sonuç**: En derin, karanlık, korku temalı içerikler

---

## 🎯 ÖNERİLEN MEDYA TAGLERİ

### POSTDEATH Videoları İçin Etiketleme Örnekleri:

1. **İlk Ölüm - Hafif İçerikler (D1)**
   ```
   VID_POSTDEATH_MERCY_SADNESS_D1_001.mp4
   VID_POSTDEATH_CALM_JOY_D1_002.mp4
   ```

2. **Orta Ölümler - Daha Derin (D2)**
   ```
   VID_POSTDEATH_COURAGE_ANGER_D2_001.mp4
   VID_POSTDEATH_SURVIVAL_FEAR_D2_002.mp4
   ```

3. **Çok Ölümler - En Karanlık (D3)**
   ```
   VID_POSTDEATH_VIOLENCE_ANGER_D3_001.mp4
   VID_POSTDEATH_CHAOS_FEAR_D3_002.mp4
   ```

### POSTDEATH Fotoğrafları İçin Etiketleme Örnekleri:

```
PHT_POSTDEATH_MERCY_SADNESS_D1_001.png
PHT_POSTDEATH_COURAGE_CALM_D2_001.png
PHT_POSTDEATH_CHAOS_FEAR_D3_001.png
```

---

## ⚠️ ÖNEMLİ NOTLAR

1. **Fallback Mekanizması**: Eğer hiç POSTDEATH içeriği yoksa, sistem RETURNINGUSER içeriklerini gösterecek
2. **Derinlik Kuralı**: Oyuncunun derinliğinden daha derin içerikler asla gösterilmez
3. **Duygu Uyumluluğu**: Bazı duygular birbiriyle uyumlu (FEAR+CONFUSION, ANGER+SADNESS)
4. **Arşiv Korunması**: `resetAllData()` çağrısı artık DeathArchive'i silmeyecek

---

## 🚀 UYGULAMA SIRASI

1. ✅ etiket_config.json güncellendi (POSTDEATH eklendi)
2. ✅ localization.json güncellendi (TR/EN çevirileri)
3. ⏳ IntelligentContentEngine.kt'ye `generateDeathEchoPlaylist()` ekle
4. ⏳ UserEntryViewModel.kt'ye `generateDeathEchoContent()` ekle
5. ⏳ UserEntryUiState'e `deathCount` ve `lastDeathCause` ekle
6. ⏳ PersistentDataManager.kt'deki `resetAllData()` bug'ını düzelt
7. ⏳ gorsel_etiketleyici.py ile 5-10 POSTDEATH videosu etiketle
8. ⏳ Test et!

---

**Bu pseudo-kod, "Ölüm Yankısı" sisteminin tüm teknik detaylarını içermektedir.**
