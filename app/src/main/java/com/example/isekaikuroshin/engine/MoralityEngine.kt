package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.utils.GameLogger

object MoralityEngine {

    // G105.2: Expanded to 50+ Turkish keywords (synonyms and variations)
    private val iyilikKeywords = listOf(
        // Yardım & Destek
        "yardım ettim", "yardım et", "destek oldum", "destekledim", "katkıda bulundum",
        // Kurtarma & Koruma
        "kurtardım", "kurtardı", "korudum", "korudu", "savundum", "gözetim", "kolladım",
        // Bağış & Paylaşım
        "bağışladım", "bağış yaptım", "verdim", "paylaştım", "ikram ettim",
        // Teselli & Şefkat
        "teselli ettim", "avuttum", "şefkat gösterdim", "merhametli", "acıdım",
        // İyileştirme & İyilik
        "iyileştirdim", "tedavi ettim", "şifa verdim", "iyilik yaptım", "hayır işledim",
        // Affetme & Hoşgörü
        "affettim", "bağışladım", "hoşgörülü", "anlayışlı", "özür diledim",
        // Dürüstlük
        "dürüst davrandım", "doğruyu söyledim", "dürüstlük", "güven verdim",
        // Diğer pozitif
        "cesaretlendirdim", "ödüllendirdim", "teşvik ettim", "iyiyi seçtim",
        "barışı sağladım", "uzlaştırdım", "adil davrandım"
    )

    private val kotulukKeywords = listOf(
        // Hırsızlık & Aldatma
        "çaldım", "çalıntı", "yağmaladım", "gasp ettim", "dolandırdım", "kandırdım",
        // Tehdit & Şiddet
        "tehdit ettim", "gözdağı verdim", "zorladım", "baskı yaptım",
        "saldırdım", "saldırdı", "dövdüm", "yaraladım", "şiddet uyguladım",
        // Öldürme
        "öldürdüm", "öldürdü", "katlettim", "cinayet işledim", "infaz ettim",
        // Yalan & Hile
        "yalan söyledim", "yalancı", "hile yaptım", "aldattım", "ihanet ettim",
        // Zulüm & Acımasızlık
        "eziyet ettim", "işkence ettim", "zulmettim", "acımasızca", "zalimce",
        // Hakaret & İncinme
        "hakaret ettim", "aşağıladım", "küçük düşürdüm", "tahkir ettim",
        // Haksızlık
        "haksızlık yaptım", "adaletsiz", "haksız davrandım", "sömürdüm",
        // Diğer negatif
        "terk ettim", "felaket getirdim", "intikam aldım", "kini besledim",
        "manipüle ettim", "kötülük yaptım", "zarar verdim"
    )

    // G105.3: Context-aware keywords - targets that reduce/negate penalties
    private val neutralTargets = listOf(
        "canavar", "canavara", "canavarı", "yaratık", "yaratığı", "yaratığa",
        "düşman", "düşmanı", "düşmana", "haydut", "haydutu", "hayduda",
        "hırsız", "hırsızı", "hırsıza", "zombi", "iskelet", "goblin",
        "kurt", "kurdu", "kurda", "ejderha", "ejderhayı"
    )

    // G105.3: Context modifiers for good deeds
    private val npcTargets = listOf(
        "köylü", "köylüye", "köylüyü", "çocuk", "çocuğa", "çocuğu",
        "yaşlı", "yaşlıya", "yaşlıyı", "kadın", "kadına", "kadını",
        "adam", "adama", "adamı", "genç", "gence", "genci",
        "insan", "insana", "insanı", "tüccar", "tüccara", "tüccarı"
    )

    fun analyzeAndGetScore(entry: String): Float {
        val lowercasedEntry = entry.lowercase()
        var score = 0.0f
        val matchedKeywords = mutableListOf<String>()

        // G105.3: Check if entry contains neutral/enemy targets
        val hasNeutralTarget = neutralTargets.any { lowercasedEntry.contains(it) }
        val hasNpcTarget = npcTargets.any { lowercasedEntry.contains(it) }

        iyilikKeywords.forEach { keyword ->
            if (lowercasedEntry.contains(keyword)) {
                // G105.3: Bonus for helping NPCs
                val bonus = if (hasNpcTarget) 0.08f else 0.05f
                score += bonus
                matchedKeywords.add("+$keyword${if (hasNpcTarget) " (NPC bonus)" else ""}")
            }
        }

        kotulukKeywords.forEach { keyword ->
            if (lowercasedEntry.contains(keyword)) {
                // G105.3: Reduced penalty if target is enemy/monster
                val penalty = if (hasNeutralTarget) 0.02f else 0.05f
                score -= penalty
                matchedKeywords.add("-$keyword${if (hasNeutralTarget) " (neutral target)" else ""}")
            }
        }

        // G105.4: Log to verify function calls and keyword matches
        if (score != 0.0f) {
            GameLogger.logSystem("MoralityEngine: Score=$score, Matched=${matchedKeywords.joinToString(", ")}")
        } else {
            GameLogger.logSystem("MoralityEngine: No keywords matched (score=0.0f)")
        }

        return score
    }
}
