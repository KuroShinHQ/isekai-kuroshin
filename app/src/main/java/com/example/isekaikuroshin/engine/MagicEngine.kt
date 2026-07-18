package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.*

object MagicEngine {

    fun getElementalCombinations(): List<ElementalCombination> {
        return listOf(
            // Fire combinations
            ElementalCombination(
                primaryElement = Element.FIRE,
                secondaryElement = Element.AIR,
                resultElement = Element.LIGHTNING,
                bonusMultiplier = 1.25f,
                newEffects = listOf(
                    SpellEffect("effect_explosive_force", "effect_explosive_force_desc", EffectType.DAMAGE, 1.5f)
                )
            ),
            ElementalCombination(
                primaryElement = Element.FIRE,
                secondaryElement = Element.EARTH,
                resultElement = Element.FIRE,
                bonusMultiplier = 1.4f,
                newEffects = listOf(
                    SpellEffect("effect_molten_rock", "effect_molten_rock_desc", EffectType.DOT, 0.5f, 10)
                )
            ),

            // Water combinations
            ElementalCombination(
                primaryElement = Element.WATER,
                secondaryElement = Element.AIR,
                resultElement = Element.ICE,
                bonusMultiplier = 1.2f,
                newEffects = listOf(
                    SpellEffect("effect_freeze", "effect_freeze_desc", EffectType.FREEZE, 1.0f, 5)
                )
            ),
            ElementalCombination(
                primaryElement = Element.WATER,
                secondaryElement = Element.EARTH,
                resultElement = Element.NATURE,
                bonusMultiplier = 1.3f,
                newEffects = listOf(
                    SpellEffect("effect_growth", "effect_growth_desc", EffectType.HOT, 0.8f, 15)
                )
            ),

            // Light and Dark combinations
            ElementalCombination(
                primaryElement = Element.LIGHT,
                secondaryElement = Element.DARK,
                resultElement = Element.VOID,
                bonusMultiplier = 2.0f,
                newEffects = listOf(
                    SpellEffect("effect_reality_tear", "effect_reality_tear_desc", EffectType.DAMAGE, 2.0f)
                )
            ),

            // Earth combinations
            ElementalCombination(
                primaryElement = Element.EARTH,
                secondaryElement = Element.LIGHTNING,
                resultElement = Element.EARTH,
                bonusMultiplier = 1.35f,
                newEffects = listOf(
                    SpellEffect("effect_magnetism", "effect_magnetism_desc", EffectType.STUN, 1.0f, 8)
                )
            )
        )
    }

    // KURAL 9: Spell names and descriptions are localization keys
    // UI will resolve these via LanguageManager.getText()
    fun getDefaultSpells(): List<Spell> {
        return listOf(
            // Fire Spells
            Spell(
                name = "spell_fireball", // Localization key
                description = "spell_fireball_desc", // Localization key
                element = Element.FIRE,
                tier = "Bronze",
                manaCost = 15,
                damage = 25,
                cooldown = 3,
                range = 1.5f,
                requiredLevel = 1
            ),
            Spell(
                name = "spell_inferno",
                description = "spell_inferno_desc",
                element = Element.FIRE,
                tier = "Gold",
                manaCost = 50,
                damage = 80,
                cooldown = 15,
                range = 2.0f,
                effects = listOf(
                    SpellEffect("effect_burn", "effect_burn_desc", EffectType.DOT, 10.0f, 8)
                ),
                requiredLevel = 20
            ),

            // Water Spells
            Spell(
                name = "spell_water_bolt",
                description = "spell_water_bolt_desc",
                element = Element.WATER,
                tier = "Bronze",
                manaCost = 12,
                damage = 20,
                cooldown = 2,
                range = 1.8f,
                requiredLevel = 1
            ),
            Spell(
                name = "spell_healing_spring",
                description = "spell_healing_spring_desc",
                element = Element.WATER,
                tier = "Silver",
                manaCost = 30,
                healing = 45,
                duration = 10,
                cooldown = 20,
                range = 1.0f,
                effects = listOf(
                    SpellEffect("effect_regeneration", "effect_regeneration_desc", EffectType.HOT, 5.0f, 10)
                ),
                requiredLevel = 8
            ),

            // Earth Spells
            Spell(
                name = "spell_stone_spike",
                description = "spell_stone_spike_desc",
                element = Element.EARTH,
                tier = "Bronze",
                manaCost = 18,
                damage = 30,
                cooldown = 4,
                range = 1.2f,
                requiredLevel = 3
            ),
            Spell(
                name = "spell_earth_shield",
                description = "spell_earth_shield_desc",
                element = Element.EARTH,
                tier = "Silver",
                manaCost = 25,
                duration = 30,
                cooldown = 25,
                range = 0.5f,
                effects = listOf(
                    SpellEffect("effect_shield", "effect_shield_desc", EffectType.SHIELD, 50.0f, 30)
                ),
                requiredLevel = 12
            ),

            // Air Spells
            Spell(
                name = "spell_wind_slash",
                description = "spell_wind_slash_desc",
                element = Element.AIR,
                tier = "Bronze",
                manaCost = 14,
                damage = 22,
                cooldown = 2,
                range = 2.0f,
                requiredLevel = 2
            ),
            Spell(
                name = "spell_hurricane",
                description = "spell_hurricane_desc",
                element = Element.AIR,
                tier = "Platinum",
                manaCost = 75,
                damage = 60,
                duration = 12,
                cooldown = 45,
                range = 3.0f,
                effects = listOf(
                    SpellEffect("effect_stun", "effect_stun_desc", EffectType.STUN, 1.0f, 5)
                ),
                requiredLevel = 35
            ),

            // Lightning Spells
            Spell(
                name = "spell_lightning_bolt",
                description = "spell_lightning_bolt_desc",
                element = Element.LIGHTNING,
                tier = "Silver",
                manaCost = 20,
                damage = 35,
                cooldown = 1,
                range = 2.5f,
                effects = listOf(
                    SpellEffect("effect_paralysis", "effect_paralysis_desc", EffectType.STUN, 1.0f, 2)
                ),
                requiredLevel = 6
            ),

            // Light Spells
            Spell(
                name = "spell_heal",
                description = "spell_heal_desc",
                element = Element.LIGHT,
                tier = "Bronze",
                manaCost = 20,
                healing = 40,
                cooldown = 5,
                range = 1.0f,
                requiredLevel = 1
            ),
            Spell(
                name = "spell_divine_strike",
                description = "spell_divine_strike_desc",
                element = Element.LIGHT,
                tier = "Gold",
                manaCost = 40,
                damage = 55,
                cooldown = 8,
                range = 1.5f,
                effects = listOf(
                    SpellEffect("effect_purify", "effect_purify_desc", EffectType.BUFF_STAT, 1.0f)
                ),
                requiredLevel = 18
            ),

            // Dark Spells
            Spell(
                name = "spell_shadow_bolt",
                description = "spell_shadow_bolt_desc",
                element = Element.DARK,
                tier = "Bronze",
                manaCost = 16,
                damage = 28,
                cooldown = 3,
                range = 1.8f,
                effects = listOf(
                    SpellEffect("effect_curse", "effect_curse_desc", EffectType.DEBUFF_STAT, 0.9f, 10)
                ),
                requiredLevel = 4
            ),
            Spell(
                name = "spell_life_drain",
                description = "spell_life_drain_desc",
                element = Element.DARK,
                tier = "Silver",
                manaCost = 30,
                damage = 25,
                healing = 20,
                cooldown = 10,
                range = 1.5f,
                requiredLevel = 10
            )
        )
    }

    fun getMagicSchools(): List<MagicSchool> {
        return listOf(
            MagicSchool(
                name = "Pyromancy",
                description = "School of fire magic",
                primaryElement = Element.FIRE,
                secondaryElements = listOf(Element.LIGHTNING, Element.EARTH),
                specialtyBonuses = mapOf(
                    "damage" to 1.2f,
                    "aoe" to 1.3f
                )
            ),
            MagicSchool(
                name = "Hydromancy",
                description = "School of water magic",
                primaryElement = Element.WATER,
                secondaryElements = listOf(Element.ICE, Element.NATURE),
                specialtyBonuses = mapOf(
                    "healing" to 1.4f,
                    "duration" to 1.25f
                )
            ),
            MagicSchool(
                name = "Geomancy",
                description = "School of earth magic",
                primaryElement = Element.EARTH,
                secondaryElements = listOf(Element.NATURE, Element.LIGHTNING),
                specialtyBonuses = mapOf(
                    "defense" to 1.5f,
                    "durability" to 1.3f
                )
            ),
            MagicSchool(
                name = "Aeromancy",
                description = "School of air magic",
                primaryElement = Element.AIR,
                secondaryElements = listOf(Element.LIGHTNING, Element.ICE),
                specialtyBonuses = mapOf(
                    "speed" to 1.3f,
                    "range" to 1.4f
                )
            ),
            MagicSchool(
                name = "Electromancy",
                description = "School of lightning magic",
                primaryElement = Element.LIGHTNING,
                secondaryElements = listOf(Element.FIRE, Element.AIR),
                specialtyBonuses = mapOf(
                    "speed" to 1.5f,
                    "criticalHit" to 1.25f
                )
            ),
            MagicSchool(
                name = "Thaumaturgy",
                description = "School of light magic",
                primaryElement = Element.LIGHT,
                secondaryElements = listOf(Element.NATURE),
                specialtyBonuses = mapOf(
                    "healing" to 1.6f,
                    "purification" to 1.8f
                )
            ),
            MagicSchool(
                name = "Necromancy",
                description = "School of dark magic",
                primaryElement = Element.DARK,
                secondaryElements = listOf(Element.VOID),
                specialtyBonuses = mapOf(
                    "lifeDrain" to 1.4f,
                    "debuff" to 1.3f
                )
            ),
            MagicSchool(
                name = "Void Magic",
                description = "School of void manipulation",
                primaryElement = Element.VOID,
                secondaryElements = listOf(Element.DARK, Element.LIGHT),
                specialtyBonuses = mapOf(
                    "reality_manipulation" to 2.0f,
                    "teleportation" to 1.5f
                )
            )
        )
    }

    fun combineSpells(spell1: Spell, spell2: Spell): Spell? {
        val combination = getElementalCombinations().find {
            (it.primaryElement == spell1.element && it.secondaryElement == spell2.element) ||
            (it.primaryElement == spell2.element && it.secondaryElement == spell1.element)
        } ?: return null

        val combinedDamage = ((spell1.damage + spell2.damage) * combination.bonusMultiplier).toLong()
        val combinedHealing = ((spell1.healing + spell2.healing) * combination.bonusMultiplier).toLong()
        val combinedManaCost = ((spell1.manaCost + spell2.manaCost) * 0.8f).toInt()

        return Spell(
            name = "${spell1.name} + ${spell2.name}",
            description = "Combined power of ${spell1.name} and ${spell2.name}",
            element = combination.resultElement,
            tier = getNextTier(maxOf(getTierLevel(spell1.tier), getTierLevel(spell2.tier))),
            manaCost = combinedManaCost,
            damage = combinedDamage,
            healing = combinedHealing,
            duration = maxOf(spell1.duration, spell2.duration),
            cooldown = maxOf(spell1.cooldown, spell2.cooldown),
            range = maxOf(spell1.range, spell2.range),
            effects = spell1.effects + spell2.effects + combination.newEffects,
            requiredLevel = maxOf(spell1.requiredLevel, spell2.requiredLevel) + 5
        )
    }

    fun getElementalWeakness(element: Element): Element {
        return when (element) {
            Element.FIRE -> Element.WATER
            Element.WATER -> Element.LIGHTNING
            Element.EARTH -> Element.AIR
            Element.AIR -> Element.EARTH
            Element.LIGHTNING -> Element.EARTH
            Element.ICE -> Element.FIRE
            Element.LIGHT -> Element.DARK
            Element.DARK -> Element.LIGHT
            Element.VOID -> Element.LIGHT
            Element.NATURE -> Element.FIRE
        }
    }

    fun getElementalStrength(element: Element): Element {
        return when (element) {
            Element.FIRE -> Element.NATURE
            Element.WATER -> Element.FIRE
            Element.EARTH -> Element.LIGHTNING
            Element.AIR -> Element.EARTH
            Element.LIGHTNING -> Element.WATER
            Element.ICE -> Element.WATER
            Element.LIGHT -> Element.VOID
            Element.DARK -> Element.LIGHT
            Element.VOID -> Element.DARK
            Element.NATURE -> Element.EARTH
        }
    }

    fun calculateSpellEffectiveness(spellElement: Element, targetElement: Element): Float {
        return when {
            getElementalStrength(spellElement) == targetElement -> 1.5f // Strong against
            getElementalWeakness(spellElement) == targetElement -> 0.75f // Weak against
            spellElement == targetElement -> 1.0f // Same element
            else -> 1.0f // Neutral
        }
    }

    private fun getTierLevel(tier: String): Int {
        return when (tier) {
            "Bronze" -> 1
            "Silver" -> 2
            "Gold" -> 3
            "Platinum" -> 4
            "Diamond" -> 5
            else -> 6 // Legendary
        }
    }

    private fun getNextTier(tierLevel: Int): String {
        return when (tierLevel) {
            1 -> "Silver"
            2 -> "Gold"
            3 -> "Platinum"
            4 -> "Diamond"
            else -> "Legendary"
        }
    }

    /**
     * TODO-G115.1: Calculate spell damage with all modifiers
     *
     * @param spell The spell being cast
     * @param casterIntelligence Caster's intelligence stat
     * @param casterSpirit Caster's spirit stat (optional, affects elemental spells)
     * @param performanceScore Gesture/cast performance (0.0-1.0, from SpellStudio)
     * @param targetElement Target's elemental affinity (for weakness/strength calculation)
     * @param statusEffectBonuses Buff/debuff modifiers from StatusEffectManager
     * @return Final damage amount
     */
    fun calculateSpellDamage(
        spell: Spell,
        casterIntelligence: Float,
        casterSpirit: Int = 50,
        performanceScore: Float = 1.0f,
        targetElement: Element? = null,
        statusEffectBonuses: Map<String, Int> = emptyMap()
    ): Int {
        // Base damage from spell
        var finalDamage = spell.damage.toFloat()

        // Intelligence scaling (INT affects spell power)
        val intelligenceMultiplier = 1.0f + (casterIntelligence / 100f) * 0.5f
        finalDamage *= intelligenceMultiplier

        // Spirit scaling (SPIRIT affects elemental spell power)
        val spiritMultiplier = 1.0f + (casterSpirit / 100f) * 0.3f
        finalDamage *= spiritMultiplier

        // Performance score (gesture accuracy from SpellStudio)
        finalDamage *= performanceScore

        // Elemental effectiveness (if target has elemental affinity)
        if (targetElement != null) {
            val elementalMultiplier = calculateSpellEffectiveness(spell.element, targetElement)
            finalDamage *= elementalMultiplier
        }

        // Status effect bonuses (buffs/debuffs)
        val intelligenceBonus = statusEffectBonuses["intelligence"] ?: 0
        val strengthBonus = statusEffectBonuses["strength"] ?: 0 // Physical component
        finalDamage += (intelligenceBonus * 0.8f) + (strengthBonus * 0.2f)

        return finalDamage.toInt().coerceAtLeast(1) // Minimum 1 damage
    }

    /**
     * TODO-G115.1: Calculate actual mana cost with modifiers
     *
     * @param spell The spell being cast
     * @param casterIntelligence Higher INT = lower mana cost
     * @param statusEffectBonuses Mana cost reduction buffs
     * @return Final mana cost
     */
    fun calculateManaCost(
        spell: Spell,
        casterIntelligence: Float,
        statusEffectBonuses: Map<String, Int> = emptyMap()
    ): Int {
        var finalCost = spell.manaCost.toFloat()

        // Intelligence reduces mana cost (smart casters are more efficient)
        val intelligenceReduction = (casterIntelligence / 200f).coerceAtMost(0.3f) // Max 30% reduction
        finalCost *= (1.0f - intelligenceReduction)

        // Status effect mana cost reduction
        val manaReduction = statusEffectBonuses["mana_cost_reduction"] ?: 0
        finalCost *= (1.0f - (manaReduction / 100f))

        return finalCost.toInt().coerceAtLeast(1) // Minimum 1 mana
    }

    /**
     * TODO-G115.1: Calculate spell healing with modifiers
     *
     * @param spell The healing spell
     * @param casterIntelligence INT affects healing power
     * @param casterSpirit SPIRIT boosts healing significantly
     * @param performanceScore Gesture/cast performance
     * @param statusEffectBonuses Healing boost buffs
     * @return Final healing amount
     */
    fun calculateSpellHealing(
        spell: Spell,
        casterIntelligence: Float,
        casterSpirit: Int = 50,
        performanceScore: Float = 1.0f,
        statusEffectBonuses: Map<String, Int> = emptyMap()
    ): Int {
        var finalHealing = spell.healing.toFloat()

        // Intelligence scaling
        val intelligenceMultiplier = 1.0f + (casterIntelligence / 100f) * 0.4f
        finalHealing *= intelligenceMultiplier

        // Spirit scaling (SPIRIT is primary stat for healing)
        val spiritMultiplier = 1.0f + (casterSpirit / 100f) * 0.6f
        finalHealing *= spiritMultiplier

        // Performance score
        finalHealing *= performanceScore

        // Status effect bonuses
        val healingBonus = statusEffectBonuses["healing_power"] ?: 0
        finalHealing += (healingBonus * 1.5f)

        return finalHealing.toInt().coerceAtLeast(1)
    }
}