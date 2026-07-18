package com.example.isekaikuroshin.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.isekaikuroshin.engine.StatType
import kotlinx.serialization.Serializable
import com.example.isekaikuroshin.data.ItemRarity // EKLENDİ
import com.example.isekaikuroshin.data.ItemType   // EKLENDİ

// 1. Yeni Veri Sınıfları

// ClassQuest tanımı buradan kaldırıldı.

data class RegistryItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val rarity: ItemRarity,
    val type: ItemType,
    val slot: String,
    val weight: Float,
    val statBonuses: Map<StatType, Float>
)

data class RegistryNPC(
    val id: String,
    val name: String,
    val personality: String,
    val backstory: String,
    val initialQuests: List<String>
)

@Serializable
data class RegistryQuest(
    val id: String,
    val title: String,  // DEPRECATED: Use titleKey for localization
    val description: String,  // DEPRECATED: Use descriptionKey for localization
    val giverNpcId: String,
    val difficulty: String = "Medium", // Easy, Medium, Hard
    val rewards: Map<String, Int> = emptyMap(), // Item ID -> Miktar
    val titleKey: String? = null,  // G135: Localization key for title (KURAL 9)
    val descriptionKey: String? = null  // G135: Localization key for description (KURAL 9)
)

// 2. ContentRegistry Nesnesi

object ContentRegistry {

    object Items {
        // 3. Örnek İçerik
        private val predefinedItems = listOf(
            RegistryItem(
                id = "cloak_of_shadows",
                name = "Gölge Pelerini",
                description = "Kullanıcısını gölgelerin içinde gizleyen, hafif ve büyülü bir pelerin.",
                icon = Icons.Default.Shield,
                rarity = ItemRarity.RARE,
                type = ItemType.EQUIPMENT,
                slot = "Back",
                weight = 1.5f,
                statBonuses = mapOf(StatType.AGI_FLAT to 5f, StatType.LUCK_FLAT to 2f)
            ),
            RegistryItem(
                id = "starforged_sword",
                name = "Yıldızçeliği Kılıcı",
                description = "Düşen yıldızların metalinden dövülmüş, keskin bir kılıç. Ay ışığında parıldar.",
                icon = Icons.Default.Build,
                rarity = ItemRarity.LEGENDARY,
                type = ItemType.EQUIPMENT,
                slot = "MainHand",
                weight = 3.0f,
                statBonuses = mapOf(StatType.STR_FLAT to 12f, StatType.AGI_FLAT to 3f)
            ),
            RegistryItem(
                id = "guardian_chestplate",
                name = "Koruyucu Göğüslük",
                description = "Kadim koruyucuların giydiği sağlam bir zırh. Ağır darbelerden korur.",
                icon = Icons.Default.Shield,
                rarity = ItemRarity.UNCOMMON,
                type = ItemType.EQUIPMENT,
                slot = "Chest",
                weight = 8.5f,
                statBonuses = mapOf(StatType.VIT_FLAT to 8f, StatType.STR_FLAT to 2f)
            ),
            RegistryItem(
                id = "minor_health_potion",
                name = "Küçük Can İksiri",
                description = "Kırmızı renkli, tatlı kokulu bir iksir. Yaraları iyileştirir ve enerji verir.",
                icon = Icons.Default.LocalPharmacy,
                rarity = ItemRarity.COMMON,
                type = ItemType.CONSUMABLE,
                slot = "Consumable",
                weight = 0.2f,
                statBonuses = mapOf() // Tüketilebilir, stat bonusu yok
            ),
            RegistryItem(
                id = "luck_charm",
                name = "Şans Tılsımı",
                description = "Eski bir büyücünün el emeği. Taşıyanın kaderini olumlu yönde etkiler.",
                icon = Icons.Default.AutoAwesome,
                rarity = ItemRarity.RARE,
                type = ItemType.EQUIPMENT,
                slot = "Accessory",
                weight = 0.1f,
                statBonuses = mapOf(StatType.LUCK_FLAT to 10f, StatType.INT_FLAT to 3f)
            )
        )

        fun getItem(id: String): RegistryItem? {
            return predefinedItems.find { it.id == id }
        }

        fun getAllItems(): List<RegistryItem> {
            return predefinedItems
        }

        fun getItemById(id: String): RegistryItem? {
            return predefinedItems.find { it.id == id }
        }
    }

    object NPCs {
        // 3. Örnek İçerik
        private val predefinedNPCs = listOf(
            RegistryNPC(
                id = "npc_elara",
                name = "Yaşlı Elara",
                personality = "Bilge, gizemli ve dünyanın unutulmuş tarihine dair derin bir bilgiye sahip. Genç maceracılara yardım etmeyi sever ama her zaman bir bedel karşılığında.",
                backstory = "Bir zamanlar kayıp bir krallığın kütüphanecisi olan Elara, şimdi ormanın derinliklerinde küçük bir kulübede yaşıyor.",
                initialQuests = listOf("quest_forgotten_legacy")
            ),
            RegistryNPC(
                id = "npc_borin",
                name = "Tüccar Borin",
                personality = "Cana yakın, pazarlık etmeyi seven ve her zaman karlı bir anlaşma peşinde koşan deneyimli bir tüccar. Müşterilerine karşı dürüst ama kurnaz.",
                backstory = "Genç yaşlarından beri farklı topraklarda ticaret yapan Borin, artık bu kasabaya yerleşmiş durumda. Eşsiz eşyalar ve nadir malzemeler konusunda uzman.",
                initialQuests = listOf()
            ),
            RegistryNPC(
                id = "npc_kael",
                name = "Usta Kael",
                personality = "Sessiz, mükemmeliyetçi ve işine son derece tutkulu bir demirci. Sabrı sonsuz ama kalitesiz işlere tahammülü yok. Ustaca işçiliği takdir eder.",
                backstory = "Dağların eteğindeki eski bir demirhanede çalışan Kael, babadan oğula geçen demircilik sanatının son temsilcilerinden. Efsanevi silahlar ürettiği rivayet edilir.",
                initialQuests = listOf()
            )
        )

        fun getNPC(id: String): RegistryNPC? {
            return predefinedNPCs.find { it.id == id }
        }

        fun getAllNPCs(): List<RegistryNPC> {
            return predefinedNPCs
        }
    }

    object Quests {
        // 3. Örnek İçerik
        private val predefinedQuests = listOf(
            RegistryQuest(
                id = "quest_forgotten_legacy",
                title = "Unutulmuş Miras",
                description = "Elara, senden kadim bir harabede saklı olan ve unutulmuş bir kraliyete ait olan bir eseri bulmanı istiyor.",
                giverNpcId = "npc_elara",
                rewards = mapOf("cloak_of_shadows" to 1)
            )
        )

        fun getQuest(id: String): RegistryQuest? {
            return predefinedQuests.find { it.id == id }
        }

        fun getAllQuests(): List<RegistryQuest> {
            return predefinedQuests
        }

        fun getQuestById(id: String): RegistryQuest? {
            return predefinedQuests.find { it.id == id }
        }
    }

    object MainQuests {
        // Ana Görevler (Developer tarafından hardcoded)
        // Bu görevler hikayenin omurgasını oluşturur
        // G135: KURAL 9 - Localization keys kullan (titleKey, descriptionKey)
        private val predefinedMainQuests = listOf(
            RegistryQuest(
                id = "main_quest_1_reach_village",
                title = "",  // Deprecated - use titleKey
                description = "",  // Deprecated - use descriptionKey
                titleKey = "main_quest_1_title",
                descriptionKey = "main_quest_1_desc",
                giverNpcId = "system", // Sistem tarafından verilen
                difficulty = "Easy",
                rewards = mapOf("minor_health_potion" to 2, "gold" to 50)
            ),
            RegistryQuest(
                id = "main_quest_2_find_magic_tutor",
                title = "",  // Deprecated - use titleKey
                description = "",  // Deprecated - use descriptionKey
                titleKey = "main_quest_2_title",
                descriptionKey = "main_quest_2_desc",
                giverNpcId = "system",
                difficulty = "Medium",
                rewards = mapOf("luck_charm" to 1, "gold" to 100)
            ),
            RegistryQuest(
                id = "main_quest_3_first_real_challenge",
                title = "",  // Deprecated - use titleKey
                description = "",  // Deprecated - use descriptionKey
                titleKey = "main_quest_3_title",
                descriptionKey = "main_quest_3_desc",
                giverNpcId = "system",
                difficulty = "Hard",
                rewards = mapOf("starforged_sword" to 1, "gold" to 200)
            )
        )

        fun getMainQuest(id: String): RegistryQuest? {
            return predefinedMainQuests.find { it.id == id }
        }

        fun getAllMainQuests(): List<RegistryQuest> {
            return predefinedMainQuests
        }

        // GM'e hikaye durumuna göre tetiklenecek ana görevi öner
        fun getNextMainQuestForStory(
            currentDay: Int,
            completedMainQuestIds: List<String>,
            reachedVillage: Boolean = false
        ): RegistryQuest? {
            // İlk ana görev: 1. gün sonunda veya kullanıcı bir yerleşkeye ulaştığında tetikle
            if (!completedMainQuestIds.contains("main_quest_1_reach_village") &&
                (currentDay >= 1 || reachedVillage)) {
                return predefinedMainQuests.find { it.id == "main_quest_1_reach_village" }
            }

            // İkinci ana görev: 1. görev tamamlandıktan sonra ve 3. gün sonunda
            if (completedMainQuestIds.contains("main_quest_1_reach_village") &&
                !completedMainQuestIds.contains("main_quest_2_find_magic_tutor") &&
                currentDay >= 3) {
                return predefinedMainQuests.find { it.id == "main_quest_2_find_magic_tutor" }
            }

            // Üçüncü ana görev: 2. görev tamamlandıktan sonra ve 7. gün sonunda
            if (completedMainQuestIds.contains("main_quest_2_find_magic_tutor") &&
                !completedMainQuestIds.contains("main_quest_3_first_real_challenge") &&
                currentDay >= 7) {
                return predefinedMainQuests.find { it.id == "main_quest_3_first_real_challenge" }
            }

            return null
        }

        // Kullanıcı bir yerleşkeye ulaştığını kontrol et
        fun checkIfReachedSettlement(storyText: String): Boolean {
            val settlementKeywords = listOf(
                "köy", "kasaba", "şehir", "yerleşke", "village", "town", "city", "settlement",
                "köye ulaş", "kasabaya ulaş", "köyde", "kasabada", "şehirde",
                "köyün", "kasabanın"
            )
            return settlementKeywords.any { keyword ->
                storyText.lowercase().contains(keyword)
            }
        }
    }

    object ClassQuests {
        // Sınıf görevlerini tanımla
        private val predefinedClassQuests = listOf(
            ClassQuest(
                id = "class_quest_warrior",
                triggeringArchetype = "Savasci",
                quest = RegistryQuest(
                    id = "quest_savascinin_yolu",
                    title = "Savaşçının Yolu",
                    description = "Arenada kendini kanıtla ve gerçek bir savaşçı olmaya hak kazan.",
                    giverNpcId = "npc_arena_master",
                    rewards = mapOf("warrior_emblem" to 1)
                )
            ),
            ClassQuest(
                id = "class_quest_explorer",
                triggeringArchetype = "Kasif",
                quest = RegistryQuest(
                    id = "quest_kasifin_yolu",
                    title = "Kaşifin Yolu",
                    description = "Bilinmeyen toprakları keşfet ve kayıp haritaları bul.",
                    giverNpcId = "npc_cartographer",
                    rewards = mapOf("explorer_compass" to 1)
                )
            ),
            ClassQuest(
                id = "class_quest_craftsman",
                triggeringArchetype = "Zanaatkar",
                quest = RegistryQuest(
                    id = "quest_zanaatkarin_yolu",
                    title = "Zanaatkârın Yolu",
                    description = "Ustaca bir eser yaratarak zanaatkârlık sanatını kanıtla.",
                    giverNpcId = "npc_master_smith",
                    rewards = mapOf("master_tools" to 1)
                )
            ),
            ClassQuest(
                id = "class_quest_mystic",
                triggeringArchetype = "Mistik",
                quest = RegistryQuest(
                    id = "quest_mistigin_yolu",
                    title = "Mistiğin Yolu",
                    description = "Kadim büyüleri öğren ve ruhani gücünü keşfet.",
                    giverNpcId = "npc_sage",
                    rewards = mapOf("mystic_crystal" to 1)
                )
            )
        )

        fun getClassQuest(archetype: String): ClassQuest? {
            return predefinedClassQuests.find { it.triggeringArchetype == archetype }
        }

        fun getAllClassQuests(): List<ClassQuest> {
            return predefinedClassQuests
        }
    }
}