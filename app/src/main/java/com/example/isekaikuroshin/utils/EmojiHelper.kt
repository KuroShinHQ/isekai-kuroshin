package com.example.isekaikuroshin.utils

import com.example.isekaikuroshin.data.StatusEffect
import com.example.isekaikuroshin.data.StatusEffectType
import com.example.isekaikuroshin.data.Skill

/**
 * G126: Emoji Helper - Fallback emoji system
 *
 * AI'dan emoji gelmezse default emoji döndürür.
 * KURAL 11: Metadata-based logic (name string'ine göre değil, type/category'e göre)
 */
object EmojiHelper {

    /**
     * StatusEffect için emoji döndür
     * AI'dan gelmişse onu kullan, yoksa type'a göre fallback
     */
    fun getStatusEffectEmoji(effect: StatusEffect): String {
        // AI emoji varsa kullan
        effect.iconEmoji?.let { return it }

        // Fallback: Type'a göre generic emoji
        return when (effect.type) {
            StatusEffectType.BUFF -> "✨"  // Generic buff icon
            StatusEffectType.DEBUFF -> "💀"  // Generic debuff icon
        }
    }

    /**
     * Skill için emoji döndür
     * AI'dan gelmişse onu kullan, yoksa element/name pattern'e göre fallback
     */
    fun getSkillEmoji(skill: Skill): String {
        // AI emoji varsa kullan
        skill.iconEmoji?.let { return it }

        // Fallback: Element type'a göre
        return when (skill.elementType.name.lowercase()) {
            "fire" -> "🔥"
            "water" -> "💧"
            "ice" -> "❄️"
            "lightning", "electric" -> "⚡"
            "earth" -> "🌍"
            "wind" -> "💨"
            "light", "holy" -> "✨"
            "dark", "shadow" -> "🌑"
            "neutral" -> "⭐"
            else -> "⚔️"  // Default: Generic skill icon
        }
    }

    /**
     * Item için emoji döndür (already exists in GMItemGained)
     * Bu sadece consistency için - gerçek implementasyon GMItemGained.iconEmoji'de
     */
    fun getItemEmoji(itemType: String?, itemIconEmoji: String?): String {
        // AI emoji varsa kullan
        itemIconEmoji?.let { return it }

        // Fallback: Item type'a göre
        return when (itemType?.uppercase()) {
            "WEAPON" -> "⚔️"
            "ARMOR", "EQUIPMENT" -> "🛡️"
            "CONSUMABLE" -> "🧪"
            "FOOD" -> "🍎"
            "MATERIAL" -> "📦"
            "QUEST" -> "📜"
            "CURRENCY" -> "💰"
            "TRINKET" -> "💍"
            else -> "📦"  // Default: Generic item
        }
    }

    /**
     * Text pattern'e göre emoji öneri (AI dokümantasyonu ile tutarlı)
     * Bu sadece development/debugging için - production'da AI emoji seçmeli
     */
    fun suggestEmojiFromText(text: String): String {
        val lowerText = text.lowercase()

        return when {
            // Combat skills
            lowerText.contains("sword") || lowerText.contains("kılıç") -> "⚔️"
            lowerText.contains("axe") || lowerText.contains("balta") -> "🪓"
            lowerText.contains("bow") || lowerText.contains("yay") -> "🏹"
            lowerText.contains("shield") || lowerText.contains("kalkan") -> "🛡️"

            // Magic skills
            lowerText.contains("fire") || lowerText.contains("ateş") -> "🔥"
            lowerText.contains("ice") || lowerText.contains("buz") -> "❄️"
            lowerText.contains("lightning") || lowerText.contains("şimşek") -> "⚡"
            lowerText.contains("heal") || lowerText.contains("iyileş") -> "💚"

            // Status effects
            lowerText.contains("poison") || lowerText.contains("zehir") -> "☠️"
            lowerText.contains("burn") || lowerText.contains("yan") -> "🔥"
            lowerText.contains("slow") || lowerText.contains("yavaş") -> "🐌"
            lowerText.contains("strength") || lowerText.contains("güç") -> "💪"
            lowerText.contains("speed") || lowerText.contains("hız") -> "⚡"

            else -> "⭐"  // Generic fallback
        }
    }
}
