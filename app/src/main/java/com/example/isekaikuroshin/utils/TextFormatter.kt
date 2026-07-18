package com.example.isekaikuroshin.utils

import java.util.Locale

/**
 * TODO-G124: AI İçerik Formatlama - Text sanitization ve formatlama utilities
 *
 * Bu utility, AI'dan gelen ve kullanıcıya gösterilecek text içeriğini temizler.
 *
 * ROOT CAUSE: AI bazen "fire_dragon_001" gibi kod formatında isimler üretiyor.
 * SOLUTION: Defensive programming - AI hata yapsa bile frontend'de temizliyoruz.
 */
object TextFormatter {

    /**
     * AI'dan gelen isimleri kullanıcı dostu formata çevirir.
     *
     * Kurallar:
     * - Underscore (_) → Boşluk
     * - Nokta (.) → Boşluk
     * - Tire (-) → Boşluk
     * - Sayılar → Temizlenir (001, 23, etc.)
     * - Her kelime başı büyük harf (Title Case)
     *
     * Örnek dönüşümler:
     * - "fire_dragon_001" → "Fire Dragon"
     * - "goblin_warrior_23" → "Goblin Warrior"
     * - "health.potion" → "Health Potion"
     * - "iron-sword" → "Iron Sword"
     * - "devil_wood" → "Devil Wood"
     *
     * @param rawName AI'dan gelen ham isim
     * @return Kullanıcı dostu formatlı isim
     */
    fun cleanName(rawName: String?): String {
        if (rawName.isNullOrBlank()) return "Unknown"

        return rawName
            // 1. Prefix'leri temizle (item_, quest_, npc_)
            .removePrefix("item_")
            .removePrefix("Item_")
            .removePrefix("quest_")
            .removePrefix("Quest_")
            .removePrefix("npc_")
            .removePrefix("NPC_")

            // 2. Özel karakterleri boşluğa çevir
            .replace('_', ' ')
            .replace('.', ' ')
            .replace('-', ' ')

            // 3. Sayıları temizle (sadece trailing sayılar)
            .replace(Regex("\\s*\\d+\\s*$"), "")

            // 4. Çoklu boşlukları tek boşluğa indir
            .replace(Regex("\\s+"), " ")

            // 5. Trim
            .trim()

            // 6. Title Case yap (her kelime başı büyük)
            .toTitleCase()

            // 7. Hala boşsa "Unknown" döndür
            .ifBlank { "Unknown" }
    }

    /**
     * String'i Title Case'e çevirir (her kelime başı büyük harf).
     *
     * Örnek:
     * - "fire dragon" → "Fire Dragon"
     * - "health potion" → "Health Potion"
     *
     * Türkçe karakterleri destekler.
     */
    private fun String.toTitleCase(): String {
        return this.split(" ")
            .joinToString(" ") { word ->
                if (word.isNotEmpty()) {
                    word.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                        else it.toString()
                    }
                } else {
                    word
                }
            }
    }

    /**
     * AI'dan gelen açıklamaları temizler.
     *
     * Kurallar:
     * - Gereksiz prefix/suffix'leri kaldırır
     * - Düzgün noktalama ekler
     *
     * @param rawDescription AI'dan gelen ham açıklama
     * @return Temizlenmiş açıklama
     */
    fun cleanDescription(rawDescription: String?): String {
        if (rawDescription.isNullOrBlank()) return ""

        var cleaned = rawDescription.trim()

        // Son nokta yoksa ekle
        if (cleaned.isNotEmpty() && !cleaned.endsWith(".") && !cleaned.endsWith("!") && !cleaned.endsWith("?")) {
            cleaned += "."
        }

        return cleaned
    }

    /**
     * Internal ID'yi kullanıcı dostu display name'e çevirir.
     *
     * Bu fonksiyon metadata-based logic için kullanılır.
     * Business logic itemId/questId kullanmalı, UI displayName kullanmalı.
     *
     * Örnek:
     * - itemId: "item_fire_dragon_001" (business logic için)
     * - displayName: "Fire Dragon" (UI için)
     *
     * @param internalId Internal ID (item_fire_dragon_001)
     * @param providedName AI'nın verdiği name (null olabilir)
     * @return Display name (Fire Dragon)
     */
    fun toDisplayName(internalId: String?, providedName: String?): String {
        // Önce AI'nın verdiği name'i kontrol et
        if (!providedName.isNullOrBlank()) {
            // AI clean name vermişse kullan, yoksa temizle
            val cleaned = cleanName(providedName)
            if (cleaned != "Unknown" && !cleaned.contains("_") && !cleaned.matches(Regex(".*\\d{2,}.*"))) {
                return cleaned
            }
        }

        // AI hatalı name verdiyse, internalId'den üret
        return cleanName(internalId)
    }

    /**
     * NPC konuşma metinlerini temizler.
     *
     * Kurallar:
     * - Kod formatındaki karakterleri temizler
     * - Tırnak işaretlerini düzenler
     *
     * @param rawDialogue AI'dan gelen ham diyalog
     * @return Temizlenmiş diyalog
     */
    fun cleanDialogue(rawDialogue: String?): String {
        if (rawDialogue.isNullOrBlank()) return ""

        return rawDialogue
            .trim()
            // Gereksiz tırnak işaretlerini düzelt
            .replace(Regex("^[\"']|[\"']$"), "")
            .trim()
    }
}
