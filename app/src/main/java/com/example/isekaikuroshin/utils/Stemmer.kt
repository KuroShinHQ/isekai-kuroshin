package com.example.isekaikuroshin.utils

/**
 * Çok Dilli Kök Bulma (Stemming) Sınıfı
 *
 * Bu sınıf, Türkçe ve İngilizce kelimeler için basit kök bulma algoritmaları
 * uygular. BM25 algoritmasının etkinliğini artırmak için kelimelerin farklı
 * çekim formlarını (kale, kaleye, kaledeki) aynı köke (kale) indirger.
 *
 * Desteklenen Diller:
 * - TR: Türkçe çekim ve yapım ekleri (-ler, -lar, -daki, -den, vb.)
 * - EN: İngilizce Porter Stemmer kuralları (-ing, -ed, -ly, -s, vb.)
 *
 * Kullanım:
 * val stemmedWord = Stemmer.stem("kaledeki", "TR") // "kale"
 * val stemmedWord = Stemmer.stem("running", "EN")  // "run"
 */
object Stemmer {

    /**
     * Ana stemming fonksiyonu - dile göre uygun stemmer'ı çağırır
     *
     * @param word Kökü bulunacak kelime
     * @param language Dil kodu ("TR" veya "EN")
     * @return Keliminin kökü
     */
    fun stem(word: String, language: String): String {
        if (word.length < 3) return word // Çok kısa kelimeler stemlenmiyor

        return when(language.uppercase()) {
            "TR" -> simpleTurkishStem(word)
            "EN" -> simpleEnglishStem(word)
            else -> word // Desteklenmeyen diller için değişiklik yapma
        }
    }

    /**
     * Türkçe için basit kök bulma algoritması
     * Yaygın çekim ve yapım eklerini uzundan kısaya doğru çıkarır
     */
    private fun simpleTurkishStem(word: String): String {
        val lowercase = word.lowercase()

        // En uzun eklerden başlayarak kontrol et
        val suffixes = listOf(
            // 5+ karakter ekler
            "lerinden", "larından", "lerinde", "larında", "leriniz", "larınız",
            "lerinin", "larının", "lerince", "larınca", "lerine", "larına",
            "lerden", "lardan", "lerde", "larda", "lerin", "ların",

            // 4 karakter ekler
            "deki", "daki", "deki", "teki", "taki", "inde", "ında", "ünde", "unde",
            "erde", "arda", "den", "dan", "ten", "tan", "nin", "nın", "nun", "nün",
            "ler", "lar", "dir", "dır", "dur", "dür", "tir", "tır", "tur", "tür",

            // 3 karakter ekler
            "ler", "lar", "den", "dan", "ten", "tan", "nin", "nın", "nun", "nün",
            "dir", "dır", "dur", "dür", "tir", "tır", "tur", "tür",
            "ken", "ken", "yor", "lik", "lık", "luk", "lük",

            // 2 karakter ekler
            "in", "ın", "un", "ün", "de", "da", "te", "ta", "le", "la",
            "ye", "ya", "ne", "na", "re", "ra", "se", "sa", "me", "ma",
            "im", "ım", "um", "üm", "en", "an", "et", "at"
        )

        for (suffix in suffixes) {
            if (lowercase.endsWith(suffix) && lowercase.length > suffix.length + 2) {
                val stemmed = lowercase.dropLast(suffix.length)
                GameLogger.logSystem("Stemmer: Turkish '$word' -> '$stemmed' (removed: '$suffix')")
                return stemmed
            }
        }

        GameLogger.logSystem("Stemmer: Turkish '$word' -> '$word' (no suffix removed)")
        return lowercase
    }

    /**
     * İngilizce için basit Porter Stemmer algoritması
     * Yaygın İngilizce eklerini belirli kurallara göre çıkarır
     */
    private fun simpleEnglishStem(word: String): String {
        val lowercase = word.lowercase()

        // Step 1: Plural ve geçmiş zaman ekleri
        var result = lowercase

        // -ies -> -y (studies -> study)
        if (result.endsWith("ies") && result.length > 4) {
            result = result.dropLast(3) + "y"
            GameLogger.logSystem("Stemmer: English '$word' -> '$result' (ies->y)")
            return result
        }

        // -ied -> -y (tried -> try)
        if (result.endsWith("ied") && result.length > 4) {
            result = result.dropLast(3) + "y"
            GameLogger.logSystem("Stemmer: English '$word' -> '$result' (ied->y)")
            return result
        }

        // -ing (running -> run, walking -> walk)
        if (result.endsWith("ing") && result.length > 6) {
            val stem = result.dropLast(3)
            // Eğer stem çift konsonantla bitiyorsa, son harfi kaldır (running -> run)
            if (stem.length >= 2 &&
                stem.last() == stem[stem.length - 2] &&
                stem.last() !in "aeiou") {
                result = stem.dropLast(1)
            } else {
                result = stem
            }
            GameLogger.logSystem("Stemmer: English '$word' -> '$result' (removed: 'ing')")
            return result
        }

        // -ed (walked -> walk, tried -> try)
        if (result.endsWith("ed") && result.length > 5) {
            val stem = result.dropLast(2)
            // Eğer stem çift konsonantla bitiyorsa, son harfi kaldır (stopped -> stop)
            if (stem.length >= 2 &&
                stem.last() == stem[stem.length - 2] &&
                stem.last() !in "aeiou") {
                result = stem.dropLast(1)
            } else {
                result = stem
            }
            GameLogger.logSystem("Stemmer: English '$word' -> '$result' (removed: 'ed')")
            return result
        }

        // -ly (quickly -> quick, happily -> happy)
        if (result.endsWith("ly") && result.length > 5) {
            result = result.dropLast(2)
            GameLogger.logSystem("Stemmer: English '$word' -> '$result' (removed: 'ly')")
            return result
        }

        // -tion -> -t (creation -> creat)
        if (result.endsWith("tion") && result.length > 6) {
            result = result.dropLast(3)
            GameLogger.logSystem("Stemmer: English '$word' -> '$result' (tion->t)")
            return result
        }

        // -sion -> -s (conversion -> convers)
        if (result.endsWith("sion") && result.length > 6) {
            result = result.dropLast(3)
            GameLogger.logSystem("Stemmer: English '$word' -> '$result' (sion->s)")
            return result
        }

        // -s (cats -> cat, dogs -> dog) - but not -ss
        if (result.endsWith("s") && !result.endsWith("ss") && result.length > 3) {
            result = result.dropLast(1)
            GameLogger.logSystem("Stemmer: English '$word' -> '$result' (removed: 's')")
            return result
        }

        GameLogger.logSystem("Stemmer: English '$word' -> '$word' (no suffix removed)")
        return result
    }

    /**
     * Test amaçlı stemming fonksiyonu - debug bilgisi olmadan
     */
    fun stemQuiet(word: String, language: String): String {
        if (word.length < 3) return word

        return when(language.uppercase()) {
            "TR" -> simpleTurkishStemQuiet(word)
            "EN" -> simpleEnglishStemQuiet(word)
            else -> word
        }
    }

    private fun simpleTurkishStemQuiet(word: String): String {
        val lowercase = word.lowercase()

        val suffixes = listOf(
            "lerinden", "larından", "lerinde", "larında", "leriniz", "larınız",
            "lerinin", "larının", "lerince", "larınca", "lerine", "larına",
            "lerden", "lardan", "lerde", "larda", "lerin", "ların",
            "deki", "daki", "teki", "taki", "inde", "ında", "ünde", "unde",
            "erde", "arda", "den", "dan", "ten", "tan", "nin", "nın", "nun", "nün",
            "ler", "lar", "dir", "dır", "dur", "dür", "tir", "tır", "tur", "tür",
            "ken", "yor", "lik", "lık", "luk", "lük",
            "in", "ın", "un", "ün", "de", "da", "te", "ta", "le", "la",
            "ye", "ya", "ne", "na", "re", "ra", "se", "sa", "me", "ma",
            "im", "ım", "um", "üm", "en", "an", "et", "at"
        )

        for (suffix in suffixes) {
            if (lowercase.endsWith(suffix) && lowercase.length > suffix.length + 2) {
                return lowercase.dropLast(suffix.length)
            }
        }
        return lowercase
    }

    private fun simpleEnglishStemQuiet(word: String): String {
        var result = word.lowercase()

        if (result.endsWith("ies") && result.length > 4) {
            return result.dropLast(3) + "y"
        }
        if (result.endsWith("ied") && result.length > 4) {
            return result.dropLast(3) + "y"
        }
        if (result.endsWith("ing") && result.length > 6) {
            val stem = result.dropLast(3)
            return if (stem.length >= 2 && stem.last() == stem[stem.length - 2] && stem.last() !in "aeiou") {
                stem.dropLast(1)
            } else stem
        }
        if (result.endsWith("ed") && result.length > 5) {
            val stem = result.dropLast(2)
            return if (stem.length >= 2 && stem.last() == stem[stem.length - 2] && stem.last() !in "aeiou") {
                stem.dropLast(1)
            } else stem
        }
        if (result.endsWith("ly") && result.length > 5) {
            return result.dropLast(2)
        }
        if (result.endsWith("tion") && result.length > 6) {
            return result.dropLast(3)
        }
        if (result.endsWith("sion") && result.length > 6) {
            return result.dropLast(3)
        }
        if (result.endsWith("s") && !result.endsWith("ss") && result.length > 3) {
            return result.dropLast(1)
        }

        return result
    }
}