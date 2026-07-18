package com.example.isekaikuroshin.data

/**
 * Medieval currency system data classes
 * Conversion rates:
 * 100 Iron = 50 Copper
 * 100 Copper = 1 Silver
 * 100 Silver = 1 Gold
 */

data class Currency(
    val iron: Int = 0,
    val copper: Int = 0,
    val silver: Int = 0,
    val gold: Int = 0
) {
    /**
     * Converts all currency to total iron value for calculations
     */
    fun toTotalIron(): Int {
        return iron + (copper * 2) + (silver * 200) + (gold * 20000)
    }

    /**
     * Creates Currency from total iron amount, automatically converting to higher denominations
     */
    companion object {
        fun fromTotalIron(totalIron: Int): Currency {
            var remaining = totalIron

            val gold = remaining / 20000
            remaining %= 20000

            val silver = remaining / 200
            remaining %= 200

            val copper = remaining / 2
            remaining %= 2

            return Currency(
                iron = remaining,
                copper = copper,
                silver = silver,
                gold = gold
            )
        }
    }

    /**
     * Adds another currency amount to this one
     */
    operator fun plus(other: Currency): Currency {
        return fromTotalIron(this.toTotalIron() + other.toTotalIron())
    }

    /**
     * Subtracts another currency amount from this one
     */
    operator fun minus(other: Currency): Currency {
        val result = this.toTotalIron() - other.toTotalIron()
        return if (result >= 0) fromTotalIron(result) else Currency()
    }

    /**
     * Checks if this currency amount is sufficient to pay for another amount
     */
    fun canAfford(cost: Currency): Boolean {
        return this.toTotalIron() >= cost.toTotalIron()
    }

    /**
     * Formats currency for display
     */
    fun toDisplayString(): String {
        val parts = mutableListOf<String>()

        if (gold > 0) parts.add("${gold}🥇")
        if (silver > 0) parts.add("${silver}🥈")
        if (copper > 0) parts.add("${copper}🥉")
        if (iron > 0) parts.add("${iron}⚫")

        return if (parts.isEmpty()) "0⚫" else parts.joinToString(" ")
    }

    /**
     * Formats currency in a compact way showing only the highest denomination
     */
    fun toCompactString(): String {
        return when {
            gold > 0 -> "${gold}🥇"
            silver > 0 -> "${silver}🥈"
            copper > 0 -> "${copper}🥉"
            iron > 0 -> "${iron}⚫"
            else -> "0⚫"
        }
    }
}

/**
 * Common currency amounts for easier usage
 */
object CurrencyAmounts {
    val ZERO = Currency()
    val ONE_IRON = Currency(iron = 1)
    val ONE_COPPER = Currency(copper = 1)
    val ONE_SILVER = Currency(silver = 1)
    val ONE_GOLD = Currency(gold = 1)

    // Common amounts
    val SMALL_POUCH = Currency(iron = 50, copper = 10)
    val MEDIUM_POUCH = Currency(copper = 50, silver = 5)
    val LARGE_POUCH = Currency(silver = 20, gold = 1)
    val TREASURE_HOARD = Currency(gold = 10)
}