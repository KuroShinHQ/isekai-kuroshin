package com.example.isekaikuroshin.engine

import android.content.Context
import android.util.Log
import com.example.isekaikuroshin.data.PlayerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * IntelligentContentEngine - "Akıllı Kalp" Medya Seçim Motoru
 *
 * Bu motor, oyuncunun AI Psikolog tarafından oluşturulan Karakter Profili'ni kullanarak,
 * en uygun medya içeriklerini seçer. "Bölüm 6"da tasarlanan Uyum Puanı algoritmasını uygular.
 *
 * Temel Prensipler:
 * 1. Arketip Eşleşmesi: Oyuncunun dominant arketipiyle medyanın arketip etiketleri karşılaştırılır
 * 2. Nitelik Eşleşmesi: ORDER/CHAOS gibi ana niteliklerin uyumu
 * 3. Duygu Eşleşmesi: Oyuncunun dominant duygularıyla medya duygularının uyumu
 * 4. Derin Çukur Kuralı: Sadece oyuncunun seviyesine uygun derinlik içerikleri gösterilir
 * 5. İç Çatışma Bonusu: Birbiriyle çelişen özellikler ekstra puan kazandırır
 */
@Singleton
class IntelligentContentEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "IntelligentContentEngine"

        // Arketip isimleri ve eşleşmeleri
        private val ARCHETYPE_MAPPINGS = mapOf(
            "WARRIOR" to listOf("savasci", "warrior", "violence", "conflict"),
            "EXPLORER" to listOf("kasif", "explorer", "journey", "adventure"),
            "CRAFTSMAN" to listOf("zanaatkar", "craftsman", "order", "creation"),
            "MYSTIC" to listOf("mistik", "mystic", "chaos", "mystery"),
            "UMBROS" to listOf("umbros", "shadow", "darkness", "evil")
        )

        // Duygu eşleşmeleri
        private val EMOTION_MAPPINGS = mapOf(
            "JOY" to listOf("joy", "happy", "happiness", "sevinc", "mutluluk"),
            "SADNESS" to listOf("sadness", "sad", "grief", "uzuntu", "keder"),
            "ANGER" to listOf("anger", "angry", "rage", "ofke", "hiddet"),
            "FEAR" to listOf("fear", "afraid", "terror", "korku", "dehset"),
            "CALM" to listOf("calm", "peace", "neutral", "sakinlik", "huzur")
        )

        // Nitelik eşleşmeleri
        private val ATTRIBUTE_MAPPINGS = mapOf(
            "ORDER" to listOf("order", "law", "discipline", "duzen", "kural"),
            "CHAOS" to listOf("chaos", "disorder", "freedom", "kaos", "ozgurluk"),
            "VIOLENCE" to listOf("violence", "combat", "aggression", "siddet", "savas"),
            "PEACE" to listOf("peace", "harmony", "calm", "baris", "huzur"),
            "CONFLICT" to listOf("conflict", "struggle", "challenge", "catisma", "mucadele")
        )
    }

    /**
     * Karakter Profili - AI Psikolog'dan gelen oyuncu analizi
     */
    @Serializable
    data class CharacterProfile(
        val mainArchetype: String = "BILINMIYOR",           // Ana arketip
        val secondaryArchetype: String? = null,             // İkincil arketip
        val dominantEmotion: String = "CALM",               // Baskın duygu
        val secondaryEmotion: String? = null,               // İkincil duygu
        val keyAttributes: List<String> = emptyList(),      // Ana nitelikler (ORDER, CHAOS, vb.)
        val attributeStrengths: Map<String, Float> = emptyMap(), // Nitelik güçleri (0.0 - 1.0)
        val emotionalDepth: String = "D1",                  // Duygusal derinlik seviyesi (D1, D2, D3)
        val internalConflicts: List<Pair<String, String>> = emptyList() // Çatışan nitelikler
    )

    /**
     * Medya eşleşme sonucu
     */
    data class MediaMatch(
        val metadata: MediaDatabaseBuilder.MediaMetadata,
        val compatibilityScore: Float,                      // Uyum puanı (0.0 - 1.0)
        val matchDetails: MatchDetails                      // Detaylı eşleşme bilgisi
    )

    /**
     * Eşleşme detayları
     */
    data class MatchDetails(
        val archetypeMatch: Float = 0f,      // Arketip eşleşme puanı
        val emotionMatch: Float = 0f,        // Duygu eşleşme puanı
        val attributeMatch: Float = 0f,      // Nitelik eşleşme puanı
        val depthMatch: Boolean = true,      // Derinlik uygun mu?
        val conflictBonus: Float = 0f        // İç çatışma bonusu
    )

    private val databaseBuilder = MediaDatabaseBuilder(context)
    private var cachedDatabase: MediaDatabaseBuilder.MediaDatabase? = null

    /**
     * Ana fonksiyon: Oyuncunun profiline göre kişiselleştirilmiş oynatma listesi oluşturur
     */
    fun generatePersonalizedPlaylist(
        playerState: PlayerState,
        maxVideos: Int = 10,
        maxPhotos: Int = 10
    ): PersonalizedPlaylist {
        Log.d(TAG, "🎯 Kişiselleştirilmiş playlist oluşturuluyor...")

        // 1. Medya veritabanını yükle veya oluştur
        val database = loadOrCreateDatabase()

        // 2. PlayerState'ten Karakter Profili'ni çıkar
        val characterProfile = extractCharacterProfile(playerState)
        Log.d(TAG, "👤 Karakter Profili: ${characterProfile.mainArchetype}, ${characterProfile.dominantEmotion}, Derinlik: ${characterProfile.emotionalDepth}")

        // GÖREV: First User vs Returning User filtreleme
        // Death Archive kontrolü ile profil tipini belirle (GameData'dan al)
        val gameData = com.example.isekaikuroshin.data.PersistentDataManager.gameData.value
        val deathCount = gameData.deathArchive.size
        val isFirstUser = deathCount == 0
        val userProfileType = if (isFirstUser) "FIRSTUSER" else "RETURNINGUSER"

        Log.d(TAG, "👤 Kullanıcı Profili: $userProfileType (Death Count: $deathCount)")

        // 3. Profil tipine göre medyaları filtrele
        val filteredVideos = database.videos.filter { video ->
            video.screenType.uppercase().contains(userProfileType) ||
            video.screenType.uppercase().contains("NEWUSER") // NEWUSER herkese uygun
        }

        val filteredPhotos = database.photos.filter { photo ->
            photo.screenType.uppercase().contains(userProfileType) ||
            photo.screenType.uppercase().contains("NEWUSER") // NEWUSER herkese uygun
        }

        Log.d(TAG, "📁 Filtrelenmiş videolar: ${filteredVideos.size} (${userProfileType})")
        Log.d(TAG, "📁 Filtrelenmiş fotoğraflar: ${filteredPhotos.size} (${userProfileType})")

        // 4. Filtrelenmiş medyaları puanla
        val scoredVideos = scoreMediaList(filteredVideos, characterProfile)
        val scoredPhotos = scoreMediaList(filteredPhotos, characterProfile)

        // 4. En yüksek puanlı içerikleri seç
        val topVideos = scoredVideos
            .sortedByDescending { it.compatibilityScore }
            .take(maxVideos)

        val topPhotos = scoredPhotos
            .sortedByDescending { it.compatibilityScore }
            .take(maxPhotos)

        // 5. Log çıktısı
        logPlaylistStats(topVideos, topPhotos)

        return PersonalizedPlaylist(
            videos = topVideos.map { it.metadata.resourceId },
            photos = topPhotos.map { it.metadata.resourceId },
            videoMatches = topVideos,
            photoMatches = topPhotos,
            characterProfile = characterProfile
        )
    }

    /**
     * PlayerState'ten Karakter Profili çıkarır
     */
    private fun extractCharacterProfile(playerState: PlayerState): CharacterProfile {
        val profile = playerState.playerProfile

        // Arketip skorlarından dominant arketipi bul
        var mainArchetype = profile.dominantArchetype.uppercase()

        // ✅ YENİ: Yeni kullanıcılar için fallback
        // Eğer arketip henüz belirlenmemişse, genel kategoriye ata
        if (mainArchetype == "BILINMIYOR" || mainArchetype.isBlank()) {
            mainArchetype = "EXPLORER"  // Varsayılan: Tüm yeni oyuncular EXPLORER olarak başlar
            Log.d(TAG, "⚠️ Arketip belirlenmemiş, varsayılan olarak EXPLORER atandı")
        }

        // Nitelikleri PlayerState'ten çıkar
        val keyAttributes = mutableListOf<String>()
        val attributeStrengths = mutableMapOf<String, Float>()

        // Morality score'a göre ORDER vs CHAOS belirle
        when {
            playerState.moralityScore > 0.3f -> {
                keyAttributes.add("ORDER")
                attributeStrengths["ORDER"] = playerState.moralityScore
            }
            playerState.moralityScore < -0.3f -> {
                keyAttributes.add("CHAOS")
                attributeStrengths["CHAOS"] = abs(playerState.moralityScore)
            }
            else -> {
                keyAttributes.add("NEUTRAL")
                attributeStrengths["NEUTRAL"] = 0.5f
            }
        }

        // Holiness/Unholy points'e göre ek nitelikler
        if (playerState.holinessPoints > 50) {
            keyAttributes.add("PEACE")
            attributeStrengths["PEACE"] = playerState.holinessPoints / 100f
        }
        if (playerState.unholyPoints > 50) {
            keyAttributes.add("VIOLENCE")
            attributeStrengths["VIOLENCE"] = playerState.unholyPoints / 100f
        }

        // Duygusal derinliği seviyeye göre belirle
        val emotionalDepth = when {
            playerState.level >= 30 -> "D3"
            playerState.level >= 15 -> "D2"
            else -> "D1"
        }

        // İç çatışmaları tespit et (zıt nitelikler varsa)
        val conflicts = mutableListOf<Pair<String, String>>()
        if (keyAttributes.contains("ORDER") && keyAttributes.contains("CHAOS")) {
            conflicts.add("ORDER" to "CHAOS")
        }
        if (keyAttributes.contains("PEACE") && keyAttributes.contains("VIOLENCE")) {
            conflicts.add("PEACE" to "VIOLENCE")
        }

        return CharacterProfile(
            mainArchetype = mainArchetype,
            dominantEmotion = inferDominantEmotion(playerState),
            keyAttributes = keyAttributes,
            attributeStrengths = attributeStrengths,
            emotionalDepth = emotionalDepth,
            internalConflicts = conflicts
        )
    }

    /**
     * PlayerState'ten baskın duyguyu çıkarır
     */
    private fun inferDominantEmotion(playerState: PlayerState): String {
        return when {
            playerState.moralityScore > 0.5f -> "JOY"
            playerState.moralityScore < -0.5f -> "SADNESS"
            playerState.unholyPoints > 70 -> "ANGER"
            playerState.holinessPoints > 70 -> "CALM"
            playerState.currentHealth < playerState.maxHealth * 0.3 -> "FEAR"
            else -> "CALM"
        }
    }

    /**
     * Medya listesini puanlar
     */
    private fun scoreMediaList(
        mediaList: List<MediaDatabaseBuilder.MediaMetadata>,
        profile: CharacterProfile
    ): List<MediaMatch> {
        return mediaList.mapNotNull { media ->
            val matchResult = calculateCompatibilityScore(media, profile)
            // ✅ YENİ: Minimum eşik düşürüldü - derinlik uygunsa göster
            if (matchResult.compatibilityScore >= 0 && matchResult.matchDetails.depthMatch) {
                matchResult
            } else {
                null
            }
        }
    }

    /**
     * UYUM PUANI ALGORİTMASI - "Akıllı Kalp"in beyni
     *
     * Bu fonksiyon, bir medya dosyasının oyuncunun profiliyle ne kadar uyumlu olduğunu hesaplar.
     */
    private fun calculateCompatibilityScore(
        media: MediaDatabaseBuilder.MediaMetadata,
        profile: CharacterProfile
    ): MediaMatch {
        var totalScore = 0f
        val details = MatchDetails()

        // 1. DERİNLİK FİLTRESİ - "Derin Çukur" Kuralı
        // Oyuncu sadece kendi seviyesine uygun derinlikteki içerikleri görebilir
        val depthMatch = isDepthCompatible(media.depth, profile.emotionalDepth)
        if (!depthMatch) {
            // Derinlik uygun değilse, bu medyayı tamamen atla
            return MediaMatch(media, 0f, details.copy(depthMatch = false))
        }

        // 2. ARKETİP EŞLEŞMESİ (Ağırlık: 0.35)
        val archetypeScore = calculateArchetypeMatch(media, profile)
        totalScore += archetypeScore * 0.35f

        // 3. NİTELİK EŞLEŞMESİ (Ağırlık: 0.30)
        val attributeScore = calculateAttributeMatch(media, profile)
        totalScore += attributeScore * 0.30f

        // 4. DUYGU EŞLEŞMESİ (Ağırlık: 0.25)
        val emotionScore = calculateEmotionMatch(media, profile)
        totalScore += emotionScore * 0.25f

        // 5. İÇ ÇATIŞMA BONUSU (Ağırlık: 0.10)
        val conflictBonus = calculateConflictBonus(media, profile)
        totalScore += conflictBonus * 0.10f

        // Final skoru 0.0 - 1.0 arasında normalize et
        val finalScore = totalScore.coerceIn(0f, 1f)

        return MediaMatch(
            metadata = media,
            compatibilityScore = finalScore,
            matchDetails = MatchDetails(
                archetypeMatch = archetypeScore,
                emotionMatch = emotionScore,
                attributeMatch = attributeScore,
                depthMatch = true,
                conflictBonus = conflictBonus
            )
        )
    }

    /**
     * Derinlik uyumluluğunu kontrol eder
     * D1 < D2 < D3 hiyerarşisi vardır
     */
    private fun isDepthCompatible(mediaDepth: String, profileDepth: String): Boolean {
        val depthLevels = mapOf("D1" to 1, "D2" to 2, "D3" to 3)
        val mediaLevel = depthLevels[mediaDepth.uppercase()] ?: 1
        val profileLevel = depthLevels[profileDepth.uppercase()] ?: 1

        // Oyuncu sadece kendi seviyesine eşit veya daha düşük içerikleri görebilir
        return mediaLevel <= profileLevel
    }

    /**
     * Arketip eşleşme puanını hesaplar
     */
    private fun calculateArchetypeMatch(
        media: MediaDatabaseBuilder.MediaMetadata,
        profile: CharacterProfile
    ): Float {
        val mediaScreenType = media.screenType.uppercase()
        val mainArchetype = profile.mainArchetype.uppercase()

        // ✅ YENİ: Özel ekran türleri için yüksek puan
        // NEWUSER ve RETURNINGUSER herkese uygun içeriklerdir
        if (mediaScreenType.contains("NEWUSER") || mediaScreenType.contains("RETURNINGUSER")) {
            return 0.7f  // %70 uyumluluk - her oyuncuya gösterilebilir
        }

        // Tam eşleşme kontrolü
        val archetypeKeywords = ARCHETYPE_MAPPINGS[mainArchetype] ?: emptyList()
        val matchScore = archetypeKeywords.count { keyword ->
            mediaScreenType.contains(keyword.uppercase())
        }.toFloat() / archetypeKeywords.size.coerceAtLeast(1)

        return matchScore
    }

    /**
     * Nitelik eşleşme puanını hesaplar
     */
    private fun calculateAttributeMatch(
        media: MediaDatabaseBuilder.MediaMetadata,
        profile: CharacterProfile
    ): Float {
        val mediaPrimaryAttr = media.primaryAttribute.uppercase()
        var totalMatch = 0f
        var totalWeight = 0f

        for (attribute in profile.keyAttributes) {
            val strength = profile.attributeStrengths[attribute] ?: 0.5f
            totalWeight += strength

            val keywords = ATTRIBUTE_MAPPINGS[attribute] ?: emptyList()
            val isMatch = keywords.any { keyword ->
                mediaPrimaryAttr.contains(keyword.uppercase())
            }

            if (isMatch) {
                totalMatch += strength
            }
        }

        return if (totalWeight > 0) totalMatch / totalWeight else 0f
    }

    /**
     * Duygu eşleşme puanını hesaplar
     */
    private fun calculateEmotionMatch(
        media: MediaDatabaseBuilder.MediaMetadata,
        profile: CharacterProfile
    ): Float {
        val mediaEmotion = media.emotion.uppercase()
        val dominantEmotion = profile.dominantEmotion.uppercase()

        val keywords = EMOTION_MAPPINGS[dominantEmotion] ?: emptyList()
        val matchScore = keywords.count { keyword ->
            mediaEmotion.contains(keyword.uppercase())
        }.toFloat() / keywords.size.coerceAtLeast(1)

        return matchScore
    }

    /**
     * İç çatışma bonusunu hesaplar
     *
     * Eğer medya, oyuncunun içindeki çatışan iki niteliği de yansıtıyorsa,
     * ekstra puan kazanır (daha derin, anlamlı içerik olduğu için)
     */
    private fun calculateConflictBonus(
        media: MediaDatabaseBuilder.MediaMetadata,
        profile: CharacterProfile
    ): Float {
        if (profile.internalConflicts.isEmpty()) {
            return 0f
        }

        var bonusScore = 0f
        for ((attr1, attr2) in profile.internalConflicts) {
            val keywords1 = ATTRIBUTE_MAPPINGS[attr1] ?: emptyList()
            val keywords2 = ATTRIBUTE_MAPPINGS[attr2] ?: emptyList()

            val hasFirstAttr = keywords1.any { media.primaryAttribute.contains(it, ignoreCase = true) }
            val hasSecondAttr = keywords2.any { media.primaryAttribute.contains(it, ignoreCase = true) }

            if (hasFirstAttr && hasSecondAttr) {
                bonusScore += 1f // Tam çatışma eşleşmesi
            }
        }

        return bonusScore.coerceAtMost(1f)
    }

    /**
     * Veritabanını yükler veya oluşturur
     */
    private fun loadOrCreateDatabase(): MediaDatabaseBuilder.MediaDatabase {
        if (cachedDatabase != null) {
            return cachedDatabase!!
        }

        var database = databaseBuilder.loadDatabase()
        if (database == null) {
            Log.d(TAG, "📦 Veritabanı bulunamadı, yenisi oluşturuluyor...")
            database = databaseBuilder.buildAndSaveDatabase()
        }

        cachedDatabase = database
        return database
    }

    /**
     * Playlist istatistiklerini loglar
     */
    private fun logPlaylistStats(videos: List<MediaMatch>, photos: List<MediaMatch>) {
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "🎬 KİŞİSELLEŞTİRİLMİŞ PLAYLIST HAZIR")
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "📹 Video sayısı: ${videos.size}")
        Log.d(TAG, "📸 Fotoğraf sayısı: ${photos.size}")

        if (videos.isNotEmpty()) {
            Log.d(TAG, "\n🏆 EN UYUMLU VİDEOLAR:")
            videos.take(5).forEach { match ->
                Log.d(TAG, "  • ${match.metadata.fileName} (Uyum: %.2f%%)".format(match.compatibilityScore * 100))
                Log.d(TAG, "    ↳ Arketip: %.0f%%, Duygu: %.0f%%, Nitelik: %.0f%%".format(
                    match.matchDetails.archetypeMatch * 100,
                    match.matchDetails.emotionMatch * 100,
                    match.matchDetails.attributeMatch * 100
                ))
            }
        }

        Log.d(TAG, "═══════════════════════════════════════")
    }

    /**
     * Kişiselleştirilmiş playlist sonucu
     */
    data class PersonalizedPlaylist(
        val videos: List<Int>,                          // Resource ID'ler
        val photos: List<Int>,                          // Resource ID'ler
        val videoMatches: List<MediaMatch>,             // Detaylı eşleşme bilgisi
        val photoMatches: List<MediaMatch>,             // Detaylı eşleşme bilgisi
        val characterProfile: CharacterProfile          // Kullanılan profil
    )

    // ========================================
    // ÖLÜM YANKISI (Death Echo) - Death Loop Sistemi
    // ========================================

    /**
     * ÖLÜM YANKISI - Ölüm sonrası kişiselleştirilmiş içerik üretimi
     *
     * Bu fonksiyon, ölen oyuncular için "Ruh Parçası" (Soul Fragment) verilerini kullanarak
     * kişiselleştirilmiş içerik üretir. Ölüm sayısı arttıkça içerik derinliği artar.
     *
     * @param deathCount Oyuncunun toplam ölüm sayısı (DeathArchive.size)
     * @param lastDeathRecord Son ölüm kaydı (en son ölümün detayları)
     * @param maxVideos Maksimum video sayısı
     * @param maxPhotos Maksimum fotoğraf sayısı
     * @return PersonalizedPlaylist (ölüm temalı içerikler)
     */
    fun generateDeathEchoPlaylist(
        deathCount: Int,
        lastDeathRecord: com.example.isekaikuroshin.data.DeathRecord?,
        maxVideos: Int = 8,
        maxPhotos: Int = 8
    ): PersonalizedPlaylist {

        Log.d(TAG, "💀 ÖLÜM YANKISI başlatılıyor...")
        Log.d(TAG, "📊 Ölüm sayısı: $deathCount")

        // ========================================
        // ADIM 1: Duygusal Derinlik Hesapla
        // ========================================
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
            lastDeathRecord.deathCause.contains("DEBUG", ignoreCase = true) -> "SADNESS"

            else -> "SADNESS" // Varsayılan duygu
        }

        Log.d(TAG, "😢 Baskın duygu: $dominantEmotion (Sebep: ${lastDeathRecord?.deathCause ?: "BILINMIYOR"})")

        // ========================================
        // ADIM 3: Veritabanından POSTDEATH İçeriklerini Filtrele
        // ========================================
        val database = loadOrCreateDatabase()

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
            val emotionScore = when {
                video.emotion.equals(dominantEmotion, ignoreCase = true) -> 0.9f // Tam eşleşme
                isEmotionCompatible(video.emotion, dominantEmotion) -> 0.5f // Uyumlu duygu
                else -> 0.3f // Uyumsuz ama gösterilebilir
            }

            val depthScore = getDepthScore(video.depth, emotionalDepth)
            val totalScore = (emotionScore * 0.7f) + (depthScore * 0.3f)

            ScoredMedia(video, totalScore)
        }

        val scoredPhotos = postDeathPhotos.map { photo ->
            val emotionScore = when {
                photo.emotion.equals(dominantEmotion, ignoreCase = true) -> 0.9f
                isEmotionCompatible(photo.emotion, dominantEmotion) -> 0.5f
                else -> 0.3f
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
            emotionalDepth = emotionalDepth
        )

        Log.d(TAG, "💀 Karakter Profili: ${characterProfile.mainArchetype} / ${characterProfile.dominantEmotion} / ${characterProfile.emotionalDepth}")

        // ========================================
        // ADIM 8: Playlist Döndür
        // ========================================
        return PersonalizedPlaylist(
            videos = finalVideos,
            photos = finalPhotos,
            videoMatches = emptyList(), // Detay gerekmez
            photoMatches = emptyList(), // Detay gerekmez
            characterProfile = characterProfile
        )
    }

    // ========================================
    // YARDIMCI FONKSİYONLAR - Death Echo
    // ========================================

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

        return when (abs(contentLevel - playerLevel)) {
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
        val database = loadOrCreateDatabase()

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
        val database = loadOrCreateDatabase()

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
        val media: MediaDatabaseBuilder.MediaMetadata,
        val score: Float
    )

    // ========================================
    // v2.0: EKSEN BAZLI ATTRIBUTE SİSTEMİ
    // ========================================

    /**
     * v2.0: Player karma'sından dominant attribute eksenini belirle
     *
     * Karma sistemindeki 6 eksenden player'ın en baskın olduğu ekseni bulur:
     * - AXIS_1_VIOLENCE_MERCY
     * - AXIS_2_CHAOS_ORDER
     * - AXIS_3_SELFISH_SACRIFICE
     * - AXIS_4_FEAR_COURAGE
     * - AXIS_5_DECEIT_LOYALTY
     * - AXIS_6_DARKNESS_LIGHT
     *
     * @param playerState Player'ın mevcut durumu
     * @return Dominant eksen ve değeri (-1.0 ... +1.0)
     */
    fun getDominantAttributeAxis(playerState: PlayerState): Pair<com.example.isekaikuroshin.models.AttributeAxis, Float> {
        // Player state'ten eksen değerlerini çıkar
        val axisValues = mutableMapOf<com.example.isekaikuroshin.models.AttributeAxis, Float>()

        // AXIS_1: VIOLENCE_MERCY
        // moralityScore > 0 → MERCY, < 0 → VIOLENCE
        val violenceMercyValue = when {
            playerState.unholyPoints > 70 -> -0.8f  // Yüksek unholy → VIOLENCE
            playerState.holinessPoints > 70 -> 0.8f  // Yüksek holiness → MERCY
            playerState.moralityScore > 0.3f -> 0.5f  // Pozitif moral → MERCY
            playerState.moralityScore < -0.3f -> -0.5f  // Negatif moral → VIOLENCE
            else -> 0.0f
        }
        axisValues[com.example.isekaikuroshin.models.AttributeAxis.AXIS_1_VIOLENCE_MERCY] = violenceMercyValue

        // AXIS_2: CHAOS_ORDER
        val chaosOrderValue = when {
            playerState.moralityScore > 0.5f -> 0.7f  // Yüksek moral → ORDER
            playerState.moralityScore < -0.5f -> -0.7f  // Düşük moral → CHAOS
            else -> playerState.moralityScore  // Direkt moralityScore kullan
        }
        axisValues[com.example.isekaikuroshin.models.AttributeAxis.AXIS_2_CHAOS_ORDER] = chaosOrderValue

        // AXIS_3: SELFISH_SACRIFICE
        // Holiness points → SACRIFICE, Unholy points → SELFISH
        val selfishSacrificeValue = when {
            playerState.holinessPoints > 80 -> 0.9f  // Yüksek holiness → SACRIFICE
            playerState.unholyPoints > 80 -> -0.9f  // Yüksek unholy → SELFISH
            else -> ((playerState.holinessPoints - playerState.unholyPoints) / 100f).coerceIn(-1f, 1f)
        }
        axisValues[com.example.isekaikuroshin.models.AttributeAxis.AXIS_3_SELFISH_SACRIFICE] = selfishSacrificeValue

        // AXIS_4: FEAR_COURAGE
        // Düşük HP → FEAR, Yüksek level + HP → COURAGE
        val fearCourageValue = when {
            playerState.currentHealth < playerState.maxHealth * 0.2 -> -0.7f  // Düşük HP → FEAR
            playerState.level > 20 && playerState.currentHealth > playerState.maxHealth * 0.8 -> 0.7f  // Yüksek level + HP → COURAGE
            else -> 0.0f
        }
        axisValues[com.example.isekaikuroshin.models.AttributeAxis.AXIS_4_FEAR_COURAGE] = fearCourageValue

        // AXIS_5: DECEIT_LOYALTY
        // Morality score'a göre (pozitif → LOYALTY, negatif → DECEIT)
        val deceitLoyaltyValue = playerState.moralityScore * 0.8f
        axisValues[com.example.isekaikuroshin.models.AttributeAxis.AXIS_5_DECEIT_LOYALTY] = deceitLoyaltyValue

        // AXIS_6: DARKNESS_LIGHT
        // Unholy vs Holiness
        val darknessLightValue = when {
            playerState.unholyPoints > 90 -> -0.95f  // Maksimum DARK
            playerState.holinessPoints > 90 -> 0.95f  // Maksimum LIGHT
            else -> ((playerState.holinessPoints - playerState.unholyPoints) / 100f).coerceIn(-1f, 1f)
        }
        axisValues[com.example.isekaikuroshin.models.AttributeAxis.AXIS_6_DARKNESS_LIGHT] = darknessLightValue

        // En yüksek mutlak değere sahip ekseni bul
        val dominantAxisEntry = axisValues.maxByOrNull { kotlin.math.abs(it.value) }
        val dominantAxis = if (dominantAxisEntry != null) {
            dominantAxisEntry.key to dominantAxisEntry.value
        } else {
            com.example.isekaikuroshin.models.AttributeAxis.AXIS_2_CHAOS_ORDER to 0.0f
        }

        val dominantAxisName = dominantAxis.first.axisName
        val dominantAxisValue = dominantAxis.second
        Log.d(TAG, "🎯 Dominant Axis: $dominantAxisName = $dominantAxisValue")

        val allAxesLog = axisValues.entries.joinToString { "${it.key.axisName}: ${it.value}" }
        Log.d(TAG, "   Tüm eksenler: $allAxesLog")

        return dominantAxis.first to dominantAxis.second
    }

    /**
     * v2.0: Player'ın depth affinity'sini hesapla
     *
     * Player'ın geçmiş medya etkileşimlerine ve seviyesine göre
     * derin içeriğe (D3-D5) ne kadar ilgi gösterdiğini ölçer.
     *
     * @return 0.0 - 1.0 (düşük → yüzeysel içerik, yüksek → derin içerik)
     */
    fun calculatePlayerDepthAffinity(playerState: PlayerState): Float {
        // Level bazlı base affinity
        val levelAffinity = when {
            playerState.level >= 40 -> 0.9f  // Yüksek seviye → derin içerik
            playerState.level >= 25 -> 0.6f
            playerState.level >= 15 -> 0.4f
            else -> 0.2f
        }

        // Holiness + Unholy toplam (yüksek karma aktivitesi → derin içerik)
        val karmaActivity = (playerState.holinessPoints + playerState.unholyPoints) / 200f
        val karmaAffinity = karmaActivity.coerceIn(0f, 1f)

        // Final affinity (weighted average)
        return (levelAffinity * 0.7f) + (karmaAffinity * 0.3f)
    }

    /**
     * v2.0: Karma karmaşıklığını hesapla
     *
     * Player'ın tüm eksenlerde dengeli dağılım göstermesi
     * (zıt nitelikler taşıması) karmaşıklık göstergesidir.
     *
     * @return 0.0 - 1.0 (düşük → basit karma, yüksek → kompleks karma)
     */
    fun calculateKarmaComplexity(playerState: PlayerState): Float {
        // Holiness ve Unholy points'in dengesi
        val holinessUnholyBalance = when {
            playerState.holinessPoints > 50 && playerState.unholyPoints > 50 -> 1.0f  // Her ikisi de yüksek → kompleks
            playerState.holinessPoints > 30 && playerState.unholyPoints > 30 -> 0.7f
            else -> 0.3f
        }

        // Morality score'un nötralliği (0'a yakınlık)
        val moralityNeutrality = 1.0f - kotlin.math.abs(playerState.moralityScore)

        // Final complexity
        return (holinessUnholyBalance * 0.6f) + (moralityNeutrality * 0.4f)
    }
}
