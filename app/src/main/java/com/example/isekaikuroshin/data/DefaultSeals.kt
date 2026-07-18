package com.example.isekaikuroshin.data

/**
 * Default Seal Templates
 *
 * Oyuna başlangıçta yüklenen varsayılan mühür şablonları.
 * Her mühür, 21 normalized hand landmark noktası içerir (MediaPipe Hands standardı).
 *
 * Landmark İndeksleri:
 * 0: Wrist (Bilek)
 * 1-4: Thumb (Başparmak)
 * 5-8: Index (İşaret parmağı)
 * 9-12: Middle (Orta parmak)
 * 13-16: Ring (Yüzük parmağı)
 * 17-20: Pinky (Serçe parmak)
 *
 * Koordinat Sistemi:
 * - (0, 0, 0) = Bilek (wrist) merkez olarak kabul edilir
 * - x: Sağ (+) / Sol (-)
 * - y: Aşağı (+) / Yukarı (-)
 * - z: Kameraya yakın (+) / Uzak (-)
 * - Değerler normalize edilmiştir [-1, 1] aralığında
 */

object DefaultSeals {

    /**
     * Tüm varsayılan mühürleri döndürür
     */
    fun getAllDefaultSeals(): List<Seal> {
        return listOf(
            // Pratik Mühürleri (Sadece Açık El)
            createPracticeOpenPalm(),

            // Element Mühürleri
            createFireSeal(),
            createWaterSeal(),
            createEarthSeal(),
            createWindSeal(),
            createLightningSeal()
        )
    }

    /**
     * SEAL 1: "Ateş Mührü" (Fire Seal)
     * Zorluk: NOVICE
     * El Pozisyonu: Açık avuç, parmaklar yukarı ve hafifçe ayrık
     */
    private fun createFireSeal(): Seal {
        return Seal(
            id = "seal_fire_basic",
            nameKey = "seal_fire_blast",
            descriptionKey = "seal_fire_blast_desc",
            loreTextKey = "seal_fire_blast_lore",
            difficulty = SealDifficulty.NOVICE,
            tier = "Tier 1",
            templateLandmarksList = mutableListOf(),  // ⚡⚡⚡ DÜZELTME: BOŞ BAŞLAR (Hayalet veri yok!)
            toleranceThreshold = 0.20f,  // Başlangıç için toleranslı
            prerequisiteSealId = null,
            minPlayerLevel = 1,
            relatedSkillIds = listOf("skill_fireball_basic"),
            masteryLevel = 0,
            practiceMetrics = PracticeMetrics()
        )
    }

    /**
     * SEAL 2: "Su Mührü" (Water Seal)
     * Zorluk: NOVICE
     * El Pozisyonu: Eller kapalı, parmaklar içe kıvrık (su tutma hareketi)
     */
    private fun createWaterSeal(): Seal {
        return Seal(
            id = "seal_water_basic",
            nameKey = "seal_water_shield",
            descriptionKey = "seal_water_shield_desc",
            loreTextKey = "seal_water_shield_lore",
            difficulty = SealDifficulty.NOVICE,
            tier = "Tier 1",
            templateLandmarksList = mutableListOf(),  // ⚡⚡⚡ DÜZELTME: BOŞ BAŞLAR
            toleranceThreshold = 0.20f,
            prerequisiteSealId = null,
            minPlayerLevel = 1,
            relatedSkillIds = listOf("skill_water_shield"),
            masteryLevel = 0,
            practiceMetrics = PracticeMetrics()
        )
    }

    /**
     * SEAL 3: "Toprak Mührü" (Earth Seal)
     * Zorluk: INTERMEDIATE
     * El Pozisyonu: Yumruk (güç ve sağlamlık simgesi)
     */
    private fun createEarthSeal(): Seal {
        return Seal(
            id = "seal_earth_basic",
            nameKey = "seal_earth_armor",
            descriptionKey = "seal_earth_armor_desc",
            loreTextKey = "seal_earth_armor_lore",
            difficulty = SealDifficulty.INTERMEDIATE,
            tier = "Tier 2",
            templateLandmarksList = mutableListOf(),  // ⚡⚡⚡ DÜZELTME: BOŞ BAŞLAR
            toleranceThreshold = 0.15f,  // Daha hassas
            prerequisiteSealId = null,  // FIXED: Temel mühür - prerequisite yok
            minPlayerLevel = 3,
            relatedSkillIds = listOf("skill_stone_armor"),
            masteryLevel = 0,
            practiceMetrics = PracticeMetrics()
        )
    }

    /**
     * SEAL 4: "Rüzgar Mührü" (Wind Seal)
     * Zorluk: INTERMEDIATE
     * El Pozisyonu: İki parmak yukarı (işaret ve orta), diğerleri kapalı
     */
    private fun createWindSeal(): Seal {
        return Seal(
            id = "seal_wind_basic",
            nameKey = "seal_wind_slash",
            descriptionKey = "seal_wind_slash_desc",
            loreTextKey = "seal_wind_slash_lore",
            difficulty = SealDifficulty.INTERMEDIATE,
            tier = "Tier 2",
            templateLandmarksList = mutableListOf(),  // ⚡⚡⚡ DÜZELTME: BOŞ BAŞLAR
            toleranceThreshold = 0.18f,
            prerequisiteSealId = null,  // FIXED: Temel mühür - prerequisite yok
            minPlayerLevel = 4,
            relatedSkillIds = listOf("skill_wind_slash"),
            masteryLevel = 0,
            practiceMetrics = PracticeMetrics()
        )
    }

    /**
     * SEAL 5: "Şimşek Mührü" (Lightning Seal)
     * Zorluk: ADVANCED
     * El Pozisyonu: İşaret parmağı düz, diğerleri hafif kıvrık (şimşek işaret eder)
     */
    private fun createLightningSeal(): Seal {
        return Seal(
            id = "seal_lightning_basic",
            nameKey = "seal_lightning_bolt",
            descriptionKey = "seal_lightning_bolt_desc",
            loreTextKey = "seal_lightning_bolt_lore",
            difficulty = SealDifficulty.ADVANCED,
            tier = "Tier 3",
            templateLandmarksList = mutableListOf(),  // ⚡⚡⚡ DÜZELTME: BOŞ BAŞLAR
            toleranceThreshold = 0.12f,  // Çok hassas
            prerequisiteSealId = null,  // FIXED: Temel mühür - prerequisite yok
            minPlayerLevel = 6,
            relatedSkillIds = listOf("skill_lightning_bolt"),
            masteryLevel = 0,
            practiceMetrics = PracticeMetrics()
        )
    }

    // ========================================
    // PRATIK MÜHÜRLERİ (PRACTICE SEALS)
    // Temel el hareketleri - Yüksek tanıma doğruluğu
    // ========================================

    /**
     * PRACTICE SEAL: "Pratik: Açık El 🖐️"
     * Zorluk: NOVICE
     * El Pozisyonu: Açık avuç, parmaklar yukarı ve ayrık
     */
    private fun createPracticeOpenPalm(): Seal {
        return Seal(
            id = "practice_open_palm",
            nameKey = "seal_practice_open_palm",
            descriptionKey = "seal_practice_open_palm_desc",
            loreTextKey = "seal_practice_open_palm_lore",
            difficulty = SealDifficulty.NOVICE,
            tier = "Pratik",
            templateLandmarksList = mutableListOf(),  // ⚡⚡⚡ DÜZELTME: BOŞ BAŞLAR
            toleranceThreshold = 0.25f,
            prerequisiteSealId = null,
            minPlayerLevel = 1,
            relatedSkillIds = emptyList(),
            masteryLevel = 0,
            practiceMetrics = PracticeMetrics()
        )
    }

    /**
     * Seal ID'sine göre seal döndür
     */
    fun getSealById(sealId: String): Seal? {
        return getAllDefaultSeals().firstOrNull { it.id == sealId }
    }

    /**
     * Zorluk seviyesine göre sealleri filtrele
     */
    fun getSealsByDifficulty(difficulty: SealDifficulty): List<Seal> {
        return getAllDefaultSeals().filter { it.difficulty == difficulty }
    }

    /**
     * Player seviyesine göre uygun sealleri döndür
     */
    fun getSealsForPlayerLevel(playerLevel: Int): List<Seal> {
        return getAllDefaultSeals().filter { it.minPlayerLevel <= playerLevel }
    }
}
