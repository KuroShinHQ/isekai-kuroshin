package com.example.isekaikuroshin.workers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
// import com.tom_roush.pdfbox.util.PDFBoxResourceLoader // Temporarily commented out
import com.example.isekaikuroshin.data.database.AppDatabase
import com.example.isekaikuroshin.data.database.DocumentChunkEntity
import com.example.isekaikuroshin.utils.TextChunker

class DocumentProcessingWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "DocWorker"
        private const val KEY_DOCUMENT_URI = "KEY_DOCUMENT_URI"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Input verisini al
            val uriString = inputData.getString(KEY_DOCUMENT_URI)
            if (uriString.isNullOrEmpty()) {
                Log.e(TAG, "URI null veya boş, işlem sonlandırılıyor")
                return@withContext Result.failure()
            }

            val uri = Uri.parse(uriString)
            Log.d(TAG, "Dosya işleme başladı: $uriString")

            // 2. Dosya MIME tipini belirle
            val mimeType = applicationContext.contentResolver.getType(uri)
            Log.d(TAG, "Belirlenen MIME tipi: $mimeType")

            // 3. MIME tipine göre dosya içeriğini oku
            val extractedText = when (mimeType) {
                "text/plain" -> {
                    Log.d(TAG, "TXT dosyası işleniyor...")
                    extractTextFromPlainText(uri)
                }
                "application/pdf" -> {
                    Log.d(TAG, "PDF dosyası işleniyor...")
                    extractTextFromPDF(uri)
                }
                else -> {
                    Log.e(TAG, "Desteklenmeyen dosya tipi: $mimeType")
                    return@withContext Result.failure()
                }
            }

            // 4. Sonucu logla (doğrulama için)
            val characterCount = extractedText.length
            val preview = if (extractedText.length > 500) {
                extractedText.substring(0, 500) + "..."
            } else {
                extractedText
            }

            Log.d(TAG, "Metin çıkarma başarılı. Toplam karakter: $characterCount. Başlangıç: $preview")

            // 5. Metni parçalara ayır
            val textChunks = TextChunker.chunkTextFixedSize(extractedText)
            Log.d(TAG, "Metin ${textChunks.size} parçaya ayrıldı")

            // 6. Parçaları DocumentChunkEntity listesine dönüştür
            val chunkEntities = textChunks.mapIndexed { index, chunk ->
                DocumentChunkEntity(
                    sourceDocumentUri = uriString,
                    chunkIndex = index,
                    content = chunk
                )
            }

            // 7. Database'e kaydet
            val database = AppDatabase.create(applicationContext)
            val chunkDao = database.documentChunkDao()

            // Önceki chunk'ları temizle (aynı dosya tekrar işlenirse)
            chunkDao.deleteChunksByUri(uriString)

            // Yeni chunk'ları kaydet
            chunkDao.insertAll(chunkEntities)

            Log.d(TAG, "İşlem tamamlandı. ${chunkEntities.size} adet metin parçası veritabanına kaydedildi.")

            // 8. Başarı ile bitir - chunk sayısını output data olarak ekle
            val outputData = workDataOf("CHUNK_COUNT" to chunkEntities.size)
            Result.success(outputData)

        } catch (e: Exception) {
            Log.e(TAG, "Dosya işleme hatası: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun extractTextFromPlainText(uri: Uri): String {
        return applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.bufferedReader().readText()
        } ?: throw IOException("TXT dosyası okunamadı")
    }

    private suspend fun extractTextFromPDF(uri: Uri): String {
        return applicationContext.contentResolver.openInputStream(uri)?.use { inputStream ->
            // PDFBoxResourceLoader already initialized in Application class
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            text
        } ?: throw IOException("PDF dosyası okunamadı")
    }
}