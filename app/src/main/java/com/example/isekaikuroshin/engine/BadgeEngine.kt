package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.Badge
import com.example.isekaikuroshin.data.BadgeType

object BadgeEngine {

    fun getDefaultBadges(): List<Badge> {
        return listOf(
            // ROZETLER - Mekanik bonuslar
            Badge(
                id = "novice_hunter",
                name = "novice_hunter",
                description = "novice_hunter_desc",
                tier = "bronze",
                type = BadgeType.BADGE,
                statBonuses = mapOf(
                    StatType.STR_PERCENT to 0.05f,  // +5% STR
                    StatType.AGI_FLAT to 2f          // +2 AGI
                )
            ),
            Badge(
                id = "swift_blade",
                name = "swift_blade",
                description = "swift_blade_desc",
                tier = "silver",
                type = BadgeType.BADGE,
                statBonuses = mapOf(
                    StatType.AGI_PERCENT to 0.15f,  // +15% AGI
                    StatType.STR_PERCENT to 0.08f   // +8% STR
                )
            ),
            Badge(
                id = "arcane_scholar",
                name = "arcane_scholar",
                description = "arcane_scholar_desc",
                tier = "gold",
                type = BadgeType.BADGE,
                statBonuses = mapOf(
                    StatType.INT_PERCENT to 0.20f,  // +20% INT
                    StatType.SPIRIT_FLAT to 5f      // +5 SPIRIT (mana related)
                )
            ),
            Badge(
                id = "iron_will",
                name = "iron_will",
                description = "iron_will_desc",
                tier = "platinum",
                type = BadgeType.BADGE,
                statBonuses = mapOf(
                    StatType.VIT_PERCENT to 0.25f,  // +25% VIT
                    StatType.SPIRIT_PERCENT to 0.15f // +15% SPIRIT
                )
            ),

            // UNVANLAR - Anlatısal etkiler
            Badge(
                id = "light_bringer",
                name = "Işık Getiren",
                description = "Karanlık güçleri alt eden kahramanlara verilen kutsal unvan",
                tier = "legendary",
                type = BadgeType.TITLE,
                narrativeEffect = "Demir Kale'deki Muhafızlar sana daha saygılı davranır."
            ),
            Badge(
                id = "dragon_slayer",
                name = "Ejderha Avcısı",
                description = "Efsanevi ejderhaları alt eden nadir savaşçılara verilen prestijli unvan",
                tier = "legendary",
                type = BadgeType.TITLE,
                narrativeEffect = "Köylüler senin karşında saygıyla eğilir."
            ),

            // BAŞARIMLAR - Kilit açma hedefleri
            Badge(
                id = "first_kill",
                name = "İlk Kan",
                description = "İlk düşmanını öldürdün",
                tier = "bronze",
                type = BadgeType.ACHIEVEMENT
            ),
            Badge(
                id = "level_ten",
                name = "Yükselişin Başlangıcı",
                description = "10. seviyeye ulaştın",
                tier = "silver",
                type = BadgeType.ACHIEVEMENT
            )
        )
    }

    fun combineBadges(badge1: Badge, badge2: Badge): Badge? {
        // Badge combination logic - returns null if incompatible
        if (badge1.tier != badge2.tier) return null

        val combinedBonuses = mutableMapOf<StatType, Float>()

        // Merge stat bonuses
        (badge1.statBonuses.keys + badge2.statBonuses.keys).forEach { stat ->
            val bonus1 = badge1.statBonuses[stat] ?: 1.0f
            val bonus2 = badge2.statBonuses[stat] ?: 1.0f
            combinedBonuses[stat] = ((bonus1 - 1.0f) + (bonus2 - 1.0f)) + 1.0f
        }

        return Badge(
            id = "combined_${badge1.id}_${badge2.id}",
            name = "combined_badge",
            description = "combined_badge_desc",
            tier = getNextTier(badge1.tier),
            type = BadgeType.BADGE,
            statBonuses = combinedBonuses
        )
    }

    private fun getNextTier(currentTier: String): String {
        return when (currentTier) {
            "bronze" -> "silver"
            "silver" -> "gold"
            "gold" -> "platinum"
            "platinum" -> "diamond"
            else -> "legendary"
        }
    }
}