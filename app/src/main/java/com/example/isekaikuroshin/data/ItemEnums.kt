package com.example.isekaikuroshin.data

enum class ItemRarity {
    COMMON,
    UNCOMMON,
    RARE,
    EPIC,
    LEGENDARY,
    MYTHIC,
    ARTIFACT
}

enum class ItemType {
    WEAPON,         // Silahlar (Kılıç, balta, yay vb.)
    ARMOR,          // Zırhlar (Göğüslük, kask, eldiven vb.)
    EQUIPMENT,      // Genel ekipman/aksesuar (Pelerin, yüzük, tılsım vb. ARMOR'a girmeyenler)
    CONSUMABLE,     // Tüketilebilirler (İksir, yiyecek, parşömen vb.)
    MATERIAL,       // Üretim/geliştirme malzemeleri (Cevher, odun, deri vb.)
    QUEST,          // Görev eşyaları (Hikaye ilerlemesi için anahtar eşyalar)
    CURRENCY,       // Para birimleri (Altın, özel taşlar vb.)
    TRINKET,        // Ufak tefek, genellikle stat bonusu veren süs eşyaları
    MISC            // Diğer / Çeşitli (Yukarıdaki kategorilere girmeyen her şey)
}

// ItemRarity → CardRarity dönüşüm fonksiyonu
fun ItemRarity.toCardRarity(): com.example.isekaikuroshin.ui.components.CardRarity {
    return when (this) {
        ItemRarity.COMMON -> com.example.isekaikuroshin.ui.components.CardRarity.COMMON
        ItemRarity.UNCOMMON -> com.example.isekaikuroshin.ui.components.CardRarity.UNCOMMON
        ItemRarity.RARE -> com.example.isekaikuroshin.ui.components.CardRarity.RARE
        ItemRarity.EPIC -> com.example.isekaikuroshin.ui.components.CardRarity.EPIC
        ItemRarity.LEGENDARY -> com.example.isekaikuroshin.ui.components.CardRarity.LEGENDARY
        ItemRarity.MYTHIC -> com.example.isekaikuroshin.ui.components.CardRarity.MYTHIC
        ItemRarity.ARTIFACT -> com.example.isekaikuroshin.ui.components.CardRarity.ARTIFACT
    }
}
