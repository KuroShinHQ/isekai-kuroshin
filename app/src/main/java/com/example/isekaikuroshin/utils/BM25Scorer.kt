package com.example.isekaikuroshin.utils

import com.example.isekaikuroshin.data.database.DocumentChunkEntity
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * BM25 (Best Matching 25) Scoring Algorithm Implementation
 *
 * Bu sınıf, metin parçalarını (document chunks) bir sorguya göre alakalılık skoruna
 * göre sıralamak için BM25 algoritmasını kullanır.
 *
 * BM25 Formülü:
 * score = IDF * (tf * (k1 + 1)) / (tf + k1 * (1 - b + b * |d|/avgdl))
 *
 * Parametre açıklamaları:
 * - tf: Term frequency (kelime sıklığı)
 * - IDF: Inverse Document Frequency (ters doküman frekansı)
 * - k1: Term frequency saturation point (1.5)
 * - b: Length normalization parameter (0.75)
 * - |d|: Document length
 * - avgdl: Average document length
 *
 * Kullanım:
 * val scorer = BM25Scorer(documentChunks)
 * val rankedChunks = scorer.rankChunks(extractedKeywords)
 */
class BM25Scorer(
    private val documentChunks: List<DocumentChunkEntity>,
    private val language: String = "TR"
) {

    // BM25 parametreleri - endüstri standardı değerler
    private val k1 = 1.5f
    private val b = 0.75f

    // Ön hesaplanmış değerler
    private val corpusTermFreq: Map<String, Int>
    private val docTermFreqs: List<Map<String, Int>>
    private val docLengths: List<Int>
    private val avgDocLength: Float
    private val idfScores: Map<String, Float>

    init {
        GameLogger.logSystem("BM25Scorer: Initializing with ${documentChunks.size} document chunks for language: $language")

        // Her doküman için kelime frekansı hesapla
        docTermFreqs = documentChunks.map { chunk ->
            tokenizeByLanguage(chunk.content, language)
        }

        // Doküman uzunluklarını hesapla
        docLengths = docTermFreqs.map { it.values.sum() }
        avgDocLength = if (docLengths.isNotEmpty()) {
            docLengths.average().toFloat()
        } else {
            0f
        }

        // Tüm korpustaki kelime frekanslarını topla
        corpusTermFreq = mutableMapOf<String, Int>().apply {
            docTermFreqs.forEach { docTerms ->
                docTerms.forEach { (term, freq) ->
                    this[term] = (this[term] ?: 0) + freq
                }
            }
        }

        // IDF skorlarını ön hesapla
        idfScores = calculateAllIDFScores()

        GameLogger.logSystem("BM25Scorer: Initialized with:")
        GameLogger.logSystem("- Total unique terms: ${corpusTermFreq.size}")
        GameLogger.logSystem("- Average document length: $avgDocLength")
        GameLogger.logSystem("- Document count: ${documentChunks.size}")
    }

    /**
     * Dile göre stop words listesi döndürür
     */
    private fun getStopWords(language: String): Set<String> {
        return when(language) {
            "TR" -> setOf(
                "ve", "ile", "bir", "bu", "o", "şu", "de", "da", "ki", "için",
                "olan", "olan", "olarak", "ancak", "fakat", "ama", "lakin",
                "den", "dan", "ten", "tan", "nin", "nın", "nun", "nün",
                "ben", "sen", "biz", "siz", "onlar", "şey", "zaman", "gibi"
            )
            "EN" -> setOf(
                "the", "a", "an", "and", "or", "but", "is", "are", "was", "were",
                "for", "with", "from", "to", "in", "on", "at", "by", "this", "that",
                "these", "those", "his", "her", "their", "my", "our", "your",
                "be", "been", "being", "have", "has", "had", "do", "does", "did",
                "will", "would", "could", "should", "may", "might", "can", "cannot"
            )
            else -> emptySet()
        }
    }

    /**
     * Dile göre metni tokenize eder ve kelime frekanslarını hesaplar
     */
    private fun tokenizeByLanguage(text: String, language: String): Map<String, Int> {
        val stopWords = getStopWords(language)

        val cleanedText = when(language) {
            "TR" -> text.lowercase().replace(Regex("[^a-züğışöç\\s]"), " ")
            "EN" -> text.lowercase().replace(Regex("[^a-z\\s]"), " ")
            else -> text.lowercase().replace(Regex("[^a-z\\s]"), " ")
        }

        return cleanedText
            .split(Regex("\\s+")) // Boşluklardan böl
            .filter { it.length >= 2 } // Çok kısa kelimeleri filtrele
            .filter { it !in stopWords } // Stop words'leri filtrele
            .map { word -> Stemmer.stem(word, language) } // Kök bulma uygula
            .groupingBy { it }
            .eachCount()
    }

    /**
     * Tüm benzersiz kelimeler için IDF skorlarını hesaplar
     */
    private fun calculateAllIDFScores(): Map<String, Float> {
        val totalDocs = documentChunks.size.toFloat()

        return corpusTermFreq.keys.associateWith { term ->
            // Bu termin kaç dokümanda geçtiğini say
            val docsContainingTerm = docTermFreqs.count { docTerms ->
                docTerms.containsKey(term)
            }.toFloat()

            // IDF hesapla: log((N - df + 0.5) / (df + 0.5))
            // Burada klasik IDF formülü kullanıyoruz
            val idf = ln((totalDocs - docsContainingTerm + 0.5f) / (docsContainingTerm + 0.5f))
            maxOf(0f, idf) // Negatif IDF skorlarını 0'a çek
        }
    }

    /**
     * Verilen anahtar kelimeler için bir dokümanın BM25 skorunu hesaplar
     */
    private fun calculateBM25Score(docIndex: Int, queryTerms: List<String>): Float {
        val docTermFreq = docTermFreqs[docIndex]
        val docLength = docLengths[docIndex].toFloat()

        var totalScore = 0f

        queryTerms.forEach { term ->
            val tf = docTermFreq[term]?.toFloat() ?: 0f
            val idf = idfScores[term] ?: 0f

            if (tf > 0f && idf > 0f) {
                // BM25 formülü: IDF * (tf * (k1 + 1)) / (tf + k1 * (1 - b + b * |d|/avgdl))
                val numerator = tf * (k1 + 1f)
                val denominator = tf + k1 * (1f - b + b * (docLength / avgDocLength))
                val score = idf * (numerator / denominator)
                totalScore += score
            }
        }

        return totalScore
    }

    /**
     * Verilen sorgu terimlerine göre dokümanları BM25 skoruna göre sıralar
     *
     * @param queryTerms Anahtar kelimeler listesi
     * @param topK En iyi K sonucu döndür (varsayılan: 5)
     * @return Skorlanmış ve sıralanmış chunk'lar
     */
    fun rankChunks(queryTerms: List<String>, topK: Int = 5): List<ScoredChunk> {
        if (queryTerms.isEmpty() || documentChunks.isEmpty()) {
            GameLogger.logSystem("BM25Scorer: Empty query terms or documents, returning empty list")
            return emptyList()
        }

        // Query terimlerini normalize et
        val normalizedQueryTerms = queryTerms.map { it.lowercase().trim() }
            .filter { it.length >= 2 }

        GameLogger.logSystem("BM25Scorer: Ranking chunks with query terms: $normalizedQueryTerms")

        // Her doküman için skor hesapla
        val scoredChunks = documentChunks.mapIndexed { index, chunk ->
            val score = calculateBM25Score(index, normalizedQueryTerms)
            ScoredChunk(
                chunk = chunk,
                score = score,
                chunkIndex = index
            )
        }

        // Skora göre sırala (yüksekten düşüğe) ve topK'yı al
        val rankedChunks = scoredChunks
            .sortedByDescending { it.score }
            .take(topK)

        // Detaylı log
        GameLogger.logSystem("BM25Scorer: Ranking results:")
        rankedChunks.forEachIndexed { rank, scoredChunk ->
            GameLogger.logSystem("  ${rank + 1}. Chunk ${scoredChunk.chunkIndex} (Score: ${String.format("%.3f", scoredChunk.score)})")
            GameLogger.logSystem("     Content preview: ${scoredChunk.chunk.content.take(100)}...")
        }

        return rankedChunks
    }

    /**
     * Dile göre anahtar kelime çıkarma fonksiyonu
     * Mevcut dil ayarına göre doğru stop words ve tokenization kullanır
     */
    fun extractKeywords(text: String): List<String> {
        val stopWords = getStopWords(language)

        val cleanedText = when(language) {
            "TR" -> text.lowercase().replace(Regex("[^a-züğışöç\\s]"), " ")
            "EN" -> text.lowercase().replace(Regex("[^a-z\\s]"), " ")
            else -> text.lowercase().replace(Regex("[^a-z\\s]"), " ")
        }

        val keywords = cleanedText
            .split(Regex("\\s+"))
            .filter { it.length >= 3 } // En az 3 karakter
            .filter { it !in stopWords } // Stop words'leri filtrele
            .map { word -> Stemmer.stem(word, language) } // Kök bulma uygula
            .distinct() // Tekrarları kaldır
            .take(10) // En fazla 10 anahtar kelime

        GameLogger.logSystem("BM25Scorer: Extracted stemmed keywords for language '$language': $keywords")
        return keywords
    }

    /**
     * Skor bilgisi içeren chunk wrapper sınıfı
     */
    data class ScoredChunk(
        val chunk: DocumentChunkEntity,
        val score: Float,
        val chunkIndex: Int
    )
}