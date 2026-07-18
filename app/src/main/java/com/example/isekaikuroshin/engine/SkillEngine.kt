package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.Skill

object SkillEngine {

    fun getDefaultSkills(): List<Skill> {
        return listOf(
            Skill(
                name = "lightning_strike",
                description = "lightning_strike_desc",
                tier = "bronze",
                evolutionProgress = 0.0f,
                manaCost = 15,
                statBonuses = mapOf(
                    StatType.INT_FLAT to 1f,      // +1 INT (magical skill)
                    StatType.AGI_PERCENT to 0.03f // +3% AGI (speed-based)
                )
            ),
            Skill(
                name = "flame_burst",
                description = "flame_burst_desc",
                tier = "bronze",
                evolutionProgress = 0.2f,
                manaCost = 20,
                statBonuses = mapOf(
                    StatType.INT_PERCENT to 0.05f, // +5% INT
                    StatType.STR_FLAT to 1f         // +1 STR (offensive skill)
                )
            ),
            Skill(
                name = "ice_shield",
                description = "ice_shield_desc",
                tier = "silver",
                evolutionProgress = 0.5f,
                manaCost = 25,
                statBonuses = mapOf(
                    StatType.VIT_PERCENT to 0.08f,   // +8% VIT (defensive)
                    StatType.SPIRIT_FLAT to 2f       // +2 SPIRIT
                )
            ),
            Skill(
                name = "wind_blade",
                description = "wind_blade_desc",
                tier = "silver",
                evolutionProgress = 0.8f,
                manaCost = 30,
                statBonuses = mapOf(
                    StatType.AGI_PERCENT to 0.12f,  // +12% AGI
                    StatType.STR_PERCENT to 0.06f   // +6% STR
                )
            ),
            Skill(
                name = "earth_spike",
                description = "earth_spike_desc",
                tier = "gold",
                evolutionProgress = 0.1f,
                manaCost = 40,
                statBonuses = mapOf(
                    StatType.STR_PERCENT to 0.15f,  // +15% STR (earth power)
                    StatType.VIT_FLAT to 3f          // +3 VIT (earth resilience)
                )
            ),
            Skill(
                name = "shadow_step",
                description = "shadow_step_desc",
                tier = "gold",
                evolutionProgress = 0.6f,
                manaCost = 35
            ),
            Skill(
                name = "divine_heal",
                description = "divine_heal_desc",
                tier = "platinum",
                evolutionProgress = 0.3f,
                manaCost = 50
            ),
            Skill(
                name = "void_blast",
                description = "void_blast_desc",
                tier = "gold",
                evolutionProgress = 0.4f,
                manaCost = 38
            ),
            Skill(
                name = "crystal_spear",
                description = "crystal_spear_desc",
                tier = "silver",
                evolutionProgress = 0.7f,
                manaCost = 32
            ),
            Skill(
                name = "blood_drain",
                description = "blood_drain_desc",
                tier = "bronze",
                evolutionProgress = 0.6f,
                manaCost = 18
            ),
            Skill(
                name = "nature_bind",
                description = "nature_bind_desc",
                tier = "gold",
                evolutionProgress = 0.2f,
                manaCost = 42
            )
        )
    }

    fun evolveSkill(skill: Skill): Skill? {
        if (skill.evolutionProgress < 1.0f) return null

        val evolvedTier = getNextTier(skill.tier)
        val evolvedName = getEvolvedName(skill.name)

        return Skill(
            name = evolvedName,
            description = "enhanced_skill_desc",
            tier = evolvedTier,
            evolutionProgress = 0.0f,
            manaCost = (skill.manaCost * 1.5f).toInt()
        )
    }

    fun trainSkill(skill: Skill, trainingPoints: Float): Skill {
        val newProgress = (skill.evolutionProgress + trainingPoints).coerceAtMost(1.0f)
        return skill.copy(evolutionProgress = newProgress)
    }

    fun learnSkillFromNPC(npcSkill: String, playerLevel: Int): Skill? {
        val requiredLevel = getSkillLevelRequirement(npcSkill)
        if (playerLevel < requiredLevel) return null

        return when (npcSkill.lowercase()) {
            "fire mastery" -> Skill(
                name = "fire_mastery",
                description = "fire_mastery_desc",
                tier = "gold",
                evolutionProgress = 0.0f,
                manaCost = 45
            )
            "water control" -> Skill(
                name = "water_control",
                description = "water_control_desc",
                tier = "silver",
                evolutionProgress = 0.0f,
                manaCost = 28
            )
            "beast communication" -> Skill(
                name = "beast_communication",
                description = "beast_communication_desc",
                tier = "bronze",
                evolutionProgress = 0.0f,
                manaCost = 10
            )
            "ancient runes" -> Skill(
                name = "ancient_runes",
                description = "ancient_runes_desc",
                tier = "platinum",
                evolutionProgress = 0.0f,
                manaCost = 60
            )
            else -> null
        }
    }

    fun combineSkills(skill1: Skill, skill2: Skill): Skill? {
        if (skill1.tier != skill2.tier) return null
        if (skill1.evolutionProgress < 0.8f || skill2.evolutionProgress < 0.8f) return null

        val combinedName = getCombinedSkillName(skill1.name, skill2.name)
        val combinedManaCost = ((skill1.manaCost + skill2.manaCost) * 0.8f).toInt()

        return Skill(
            name = combinedName, // Changed from "combined_skill" to combinedName
            description = "combined_skill_desc",
            tier = getNextTier(skill1.tier),
            evolutionProgress = 0.0f,
            manaCost = combinedManaCost
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

    private fun getEvolvedName(skillName: String): String {
        return when (skillName) {
            "lightning_strike" -> "thunder_bolt"
            "flame_burst" -> "inferno_blast"
            "ice_shield" -> "frost_barrier"
            "wind_blade" -> "tempest_slash"
            "earth_spike" -> "earthquake_spear"
            "shadow_step" -> "void_walk"
            "divine_heal" -> "celestial_restoration"
            "void_blast" -> "chaos_eruption"
            "crystal_spear" -> "diamond_lance"
            "blood_drain" -> "vampiric_essence"
            "nature_bind" -> "forest_dominion"
            else -> "enhanced_skill"
        }
    }

    private fun getCombinedSkillName(skill1Name: String, skill2Name: String): String {
        return when {
            (skill1Name.contains("Fire") || skill1Name.contains("Flame")) &&
            (skill2Name.contains("Wind") || skill2Name.contains("Air")) -> "Inferno Tornado"

            (skill1Name.contains("Ice") || skill1Name.contains("Frost")) &&
            (skill2Name.contains("Lightning") || skill2Name.contains("Thunder")) -> "Frozen Lightning"

            (skill1Name.contains("Earth") || skill1Name.contains("Stone")) &&
            (skill2Name.contains("Fire") || skill2Name.contains("Flame")) -> "Magma Eruption"

            (skill1Name.contains("Water") || skill1Name.contains("Ice")) &&
            (skill2Name.contains("Wind") || skill2Name.contains("Air")) -> "Frost Storm"

            else -> "${skill1Name} + ${skill2Name}"
        }
    }

    private fun getSkillLevelRequirement(skillName: String): Int {
        return when (skillName.lowercase()) {
            "fire mastery" -> 25
            "water control" -> 15
            "beast communication" -> 5
            "ancient runes" -> 40
            else -> 10
        }
    }
}