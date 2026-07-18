# Bölüm 6: Ölüm Döngüsü ve Karakter Yankısı Entegrasyon Planı

**Görev ID:** KRM-SYS-19-DEATH-LOOP-ANALYSIS
**Rapor Tarihi:** 2025-10-16
**Durum:** ARAŞTIRMA TAMAMLANDI - UYGULAMA PLANI HAZIR

---

## Executive Summary (Yönetici Özeti)

Bu rapor, oyuncunun ölümü sonrası "gerçek yeniden başlatma" deneyimini zenginleştirmek için tasarlanan **"Ölüm Döngüsü" (Death Loop)** mekaniğinin teknik fizibilitesini, risklerini ve uygulama planını sunar.

**Ana Hedef:** Oyuncu öldüğünde ve Umbros anlaşmasını reddettiğinde, PlayerState sıfırlanır ANCAK ölüm deneyimi "Ruh Parçası" (Soul Fragment) olarak saklanır. Oyuncu yeni karakterle oyuna döndüğünde (FIRSTUSER ekranı), bu "ruh yankısı" özel içerikler (videolar, fotoğraflar) göstermek için kullanılır.

**Özet Bulgular:**
- ✅ Mevcut sistemde `DeathArchive` mekanizması zaten var - genişletilebilir
- ✅ Umbros reddedildiğinde oyuncu ölü kalır - doğru akış mevcut
- ⚠️ `PersistentDataManager.resetAllData()` DeathArchive'i siliyor - **BUG BULUNDU!**
- ✅ `IntelligentContentEngine` kolayca genişletilebilir

---

## 1. Kalıcı Veri ("Ruh Parçası") Mekanizması

### 1.1 Mevcut DeathArchive Analizi

**Dosya:** `app/src/main/java/com/example/isekaikuroshin/data/PersistentDataManager.kt`

#### DeathRecord Veri Yapısı (Satır 27-31)

```kotlin
@Serializable
data class DeathRecord(
    val characterName: String,          // Ölen karakterin adı
    val deathDay: Int,                  // Ölüm günü (oyun içi zaman)
    val deathLocation: String,          // Ölüm yeri
    val deathCause: String,             // Ölüm sebebi
    val totalPlayTime: Long,            // Toplam oyun süresi (ms)
    val finalStats: PlayerStatsPersistent  // Final istatistikler
)
```

#### DeathArchive Konumu

`PersistentGameData` içinde: `val deathArchive: List<DeathRecord> = emptyList()` (Satır 24)

---

### 1.2 🐛 **KRİTİK BUG: resetAllData() DeathArchive'i Siliyor!**

**Dosya:** `PersistentDataManager.kt` (Satır 608-639)

**Sorun:**
```kotlin
fun resetAllData() {
    // SATIR 612: TÜM VERİLERİ SIFIRLA
    _gameData.value = PersistentGameData()  // ← YENİ BOŞNESNE OLUŞTURULUYOR!
    saveGameData()

    // DeathArchive burada kayboldu! ❌
}
```

**Sonuç:** Oyuncu öldüğünde `DeathManager.recordDeath()` arşive ekliyor, ancak `resetGame()` çağrıldığında `resetAllData()` tüm arşivi siliyar.

---

### 1.3 ✅ **ÇÖZÜM: resetAllData() Düzeltmesi**

**Önerilen Değişiklik** (Satır 608-639):

```kotlin
fun resetAllData() {
    android.util.Log.d("PersistentDataManager",
        "TODO-FIX-01: Resetting ALL user data INCLUDING API key")

    // ✅ ÖNCE: Ölüm arşivini koru
    val existingDeathArchive = _gameData.value.deathArchive

    // TÜM OYUN VERİLERİNİ SIFIRLA
    _gameData.value = PersistentGameData(
        deathArchive = existingDeathArchive  // ← ARŞİVİ GERİ YÜKLEyorum
    )
    saveGameData()

    // Dil pratiği sıfırlama...
    val languagesToClear = listOf("İngilizce", "Japanese", "Español", "Français", "Deutsch")
    for (language in languagesToClear) {
        val progressKey = "language_progress_$language"
        sharedPrefs.edit().remove(progressKey).apply()
    }

    // GameState sıfırlama...
    sharedPrefs.edit().remove("current_game_state").apply()

    android.util.Log.d("PersistentDataManager",
        "✅ FIX-DEATH-02: Death Archive preserved (${existingDeathArchive.size} records)")
}
```

**Değişiklik Özeti:**
- `existingDeathArchive` değişkeni eski arşivi tutar
- Yeni `PersistentGameData` oluştururken arşivi constructor'a veririz
- Log mesajı arşivin korunduğunu doğrular

---

### 1.4 Teknik Saklama Yöntemi Karşılaştırması

| Yöntem | Konum | Avantajlar | Dezavantajlar | Önerilen Mi? |
|--------|-------|------------|---------------|--------------|
| **SharedPreferences** | `PersistentDataManager` | ✅ Zaten kullanılıyor<br>✅ Hızlı okuma/yazma<br>✅ Basit API | ⚠️ "Verileri Temizle" ile silinir<br>⚠️ Boyut sınırı (~10MB) | **✅ EVET** (Mevcut sistem) |
| **Internal Storage (.json)** | `filesDir/soul_fragment.json` | ✅ Daha büyük dosyalar<br>✅ Yapılandırılmış veri | ⚠️ "Verileri Temizle" ile silinir | ❌ Hayır (gereksiz) |
| **Room Database (Ayrı Tablo)** | `SoulFragmentEntity` | ✅ İlişkisel sorgular<br>✅ Type-safe | ⚠️ "Verileri Temizle" ile silinir<br>⚠️ Daha karmaşık | ❌ Hayır (overkill) |
| **External Storage** | `Android/data/.../soul.json` | ✅ Uygulama silinse bile kalır | ❌ Android 11+ permission sorunları<br>❌ Güvenlik riski | ❌ HAYIR |

**Karar:** **SharedPreferences (PersistentDataManager)** kullanmaya devam edelim. `resetAllData()` düzeltmesi yeterli.

---

## 2. gorsel_etiketleyici.py İçin Yeni Etiketleme Stratejisi

### 2.1 Üç Seçenek Karşılaştırması

#### Seçenek A: Yeni SCREEN_TYPE Ekle

**Önerilen Etiket:** `POSTDEATH_USER` (Ölüm Sonrası Kullanıcı)

**etiket_config.json Değişikliği:**
```json
{
  "SCREEN_TYPES": [
    "NEWUSER",
    "RETURNINGUSER",
    "POSTDEATH_USER",  // ← YENİ
    "JOURNEY",
    "UMBROS"
  ]
}
```

**Örnek Dosya Adı:**
```
VID_POSTDEATH_REGRET_SADNESS_D2_001.mp4
VID_POSTDEATH_ACCEPTANCE_CALM_D1_001.mp4
PHT_POSTDEATH_MEMENTO_SADNESS_D3_001.png
```

**Avantajlar:**
- ✅ Semantik olarak net: "Bu içerik ölen oyuncular içindir"
- ✅ `IntelligentContentEngine` kolay filtreler: `if (deathCount > 0) filter(POSTDEATH_USER)`
- ✅ Ölüm sayısına göre farklı derinlikler kullanılabilir (D1: 1. ölüm, D2: 5. ölüm, vb.)

**Dezavantajlar:**
- ⚠️ Yeni ekran kategorisi eklemek, mevcut sistemi genişletir (ama sorun değil)
- ⚠️ NEWUSER içerikleriyle harmanlanması gerekir (algorithm'e ek mantık gerekir)

---

#### Seçenek B: Yeni ATTRIBUTE Ekle

**Önerilen Etiket:** `DEATH_RELATED`, `AFTERLIFE`, `MEMENTO`

**etiket_config.json Değişikliği:**
```json
{
  "ATTRIBUTES": [
    "ORDER",
    "CHAOS",
    "VIOLENCE",
    "PEACE",
    "CONFLICT",
    "DEATH_RELATED",  // ← YENİ
    "AFTERLIFE",      // ← YENİ
    "MEMENTO"         // ← YENİ
  ]
}
```

**Örnek Dosya Adı:**
```
VID_NEWUSER_DEATH_RELATED_SADNESS_D2_001.mp4
VID_JOURNEY_AFTERLIFE_CALM_D1_001.mp4
PHT_RETURNINGUSER_MEMENTO_SADNESS_D3_001.png
```

**Avantajlar:**
- ✅ Ölüm içerikleri birden fazla ekran türünde kullanılabilir (NEWUSER, JOURNEY, vb.)
- ✅ Daha esnek: Ölüm teması farklı bağlamlarda gösterilebilir

**Dezavantajlar:**
- ❌ Semantik olarak belirsiz: "DEATH_RELATED" geniş bir kategori
- ❌ Algorithm'de hangi ATTRIBUTE'un öncelikli olduğu karışabilir

---

#### Seçenek C: Derinlik (DEPTH) Kullanımı

**Fikir:** Oyuncunun ölüm sayısına göre `DEPTH` etiketini yorumla:
- D1: 1-2 ölüm (yüzeysel keder)
- D2: 3-5 ölüm (derin keder)
- D3: 6+ ölüm (varoluşsal kriz)

**Örnek Dosya Adı:**
```
VID_NEWUSER_REGRET_SADNESS_D1_001.mp4  // 1. ölüm
VID_NEWUSER_DESPAIR_SADNESS_D2_001.mp4  // 5. ölüm
VID_NEWUSER_VOID_SADNESS_D3_001.mp4     // 10. ölüm
```

**Avantajlar:**
- ✅ Mevcut `DEPTH` sistemini yeniden kullanıyoruz
- ✅ Kod değişikliği minimal

**Dezavantajlar:**
- ❌ **Semantik karışıklık:** DEPTH zaten "duygusal derinlik" için kullanılıyor
- ❌ Oyuncu seviyesi (level) ile ölüm sayısı karışabilir
- ❌ Gelecekte sistem genişlediğinde karışıklık yaratır

---

### 2.2 📌 **ÖNERİLEN STRATEJI: SEÇENEyK A (POSTDEATH_USER)**

**Gerekçe:**
1. **Semantik Netlik:** "POSTDEATH" kelimesi anlamı açıkça belirtir
2. **Kolay Filtreleme:** Algorithm'de basit bir kontrol: `deathCount > 0 → select POSTDEATH_USER`
3. **Esnek Derinlik:** Ölüm sayısına göre D1/D2/D3 kullanılabilir
4. **NEWUSER ile Harmanlama:** İlk başlangıç ekranında hem NEWUSER hem POSTDEATH içerikleri gösterilebilir

**Uygulama:**
```python
# gorsel_etiketleyici.py içinde
SCREEN_TYPES = [
    "NEWUSER",
    "RETURNINGUSER",
    "POSTDEATH_USER",  # Yeni ekleme
    "JOURNEY",
    "UMBROS"
]
```

---

## 3. IntelligentContentEngine Güncellenmesi ("Yankı" Mantığı)

### 3.1 Hedef Fonksiyon Tespiti

**Dosya:** `app/src/main/java/com/example/isekaikuroshin/engine/IntelligentContentEngine.kt`

#### Hedef Fonksiyonlar:

| Fonksiyon | Satırlar | Değişiklik Gereksinimi |
|-----------|---------|------------------------|
| `generatePersonalizedPlaylist()` | 101-138 | ✅ Derinlik parametresi ekle |
| `extractCharacterProfile()` | 144-202 | ✅ Ölüm sayısı entegrasyonu |
| `scoreMediaList()` | 230-243 | ⚠️ POSTDEATH filtresi ekle |
| `calculateArchetypeMatch()` | 313-333 | ⚠️ POSTDEATH bonusu ekle |

---

### 3.2 UserEntryViewModel Güncellemesi

**Dosya:** `app/src/main/java/com/example/isekaikuroshin/ui/intro/UserEntryViewModel.kt`

#### determineUserFlow() Mantığı (Satır 46-75)

**ÖNCE (Mevcut Durum):**
```kotlin
private fun determineUserFlow() {
    val isFirst = PersistentDataManager.isFirstLaunch()
    val playerData = PersistentDataManager.gameData.value.playerData

    val flowState = when {
        isFirst -> UserFlowState.YENI_KULLANICI
        !playerData.isAlive -> UserFlowState.OLUM_SONRASI  // Umbros ekranına gider
        playerData.name.isBlank() -> UserFlowState.YENI_KULLANICI
        else -> UserFlowState.GERI_DONEN_KULLANICI
    }

    // ...
}
```

**SONRA (Ölüm Döngüsü Entegrasyonu):**
```kotlin
private fun determineUserFlow() {
    val isFirst = PersistentDataManager.isFirstLaunch()
    val playerData = PersistentDataManager.gameData.value.playerData
    val deathArchive = PersistentDataManager.gameData.value.deathArchive

    // ✅ YENİ: Ölüm sayısını hesapla
    val deathCount = deathArchive.size
    val isReturningFromDeath = deathCount > 0 && playerData.name.isBlank()

    val flowState = when {
        isReturningFromDeath -> UserFlowState.YENI_KULLANICI  // ← ÖZEL DURUM
        isFirst -> UserFlowState.YENI_KULLANICI
        !playerData.isAlive -> UserFlowState.OLUM_SONRASI
        playerData.name.isBlank() -> UserFlowState.YENI_KULLANICI
        else -> UserFlowState.GERI_DONEN_KULLANICI
    }

    // ✅ YENİ: Ölümden dönen kullanıcı için özel playlist
    if (isReturningFromDeath) {
        viewModelScope.launch {
            generateDeathEchoContent(deathCount)  // ← YENİ FONKSİYON
        }
    } else if (flowState == UserFlowState.GERI_DONEN_KULLANICI) {
        viewModelScope.launch {
            generatePersonalizedContent()
        }
    }

    // UI State güncellemesi...
    _uiState.value = UserEntryUiState(
        flowState = flowState,
        deathCount = deathCount,  // ← YENİ ALAN
        // ...
    )
}

/**
 * Ölümden dönen kullanıcı için "ruh yankısı" içerikler üretir
 * TODO-DEATH-01: Death Echo Content Generation
 */
private fun generateDeathEchoContent(deathCount: Int) {
    try {
        GameLogger.logSystem("💀 Ölüm yankısı içerik oluşturuluyor... (Ölüm sayısı: $deathCount)")

        // Oyuncunun güncel PlayerState'ini al (boş karaktersin dola kullanışlı değil)
        val playerState = gameStateManager.gameState.value.playerState

        // Ölüm arşivinden son ölüm kaydını al
        val lastDeath = PersistentDataManager.gameData.value.deathArchive.lastOrNull()

        // IntelligentContentEngine ile özel playlist oluştur
        val playlist = intelligentContentEngine.generateDeathEchoPlaylist(
            deathCount = deathCount,
            lastDeathRecord = lastDeath,
            maxVideos = 8,  // Daha az video (ölüm teması ağır olmamalı)
            maxPhotos = 8
        )

        // UI State'i güncelle
        _uiState.value = _uiState.value.copy(
            dynamicVideoIds = playlist.videos,
            dynamicPhotoIds = playlist.photos,
            isDeathEcho = true  // ← YENİ BAYRAK
        )

        GameLogger.logSystem("💀 Ölüm yankısı hazır: ${playlist.videos.size} video, ${playlist.photos.size} fotoğraf")

    } catch (e: Exception) {
        GameLogger.logError("IntelligentContentEngine", "Ölüm yankısı hatası", e)
    }
}
```

**Değişiklik Özeti:**
1. `deathCount` hesaplanır
2. `isReturningFromDeath` yeni kullanıcı mı ama ölüm geçmişi var mı kontrol eder
3. Eğer ölümden dönüyorsa `generateDeathEchoContent()` çağrılır
4. UI State'e `deathCount` ve `isDeathEcho` bayrakları eklenir

---

### 3.3 IntelligentContentEngine Pseudo-Kod

**Dosya:** `IntelligentContentEngine.kt`

#### YENİ FONKSİYON: generateDeathEchoPlaylist()

```kotlin
/**
 * Ölümden dönen oyuncu için "ruh yankısı" playlist oluşturur
 * TODO-DEATH-02: Death Echo Playlist Algorithm
 *
 * @param deathCount Toplam ölüm sayısı
 * @param lastDeathRecord Son ölüm kaydı (opsiyonel)
 * @param maxVideos Maksimum video sayısı
 * @param maxPhotos Maksimum fotoğraf sayısı
 * @return Kişiselleştirilmiş playlist
 */
fun generateDeathEchoPlaylist(
    deathCount: Int,
    lastDeathRecord: DeathRecord?,
    maxVideos: Int = 8,
    maxPhotos: Int = 8
): PersonalizedPlaylist {
    Log.d(TAG, "💀 Ölüm yankısı playlist'i oluşturuluyor...")
    Log.d(TAG, "   Ölüm sayısı: $deathCount")
    Log.d(TAG, "   Son ölüm: ${lastDeathRecord?.characterName} (${lastDeathRecord?.deathCause})")

    // 1. Medya veritabanını yükle
    val database = loadOrCreateDatabase()

    // 2. Duygusal derinliği ölüm sayısına göre belirle
    val emotionalDepth = when {
        deathCount >= 6 -> "D3"  // Varoluşsal kriz (6+ ölüm)
        deathCount >= 3 -> "D2"  // Derin keder (3-5 ölüm)
        else -> "D1"             // Yüzeysel keder (1-2 ölüm)
    }

    // 3. POSTDEATH_USER içeriklerini filtrele
    val postDeathVideos = database.videos.filter { video ->
        video.screenType.uppercase().contains("POSTDEATH") &&
        isDepthCompatible(video.depth, emotionalDepth)
    }

    val postDeathPhotos = database.photos.filter { photo ->
        photo.screenType.uppercase().contains("POSTDEATH") &&
        isDepthCompatible(photo.depth, emotionalDepth)
    }

    // 4. Eğer yeterli POSTDEATH içeriği yoksa, NEWUSER içerikleriyle tamamla
    val fallbackVideos = if (postDeathVideos.size < maxVideos) {
        val needed = maxVideos - postDeathVideos.size
        database.videos.filter { video ->
            video.screenType.uppercase().contains("NEWUSER") &&
            isDepthCompatible(video.depth, emotionalDepth) &&
            // Ölüm temasıyla uyumlu duygular: SADNESS, CALM, FEAR
            video.emotion.uppercase() in listOf("SADNESS", "CALM", "FEAR")
        }.take(needed)
    } else emptyList()

    val fallbackPhotos = if (postDeathPhotos.size < maxPhotos) {
        val needed = maxPhotos - postDeathPhotos.size
        database.photos.filter { photo ->
            photo.screenType.uppercase().contains("NEWUSER") &&
            isDepthCompatible(photo.depth, emotionalDepth) &&
            photo.emotion.uppercase() in listOf("SADNESS", "CALM", "FEAR")
        }.take(needed)
    } else emptyList()

    // 5. Listeleri birleştir
    val finalVideos = (postDeathVideos + fallbackVideos).take(maxVideos)
    val finalPhotos = (postDeathPhotos + fallbackPhotos).take(maxPhotos)

    // 6. Öncelik sırasına göre sırala
    val sortedVideos = sortByDeathRelevance(finalVideos, lastDeathRecord)
    val sortedPhotos = sortByDeathRelevance(finalPhotos, lastDeathRecord)

    // 7. Log çıktısı
    logDeathEchoStats(sortedVideos, sortedPhotos, deathCount, emotionalDepth)

    return PersonalizedPlaylist(
        videos = sortedVideos.map { it.resourceId },
        photos = sortedPhotos.map { it.resourceId },
        videoMatches = sortedVideos.map { MediaMatch(it, 1.0f, MatchDetails()) },
        photoMatches = sortedPhotos.map { MediaMatch(it, 1.0f, MatchDetails()) },
        characterProfile = CharacterProfile(
            mainArchetype = "EXPLORER",  // Varsayılan
            dominantEmotion = "SADNESS",  // Ölüm teması
            emotionalDepth = emotionalDepth
        )
    )
}

/**
 * Medyaları ölüm ilgisine göre sıralar
 */
private fun sortByDeathRelevance(
    mediaList: List<MediaDatabaseBuilder.MediaMetadata>,
    lastDeath: DeathRecord?
): List<MediaDatabaseBuilder.MediaMetadata> {
    return mediaList.sortedByDescending { media ->
        var relevance = 0f

        // POSTDEATH içerikler en yüksek öncelik
        if (media.screenType.uppercase().contains("POSTDEATH")) {
            relevance += 100f
        }

        // Ölüm sebebiyle uyumlu nitelikler
        lastDeath?.let { death ->
            when (death.deathCause.uppercase()) {
                "COMBAT" -> {
                    if (media.primaryAttribute.uppercase() == "VIOLENCE") relevance += 50f
                }
                "HUNGER" -> {
                    if (media.emotion.uppercase() == "SADNESS") relevance += 50f
                }
                // Diğer ölüm sebepleri...
            }
        }

        // SADNESS, CALM, FEAR duygularına öncelik
        if (media.emotion.uppercase() in listOf("SADNESS", "CALM", "FEAR")) {
            relevance += 30f
        }

        relevance
    }
}

/**
 * Ölüm yankısı istatistiklerini loglar
 */
private fun logDeathEchoStats(
    videos: List<MediaDatabaseBuilder.MediaMetadata>,
    photos: List<MediaDatabaseBuilder.MediaMetadata>,
    deathCount: Int,
    emotionalDepth: String
) {
    Log.d(TAG, "═══════════════════════════════════════")
    Log.d(TAG, "💀 ÖLÜM YANKISI PLAYLIST HAZIR")
    Log.d(TAG, "═══════════════════════════════════════")
    Log.d(TAG, "Ölüm Sayısı: $deathCount")
    Log.d(TAG, "Duygusal Derinlik: $emotionalDepth")
    Log.d(TAG, "📹 Video sayısı: ${videos.size}")
    Log.d(TAG, "📸 Fotoğraf sayısı: ${photos.size}")

    val postDeathCount = videos.count { it.screenType.uppercase().contains("POSTDEATH") }
    val fallbackCount = videos.size - postDeathCount

    Log.d(TAG, "   ↳ POSTDEATH içerikler: $postDeathCount")
    Log.d(TAG, "   ↳ Fallback (NEWUSER): $fallbackCount")

    if (videos.isNotEmpty()) {
        Log.d(TAG, "\n💀 ÖNE ÇIKAN İÇERİKLER:")
        videos.take(3).forEach { media ->
            Log.d(TAG, "  • ${media.fileName}")
            Log.d(TAG, "    ↳ ${media.screenType} / ${media.emotion}")
        }
    }

    Log.d(TAG, "═══════════════════════════════════════")
}
```

**Algorithm Özeti:**
1. Ölüm sayısına göre duygusal derinlik belirlenir (D1/D2/D3)
2. POSTDEATH_USER içerikler öncelik alır
3. Yetersiz POSTDEATH varsa, uyumlu NEWUSER içerikleri eklenir
4. Son ölüm sebebine göre içerikler sıralanır
5. SADNESS, CALM, FEAR duygularına öncelik verilir

---

## 4. Sistem Çapında Etki Analizi

### 4.1 GameSettings: "Ölümü Test Et" Butonu Entegrasyonu

**Dosya:** `app/src/main/java/com/example/isekaikuroshin/ui/settings/DeveloperOptionsSection.kt`

#### Mevcut İşleyiş (Satır 580-593):

```kotlin
// Ölümü test et onay dialogu
DeathTestDialog(
    onConfirm = {
        // Oyuncuyu ölü olarak işaretle
        PersistentDataManager.updatePlayerData { playerData ->
            playerData.copy(isAlive = false)
        }

        // Umbros transition'a yönlendir
        navController.navigate("umbros_transition") {
            popUpTo("settings") { inclusive = false }
        }
    }
)
```

#### ✅ **ÖNERİLEN GELİŞTİRME:**

```kotlin
DeathTestDialog(
    onConfirm = {
        // 1. Ölüm kaydı oluştur (gerçek bir ölüm gibi)
        PersistentDataManager.recordDeath("DEBUG_TEST")

        // 2. Oyuncuyu ölü olarak işaretle
        PersistentDataManager.updatePlayerData { playerData ->
            playerData.copy(isAlive = false)
        }

        // 3. Umbros transition'a yönlendir
        navController.navigate("umbros_transition") {
            popUpTo("settings") { inclusive = false }
        }

        GameLogger.logSystem("🧪 DEBUG: Ölüm testi yapıldı - DeathArchive güncellendi")
    }
)
```

**Değişiklik:**
- `PersistentDataManager.recordDeath("DEBUG_TEST")` çağrısı eklendi
- Bu, ölüm arşivine gerçek bir kayıt ekler
- Sonraki yeniden başlatmada POSTDEATH içerikleri gösterilir

---

### 4.2 UmbrosTransitionScreen: "Reddet" Seçeneği

**Dosya:** `app/src/main/java/com/example/isekaikuroshin/ui/dashboard/UmbrosTransitionScreen.kt`

#### Mevcut İşleyiş (Satır 353-361):

```kotlin
Button(
    onClick = {
        // Paktı reddet - dashboard'a dön
        PersistentDataManager.setFirstAIDialogVisitCompleted()
        navController.navigate("dashboard") {
            popUpTo("dashboard") { inclusive = false }
        }
        onTransitionComplete()
    }
)
```

#### ⚠️ **SORUN TESPİTİ:**

Oyuncu paktı reddederse `isAlive = false` kalır, dashboard'a döner ama **oyun oynanamaz**.

#### ✅ **ÖNERİLEN AKIŞ DEĞİŞİKLİĞİ:**

**Seçenek 1: Otomatik Sıfırlama (Önerilen)**
```kotlin
Button(
    onClick = {
        // Paktı reddet - oyunu sıfırla ve yeniden başlat
        viewModelScope.launch {
            GameLogger.logSystem("💀 Umbros anlaşması reddedildi - Oyun sıfırlanıyor...")

            // 1. Ölüm kaydı zaten var (recordDeath daha önce çağrıldı)

            // 2. Oyunu sıfırla
            gameStateManager.resetGame()  // Bu resetAllData()'yı çağırır (arşiv korunacak)

            // 3. Uygulamayı yeniden başlat (UserEntry ekranına dön)
            navController.navigate("user_entry") {
                popUpTo(0) { inclusive = true }  // Tüm back stack'i temizle
            }

            // 4. Başarı mesajı
            GameLogger.logSystem("✅ Yeni yolculuk başlıyor - Ölüm arşivi: ${PersistentDataManager.gameData.value.deathArchive.size} kayıt")
        }
    },
    text = { Text("REDDET VE YENİDEN BAŞLA") }
)
```

**Seçenek 2: Manuel Yeniden Başlatma**
```kotlin
Button(
    onClick = {
        // Kullanıcıya uyarı göster
        showDialog = true
    },
    text = { Text("REDDET") }
)

if (showDialog) {
    AlertDialog(
        title = { Text("Ölümü Kabul Et") },
        text = { Text("Umbros'un teklifini reddettiniz. Oyun sıfırlanacak ve yeni bir karakterle başlayacaksınız.") },
        onDismissRequest = { showDialog = false },
        confirmButton = {
            TextButton(onClick = {
                viewModelScope.launch {
                    gameStateManager.resetGame()
                    navController.navigate("user_entry") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }) {
                Text("YENİDEN BAŞLA")
            }
        },
        dismissButton = {
            TextButton(onClick = { showDialog = false }) {
                Text("İPTAL")
            }
        }
    )
}
```

**Önerilen:** **Seçenek 1** (Otomatik sıfırlama) - Daha akıcı deneyim.

---

### 4.3 PlayerState ve Veri Yönetimi

#### Sıfırlama İşlemi Sırasında Korunacak/Silinecek Veriler

| Veri Kategorisi | Korunmalı mı? | Gerekçe |
|-----------------|---------------|---------|
| **DeathArchive** | ✅ KORUNMALI | Ölüm geçmişi "ruh yankısı" için gerekli |
| **PlayerName, Level, Stats** | ❌ SİLİNMELİ | Yeni karakter baştan başlamalı |
| **Inventory, Equipment** | ❌ SİLİNMELİ | Yeni karakterin eşyası yok |
| **Quests, Completed Quests** | ❌ SİLİNMELİ | Yeni karakterin görevi yok |
| **Known Characters** | ❌ SİLİNMELİ | Yeni karakter kimseyi tanımıyor |
| **API Key** | ⚠️ TARTIŞMALI | Kullanıcı kararına bırakılabilir |
| **Language Progress** | ⚠️ TARTIŞMALI | Kullanıcı kararına bırakılabilir |
| **Game Settings (volume, vb.)** | ✅ KORUNMALI | Kullanıcı tercihleri korunmalı |

#### Önerilen `resetAllData()` İyileştirmesi

**ÖNCE:**
```kotlin
fun resetAllData() {
    _gameData.value = PersistentGameData()  // Her şeyi sil
    saveGameData()
    // ...
}
```

**SONRA:**
```kotlin
/**
 * Oyun verilerini sıfırlar, ancak belirli verileri korur
 * TODO-DEATH-03: Selective Reset Logic
 *
 * @param keepDeathArchive Ölüm arşivini koru (varsayılan: true)
 * @param keepSettings Ayarları koru (varsayılan: true)
 * @param keepAPIKey API key'i koru (varsayılan: false - güvenlik için)
 */
fun resetAllData(
    keepDeathArchive: Boolean = true,
    keepSettings: Boolean = true,
    keepAPIKey: Boolean = false
) {
    android.util.Log.d("PersistentDataManager",
        "🔄 Oyun sıfırlanıyor... (DeathArchive: $keepDeathArchive, Settings: $keepSettings, APIKey: $keepAPIKey)")

    val currentData = _gameData.value

    // Korunacak verileri sakla
    val preservedDeathArchive = if (keepDeathArchive) currentData.deathArchive else emptyList()
    val preservedSettings = if (keepSettings) currentData.gameSettings else GameSettings()
    val preservedDebugSettings = if (keepSettings) currentData.debugSettings else DebugSettings()

    // Yeni temiz veri oluştur, korunan verileri geri yükle
    _gameData.value = PersistentGameData(
        deathArchive = preservedDeathArchive,
        gameSettings = preservedSettings,
        debugSettings = preservedDebugSettings
    )
    saveGameData()

    // API Key kontrolü
    if (!keepAPIKey) {
        clearAPIKey()
    }

    // Dil pratiği sıfırlama...
    val languagesToClear = listOf("İngilizce", "Japanese", "Español", "Français", "Deutsch")
    for (language in languagesToClear) {
        val progressKey = "language_progress_$language"
        sharedPrefs.edit().remove(progressKey).apply()
    }

    // GameState sıfırlama...
    sharedPrefs.edit().remove("current_game_state").apply()

    android.util.Log.d("PersistentDataManager",
        "✅ Sıfırlama tamamlandı - DeathArchive: ${preservedDeathArchive.size} kayıt")
}
```

**Değişiklik Özeti:**
- `resetAllData()` artık parametrelerle kontrol edilebilir
- Varsayılan davranış: DeathArchive ve Settings korunur
- API Key varsayılan olarak silinir (güvenlik)
- Log mesajları iyileştirildi

---

## 5. Journey Ölümü Entegrasyonu

### 5.1 Sorunun Tanımı

**Kullanıcı İsteği:**
> "journelde ölünce umbros 'ölümü test et' butonuyla aynı işlem yapıyor dimi? yani ölüm mekaniğine ekletmeyi entegrasyonu düşünüyorum"

**Cevap:** **Evet, journal'de (macera sırasında) ölüm de aynı akışı tetiklemeli.**

---

### 5.2 Journal Ölümü Akışı

#### Mevcut Akış (DeathManager)

**Dosya:** `app/src/main/java/com/example/isekaikuroshin/engine/DeathManager.kt` (Satır 32-67)

```kotlin
fun checkForDeath(playerState: PlayerState): Boolean {
    if (playerState.currentHealth <= 0) {
        triggerDeathSequence(playerState)
        return true
    }
    return false
}

private suspend fun triggerDeathSequence(playerState: PlayerState) {
    // 1. Ölüm kaydı oluştur
    val deathEvent = DeathEvent(
        timestamp = System.currentTimeMillis(),
        playerLevel = playerState.level,
        causeOfDeath = determineCauseOfDeath(),
        finalPlayerState = playerState.copy()
    )

    // 2. Event yayın yap
    _deathEvent.tryEmit(deathEvent)

    // 3. Ölüm kaydını kaydet
    PersistentDataManager.recordDeath(deathEvent.causeOfDeath.name)

    // 4. Oyunu sıfırla
    gameStateManager.resetGame()

    GameLogger.logSystem("💀 Oyuncu öldü: ${deathEvent.causeOfDeath.name}")
}
```

#### ✅ **ÖNERİ: UI Navigation Entegrasyonu**

**Sorun:** `DeathManager` oyunu sıfırlıyor ama UI'ı Umbros ekranına yönlendirmiyor.

**Çözüm:** `GameViewModel` veya `AdventureViewModel` death event'ini dinlemeli:

```kotlin
// AdventureViewModel.kt veya MainViewModel.kt içinde

init {
    // DeathManager'ın death event'ini dinle
    viewModelScope.launch {
        deathManager.deathEvent.collect { deathEvent ->
            GameLogger.logSystem("💀 Ölüm event'i algılandı - UI yönlendiriliyor...")

            // UI'ı Umbros ekranına yönlendir
            _navigationEvent.emit(NavigationEvent.NavigateToUmbros(deathEvent))
        }
    }
}

sealed class NavigationEvent {
    data class NavigateToUmbros(val deathEvent: DeathEvent) : NavigationEvent()
    // Diğer navigation event'leri...
}
```

**MainActivity.kt içinde:**
```kotlin
// ViewModel'den navigation event'lerini dinle
LaunchedEffect(Unit) {
    viewModel.navigationEvent.collect { event ->
        when (event) {
            is NavigationEvent.NavigateToUmbros -> {
                navController.navigate("umbros_transition") {
                    popUpTo("dashboard") { inclusive = true }
                }
            }
            // Diğer event'ler...
        }
    }
}
```

---

### 5.3 Entegrasyon Özeti

| Ölüm Kaynağı | Tetikleme Yöntemi | DeathArchive Kaydı | UI Yönlendirme | Sıfırlama |
|--------------|-------------------|-------------------|----------------|-----------|
| **Journal (Combat)** | `DeathManager.checkForDeath()` | ✅ `recordDeath()` | ✅ Umbros ekranı | ✅ `resetGame()` |
| **Debug Test** | "Ölümü Test Et" butonu | ✅ `recordDeath()` | ✅ Umbros ekranı | ❌ Manuel sıfırlama |
| **Umbros Reddi** | "Reddet" butonu | ⚠️ Zaten kayıtlı | ✅ UserEntry ekranı | ✅ `resetGame()` |

**Sonuç:** Tüm ölüm kaynakları aynı akışı kullanır - **tutarlılık sağlanmış** ✅

---

## 6. Uygulama Planı (Faz Bazlı)

### Faz 1: Veri Altyapısı Düzeltmesi (1 gün)

**Görevler:**
1. ✅ `PersistentDataManager.resetAllData()` düzeltmesi (keepDeathArchive parametresi)
2. ✅ Unit test: DeathArchive'in korunduğunu doğrula
3. ✅ `UserEntryUiState`'e `deathCount` ve `isDeathEcho` alanları ekle

**Kod Değişiklikleri:**
- `PersistentDataManager.kt` (Satır 608-639)
- `UserEntryViewModel.kt` (Satır 22-28)

---

### Faz 2: gorsel_etiketleyici.py Güncellemesi (1 gün)

**Görevler:**
1. ✅ `etiket_config.json`'a `POSTDEATH_USER` ekle
2. ✅ Örnek medya dosyalarını etiketle (5-10 adet test videosu)
3. ✅ `MediaDatabaseBuilder` testi: POSTDEATH içerikleri okuyabildiğini doğrula

**Dosya Değişiklikleri:**
- `gorsel_etiketleyici/etiket_config.json`
- Test medya dosyaları: `res/raw/vid_postdeath_*.mp4`

---

### Faz 3: IntelligentContentEngine Genişletmesi (2 gün)

**Görevler:**
1. ✅ `generateDeathEchoPlaylist()` fonksiyonu implement et
2. ✅ `sortByDeathRelevance()` yardımcı fonksiyonu ekle
3. ✅ `logDeathEchoStats()` log fonksiyonu ekle
4. ✅ Unit test: Ölüm sayısına göre doğru derinlik seçimi

**Kod Değişiklikleri:**
- `IntelligentContentEngine.kt` (~100 satır yeni kod)

---

### Faz 4: UserEntryViewModel Entegrasyonu (1 gün)

**Görevler:**
1. ✅ `determineUserFlow()` mantığını genişlet (isReturningFromDeath)
2. ✅ `generateDeathEchoContent()` fonksiyonu ekle
3. ✅ Integration test: Ölümden dönüş akışı

**Kod Değişiklikleri:**
- `UserEntryViewModel.kt` (Satır 46-106)

---

### Faz 5: UI ve Navigation Entegrasyonu (2 gün)

**Görevler:**
1. ✅ UmbrosTransitionScreen "Reddet" butonu akışını güncelle
2. ✅ DeathManager event'ini ViewModel'de dinle
3. ✅ AdventureScreen'den Umbros'a yönlendirme
4. ✅ End-to-end test: Combat ölümünden yeniden başlatmaya kadar

**Kod Değişiklikleri:**
- `UmbrosTransitionScreen.kt` (Satır 353-361)
- `AdventureViewModel.kt` (yeni navigation event listener)
- `MainActivity.kt` (navigation event handler)

---

### Faz 6: Test ve Optimizasyon (2 gün)

**Görevler:**
1. ✅ Manuel test: 5 kez ölüp yeniden başla, arşiv korunuyor mu?
2. ✅ Log analizi: POSTDEATH içerikler doğru gösteriliyor mu?
3. ✅ Performance test: Playlist oluşturma süresi
4. ✅ Bug fixing

**Toplam Tahmini Süre:** **9 gün** (tek developer için)

---

## 7. Riskler ve Çözüm Önerileri

### Risk 1: DeathArchive Veri Kaybı

**Risk:** `resetAllData()` hala arşivi silebilir

**Çözüm:**
- ✅ Defensive programming: `resetAllData(keepDeathArchive = true)` varsayılan davranış
- ✅ Unit test: Arşiv korunma kontrolü
- ✅ Log mesajları: Her sıfırlamada arşiv sayısı loglanmalı

**Risk Seviyesi:** DÜŞÜK (düzeltme basit)

---

### Risk 2: Yetersiz POSTDEATH İçerik

**Risk:** Kullanıcı 20 kez ölürse ama sadece 5 POSTDEATH videosu varsa?

**Çözüm:**
- ✅ Fallback mekanizması: NEWUSER içerikleriyle tamamla
- ✅ Log uyarısı: "POSTDEATH içerik yetersiz, fallback kullanılıyor"
- ✅ Gelecekte daha fazla POSTDEATH medyası ekle

**Risk Seviyesi:** DÜŞÜK (fallback zaten tasarlandı)

---

### Risk 3: UI Navigation Karışıklığı

**Risk:** Oyuncu Umbros'tan çıkınca dashboard'a mı UserEntry'e mi gitmeli?

**Çözüm:**
- ✅ Net akış tanımı:
  - **Kabul Et** → Dashboard (isAlive = true, resurrection)
  - **Reddet** → UserEntry (resetGame, isAlive sıfırlanır)
- ✅ Integration test: Her iki akışı test et

**Risk Seviyesi:** ORTA (UX hassasiyeti)

---

## 8. Başarı Metrikleri (KPI)

### Teknik Metrikler

| Metrik | Hedef | Ölçüm Yöntemi |
|--------|-------|---------------|
| DeathArchive Korunma Oranı | %100 | Unit test |
| POSTDEATH İçerik Gösterim Oranı | >%50 | Log analizi |
| Playlist Oluşturma Süresi | <500ms | Performance test |
| Ölümden Yeniden Başlatmaya Süre | <3 saniye | UI test |

### Kullanıcı Deneyimi Metrikleri

| Metrik | Hedef | Ölçüm Yöntemi |
|--------|-------|---------------|
| Kullanıcı Ölüm Arşivi Büyüklüğü | Ortalama 3-5 kayıt | Analytics |
| POSTDEATH Video İzlenme Oranı | >%70 | Video analytics |
| Umbros Reddetme Oranı | ~%30-40 | Analytics |

---

## 9. Sonuç ve Öneriler

### Teknik Fizibilite: ✅ YÜKSEK

**Nedenleri:**
1. Mevcut `DeathArchive` mekanizması zaten var
2. `IntelligentContentEngine` kolayca genişletilebilir
3. Minimal kod değişikliği (toplam ~500 satır)
4. Backward compatible (eski kayıtlar bozulmaz)

---

### Önerilen Strateji: **SEÇENek A (POSTDEATH_USER)**

**Gerekçe:**
- Semantik açıklık
- Kolay implementasyon
- Esnek derinlik kullanımı
- NEWUSER ile uyumlu

---

### Kritik Eylem Maddeleri

1. **Hemen Yapılacaklar:**
   - [ ] `PersistentDataManager.resetAllData()` bug fix
   - [ ] `gorsel_etiketleyici.py` güncelleme
   - [ ] 5-10 test POSTDEATH medyası oluştur

2. **Orta Vadede:**
   - [ ] `IntelligentContentEngine.generateDeathEchoPlaylist()` implement
   - [ ] UmbrosTransitionScreen akış düzeltmesi
   - [ ] End-to-end test

3. **Uzun Vadede:**
   - [ ] 50+ POSTDEATH medya içeriği üret
   - [ ] Ölüm sebepçrine göre özel içerikler (combat vs hunger vs umbros)
   - [ ] Analytics entegrasyonu

---

## 10. Kod Değişiklikleri Özeti

### Değişecek Dosyalar (Toplam: 6 dosya)

| # | Dosya | Satırlar | Değişiklik Türü |
|---|-------|---------|-----------------|
| 1 | `PersistentDataManager.kt` | 608-639 | Bug fix + parametre ekleme |
| 2 | `UserEntryViewModel.kt` | 22-28, 46-106 | Yeni fonksiyon + UI State |
| 3 | `IntelligentContentEngine.kt` | +100 satır | Yeni fonksiyon |
| 4 | `UmbrosTransitionScreen.kt` | 353-361 | Akış değişikliği |
| 5 | `AdventureViewModel.kt` | +30 satır | Event listener |
| 6 | `gorsel_etiketleyici/etiket_config.json` | +1 satır | Yeni kategori |

**Toplam Yeni Kod:** ~200 satır
**Toplam Değişen Kod:** ~50 satır
**Risk Seviyesi:** ORTA (kritik sistemlere dokunulmuyor)

---

## Ek A: Örnek Medya Dosya İsimleri

### POSTDEATH_USER İçerikleri

```
# 1. Ölüm İçin (D1)
VID_POSTDEATH_REGRET_SADNESS_D1_001.mp4       // "Pişmanlık" teması
VID_POSTDEATH_ACCEPTANCE_CALM_D1_001.mp4      // "Kabullenme" teması
PHT_POSTDEATH_MEMENTO_SADNESS_D1_001.png      // Hatıra fotoğrafı

# 3-5. Ölüm İçin (D2)
VID_POSTDEATH_DESPAIR_SADNESS_D2_001.mp4      // "Umutsuzluk" teması
VID_POSTDEATH_REFLECTION_CALM_D2_001.mp4      // "Yansıma" teması
PHT_POSTDEATH_GRAVE_SADNESS_D2_001.png        // Mezar fotoğrafı

# 6+ Ölüm İçin (D3)
VID_POSTDEATH_VOID_FEAR_D3_001.mp4            // "Boşluk" teması
VID_POSTDEATH_REINCARNATION_CALM_D3_001.mp4   // "Reenkarnasyon" teması
PHT_POSTDEATH_AFTERLIFE_CALM_D3_001.png       // Öteki dünya fotoğrafı
```

---

## Ek B: Test Senaryoları

### Senaryo 1: İlk Ölüm

1. Oyuncu combatte ölür (`HP <= 0`)
2. `DeathManager.triggerDeathSequence()` çağrılır
3. Ölüm kaydı arşive eklenir (deathCount = 1)
4. Oyun sıfırlanır, Umbros ekranı gösterilir
5. Oyuncu paktı reddeder
6. UserEntry ekranı açılır
7. `generateDeathEchoContent(deathCount=1)` çalışır
8. POSTDEATH_D1 içerikler + NEWUSER fallback gösterilir

**Beklenen Sonuç:**
- ✅ DeathArchive: 1 kayıt
- ✅ Playlist: 6-8 video (50% POSTDEATH, 50% NEWUSER)
- ✅ Duygular: SADNESS, CALM ağırlıklı

---

### Senaryo 2: 5. Ölüm

1. Oyuncu 5. kez ölür
2. Arşive 5. kayıt eklenir (deathCount = 5)
3. `generateDeathEchoContent(deathCount=5)` çalışır
4. Duygusal derinlik: D2 (3-5 ölüm)
5. POSTDEATH_D2 içerikler öncelik alır

**Beklenen Sonuç:**
- ✅ DeathArchive: 5 kayıt
- ✅ Playlist: D2 derinliği içerikler
- ✅ Duygular: Daha derin (DESPAIR, REFLECTION)

---

## Ek C: Log Örneği

```
💀 Ölüm event'i algılandı - UI yönlendiriliyor...
🔄 Oyun sıfırlanıyor... (DeathArchive: true, Settings: true, APIKey: false)
✅ Sıfırlama tamamlandı - DeathArchive: 5 kayıt
🧠 Akıllı Kalp motoru başlatılıyor...
💀 Ölüm yankısı playlist'i oluşturuluyor...
   Ölüm sayısı: 5
   Son ölüm: Kuroshin (COMBAT)
═══════════════════════════════════════
💀 ÖLÜM YANKISI PLAYLIST HAZIR
═══════════════════════════════════════
Ölüm Sayısı: 5
Duygusal Derinlik: D2
📹 Video sayısı: 8
📸 Fotoğraf sayısı: 8
   ↳ POSTDEATH içerikler: 6
   ↳ Fallback (NEWUSER): 2

💀 ÖNE ÇIKAN İÇERİKLER:
  • vid_postdeath_despair_sadness_d2_001
    ↳ POSTDEATH_USER / SADNESS
  • vid_postdeath_reflection_calm_d2_001
    ↳ POSTDEATH_USER / CALM
  • vid_newuser_chaos_sadness_d2_001
    ↳ NEWUSER / SADNESS
═══════════════════════════════════════
✨ Kişiselleştirilmiş içerik hazır: 8 video, 8 fotoğraf
👤 Karakter Profili: EXPLORER / SADNESS
```

---

## Sonuç

**"Ölüm Döngüsü" mekaniği teknik olarak mümkündür ve mevcut sistemle uyumludur.**

**Kritik Başarı Faktörleri:**
1. `PersistentDataManager.resetAllData()` bug fix
2. Yeterli POSTDEATH medya içeriği üretimi
3. UX akışının netleştirilmesi (Umbros → UserEntry)

**Sonraki Adım:** Faz 1 (Veri Altyapısı Düzeltmesi) ile başlanabilir.

---

**Rapor Sonu**
