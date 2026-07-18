package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

/**
 * Zanaat tarifini temsil eden veri modeli
 */
@Serializable
data class CraftingRecipe(
    val id: String,
    val name: String,
    val description: String,
    val requiredMaterials: Map<String, Int>, // Malzeme ID'si -> Miktar
    val requiredLevel: Int = 1,
    val craftingTime: Int = 1, // Dakika cinsinden
    val resultItem: EquippedItemZ6,
    val category: CraftingCategory = CraftingCategory.BASIC
)

/**
 * Zanaat kategorileri
 */
@Serializable
enum class CraftingCategory {
    BASIC,      // Temel eşyalar
    WEAPONS,    // Silahlar
    ARMOR,      // Zırhlar
    TOOLS,      // Aletler
    POTIONS,    // İksirler
    SPECIAL     // Özel eşyalar
}

/**
 * Malzeme türleri - ResourcesZ4 ile uyumlu
 */
enum class MaterialType(val displayName: String, val resourceKey: String) {
    DAL("Dal", "dal"),
    TAS("Taş", "tas"),
    ODUN("Odun", "odun"),
    DERI("Deri", "deri"),
    DEMIR("Demir", "demir")
}

/**
 * Zanaat sistemi için hazır tarifler
 */
object CraftingRecipes {

    val WOODEN_SWORD = CraftingRecipe(
        id = "wooden_sword",
        name = "Ahşap Kılıç",
        description = "Temel ahşap malzemelerden yapılmış basit bir kılıç.",
        requiredMaterials = mapOf(
            "odun" to 3,
            "dal" to 2
        ),
        requiredLevel = 1,
        craftingTime = 5,
        resultItem = EquippedItemZ6(
            name = "Ahşap Kılıç",
            description = "Basit ama işlevsel bir ahşap kılıç. Yeni başlayanlar için idealdir.",
            weight = 1.2f,
            rarity = "COMMON",
            statBonuses = mapOf("strength" to 1.1f, "physicalAttack" to 1.2f)
        ),
        category = CraftingCategory.WEAPONS
    )

    val LEATHER_ARMOR = CraftingRecipe(
        id = "leather_armor",
        name = "Deri Zırh",
        description = "Dayanıklı deri malzemelerden yapılmış koruyucu zırh.",
        requiredMaterials = mapOf(
            "deri" to 4,
            "dal" to 1
        ),
        requiredLevel = 2,
        craftingTime = 8,
        resultItem = EquippedItemZ6(
            name = "Deri Zırh",
            description = "Esnek ve koruyucu deri zırh. Hareket kabiliyetini sınırlamaz.",
            weight = 2.5f,
            rarity = "COMMON",
            statBonuses = mapOf("defense" to 1.3f, "vitality" to 1.1f)
        ),
        category = CraftingCategory.ARMOR
    )

    val STONE_AXE = CraftingRecipe(
        id = "stone_axe",
        name = "Taş Balta",
        description = "Keskin taş ve sağlam odundan yapılmış balta.",
        requiredMaterials = mapOf(
            "tas" to 2,
            "odun" to 2,
            "deri" to 1
        ),
        requiredLevel = 2,
        craftingTime = 6,
        resultItem = EquippedItemZ6(
            name = "Taş Balta",
            description = "Ağaç kesme ve savaşta kullanılabilen çok amaçlı taş balta.",
            weight = 2.0f,
            rarity = "COMMON",
            statBonuses = mapOf("strength" to 1.2f, "physicalAttack" to 1.15f)
        ),
        category = CraftingCategory.TOOLS
    )

    val IRON_DAGGER = CraftingRecipe(
        id = "iron_dagger",
        name = "Demir Hançer",
        description = "Keskin demir ağızlı hızlı saldırı silahı.",
        requiredMaterials = mapOf(
            "demir" to 2,
            "deri" to 1,
            "dal" to 1
        ),
        requiredLevel = 3,
        craftingTime = 10,
        resultItem = EquippedItemZ6(
            name = "Demir Hançer",
            description = "Hızlı ve ölümcül. Çeviklik gerektiren savaşlar için ideal.",
            weight = 0.8f,
            rarity = "UNCOMMON",
            statBonuses = mapOf("agility" to 1.2f, "physicalAttack" to 1.3f, "critChance" to 1.1f)
        ),
        category = CraftingCategory.WEAPONS
    )

    val HEALING_POTION = CraftingRecipe(
        id = "healing_potion",
        name = "İyileştirme İksiri",
        description = "Doğal bitkilerden hazırlanmış şifa içeceği.",
        requiredMaterials = mapOf(
            "dal" to 3 // Şifalı bitkiler olarak
        ),
        requiredLevel = 1,
        craftingTime = 3,
        resultItem = EquippedItemZ6(
            name = "İyileştirme İksiri",
            description = "Anında sağlık yeniler. Tek kullanımlık.",
            weight = 0.2f,
            rarity = "COMMON",
            statBonuses = mapOf("currentHealth" to 1.0f) // Özel kullanım gerekir
        ),
        category = CraftingCategory.POTIONS
    )

    fun getAllRecipes(): List<CraftingRecipe> {
        return listOf(
            WOODEN_SWORD,
            LEATHER_ARMOR,
            STONE_AXE,
            IRON_DAGGER,
            HEALING_POTION
        )
    }

    fun getRecipesByCategory(category: CraftingCategory): List<CraftingRecipe> {
        return getAllRecipes().filter { it.category == category }
    }

    fun getRecipeById(id: String): CraftingRecipe? {
        return getAllRecipes().find { it.id == id }
    }
}