package com.example.isekaikuroshin.engine.mind

import android.util.Log
import com.example.isekaikuroshin.engine.GameMasterEngine
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TODO-HUB-12: Dil Öğrenim Motoru (Faz 3 - Prototip)
 *
 * SYS-20 raporunda tasarlanan LanguageLearningEngine'in ilk prototip implementasyonu.
 * Bu motor, GameMasterEngine'i bir "dil eğitmeni" rolünde kullanarak
 * sohbet tabanlı dil öğrenim oturumları başlatır.
 *
 * Yetenekler:
 * - AI tabanlı dil eğitmeni rolü
 * - Seviye bazlı öğretim (A1, A2, B1, B2, C1, C2)
 * - Yapılandırılmış JSON yanıtları
 * - Kelime hazinesi ve dil bilgisi noktaları
 */
@Singleton
class LanguageLearningEngine @Inject constructor(
    private val gameMasterEngine: GameMasterEngine
) {

    companion object {
        private const val TAG = "LanguageLearningEngine"
    }

    /**
     * TODO-HUB-12: Dil öğrenim oturumu başlatır
     *
     * @param language Öğrenilecek dil (örn: "İngilizce", "Japanese")
     * @param level CEFR seviyesi (A1, A2, B1, B2, C1, C2)
     * @return Başarılı sonuç veya hata
     */
    suspend fun startLanguageSession(
        language: String,
        level: String
    ): Result<LanguageSessionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                GameLogger.logSystem("TODO-HUB-12: Dil öğrenim oturumu başlatılıyor...")
                GameLogger.logSystem("TODO-HUB-12: Dil: $language, Seviye: $level")

                // 1. Dil eğitmeni prompt'unu oluştur
                val tutorPrompt = buildLanguageTutorPrompt(language, level)

                GameLogger.logSystem("TODO-HUB-12: Dil eğitmeni prompt'u oluşturuldu")
                GameLogger.logSystem("=== DİL EĞİTMENİ PROMPT BAŞLANGIÇ ===")
                GameLogger.logSystem(tutorPrompt)
                GameLogger.logSystem("=== DİL EĞİTMENİ PROMPT BİTİŞ ===")

                // 2. GameMasterEngine üzerinden AI'a gönder
                val gmResponse = gameMasterEngine.processPromptDirectly(tutorPrompt)

                if (gmResponse == null) {
                    GameLogger.logError(TAG, "TODO-HUB-12: GameMasterEngine null yanıt döndürdü")
                    return@withContext Result.failure(Exception("Dil eğitmeni yanıt vermedi"))
                }

                GameLogger.logSystem("TODO-HUB-12: GameMasterEngine yanıtı alındı")
                GameLogger.logSystem("TODO-HUB-12: GM Response: $gmResponse")

                // 3. GMResponse'dan LanguageSessionResponse'a dönüştür
                val sessionResponse = parseLanguageSessionResponse(gmResponse.journalEntry)

                if (sessionResponse == null) {
                    GameLogger.logError(TAG, "TODO-HUB-12: Dil oturumu yanıtı parse edilemedi")
                    return@withContext Result.failure(Exception("Yanıt formatı hatalı"))
                }

                GameLogger.logSystem("TODO-HUB-12: ✅ Dil oturumu başarıyla başlatıldı")
                GameLogger.logSystem("TODO-HUB-12: Session Response: $sessionResponse")

                Result.success(sessionResponse)

            } catch (e: Exception) {
                GameLogger.logError(TAG, "TODO-HUB-12: Dil oturumu başlatma hatası", e)
                Log.e(TAG, "Language session error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * TODO-HUB-13: Mevcut oturumu devam ettirir
     *
     * Kullanıcının cevabını AI'a gönderir ve yeni bir eğitmen yanıtı alır
     *
     * @param chatHistory Tüm konuşma geçmişi (formatlanmış string)
     * @param userReply Kullanıcının son cevabı
     * @param language Öğrenilen dil
     * @param level CEFR seviyesi
     * @return AI'ın yeni yanıtı
     */
    suspend fun continueSession(
        chatHistory: String,
        userReply: String,
        language: String = "İngilizce",
        level: String = "A2"
    ): Result<Pair<String, String?>> {
        return withContext(Dispatchers.IO) {
            try {
                GameLogger.logSystem("TODO-HUB-13: Dil oturumu devam ettiriliyor...")
                GameLogger.logSystem("TODO-HUB-13: Kullanıcı cevabı: $userReply")

                // 1. Devam prompt'unu oluştur
                val continuePrompt = buildContinuePrompt(chatHistory, userReply, language, level)

                GameLogger.logSystem("TODO-HUB-13: Devam prompt'u oluşturuldu")
                GameLogger.logSystem("=== DEV AM PROMPT BAŞLANGIÇ ===")
                GameLogger.logSystem(continuePrompt)
                GameLogger.logSystem("=== DEVAM PROMPT BİTİŞ ===")

                // 2. GameMasterEngine üzerinden AI'a gönder
                val gmResponse = gameMasterEngine.processPromptDirectly(continuePrompt)

                if (gmResponse == null) {
                    GameLogger.logError(TAG, "TODO-HUB-13: GameMasterEngine null yanıt döndürdü")
                    return@withContext Result.failure(Exception("AI yanıt vermedi"))
                }

                GameLogger.logSystem("TODO-HUB-13: AI yanıtı alındı")
                GameLogger.logSystem("TODO-HUB-13: Yanıt: ${gmResponse.journalEntry}")

                // TODO-HUB-14: Extract expected response
                val fullResponse = gmResponse.journalEntry
                val (displayMessage, expectedResponse) = extractExpectedResponse(fullResponse)

                GameLogger.logSystem("TODO-HUB-14: Expected response extracted: $expectedResponse")

                Result.success(displayMessage to expectedResponse)

            } catch (e: Exception) {
                GameLogger.logError(TAG, "TODO-HUB-13: Oturum devam ettirme hatası", e)
                Log.e(TAG, "Continue session error", e)
                Result.failure(e)
            }
        }
    }

    /**
     * TODO-HUB-14: AI yanıtından [EXPECTED] tagını ayıklar
     *
     * @param fullResponse AI'dan gelen tam yanıt
     * @return Pair(kullanıcıya gösterilecek mesaj, beklenen cevap)
     */
    private fun extractExpectedResponse(fullResponse: String): Pair<String, String?> {
        val expectedTag = "[EXPECTED]:"
        val index = fullResponse.indexOf(expectedTag)

        return if (index != -1) {
            val displayMessage = fullResponse.substring(0, index).trim()
            val expectedResponse = fullResponse.substring(index + expectedTag.length).trim()
            displayMessage to expectedResponse
        } else {
            fullResponse to null
        }
    }

    /**
     * TODO-HUB-13: Oturum devam prompt'u oluşturur
     *
     * HATA #2 FIX: JSON formatı talep eden katı şablon eklendi
     * Mevcut konuşma geçmişini ve kullanıcının yeni cevabını içeren prompt
     */
    private fun buildContinuePrompt(
        chatHistory: String,
        userReply: String,
        language: String,
        level: String
    ): String {
        // G120: Interface dili Türkçe mi İngilizce mi?
        val currentLang = com.example.isekaikuroshin.data.LanguageManager.currentLanguage.value

        return if (currentLang == "EN") {
            // İNGİLİZCE CONTINUE PROMPT
            """
SYSTEM ROLE: You are a professional AI language tutor teaching $language.

Student's Level: $level (CEFR standard)

Conversation so far:
$chatHistory

Student's latest answer: "$userReply"

Your task:
1. Evaluate the student's answer (is it correct, wrong, or incomplete?)
2. Provide constructive feedback
3. Make corrections if necessary
4. Continue the conversation naturally
5. Ask the student a new question or give them a new task

Response Rules:
- Be encouraging and positive
- Correct mistakes gently
- Use language appropriate for the level
- Give short and clear answers (2-4 sentences)

STRICT RULE: Provide your response ONLY in the following JSON format. Don't add any other text!

REQUIRED JSON FORMAT:

{
  "journalEntry": "<Tutor's message>[EXPECTED]: <ideal answer example>",
  "itemsGained": [],
  "questsUpdated": [],
  "statsChanged": {}
}

FORMAT WITHIN journalEntry:
"<Feedback and evaluation>. <Corrections if any>. <New question or task>.

[EXPECTED]: <Ideal short answer example for the new question>"

EXAMPLE COMPLETE JSON RESPONSE:
{
  "journalEntry": "Great! Your sentence is almost perfect. Instead of 'I wake up at 7 AM', it would be more natural to say 'I usually wake up at 7 AM'. Now another question: What do you eat for breakfast?\n\n[EXPECTED]: I usually eat toast and eggs for breakfast.",
  "itemsGained": [],
  "questsUpdated": [],
  "statsChanged": {}
}

WARNING: NEVER go outside the JSON format! Don't add other text!

Now provide your response in the JSON format above.
        """.trimIndent()
        } else {
            // TÜRKÇE CONTINUE PROMPT
            """
SİSTEM ROLÜ: Sen $language öğreten profesyonel bir AI dil eğitmenisin.

Öğrenci Seviyesi: $level (CEFR standardı)

Şu ana kadarki konuşma:
$chatHistory

Öğrencinin son cevabı: "$userReply"

Görevin:
1. Öğrencinin cevabını değerlendir (doğru mu, yanlış mı, eksik mi?)
2. Yapıcı geri bildirim ver
3. Eğer gerekirse düzeltmelerde bulun
4. Konuşmayı doğal bir şekilde devam ettir
5. Öğrenciye yeni bir soru sor veya yeni bir görev ver

Yanıt Kuralları:
- Cesaretlendirici ve olumlu ol
- Hataları nazikçe düzelt
- Seviyeye uygun dil kullan
- Kısa ve net cevaplar ver (2-4 cümle)

KESİN KURAL: Yanıtını SADECE ve SADECE aşağıdaki JSON formatında ver. Başka hiçbir metin ekleme!

İSTENEN JSON FORMATI:

{
  "journalEntry": "<Eğitmen mesajı>[EXPECTED]: <ideal cevap örneği>",
  "itemsGained": [],
  "questsUpdated": [],
  "statsChanged": {}
}

journalEntry İÇİNDEKİ FORMAT:
"<Geri bildirim ve değerlendirme>. <Düzeltmeler varsa>. <Yeni soru veya görev>.

[EXPECTED]: <Yeni soruna ideal kısa cevap örneği>"

ÖRNEK TAM JSON YANIT:
{
  "journalEntry": "Harika! Cümleniz neredeyse mükemmel. 'I wake up at 7 AM' yerine 'I usually wake up at 7 AM' deseydiniz daha doğal olurdu. Şimdi size başka bir soru: Kahvaltıda ne yersiniz?\n\n[EXPECTED]: I usually eat toast and eggs for breakfast.",
  "itemsGained": [],
  "questsUpdated": [],
  "statsChanged": {}
}

UYARI: JSON formatının dışına ASLA çıkma! Başka metin ekleme!

Şimdi yukarıdaki JSON formatında yanıtını ver.
        """.trimIndent()
        }
    }

    /**
     * TODO-HUB-12: Dil eğitmeni prompt şablonu oluşturur
     *
     * Bu fonksiyon, AI'a "dil eğitmeni" rolü veren ve yapılandırılmış JSON
     * yanıt döndürmesini isteyen prompt'u oluşturur.
     *
     * @param language Öğretilecek dil
     * @param level CEFR seviyesi
     * @return Tam prompt metni
     */
    private fun buildLanguageTutorPrompt(language: String, level: String): String {
        // G120: Interface dili Türkçe mi İngilizce mi?
        val currentLang = com.example.isekaikuroshin.data.LanguageManager.currentLanguage.value

        return if (currentLang == "EN") {
            // İNGİLİZCE PROMPT (Interface = EN)
            """
You are a professional AI language tutor teaching $language.

Your task:
1. Student's level: $level (CEFR standard)
2. Choose a simple topic suitable for this level (daily life, hobbies, food, travel, etc.)
3. Ask the student a question or give them a simple task
4. Provide vocabulary and grammar hints to support learning

Important Rules:
- Use language appropriate for the student's level (don't use overly difficult words)
- Be encouraging and positive
- Give clear and understandable explanations
- Provide practical, doable tasks

Provide your response EXACTLY in this JSON format (don't add any other text):

{
  "journalEntry": "<Tutor's message - question or task for the student>",
  "itemsGained": [],
  "questsUpdated": [],
  "statsChanged": {}
}

Provide information in the following structure within journalEntry:
- Main message/question
- [Vocabulary]: At least 3 important words and their meanings
- [Grammar Tip]: 1 simple grammar rule related to the topic
- [Example Sentence]: An example sentence for the task

Example journalEntry format:
"Hello! Today we'll practice 'Daily Routines'.

Question: What do you usually do in the mornings? Please answer in 2-3 sentences.

[Vocabulary]:
- wake up: to stop sleeping
- breakfast: the first meal of the day
- usually: normally, most of the time

[Grammar Tip]:
When talking about daily routines, we use 'Simple Present Tense'. Example: I wake up, I eat breakfast

[Example Sentence]:
I usually wake up at 7 AM and eat breakfast with my family.

Are you ready? Share your answer!"

Now start a language learning session in the above format, suitable for $level level.
        """.trimIndent()
        } else {
            // TÜRKÇE PROMPT (Interface = TR)
            """
Sen $language öğreten profesyonel bir AI dil eğitmenisin.

Görevin:
1. Öğrencinin seviyesi: $level (CEFR standardı)
2. Bu seviyeye uygun basit bir konu seç (günlük yaşam, hobiler, yemek, seyahat vb.)
3. Öğrenciye bir soru sor veya basit bir görev ver
4. Öğrenmeyi destekleyecek kelime ve dil bilgisi ipuçları ver

Önemli Kurallar:
- Öğrencinin seviyesine uygun dil kullan (çok zor kelimeler kullanma)
- Cesaretlendirici ve olumlu ol
- Net ve anlaşılır açıklamalar yap
- Pratik yapılabilir görevler ver

Yanıtını tam olarak şu JSON formatında ver (başka metin ekleme):

{
  "journalEntry": "<Eğitmenin mesajı - öğrenciye soru veya görev>",
  "itemsGained": [],
  "questsUpdated": [],
  "statsChanged": {}
}

journalEntry içinde şu yapıda bilgi ver:
- Ana mesaj/soru
- [Kelime Hazinesi]: En az 3 önemli kelime ve anlamları
- [Dil Bilgisi İpucu]: Konuyla ilgili 1 basit dil bilgisi kuralı
- [Örnek Cümle]: Görev için örnek bir cümle

Örnek journalEntry formatı:
"Merhaba! Bugün 'Günlük Rutinler' konusunda pratik yapacağız.

Soru: Sabahları genellikle ne yaparsınız? Lütfen 2-3 cümle ile cevaplayın.

[Kelime Hazinesi]:
- wake up: uyanmak
- breakfast: kahvaltı
- usually: genellikle

[Dil Bilgisi İpucu]:
Günlük rutinlerden bahsederken 'Simple Present Tense' kullanırız. Örnek: I wake up, I eat breakfast

[Örnek Cümle]:
I usually wake up at 7 AM and eat breakfast with my family.

Hazır mısınız? Yanıtınızı paylaşın!"

Şimdi yukarıdaki formatta, $level seviyesine uygun bir dil öğrenim oturumu başlat.
        """.trimIndent()
        }
    }

    /**
     * TODO-HUB-12: JSON yanıtını LanguageSessionResponse'a parse eder
     *
     * AI'dan gelen journalEntry içindeki yapılandırılmış metni ayrıştırır.
     *
     * @param journalEntry AI'dan gelen journalEntry metni
     * @return Parse edilmiş LanguageSessionResponse veya null
     */
    private fun parseLanguageSessionResponse(journalEntry: String): LanguageSessionResponse? {
        return try {
            // journalEntry içindeki yapılandırılmış bilgiyi parse et
            val tutorMessage = extractSection(journalEntry, "Merhaba!", "[Kelime Hazinesi]")
            val vocabulary = extractVocabulary(journalEntry)
            val grammarPoint = extractSection(journalEntry, "[Dil Bilgisi İpucu]:", "[Örnek Cümle]")
            val exampleSentence = extractSection(journalEntry, "[Örnek Cümle]:", null)

            LanguageSessionResponse(
                tutorMessage = tutorMessage ?: journalEntry,
                vocabulary = vocabulary,
                grammarPoint = grammarPoint,
                exampleSentence = exampleSentence,
                fullResponse = journalEntry
            )
        } catch (e: Exception) {
            GameLogger.logError(TAG, "TODO-HUB-12: Parse hatası", e)
            // Fallback: Tüm metni tutorMessage olarak kullan
            LanguageSessionResponse(
                tutorMessage = journalEntry,
                vocabulary = emptyList(),
                grammarPoint = null,
                exampleSentence = null,
                fullResponse = journalEntry
            )
        }
    }

    /**
     * Metinden belirli bir section'ı çıkarır
     */
    private fun extractSection(text: String, startMarker: String, endMarker: String?): String? {
        return try {
            val startIndex = text.indexOf(startMarker)
            if (startIndex == -1) return null

            val endIndex = if (endMarker != null) {
                val idx = text.indexOf(endMarker, startIndex + startMarker.length)
                if (idx == -1) text.length else idx
            } else {
                text.length
            }

            text.substring(startIndex + startMarker.length, endIndex).trim()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Kelime haznesini çıkarır
     */
    private fun extractVocabulary(text: String): List<VocabularyItem> {
        return try {
            val vocabSection = extractSection(text, "[Kelime Hazinesi]:", "[Dil Bilgisi İpucu]") ?: return emptyList()

            val items = mutableListOf<VocabularyItem>()
            val lines = vocabSection.split("\n")

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("-")) {
                    val parts = trimmed.substring(1).split(":")
                    if (parts.size >= 2) {
                        items.add(
                            VocabularyItem(
                                word = parts[0].trim(),
                                meaning = parts[1].trim()
                            )
                        )
                    }
                }
            }

            items
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * TODO-HUB-12: Dil öğrenim oturumu yanıtı
 *
 * AI dil eğitmeninden dönen yapılandırılmış veri
 */
data class LanguageSessionResponse(
    /**
     * Eğitmenin ana mesajı (soru veya görev)
     */
    val tutorMessage: String,

    /**
     * Öğretilen kelimeler
     */
    val vocabulary: List<VocabularyItem>,

    /**
     * Dil bilgisi ipucu
     */
    val grammarPoint: String?,

    /**
     * Örnek cümle
     */
    val exampleSentence: String?,

    /**
     * Tam AI yanıtı (log/debug için)
     */
    val fullResponse: String,

    /**
     * TODO-HUB-14: AI'ın beklediği ideal cevap
     * (Kullanıcının cevabını değerlendirmek için)
     */
    val expectedResponse: String? = null
)

/**
 * TODO-HUB-12: Kelime hazinesi öğesi
 */
data class VocabularyItem(
    val word: String,
    val meaning: String
)

/**
 * TODO-HUB-13: Sohbet mesajı
 *
 * İnteraktif dil öğrenim sohbetinde bir mesajı temsil eder
 */
data class ChatMessage(
    /**
     * Mesaj metni
     */
    val text: String,

    /**
     * Mesajın kullanıcıdan mı AI'dan mı geldiği
     */
    val isFromUser: Boolean,

    /**
     * Mesaj gönderilme zamanı (timestamp)
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * TODO-HUB-14: AI mesajı için beklenen kullanıcı cevabı
     * (Sadece AI mesajları için kullanılır, kullanıcı mesajlarında null)
     */
    val expectedResponse: String? = null
)
