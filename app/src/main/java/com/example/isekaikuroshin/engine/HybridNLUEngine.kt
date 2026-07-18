package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.utils.GameLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TODO-HUB-01: Hibrit NLU Motoru - Ana Koordinatör
 *
 * Health & Mind Hub için hibrit doğal dil anlama motoru.
 * İki seviyeli yaklaşım:
 * 1. Hızlı Yol: HealthMindCommandParser ile basit komutları anlık çözer
 * 2. Akıllı Yol: Karmaşık komutlar için GameMasterEngine'i niyet tanıma motoru olarak kullanır
 */
@Singleton
class HybridNLUEngine @Inject constructor(
    private val gameMasterEngine: GameMasterEngine
) {
    private val healthMindCommandParser = HealthMindCommandParser()
    companion object {
        private const val TAG = "HybridNLUEngine"
        private const val CONFIDENCE_THRESHOLD = 0.8 // Hızlı yol için minimum güven skoru
    }

    /**
     * Ana komut ayrıştırma fonksiyonu
     * Hibrit yaklaşım: Önce hızlı ayrıştırıcı, sonra AI destekli analiz
     *
     * @param input Kullanıcının doğal dil girdisi
     * @return CommandResult - Ayrıştırılan komut ve parametreler
     */
    suspend fun parseCommand(input: String): CommandResult {
        try {
            GameLogger.logSystem("=== HYBRID NLU ENGINE: Starting command analysis ===")
            GameLogger.logSystem("Input: '$input'")

            // ADIM 1: Hızlı Yol - HealthMindCommandParser ile basit komut tespiti
            GameLogger.logSystem("=== PHASE 1: Fast Path Analysis ===")
            val quickResult = healthMindCommandParser.parseCommand(input)

            if (quickResult != null) {
                GameLogger.logSystem("Quick parser result: ${quickResult.commandType}, confidence: ${quickResult.confidence}")

                // Güven skoru yeterince yüksekse hızlı sonucu döndür
                if (quickResult.confidence >= CONFIDENCE_THRESHOLD) {
                    GameLogger.logSystem("=== FAST PATH SUCCESS ===")
                    GameLogger.logSystem("High confidence (${quickResult.confidence}), using quick parser result")
                    GameLogger.logSystem("Final command: ${quickResult.commandType}")
                    GameLogger.logSystem("Parameters: ${quickResult.parameters}")
                    return quickResult
                } else {
                    GameLogger.logSystem("=== FAST PATH UNCERTAIN ===")
                    GameLogger.logSystem("Low confidence (${quickResult.confidence}), proceeding to AI analysis")
                }
            } else {
                GameLogger.logSystem("=== FAST PATH FAILED ===")
                GameLogger.logSystem("Quick parser found no matching pattern, proceeding to AI analysis")
            }

            // ADIM 2: Akıllı Yol - GameMasterEngine ile niyet tanıma
            GameLogger.logSystem("=== PHASE 2: AI-Powered Intent Recognition ===")
            val aiResult = analyzeWithGameMaster(input)

            if (aiResult != null) {
                GameLogger.logSystem("=== AI PATH SUCCESS ===")
                GameLogger.logSystem("AI recognized command: ${aiResult.commandType}")
                GameLogger.logSystem("AI extracted parameters: ${aiResult.parameters}")

                // HATA #3 FIX: AI sonucu varsa ve güven skoru yeterince yüksekse bunu kullan
                // Quick result ile karşılaştırma yapma, AI'ın sonucunu tercih et
                if (aiResult.confidence >= 0.5) {
                    GameLogger.logSystem("✅ HATA #3 FIX: Using AI result (confidence: ${aiResult.confidence})")
                    return aiResult
                } else {
                    GameLogger.logSystem("⚠️ AI result has low confidence (${aiResult.confidence})")
                }
            }

            // ADIM 3: Fallback - Eğer AI de başarısız olursa veya güven düşükse, hızlı sonucu kullan
            if (quickResult != null) {
                GameLogger.logSystem("=== FALLBACK TO QUICK PARSER ===")
                GameLogger.logSystem("AI failed or low confidence, using quick result")
                GameLogger.logSystem("Quick result command: ${quickResult.commandType}")
                return quickResult
            }

            // ADIM 4: Son çare - Hiçbir şey bulunamadıysa UNKNOWN komutu
            GameLogger.logSystem("=== FINAL FALLBACK ===")
            GameLogger.logSystem("Both parsers failed, returning UNKNOWN command")
            return CommandResult(
                commandType = "UNKNOWN",
                parameters = mapOf("description" to input),
                confidence = 0.1
            )

        } catch (e: Exception) {
            GameLogger.logError(TAG, "Error in hybrid command parsing", e)
            // Hata durumunda güvenli fallback - UNKNOWN döndür
            return CommandResult(
                commandType = "UNKNOWN",
                parameters = mapOf("description" to input),
                confidence = 0.0
            )
        } finally {
            GameLogger.logSystem("=== HYBRID NLU ENGINE: Command analysis complete ===")
        }
    }

    /**
     * GameMasterEngine'i niyet tanıma motoru olarak kullanır
     * Özel komut tanıma prompt'u ile AI'dan analiz ister
     */
    private suspend fun analyzeWithGameMaster(input: String): CommandResult? {
        try {
            GameLogger.logSystem("=== AI INTENT RECOGNITION ===")
            GameLogger.logSystem("Building command recognition prompt for: '$input'")

            val commandRecognitionPrompt = buildCommandRecognitionPrompt(input)
            GameLogger.logSystem("=== AI PROMPT ===")
            GameLogger.logSystem(commandRecognitionPrompt)
            GameLogger.logSystem("=== END AI PROMPT ===")

            // GameMasterEngine'in processPromptDirectly fonksiyonunu kullan
            val gmResponse = gameMasterEngine.processPromptDirectly(commandRecognitionPrompt)

            if (gmResponse != null) {
                GameLogger.logSystem("=== AI RESPONSE RECEIVED ===")
                GameLogger.logSystem("AI Response: $gmResponse")

                // GMResponse'dan CommandResult'a dönüşüm
                return parseGMResponseToCommand(gmResponse, input)
            } else {
                GameLogger.logSystem("AI intent recognition returned null")
                return null
            }

        } catch (e: Exception) {
            GameLogger.logError(TAG, "Error in AI intent recognition", e)
            return null
        }
    }

    /**
     * Niyet tanıma için özel prompt oluşturur
     */
    private fun buildCommandRecognitionPrompt(input: String): String {
        return """
[KOMUT TANıMA GÖREVI]
Sen bir Health & Mind Hub için komut tanıma sistemisin. Kullanıcının doğal dil girdisini analiz edip hangi komutu kastettiğini belirlemen gerekiyor.

KULLANICI GIRDISi: "$input"

TANıYABILECEĞIN KOMUTLAR:
1. SET_ALARM - Alarm kurma (örn: "alarmı 07:30'a kur", "yarın sabah 6'da uyandır")
2. LOG_WEIGHT - Kilo kaydetme (örn: "kilo: 75 kg", "85 kilo oldum", "ağırlığım 70")
3. LOG_BLOOD_PRESSURE - Tansiyon kaydetme (örn: "tansiyon: 120/80", "kan basıncım 130/85")
4. LOG_HEART_RATE - Nabız kaydetme (örn: "nabzım 75", "kalp atışım 80", "pulse 72")
5. LOG_SLEEP - Uyku kaydetme (örn: "8 saat uyudum", "dün gece 7.5 saat uyku")
6. LOG_FOOD - Yemek kaydetme (örn: "kahvaltıda omlet yedim", "dün akşam pizza", "elma yedim")
7. LOG_EXERCISE - Egzersiz kaydetme (örn: "30 dakika koştum", "spor yaptım", "yürüyüş")
8. LOG_WATER - Su kaydetme (örn: "500 ml su içtim", "2 bardak su", "1 litre su")
9. LOG_MEDICATION - İlaç kaydetme (örn: "vitaminimi aldım", "ilaç içtim", "aspirin")

GÖREVIN: Yukarıdaki girdinin hangi komut türü olduğunu belirle ve ilgili parametreleri çıkar.

SADECE AŞAĞıDAKI JSON FORMATINDA CEVAP VER:

{
  "journalEntry": "Kullanıcı [komut türü açıklaması]",
  "commandRecognition": {
    "detectedCommand": "[KOMUT_TÜRÜ]",
    "parameters": {
      "param1": "değer1",
      "param2": "değer2"
    },
    "confidence": 0.9
  }
}

PARAMETRE ÖRNEKLERi:
- SET_ALARM: {"time": "07:30"}
- LOG_WEIGHT: {"weight": "75", "unit": "kg"}
- LOG_BLOOD_PRESSURE: {"systolic": "120", "diastolic": "80"}
- LOG_HEART_RATE: {"bpm": "75"}
- LOG_SLEEP: {"duration": "8", "unit": "hours"}
- LOG_WATER: {"amount": "500", "unit": "ml"}
- LOG_FOOD: {"description": "omlet"}
- LOG_EXERCISE: {"description": "30 dakika koşu", "duration": "30"}
- LOG_MEDICATION: {"description": "vitamin"}

ÖZEL KURALLAR:
1. Eğer girdi belirsizse, en yakın komut türünü seç ve confidence'ı düşük tut (0.3-0.5)
2. Parametreleri mümkün olduğunca çıkarmaya çalış
3. Confidence skoru: 0.9+ (çok net), 0.7-0.8 (net), 0.5-0.6 (orta), 0.3-0.4 (belirsiz)
4. journalEntry kısa olsun, sadece ne tespit ettiğini açıkla

ŞiMDi ANALIZ ET: "$input"
""".trimIndent()
    }

    /**
     * GMResponse'dan CommandResult'a dönüşüm yapar
     */
    private fun parseGMResponseToCommand(gmResponse: com.example.isekaikuroshin.data.GMResponse, originalInput: String): CommandResult? {
        try {
            GameLogger.logSystem("=== PARSING GM RESPONSE TO COMMAND ===")

            // GMResponse'da custom field olup olmadığını kontrol et
            // Eğer journalEntry'de "commandRecognition" var mı diye bak
            val journalText = gmResponse.journalEntry

            // Basit bir JSON parsing attempt
            if (journalText.contains("\"detectedCommand\"")) {
                // JSON içinde komut bilgisi var
                val commandPattern = "\"detectedCommand\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                val parametersPattern = "\"parameters\"\\s*:\\s*\\{([^}]+)\\}".toRegex()
                val confidencePattern = "\"confidence\"\\s*:\\s*([0-9.]+)".toRegex()

                val commandMatch = commandPattern.find(journalText)
                val parametersMatch = parametersPattern.find(journalText)
                val confidenceMatch = confidencePattern.find(journalText)

                if (commandMatch != null) {
                    val detectedCommand = commandMatch.groupValues[1]
                    val parameters = mutableMapOf<String, String>()
                    var confidence = 0.7

                    // Parametreleri parse et
                    if (parametersMatch != null) {
                        val paramString = parametersMatch.groupValues[1]
                        val paramPairs = paramString.split(",")
                        for (pair in paramPairs) {
                            val keyValue = pair.split(":")
                            if (keyValue.size == 2) {
                                val key = keyValue[0].trim().removeSurrounding("\"")
                                val value = keyValue[1].trim().removeSurrounding("\"")
                                parameters[key] = value
                            }
                        }
                    }

                    // Confidence parse et
                    if (confidenceMatch != null) {
                        confidence = confidenceMatch.groupValues[1].toDoubleOrNull() ?: 0.7
                    }

                    GameLogger.logSystem("=== AI COMMAND PARSED ===")
                    GameLogger.logSystem("Detected command: $detectedCommand")
                    GameLogger.logSystem("Parameters: $parameters")
                    GameLogger.logSystem("Confidence: $confidence")

                    return CommandResult(
                        commandType = detectedCommand,
                        parameters = parameters,
                        confidence = confidence
                    )
                }
            }

            // Fallback: GMResponse'dan varsayılan bir komut oluştur
            GameLogger.logSystem("Could not parse specific command from AI, using fallback")
            return CommandResult(
                commandType = "LOG_FOOD",
                parameters = mapOf("description" to originalInput),
                confidence = 0.4
            )

        } catch (e: Exception) {
            GameLogger.logError(TAG, "Error parsing GM response to command", e)
            return null
        }
    }

    /**
     * Test fonksiyonu - Belirli girdiler için sistem performansını test eder
     */
    suspend fun runTestSuite(): Map<String, CommandResult> {
        GameLogger.logSystem("=== HYBRID NLU ENGINE TEST SUITE ===")

        val testInputs = listOf(
            "kilo: 85 kg",
            "alarmı 07:30'a kur",
            "dün akşam yediğim pizzayı kaydet",
            "tansiyon: 120/80",
            "8 saat uyudum",
            "500 ml su içtim",
            "vitaminimi aldım",
            "30 dakika koştum"
        )

        val results = mutableMapOf<String, CommandResult>()

        for (input in testInputs) {
            GameLogger.logSystem("Testing input: '$input'")
            val result = parseCommand(input)
            results[input] = result
            GameLogger.logSystem("Result: ${result.commandType} (confidence: ${result.confidence})")
        }

        GameLogger.logSystem("=== TEST SUITE COMPLETE ===")
        return results
    }
}