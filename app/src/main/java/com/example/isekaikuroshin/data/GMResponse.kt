package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable
import com.example.isekaikuroshin.data.npc.NPCMemoryEvent
import com.example.isekaikuroshin.utils.TextFormatter

/**
 * GM'den dönen item bilgisini temsil eden veri sınıfı
 * @param itemId Item'ın benzersiz ID'si (örn: "item_forest_berries")
 * @param name Item'ın görünen adı (örn: "Orman Yemişleri")
 * @param description Item'ın açıklaması
 * @param type Item tipi ("CONSUMABLE", "EQUIPMENT", "MATERIAL", "QUEST", vb.)
 * @param rarity AI tarafından belirtilen nadirlik ("COMMON", "RARE", "LEGENDARY" vb.)
 * @param equipmentSlot G80: Ekipman slot'u ("MAIN_HAND", "HEAD", "CHEST", "NONE", vb.)
 * @param iconEmoji G135.3: Item için emoji icon (örn: "⚔️", "🍎", "🛡️")
 */
@Serializable
data class GMItemGained(
    val itemId: String? = null,
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
    val rarity: String? = null,
    val equipmentSlot: String? = null,  // G80: Equipment slot info
    val iconEmoji: String? = null  // G135.3: Emoji icon
) {
    /**
     * TODO-G124: AI İçerik Formatlama
     *
     * KURAL 11: Metadata-based logic - String karşılaştırması yerine ID kullan
     * KURAL 9: Hardcoded text yerine temizlenmiş display name kullan
     *
     * UI için kullanılacak temizlenmiş display name.
     * AI "fire_dragon_001" verse bile "Fire Dragon" döner.
     *
     * Business logic itemId kullanmalı, UI displayName kullanmalı.
     */
    fun displayName(): String {
        return TextFormatter.toDisplayName(itemId, name)
    }

    /**
     * TODO-G124: Temizlenmiş açıklama
     */
    fun displayDescription(): String {
        return TextFormatter.cleanDescription(description)
    }
}

/**
 * G79: GM'den dönen yeni quest bilgisini temsil eden veri sınıfı
 * @param questId Quest'in benzersiz ID'si (örn: "quest_find_herbs")
 * @param title Quest'in başlığı (örn: "Şifalı Otlar Bul")
 * @param description Quest'in açıklaması
 * @param giver Quest'i veren karakter
 * @param dueDay Quest'in bitiş günü (-1 ise süresiz)
 * @param rewards Quest'i tamamlayınca kazanılacak ödüller
 */
@Serializable
data class GMQuestGained(
    val questId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val giver: String? = null,
    val dueDay: Int = -1,
    val rewards: List<String> = emptyList()
) {
    /**
     * TODO-G124: AI İçerik Formatlama
     * Quest başlığını temizler (UI için)
     */
    fun displayTitle(): String {
        return TextFormatter.cleanName(title)
    }

    /**
     * TODO-G124: Quest açıklamasını temizler (UI için)
     */
    fun displayDescription(): String {
        return TextFormatter.cleanDescription(description)
    }

    /**
     * TODO-G124: Quest veren karakterin temizlenmiş adı (UI için)
     */
    fun displayGiver(): String {
        return TextFormatter.cleanName(giver)
    }
}

/**
 * GM'in bir NPC'nin durumunu değiştirmek için kullandığı komut yapısı
 * TODO-NEM-03: GM Entegrasyonu kapsamında eklendi
 * TODO-NEM-04: Skills, titles, badges desteği eklendi
 */
@Serializable
data class NPCStateChange(
    val npcId: String? = null,
    val levelChange: Int? = null,          // Level artışı/azalışı (±1, ±2 vb.)
    val statChanges: Map<String, Int> = emptyMap(), // Stat değişiklikleri ("strength" to +2)
    val loyaltyChange: Int? = null,        // Loyalty değişikliği (-50 to +50)
    val memoryAdded: NPCMemoryEvent? = null, // Yeni hafıza kaydı
    val moodOverride: String? = null,      // Mood değişikliği ("ANGRY", "FEARFUL" vb.)
    val personalityTraitAdded: String? = null, // Yeni kişilik özelliği
    val locationChange: String? = null,    // NPC'nin yeni konumu

    // ============ ADVANCED FEATURES (TODO-NEM-04) ============
    val skillsLearned: List<String> = emptyList(),   // GM'in verdiği yeni yetenekler
    val titlesGained: List<String> = emptyList(),    // GM'in verdiği yeni unvanlar
    val badgesEarned: List<String> = emptyList()     // GM'in verdiği yeni rozetler
) {
    /**
     * TODO-G124: AI İçerik Formatlama
     * Skills/titles/badges'i temizler (UI için)
     */
    fun displaySkills(): List<String> {
        return skillsLearned.map { TextFormatter.cleanName(it) }
    }

    fun displayTitles(): List<String> {
        return titlesGained.map { TextFormatter.cleanName(it) }
    }

    fun displayBadges(): List<String> {
        return badgesEarned.map { TextFormatter.cleanName(it) }
    }

    fun displayPersonalityTrait(): String {
        return TextFormatter.cleanName(personalityTraitAdded)
    }

    fun displayLocation(): String {
        return TextFormatter.cleanName(locationChange)
    }
}

/**
 * GM'in bir NPC'yi Nemesis'e terfi ettirmek için kullandığı özel komut
 * TODO-NEM-03: GM Entegrasyonu kapsamında eklendi
 */
@Serializable
data class NemesisEvolution(
    val npcId: String? = null,
    val newNemesisLevel: Int? = null,              // Yeni Nemesis seviyesi (1-5)
    val vendettaReason: String? = null,            // Nemesis olma sebebi
    val adaptationTraits: List<String> = emptyList(), // Yeni adaptasyon özellikleri
    val evolutionDescription: String? = null       // Nemesis evriminin hikayesi
) {
    /**
     * TODO-G124: AI İçerik Formatlama
     * Nemesis evolution data için display helpers
     */
    fun displayVendettaReason(): String {
        return TextFormatter.cleanDescription(vendettaReason)
    }

    fun displayAdaptationTraits(): List<String> {
        return adaptationTraits.map { TextFormatter.cleanName(it) }
    }

    fun displayEvolutionDescription(): String {
        return TextFormatter.cleanDescription(evolutionDescription)
    }
}

/**
 * GM'in makro politik olayları tetiklemek için kullandığı komut yapısı
 * TODO-NEM-09: AI Güdümlü Makro Olaylar kapsamında eklendi
 */
@Serializable
data class MacroEvent(
    val eventType: String? = null,                 // "WAR_DECLARATION", "PEACE_TREATY"
    val factionIds: List<String> = emptyList(),          // Etkilenecek fraksiyon ID'leri
    val reason: String? = null                     // AI'ın stratejik gerekçesi
)

/**
 * G81: GM'in oyun mekaniklerini tetiklemek için kullandığı yapılandırılmış aksiyon
 * ActionExecutorEngine tarafından işlenecek komutlar
 */
@Serializable
data class StructuredAction(
    val actionType: String,                        // "START_COMBAT", "END_COMBAT", "UPDATE_ENEMY_HEALTH"
    val parameters: Map<String, String> = emptyMap() // Aksiyona özel parametreler
)

/**
 * GameMaster'dan dönen yapılandırılmış JSON yanıtını temsil eden veri sınıfı
 *
 * Bu sınıf, GM'in sadece hikaye metni değil, aynı zamanda oyun mekaniklerini
 * etkileyebilecek komutlar da üretmesini sağlar. GM'in "elleri" olarak işlev görür.
 *
 * @param journalEntry Oyuncunun günlüğüne eklenecek hikaye metni
 * @param itemsGained Oyuncunun kazandığı item'lar (ID ve nadirlik bilgisi ile)
 * @param questsUpdated Güncellenmesi gereken görev ID'leri listesi
 * @param statsChanged Değişen karakteristikler (örn: "strength" to 2, "health" to -10)
 * @param weatherChange GM'in değiştireceği hava durumu ("RAINY", "FOGGY", "SUNNY", "STORMY", "SNOWY", "CLOUDY", "WINDY", "HOT", "COLD", "BLIZZARD")
 * @param timeShift GM'in ayarlayacağı zaman dilimi ("MORNING", "NOON", "EVENING", "NIGHT", "DEEP_NIGHT")
 * @param locationsUnlocked GM'in kilidini açacağı yeni lokasyonlar (["dark_forest", "ancient_ruins"])
 * @param locationChange GM'in oyuncuyu taşıyacağı yeni lokasyon ("ancient_tower")
 */
@Serializable
data class GMResponse(
    val journalEntry: String, // Anlatılacak hikaye metni
    val itemsGained: List<GMItemGained> = emptyList(), // Kazanılan item'lar (nadirlik ile)
    val itemsConsumed: List<String> = emptyList(), // G146: Crafting'de harcanan itemlar (display name format)
    val questsGained: List<GMQuestGained> = emptyList(), // G79: Yeni kazanılan görevler
    val questsUpdated: List<String> = emptyList(), // Güncellenen görev ID'leri
    val statsChanged: Map<String, Int> = emptyMap(), // Değişen stat'lar (örn: "strength" to 1)

    // ============ ÇEVRE KONTROLLERI (FAZ 1) ============
    val weatherChange: String? = null, // GM'in değiştireceği hava durumu
    val timeShift: String? = null,     // GM'in ayarlayacağı zaman dilimi (MORNING, NOON, AFTERNOON, EVENING, NIGHT)
    val dateChange: Int? = null,       // G147: Kaç gün ilerleteceği (+1, +7, etc.)

    // ============ LOKASYON KONTROLLERI (FAZ 2) ============
    val locationsUnlocked: List<String> = emptyList(), // GM'in kilidini açacağı yeni lokasyonlar
    val locationChange: String? = null,                 // GM'in oyuncuyu taşıyacağı yeni lokasyon

    // ============ NPC KONTROLLERI (FAZ 3 - TODO-NEM-03) ============
    val npcStateChange: NPCStateChange? = null,         // GM'in NPC durumu değişiklikleri
    val nemesisEvolution: NemesisEvolution? = null,     // GM'in Nemesis evrimi komutları

    // ============ MAKRO OLAYLAR (FAZ 4 - TODO-NEM-09) ============
    val macroEvent: MacroEvent? = null,                  // GM'in makro politik kararları

    // ============ OYUN MEKANİĞİ KOMUTLARI (G81: Combat System) ============
    val structuredActions: List<StructuredAction> = emptyList(), // GM'in tetikleyeceği oyun aksiyonları

    // ============ STATUS EFFECTS (G82: Buff/Debuff System - DEPRECATED) ============
    val statusEffectsApplied: List<StatusEffect> = emptyList(), // GM'in uygulayacağı buff/debuff'lar (LEGACY - G82)

    // ============ STATUS EFFECTS V2 (G118: Yarı Katı/Yarı Esnek Sistem) ============
    val activeStatusEffects: List<ActiveStatusEffectG118>? = null, // AI-generated status effects (emote-based UI)

    // ============ MORALITY ANALYSIS (G105.1: AI Sentiment) ============
    val moralityDelta: Float? = null, // AI'nın sentiment analysis sonucu (-1.0 to +1.0)

    // ============ ARCHETYPE TRACKING (G106.2: ProfileUpdaterEngine) ============
    val playerAction: String? = null, // Oyuncunun aksiyonu: "ATTACK", "CAST_SPELL", "CRAFT", etc.

    // ============ REALITY VALIDATION (G143: Battle Reality Check) ============
    val impossibleAction: Boolean? = null, // Player'ın aksiyonu imkansız mı? (spell/item yok, mana yetersiz, fiziksel imkansız)
    val penaltyDescription: String? = null // İmkansız aksiyonun açıklaması (örn: "You don't have the 'fireball' spell")
) {
    /**
     * G136: Validate if GMResponse has sufficient content generation
     * Returns true if response meets content requirements
     */
    fun hasMinimalContent(): Boolean {
        return journalEntry.isNotBlank()
    }

    /**
     * G136: Check if moralityDelta is present (should be in EVERY response)
     */
    fun hasMoralityDelta(): Boolean {
        return moralityDelta != null
    }

    /**
     * G136: Check if response has any meaningful game content
     * (items, quests, NPCs, or stats)
     */
    fun hasGameContent(): Boolean {
        return itemsGained.isNotEmpty() ||
                questsGained.isNotEmpty() ||
                npcStateChange != null ||
                statsChanged.isNotEmpty() ||
                statusEffectsApplied.isNotEmpty()
    }

    /**
     * G136: Full validation - logs warnings for missing content
     */
    fun validate() {
        if (!hasMinimalContent()) {
            com.example.isekaikuroshin.utils.GameLogger.logWarning("GMResponse", "G136: Empty journalEntry")
        }
        if (!hasMoralityDelta()) {
            com.example.isekaikuroshin.utils.GameLogger.logWarning("GMResponse", "G136: Missing moralityDelta - AI should ALWAYS provide this!")
        }
    }
}