package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.utils.GameLogger
import java.util.regex.Pattern

/**
 * TODO-HUB-01: Hibrit NLU Motoru - Anahtar Kelime Tabanlı Hızlı Ayrıştırıcı
 *
 * Health & Mind Hub için basit komutları tanıyan anahtar kelime ayrıştırıcısı.
 * "kilo: 85", "alarm: 07:30" gibi basit komutları yüksek güven skoruyla hızlıca çözer.
 */

/**
 * Komut ayrıştırma sonucu
 * @param commandType Tanınan komut türü (SET_ALARM, LOG_WEIGHT vb.)
 * @param parameters Komutun parametreleri (Map<String, String>)
 * @param confidence Güven skoru (0.0 - 1.0 arası)
 */
data class CommandResult(
    val commandType: String,
    val parameters: Map<String, String>,
    val confidence: Double
)

class HealthMindCommandParser {

    companion object {
        private const val TAG = "HealthMindCommandParser"

        // Komut anahtar kelimeleri ve karşılık gelen komut türleri
        private val COMMAND_KEYWORDS = mapOf(
            // Kilo/Ağırlık komutları
            "kilo" to "LOG_WEIGHT",
            "ağırlık" to "LOG_WEIGHT",
            "weight" to "LOG_WEIGHT",
            "kilogram" to "LOG_WEIGHT",

            // Alarm komutları
            "alarm" to "SET_ALARM",
            "alarmı" to "SET_ALARM",
            "alarmi" to "SET_ALARM",
            "uyandır" to "SET_ALARM",
            "wake" to "SET_ALARM",

            // Kan basıncı komutları
            "tansiyon" to "LOG_BLOOD_PRESSURE",
            "kan basıncı" to "LOG_BLOOD_PRESSURE",
            "blood pressure" to "LOG_BLOOD_PRESSURE",
            "bp" to "LOG_BLOOD_PRESSURE",

            // Kalp atışı komutları
            "nabız" to "LOG_HEART_RATE",
            "kalp" to "LOG_HEART_RATE",
            "heart rate" to "LOG_HEART_RATE",
            "pulse" to "LOG_HEART_RATE",

            // Uyku komutları
            "uyku" to "LOG_SLEEP",
            "sleep" to "LOG_SLEEP",
            "uyudum" to "LOG_SLEEP",
            "slept" to "LOG_SLEEP",

            // Yemek komutları
            "yemek" to "LOG_FOOD",
            "yedim" to "LOG_FOOD",
            "food" to "LOG_FOOD",
            "ate" to "LOG_FOOD",
            "kahvaltı" to "LOG_FOOD",
            "öğle" to "LOG_FOOD",
            "akşam" to "LOG_FOOD",
            "öğün" to "LOG_FOOD",
            "beslenme" to "LOG_FOOD",
            "nutrition" to "LOG_FOOD",
            "içtim" to "LOG_FOOD",
            "drank" to "LOG_FOOD",
            "meal" to "LOG_FOOD",

            // Egzersiz komutları
            "egzersiz" to "LOG_EXERCISE",
            "spor" to "LOG_EXERCISE",
            "exercise" to "LOG_EXERCISE",
            "workout" to "LOG_EXERCISE",
            "koştum" to "LOG_EXERCISE",
            "yürüdüm" to "LOG_EXERCISE",
            "antrenman" to "LOG_EXERCISE",
            "koşu" to "LOG_EXERCISE",
            "yürüyüş" to "LOG_EXERCISE",
            "running" to "LOG_EXERCISE",
            "walking" to "LOG_EXERCISE",

            // Su komutları
            "su" to "LOG_WATER",
            "içtim" to "LOG_WATER",
            "water" to "LOG_WATER",
            "drank" to "LOG_WATER",

            // İlaç komutları
            "ilaç" to "LOG_MEDICATION",
            "medicine" to "LOG_MEDICATION",
            "medication" to "LOG_MEDICATION",
            "aldım" to "LOG_MEDICATION",
            "took" to "LOG_MEDICATION"
        )

        // Zaman pattern'leri
        private val TIME_PATTERNS = listOf(
            Pattern.compile("(\\d{1,2}):(\\d{2})"), // 07:30, 14:15
            Pattern.compile("(\\d{1,2})\\.(\\d{2})"), // 07.30, 14.15
            Pattern.compile("(\\d{1,2})\\s*(saat|o'clock)") // 7 saat, 7 o'clock
        )

        // Sayı pattern'leri
        private val NUMBER_PATTERNS = listOf(
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(kg|kilogram|kilo)"), // 85 kg, 75.5 kg
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(gr|gram)"), // 500 gr
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(lt|litre|liter)"), // 2.5 lt
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(ml)"), // 250 ml
            Pattern.compile("(\\d+)\\s*(bpm|atım)"), // 75 bpm, 80 atım
            Pattern.compile("(\\d+)/(\\d+)"), // 120/80 (tansiyon)
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(saat|hour|dakika|minute)"), // 8 saat, 30 dakika
            Pattern.compile("(\\d+(?:\\.\\d+)?)") // Sadece sayı
        )
    }

    /**
     * Ana ayrıştırma fonksiyonu
     * @param input Kullanıcı girdisi
     * @return CommandResult veya null (tanınmadıysa)
     */
    fun parseCommand(input: String): CommandResult? {
        try {
            GameLogger.logSystem("=== HEALTH MIND COMMAND PARSER ===")
            GameLogger.logSystem("Input: '$input'")

            val normalizedInput = input.lowercase().trim()

            // 1. Anahtar kelime tespiti
            val detectedCommand = detectCommandType(normalizedInput)
            if (detectedCommand == null) {
                GameLogger.logSystem("No command keyword detected")
                return null
            }

            GameLogger.logSystem("Detected command type: $detectedCommand")

            // 2. Parametreleri çıkar
            val parameters = extractParameters(normalizedInput, detectedCommand)

            // 3. Güven skorunu hesapla
            val confidence = calculateConfidence(normalizedInput, detectedCommand, parameters)

            val result = CommandResult(
                commandType = detectedCommand,
                parameters = parameters,
                confidence = confidence
            )

            GameLogger.logSystem("=== PARSER RESULT ===")
            GameLogger.logSystem("Command: $detectedCommand")
            GameLogger.logSystem("Parameters: $parameters")
            GameLogger.logSystem("Confidence: $confidence")

            return result

        } catch (e: Exception) {
            GameLogger.logError(TAG, "Error parsing command: $input", e)
            return null
        }
    }

    /**
     * Girdi metninden komut türünü tespit eder
     */
    private fun detectCommandType(input: String): String? {
        // En uzun eşleşmeyi bul (öncelik sırası)
        var bestMatch: String? = null
        var longestKeyword = ""

        COMMAND_KEYWORDS.forEach { (keyword, commandType) ->
            if (input.contains(keyword) && keyword.length > longestKeyword.length) {
                bestMatch = commandType
                longestKeyword = keyword
            }
        }

        return bestMatch
    }

    /**
     * Komut türüne göre parametreleri çıkarır
     */
    private fun extractParameters(input: String, commandType: String): Map<String, String> {
        val parameters = mutableMapOf<String, String>()

        when (commandType) {
            "SET_ALARM" -> {
                extractTimeParameter(input)?.let { time ->
                    parameters["time"] = time
                }
            }

            "LOG_WEIGHT" -> {
                extractWeightParameter(input)?.let { weight ->
                    parameters["weight"] = weight
                    parameters["unit"] = "kg"
                }
            }

            "LOG_BLOOD_PRESSURE" -> {
                extractBloodPressureParameter(input)?.let { bp ->
                    val parts = bp.split("/")
                    if (parts.size == 2) {
                        parameters["systolic"] = parts[0]
                        parameters["diastolic"] = parts[1]
                    }
                }
            }

            "LOG_HEART_RATE" -> {
                extractHeartRateParameter(input)?.let { hr ->
                    parameters["bpm"] = hr
                }
            }

            "LOG_SLEEP" -> {
                extractSleepParameter(input)?.let { sleep ->
                    parameters["duration"] = sleep
                    parameters["unit"] = "hours"
                }
            }

            "LOG_WATER" -> {
                extractVolumeParameter(input)?.let { volume ->
                    parameters["amount"] = volume
                    parameters["unit"] = "ml"
                }
            }

            "LOG_EXERCISE" -> {
                // Süre parametresi çıkar
                extractExerciseDuration(input)?.let { duration ->
                    parameters["duration"] = duration
                    parameters["durationUnit"] = "minutes"
                }

                // Aktivite tipi çıkar
                extractExerciseType(input)?.let { type ->
                    parameters["exerciseType"] = type
                }

                // Genel açıklama
                parameters["description"] = input
            }

            "LOG_FOOD" -> {
                // Öğün türünü çıkar
                extractMealType(input)?.let { mealType ->
                    parameters["mealType"] = mealType
                }

                // Yemek adını çıkar
                extractMealName(input)?.let { mealName ->
                    parameters["mealName"] = mealName
                }

                // Genel açıklama
                parameters["description"] = input
            }

            "LOG_MEDICATION" -> {
                // Bu komutlar için metin içeriğini al
                parameters["description"] = input
            }
        }

        return parameters
    }

    /**
     * Zaman parametresi çıkarır (07:30, 14.15 vb.)
     */
    private fun extractTimeParameter(input: String): String? {
        TIME_PATTERNS.forEach { pattern ->
            val matcher = pattern.matcher(input)
            if (matcher.find()) {
                return when (pattern.pattern()) {
                    "(\\d{1,2}):(\\d{2})" -> "${matcher.group(1)}:${matcher.group(2)}"
                    "(\\d{1,2})\\.(\\d{2})" -> "${matcher.group(1)}:${matcher.group(2)}"
                    "(\\d{1,2})\\s*(saat|o'clock)" -> "${matcher.group(1)}:00"
                    else -> matcher.group(1)
                }
            }
        }
        return null
    }

    /**
     * Ağırlık parametresi çıkarır (85 kg, 75.5 kilo vb.)
     */
    private fun extractWeightParameter(input: String): String? {
        val pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(kg|kilogram|kilo)?")
        val matcher = pattern.matcher(input)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    /**
     * Kan basıncı parametresi çıkarır (120/80 vb.)
     */
    private fun extractBloodPressureParameter(input: String): String? {
        val pattern = Pattern.compile("(\\d+)/(\\d+)")
        val matcher = pattern.matcher(input)
        if (matcher.find()) {
            return "${matcher.group(1)}/${matcher.group(2)}"
        }
        return null
    }

    /**
     * Kalp atışı parametresi çıkarır (75 bpm vb.)
     */
    private fun extractHeartRateParameter(input: String): String? {
        val pattern = Pattern.compile("(\\d+)\\s*(bpm|atım)?")
        val matcher = pattern.matcher(input)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    /**
     * Uyku süresi parametresi çıkarır (8 saat vb.)
     */
    private fun extractSleepParameter(input: String): String? {
        val pattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(saat|hour)")
        val matcher = pattern.matcher(input)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    /**
     * Hacim parametresi çıkarır (250 ml, 2.5 lt vb.)
     */
    private fun extractVolumeParameter(input: String): String? {
        val mlPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(ml)")
        val mlMatcher = mlPattern.matcher(input)
        if (mlMatcher.find()) {
            return mlMatcher.group(1)
        }

        val ltPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(lt|litre|liter)")
        val ltMatcher = ltPattern.matcher(input)
        if (ltMatcher.find()) {
            val liters = ltMatcher.group(1)?.toDoubleOrNull() ?: return null
            return (liters * 1000).toInt().toString() // Convert to ml
        }

        return null
    }

    /**
     * Egzersiz süresi parametresi çıkarır (30 dakika, 1 saat vb.)
     */
    private fun extractExerciseDuration(input: String): String? {
        // Dakika pattern'leri
        val minutePattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(dakika|minute|dk|min)")
        val minuteMatcher = minutePattern.matcher(input)
        if (minuteMatcher.find()) {
            return minuteMatcher.group(1)
        }

        // Saat pattern'leri - dakikaya çevir
        val hourPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(saat|hour|sa)")
        val hourMatcher = hourPattern.matcher(input)
        if (hourMatcher.find()) {
            val hours = hourMatcher.group(1)?.toDoubleOrNull() ?: return null
            return (hours * 60).toInt().toString()
        }

        // Sadece sayı varsa dakika olarak kabul et
        val numberPattern = Pattern.compile("(\\d+)\\s*(?:dakika|minute|dk|min)?")
        val numberMatcher = numberPattern.matcher(input)
        if (numberMatcher.find()) {
            // Eğer aktivite tipi varsa ve süre belirtilmemişse varsayılan 30 dakika
            return numberMatcher.group(1)
        }

        return null
    }

    /**
     * Egzersiz tipi parametresi çıkarır (koşu, yürüyüş vb.)
     */
    private fun extractExerciseType(input: String): String? {
        val exerciseTypes = mapOf(
            "koşu" to "RUNNING",
            "koştum" to "RUNNING",
            "running" to "RUNNING",
            "run" to "RUNNING",
            "yürüyüş" to "WALKING",
            "yürüdüm" to "WALKING",
            "walking" to "WALKING",
            "walk" to "WALKING",
            "bisiklet" to "CYCLING",
            "cycling" to "CYCLING",
            "bike" to "CYCLING",
            "yüzme" to "SWIMMING",
            "swimming" to "SWIMMING",
            "fitness" to "FITNESS",
            "gym" to "FITNESS",
            "spor salonu" to "FITNESS",
            "antrenman" to "FITNESS",
            "workout" to "FITNESS",
            "yoga" to "YOGA",
            "pilates" to "PILATES",
            "futbol" to "FOOTBALL",
            "basketbol" to "BASKETBALL",
            "tenis" to "TENNIS"
        )

        // En uzun eşleşmeyi bul
        var bestMatch: String? = null
        var longestKeyword = ""

        exerciseTypes.forEach { (keyword, type) ->
            if (input.contains(keyword) && keyword.length > longestKeyword.length) {
                bestMatch = type
                longestKeyword = keyword
            }
        }

        return bestMatch ?: "OTHER" // Varsayılan
    }

    /**
     * Öğün türünü çıkarır (kahvaltı, öğle, akşam vb.)
     */
    private fun extractMealType(input: String): String? {
        val mealTypes = mapOf(
            "kahvaltı" to "BREAKFAST",
            "kahvaltıda" to "BREAKFAST",
            "breakfast" to "BREAKFAST",
            "sabah" to "BREAKFAST",

            "öğle" to "LUNCH",
            "öğlen" to "LUNCH",
            "öğle yemeği" to "LUNCH",
            "lunch" to "LUNCH",
            "öğle vakti" to "LUNCH",

            "akşam" to "DINNER",
            "akşam yemeği" to "DINNER",
            "dinner" to "DINNER",
            "akşam vakti" to "DINNER",

            "atıştırmalık" to "SNACK",
            "snack" to "SNACK",
            "aperatif" to "SNACK",
            "ara öğün" to "SNACK"
        )

        // En uzun eşleşmeyi bul
        var bestMatch: String? = null
        var longestKeyword = ""

        mealTypes.forEach { (keyword, type) ->
            if (input.contains(keyword) && keyword.length > longestKeyword.length) {
                bestMatch = type
                longestKeyword = keyword
            }
        }

        return bestMatch
    }

    /**
     * Yemek adını çıkarır (basit text parsing)
     */
    private fun extractMealName(input: String): String? {
        // Öğün türü kelimelerini temizle
        val mealKeywords = listOf("kahvaltı", "kahvaltıda", "öğle", "öğlen", "akşam", "yedim", "içtim", "yemek", "food", "ate", "drank")
        val connectors = listOf("da", "de", "ta", "te", "da", "de", "yedim", "içtim", "ile", "with", "and", "ve")

        var cleaned = input.lowercase().trim()

        // Öğün türü kelimelerini kaldır
        mealKeywords.forEach { keyword ->
            cleaned = cleaned.replace(keyword, "").trim()
        }

        // Bağlaçları kaldır
        connectors.forEach { connector ->
            cleaned = cleaned.replace(connector, "").trim()
        }

        // Sayıları temizle (2 yumurta -> yumurta)
        cleaned = cleaned.replace(Regex("\\d+\\s*"), "").trim()

        // Boş değilse ve anlamlı uzunluktaysa döndür
        return if (cleaned.isNotEmpty() && cleaned.length > 2) {
            cleaned.split(" ").joinToString(" ") { it.trim() }.trim()
        } else null
    }

    /**
     * Güven skorunu hesaplar
     */
    private fun calculateConfidence(input: String, commandType: String, parameters: Map<String, String>): Double {
        var confidence = 0.5 // Base confidence

        // Anahtar kelime eşleşmesi bonus
        val matchingKeywords = COMMAND_KEYWORDS.filter { (keyword, type) ->
            type == commandType && input.contains(keyword)
        }
        confidence += matchingKeywords.size * 0.2

        // Parametre bulma bonus
        confidence += parameters.size * 0.1

        // Özel durum bonusları
        when (commandType) {
            "SET_ALARM" -> {
                if (parameters.containsKey("time")) confidence += 0.3
            }
            "LOG_WEIGHT" -> {
                if (parameters.containsKey("weight")) confidence += 0.3
            }
            "LOG_BLOOD_PRESSURE" -> {
                if (parameters.containsKey("systolic") && parameters.containsKey("diastolic")) {
                    confidence += 0.4
                }
            }
        }

        // Belirsizlik cezaları
        if (input.length > 50) confidence -= 0.1 // Çok uzun girdiler belirsizdir
        if (input.split(" ").size > 10) confidence -= 0.1 // Çok kelimeli girdiler belirsizdir

        return minOf(1.0, maxOf(0.0, confidence))
    }
}