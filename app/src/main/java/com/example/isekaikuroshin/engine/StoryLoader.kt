package com.example.isekaikuroshin.engine

import android.content.Context
import com.example.isekaikuroshin.utils.GameLogger
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * StoryLoader - RAG Sistemi için Harici Kitap Yükleme ve Parçalara Ayırma
 *
 * Bu sınıf, assets klasöründeki metin dosyalarını okuyarak, onları
 * anlamlı metin bloklarına (chunk) böler ve RAG sistemi için kullanılabilir hale getirir.
 *
 * KULLANIM:
 * ```kotlin
 * val loader = StoryLoader(context)
 * val chunks = loader.loadAndChunkStory("stories/monte_kristo_kontu_en.txt")
 * ```
 */
class StoryLoader(private val context: Context) {

    companion object {
        private const val TAG = "StoryLoader"

        // Chunk boyutu parametreleri
        private const val CHUNK_SIZE_CHARS = 1000  // Her chunk maksimum 1000 karakter
        private const val CHUNK_OVERLAP = 200      // Chunk'lar arası 200 karakter örtüşme

        // Paragraf tanımlayıcılar
        private val PARAGRAPH_SEPARATORS = listOf("\n\n", "\r\n\r\n")

        // Basit İngilizce stopwords listesi
        private val STOPWORDS = setOf(
            "the", "and", "is", "in", "to", "of", "a", "that", "it", "was", "for", "on",
            "with", "as", "his", "her", "he", "she", "at", "by", "from", "this", "be",
            "are", "or", "an", "but", "not", "you", "all", "were", "we", "when", "your",
            "can", "said", "there", "use", "each", "which", "do", "how", "their", "if",
            "will", "up", "other", "about", "out", "many", "then", "them", "these", "so",
            "some", "would", "make", "like", "him", "into", "time", "has", "look", "two",
            "more", "go", "see", "no", "way", "could", "my", "than", "been", "who", "its"
        )
    }

    /**
     * Assets klasöründen metin dosyası okur ve chunk'lara ayırır
     *
     * @param assetPath Assets klasöründeki dosya yolu (örn: "stories/monte_kristo_kontu_en.txt")
     * @return List<String> - Metin parçaları listesi
     */
    fun loadAndChunkStory(assetPath: String): List<String> {
        GameLogger.logSystem("$TAG: Hikaye yükleniyor: $assetPath")

        return try {
            // Assets'ten dosyayı oku
            val fullText = readAssetFile(assetPath)

            if (fullText.isEmpty()) {
                GameLogger.logSystem("$TAG: ⚠️ Dosya boş veya okunamadı")
                return emptyList()
            }

            GameLogger.logSystem("$TAG: ✅ Dosya okundu, boyut: ${fullText.length} karakter")

            // Metni chunk'lara ayır
            val chunks = chunkText(fullText)

            GameLogger.logSystem("$TAG: ✅ Metin ${chunks.size} chunk'a ayrıldı")
            GameLogger.logSystem("$TAG: Ortalama chunk boyutu: ${chunks.map { it.length }.average().toInt()} karakter")

            chunks

        } catch (e: Exception) {
            GameLogger.logError(TAG, "Hikaye yükleme hatası: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Assets klasöründen dosya okur
     */
    private fun readAssetFile(assetPath: String): String {
        return try {
            context.assets.open(assetPath).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            GameLogger.logError(TAG, "Assets dosyası okunamadı: $assetPath", e)
            ""
        }
    }

    /**
     * Metni anlamlı chunk'lara ayırır
     *
     * Stratejiler:
     * 1. Önce paragraflara böl
     * 2. Eğer paragraf CHUNK_SIZE'dan büyükse, cümlelere böl
     * 3. CHUNK_OVERLAP ile örtüşen chunk'lar oluştur
     *
     * @param text Tam metin
     * @return List<String> - Chunk listesi
     */
    private fun chunkText(text: String): List<String> {
        val chunks = mutableListOf<String>()

        // 1. Metni paragraflara ayır
        val paragraphs = text.split(Regex("\\n\\n+|\\r\\n\\r\\n+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        GameLogger.logSystem("$TAG: Toplam ${paragraphs.size} paragraf bulundu")

        var currentChunk = StringBuilder()

        for (paragraph in paragraphs) {
            // Eğer mevcut chunk + yeni paragraf CHUNK_SIZE'ı aşmazsa birleştir
            if (currentChunk.length + paragraph.length + 2 <= CHUNK_SIZE_CHARS) {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append("\n\n")
                }
                currentChunk.append(paragraph)
            } else {
                // Mevcut chunk'ı kaydet (boş değilse)
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                }

                // Eğer paragraf kendisi CHUNK_SIZE'dan büyükse, parçalara böl
                if (paragraph.length > CHUNK_SIZE_CHARS) {
                    chunks.addAll(splitLargeParagraph(paragraph))
                    currentChunk = StringBuilder()
                } else {
                    // Yeni chunk'a başla
                    currentChunk = StringBuilder(paragraph)
                }
            }
        }

        // Son chunk'ı ekle
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }

        // Overlap ile chunk'ları yeniden düzenle
        return addOverlap(chunks)
    }

    /**
     * Büyük paragrafı cümlelere göre böler
     */
    private fun splitLargeParagraph(paragraph: String): List<String> {
        val chunks = mutableListOf<String>()
        val sentences = paragraph.split(Regex("(?<=[.!?])\\s+"))

        var currentChunk = StringBuilder()

        for (sentence in sentences) {
            if (currentChunk.length + sentence.length + 1 <= CHUNK_SIZE_CHARS) {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append(" ")
                }
                currentChunk.append(sentence)
            } else {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                }

                // Eğer tek cümle bile CHUNK_SIZE'dan büyükse, karakter bazlı böl
                if (sentence.length > CHUNK_SIZE_CHARS) {
                    chunks.addAll(splitByCharacters(sentence))
                    currentChunk = StringBuilder()
                } else {
                    currentChunk = StringBuilder(sentence)
                }
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }

        return chunks
    }

    /**
     * Çok uzun metni karakter bazlı böler (son çare)
     */
    private fun splitByCharacters(text: String): List<String> {
        val chunks = mutableListOf<String>()
        var start = 0

        while (start < text.length) {
            val end = minOf(start + CHUNK_SIZE_CHARS, text.length)
            chunks.add(text.substring(start, end))
            start += CHUNK_SIZE_CHARS - CHUNK_OVERLAP
        }

        return chunks
    }

    /**
     * Chunk'lar arasında overlap (örtüşme) ekler
     * Bu, chunk sınırlarındaki bilgi kaybını önler
     */
    private fun addOverlap(chunks: List<String>): List<String> {
        if (chunks.size <= 1) return chunks

        val overlappedChunks = mutableListOf<String>()

        for (i in chunks.indices) {
            val chunk = chunks[i]

            // Son chunk hariç, her chunk'a bir sonraki chunk'tan overlap ekle
            if (i < chunks.size - 1) {
                val nextChunk = chunks[i + 1]
                val overlapText = nextChunk.take(CHUNK_OVERLAP)
                overlappedChunks.add(chunk + "\n\n[DEVAM EDIYOR...]\n\n" + overlapText)
            } else {
                overlappedChunks.add(chunk)
            }
        }

        return overlappedChunks
    }

    /**
     * Verilen query'ye en uygun chunk'ları seçer (basit TF-IDF yaklaşımı)
     *
     * @param query Kullanıcı sorgusu
     * @param chunks Tüm chunk'lar
     * @param topK Kaç chunk döndürüleceği
     * @return List<String> - En alakalı chunk'lar
     */
    fun selectRelevantChunks(query: String, chunks: List<String>, topK: Int = 3): List<String> {
        if (chunks.isEmpty()) return emptyList()

        // Query'yi kelime listesine çevir (lowercase, stopwords hariç)
        val queryWords = tokenize(query.lowercase())

        if (queryWords.isEmpty()) {
            // Query boşsa, ilk topK chunk'ı döndür
            return chunks.take(topK)
        }

        // Her chunk için skor hesapla
        val scoredChunks = chunks.mapIndexed { index, chunk ->
            val chunkWords = tokenize(chunk.lowercase())

            // Basit skorlama: Query kelimelerinin chunk'ta kaç kez geçtiği
            val score = queryWords.sumOf { queryWord ->
                chunkWords.count { it.contains(queryWord) || queryWord.contains(it) }
            }

            Triple(index, chunk, score)
        }

        // Skora göre sırala ve en yüksek topK'yı döndür
        val topChunks = scoredChunks
            .sortedByDescending { it.third }
            .take(topK)

        GameLogger.logSystem("$TAG: Query için ${topChunks.size} alakalı chunk seçildi")
        topChunks.forEachIndexed { i, (index, _, score) ->
            GameLogger.logSystem("$TAG:   ${i+1}. Chunk #$index (Skor: $score)")
        }

        return topChunks.map { it.second }
    }

    /**
     * Metni tokenlara ayırır (kelimeler)
     */
    private fun tokenize(text: String): List<String> {
        return text.split(Regex("\\W+"))
            .filter { it.isNotEmpty() && it.length > 2 } // 2 karakterden uzun kelimeler
            .filterNot { it in STOPWORDS }
    }
}
