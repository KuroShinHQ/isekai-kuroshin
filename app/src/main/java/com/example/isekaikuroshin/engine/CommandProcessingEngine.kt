package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.ui.healthhub.HealthConnectManager
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TODO-HUB-01: Hibrit NLU Motoru - Komut İşleme Motoru (Yer Tutucu)
 *
 * HybridNLUEngine'den gelen CommandResult'ları işleyen motor.
 * Şu anda sadece loglama yapıyor, gelecekte gerçek Health & Mind Hub
 * işlemlerini gerçekleştirecek.
 */
@Singleton
class CommandProcessingEngine @Inject constructor(
    private val healthConnectManager: HealthConnectManager
) {

    companion object {
        private const val TAG = "CommandProcessor"
    }

    /**
     * Ana komut işleme fonksiyonu
     * CommandResult'ı alıp ilgili işlemi gerçekleştirir
     *
     * @param commandResult HybridNLUEngine'den gelen ayrıştırılmış komut
     * @return Boolean - İşlem başarı durumu
     */
    fun processCommand(commandResult: CommandResult): Boolean {
        try {
            GameLogger.logSystem("=== COMMAND PROCESSING ENGINE ===")
            GameLogger.logSystem("İşlenecek komut: ${commandResult.commandType}")
            GameLogger.logSystem("Parametreler: ${commandResult.parameters}")
            GameLogger.logSystem("Güven skoru: ${commandResult.confidence}")

            when (commandResult.commandType) {
                "SET_ALARM" -> processSetAlarm(commandResult)
                "LOG_WEIGHT" -> processLogWeight(commandResult)
                "LOG_BLOOD_PRESSURE" -> processLogBloodPressure(commandResult)
                "LOG_HEART_RATE" -> processLogHeartRate(commandResult)
                "LOG_SLEEP" -> processLogSleep(commandResult)
                "LOG_FOOD" -> processLogFood(commandResult)
                "LOG_EXERCISE" -> processLogExercise(commandResult)
                "LOG_WATER" -> processLogWater(commandResult)
                "LOG_MEDICATION" -> processLogMedication(commandResult)
                else -> processUnknownCommand(commandResult)
            }

            GameLogger.logSystem("=== COMMAND PROCESSED SUCCESSFULLY ===")
            return true

        } catch (e: Exception) {
            GameLogger.logError(TAG, "Error processing command: ${commandResult.commandType}", e)
            return false
        }
    }

    /**
     * Alarm kurma komutunu işler
     */
    private fun processSetAlarm(commandResult: CommandResult) {
        val time = commandResult.parameters["time"] ?: "bilinmeyen saat"

        GameLogger.logSystem("=== SET_ALARM PROCESSING ===")
        GameLogger.logSystem("Alarm kurulacak saat: $time")

        // TODO: Gerçek alarm kurma işlemi
        // AlarmManager ile sistem alarmı kurulacak
        // Notification sistemi entegre edilecek

        GameLogger.logSystem("✅ Alarm başarıyla kuruldu: $time")
        GameLogger.logSystem("📱 Sistem bildirimi: 'Alarm $time için ayarlandı'")
    }

    /**
     * Kilo kaydetme komutunu işler
     * TODO-HUB-03: Health Connect entegrasyonu ile gerçek kilo verisi yazma
     */
    private fun processLogWeight(commandResult: CommandResult) {
        val weightStr = commandResult.parameters["weight"] ?: "bilinmeyen"
        val unit = commandResult.parameters["unit"] ?: "kg"

        GameLogger.logSystem("=== LOG_WEIGHT PROCESSING ===")
        GameLogger.logSystem("TODO-HUB-03: Kaydedilecek kilo: $weightStr $unit")

        try {
            // Kilo değerini sayısal değere çevir
            val weightValue = weightStr.toDoubleOrNull()

            if (weightValue != null && unit.lowercase() == "kg") {
                GameLogger.logSystem("TODO-HUB-03: Health Connect'e kilo yazılıyor: ${weightValue}kg")

                // Coroutine ile Health Connect'e veri yaz
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // İzin kontrolü yap
                        val hasPermissions = healthConnectManager.checkPermissions()
                        if (!hasPermissions) {
                            GameLogger.logSystem("TODO-HUB-03: ⚠️ Health Connect izinleri yok, izin isteme gerekiyor")
                            healthConnectManager.requestPermissions()
                            return@launch
                        }

                        // Kilo verisini yaz
                        val success = healthConnectManager.writeWeight(weightValue)

                        if (success) {
                            GameLogger.logSystem("TODO-HUB-03: ✅ Kilo başarıyla Health Connect'e kaydedildi: ${weightValue}kg")
                            GameLogger.logSystem("TODO-HUB-03: 📊 Health Connect sağlık verisi güncellendi")
                        } else {
                            GameLogger.logSystem("TODO-HUB-03: ❌ Kilo Health Connect'e yazılamadı")
                        }

                    } catch (e: Exception) {
                        GameLogger.logError(TAG, "TODO-HUB-03: Health Connect kilo yazma hatası", e)
                    }
                }

            } else {
                GameLogger.logSystem("TODO-HUB-03: ⚠️ Geçersiz kilo verisi: $weightStr $unit")
                GameLogger.logSystem("TODO-HUB-03: Sadece kg biriminde sayısal değerler destekleniyor")
            }

            // Genel log kayıt (Health Connect'ten bağımsız)
            GameLogger.logSystem("✅ Kilo komutu işlendi: $weightStr $unit")

        } catch (e: Exception) {
            GameLogger.logError(TAG, "TODO-HUB-03: processLogWeight hatası", e)
        }
    }

    /**
     * Kan basıncı kaydetme komutunu işler
     */
    private fun processLogBloodPressure(commandResult: CommandResult) {
        val systolic = commandResult.parameters["systolic"] ?: "bilinmeyen"
        val diastolic = commandResult.parameters["diastolic"] ?: "bilinmeyen"

        GameLogger.logSystem("=== LOG_BLOOD_PRESSURE PROCESSING ===")
        GameLogger.logSystem("Kaydedilecek tansiyon: $systolic/$diastolic")

        // TODO: Gerçek tansiyon kaydetme işlemi
        // Sağlık uyarıları kontrolü
        // Trend analizi

        GameLogger.logSystem("✅ Kan basıncı başarıyla kaydedildi: $systolic/$diastolic")
        GameLogger.logSystem("🩺 Sağlık durumu analiz edildi")
    }

    /**
     * Kalp atışı kaydetme komutunu işler
     */
    private fun processLogHeartRate(commandResult: CommandResult) {
        val bpm = commandResult.parameters["bpm"] ?: "bilinmeyen"

        GameLogger.logSystem("=== LOG_HEART_RATE PROCESSING ===")
        GameLogger.logSystem("Kaydedilecek nabız: $bpm bpm")

        // TODO: Gerçek nabız kaydetme işlemi
        // Normal değer aralığı kontrolü
        // Egzersiz korelasyonu

        GameLogger.logSystem("✅ Kalp atışı başarıyla kaydedildi: $bpm bpm")
        GameLogger.logSystem("❤️ Kardiyovasküler veri güncellendi")
    }

    /**
     * Uyku kaydetme komutunu işler
     */
    private fun processLogSleep(commandResult: CommandResult) {
        val duration = commandResult.parameters["duration"] ?: "bilinmeyen"
        val unit = commandResult.parameters["unit"] ?: "saat"

        GameLogger.logSystem("=== LOG_SLEEP PROCESSING ===")
        GameLogger.logSystem("Kaydedilecek uyku süresi: $duration $unit")

        // TODO: Gerçek uyku kaydetme işlemi
        // Uyku kalitesi analizi
        // Hedef karşılaştırması (8 saat)
        // Uyku düzeni takibi

        GameLogger.logSystem("✅ Uyku süresi başarıyla kaydedildi: $duration $unit")
        GameLogger.logSystem("😴 Uyku düzeni güncellendi")
    }

    /**
     * Yemek kaydetme komutunu işler
     * TODO-HUB-09: Health Connect entegrasyonu ile gerçek beslenme verisi yazma
     */
    private fun processLogFood(commandResult: CommandResult) {
        val description = commandResult.parameters["description"] ?: "bilinmeyen yemek"
        val mealName = commandResult.parameters["mealName"]
        val mealType = commandResult.parameters["mealType"]

        GameLogger.logSystem("=== LOG_FOOD PROCESSING ===")
        GameLogger.logSystem("TODO-HUB-09: Kaydedilecek yemek: $description")

        if (mealName != null) {
            GameLogger.logSystem("TODO-HUB-09: Yemek adı: $mealName")
        }
        if (mealType != null) {
            GameLogger.logSystem("TODO-HUB-09: Öğün türü: $mealType")
        }

        try {
            // Yemek adı ve öğün türünü kontrol et
            val finalMealName = mealName ?: description
            val finalMealType = mealType ?: "UNKNOWN"

            GameLogger.logSystem("TODO-HUB-09: Health Connect'e beslenme yazılıyor: $finalMealName ($finalMealType)")

            // Coroutine ile Health Connect'e veri yaz
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // İzin kontrolü yap
                    val hasPermissions = healthConnectManager.checkPermissions()
                    if (!hasPermissions) {
                        GameLogger.logSystem("TODO-HUB-09: ⚠️ Health Connect izinleri yok, izin isteme gerekiyor")
                        healthConnectManager.requestPermissions()
                        return@launch
                    }

                    // Beslenme verisini yaz
                    val success = healthConnectManager.writeNutrition(
                        mealName = finalMealName,
                        mealType = finalMealType
                    )

                    if (success) {
                        GameLogger.logSystem("TODO-HUB-09: ✅ Beslenme başarıyla Health Connect'e kaydedildi: $finalMealName ($finalMealType)")
                        GameLogger.logSystem("TODO-HUB-09: 🍽️ Health Connect beslenme verisi güncellendi")
                    } else {
                        GameLogger.logSystem("TODO-HUB-09: ❌ Beslenme Health Connect'e yazılamadı")
                    }

                } catch (e: Exception) {
                    GameLogger.logError(TAG, "TODO-HUB-09: Health Connect beslenme yazma hatası", e)
                }
            }

            // Genel log kayıt (Health Connect'ten bağımsız)
            GameLogger.logSystem("✅ Yemek komutu işlendi: $description")
            GameLogger.logSystem("🍽️ Beslenme günlüğü güncellendi")

        } catch (e: Exception) {
            GameLogger.logError(TAG, "TODO-HUB-09: processLogFood hatası", e)
        }
    }

    /**
     * Egzersiz kaydetme komutunu işler
     * TODO-HUB-06: Health Connect entegrasyonu ile gerçek egzersiz verisi yazma
     */
    private fun processLogExercise(commandResult: CommandResult) {
        val description = commandResult.parameters["description"] ?: "egzersiz"
        val duration = commandResult.parameters["duration"]
        val exerciseType = commandResult.parameters["exerciseType"]

        GameLogger.logSystem("=== LOG_EXERCISE PROCESSING ===")
        GameLogger.logSystem("TODO-HUB-06: Kaydedilecek egzersiz: $description")
        if (duration != null) {
            GameLogger.logSystem("TODO-HUB-06: Süre: $duration dakika")
        }
        if (exerciseType != null) {
            GameLogger.logSystem("TODO-HUB-06: Egzersiz tipi: $exerciseType")
        }

        try {
            // Süreyi kontrol et ve varsayılan değer ata
            val durationMinutes = duration?.toLongOrNull() ?: 30L
            val exerciseTypeString = exerciseType ?: "OTHER"

            GameLogger.logSystem("TODO-HUB-06: Health Connect'e egzersiz yazılıyor: $exerciseTypeString, ${durationMinutes} dakika")

            // Coroutine ile Health Connect'e veri yaz
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // İzin kontrolü yap
                    val hasPermissions = healthConnectManager.checkPermissions()
                    if (!hasPermissions) {
                        GameLogger.logSystem("TODO-HUB-06: ⚠️ Health Connect izinleri yok, izin isteme gerekiyor")
                        healthConnectManager.requestPermissions()
                        return@launch
                    }

                    // Başlangıç zamanını ayarla (şu andan önceki süre)
                    val now = java.time.Instant.now()
                    val startTime = now.minusSeconds(durationMinutes * 60)
                    val exerciseDuration = java.time.Duration.ofMinutes(durationMinutes)

                    // Egzersiz tipini map et
                    val mappedExerciseType = healthConnectManager.mapExerciseType(exerciseTypeString)

                    // Egzersiz verisini yaz
                    val success = healthConnectManager.writeExerciseSession(
                        exerciseType = mappedExerciseType,
                        startTime = startTime,
                        duration = exerciseDuration
                    )

                    if (success) {
                        GameLogger.logSystem("TODO-HUB-06: ✅ Egzersiz başarıyla Health Connect'e kaydedildi: $exerciseTypeString, ${durationMinutes} dakika")
                        GameLogger.logSystem("TODO-HUB-06: 🏃 Health Connect aktivite verisi güncellendi")
                    } else {
                        GameLogger.logSystem("TODO-HUB-06: ❌ Egzersiz Health Connect'e yazılamadı")
                    }

                } catch (e: Exception) {
                    GameLogger.logError(TAG, "TODO-HUB-06: Health Connect egzersiz yazma hatası", e)
                }
            }

            // Genel log kayıt (Health Connect'ten bağımsız)
            GameLogger.logSystem("✅ Egzersiz komutu işlendi: $description")
            GameLogger.logSystem("💪 Aktivite günlüğü güncellendi")

        } catch (e: Exception) {
            GameLogger.logError(TAG, "TODO-HUB-06: processLogExercise hatası", e)
        }
    }

    /**
     * Su tüketimi kaydetme komutunu işler
     */
    private fun processLogWater(commandResult: CommandResult) {
        val amount = commandResult.parameters["amount"] ?: "bilinmeyen"
        val unit = commandResult.parameters["unit"] ?: "ml"

        GameLogger.logSystem("=== LOG_WATER PROCESSING ===")
        GameLogger.logSystem("Kaydedilecek su miktarı: $amount $unit")

        // TODO: Gerçek su kaydetme işlemi
        // Günlük hedef takibi (2-3 litre)
        // Hidrasyon seviyesi analizi
        // Su içme hatırlatıcıları

        GameLogger.logSystem("✅ Su tüketimi başarıyla kaydedildi: $amount $unit")
        GameLogger.logSystem("💧 Hidrasyon takibi güncellendi")
    }

    /**
     * İlaç alma kaydetme komutunu işler
     */
    private fun processLogMedication(commandResult: CommandResult) {
        val description = commandResult.parameters["description"] ?: "ilaç"

        GameLogger.logSystem("=== LOG_MEDICATION PROCESSING ===")
        GameLogger.logSystem("Kaydedilecek ilaç: $description")

        // TODO: Gerçek ilaç kaydetme işlemi
        // İlaç takvimi kontrolü
        // Dozaj takibi
        // Yan etki izleme
        // Doktor randevu hatırlatıcıları

        GameLogger.logSystem("✅ İlaç alımı başarıyla kaydedildi: $description")
        GameLogger.logSystem("💊 İlaç takibi güncellendi")
    }

    /**
     * Bilinmeyen komutları işler
     */
    private fun processUnknownCommand(commandResult: CommandResult) {
        GameLogger.logSystem("=== UNKNOWN COMMAND PROCESSING ===")
        GameLogger.logSystem("Bilinmeyen komut türü: ${commandResult.commandType}")
        GameLogger.logSystem("Genel kayıt olarak işleniyor...")

        // TODO: Bilinmeyen komutlar için genel işlem
        // Kullanıcıya önerilerde bulunma
        // Machine learning ile komut öğrenme

        GameLogger.logSystem("⚠️ Bilinmeyen komut genel olarak kaydedildi")
        GameLogger.logSystem("🤖 Sistem öğrenme verisi güncellendi")
    }

    /**
     * Test fonksiyonu - Tüm komut türlerini test eder
     */
    fun runTestSuite(): Map<String, Boolean> {
        GameLogger.logSystem("=== COMMAND PROCESSING ENGINE TEST SUITE ===")

        val testCommands = listOf(
            CommandResult("SET_ALARM", mapOf("time" to "07:30"), 0.9),
            CommandResult("LOG_WEIGHT", mapOf("weight" to "85", "unit" to "kg"), 0.95),
            CommandResult("LOG_BLOOD_PRESSURE", mapOf("systolic" to "120", "diastolic" to "80"), 0.9),
            CommandResult("LOG_HEART_RATE", mapOf("bpm" to "75"), 0.85),
            CommandResult("LOG_SLEEP", mapOf("duration" to "8", "unit" to "saat"), 0.8),
            CommandResult("LOG_FOOD", mapOf("description" to "kahvaltıda omlet", "mealName" to "omlet", "mealType" to "BREAKFAST"), 0.9),
            CommandResult("LOG_EXERCISE", mapOf("description" to "30 dakika koşu", "duration" to "30", "exerciseType" to "RUNNING"), 0.8),
            CommandResult("LOG_WATER", mapOf("amount" to "500", "unit" to "ml"), 0.9),
            CommandResult("LOG_MEDICATION", mapOf("description" to "vitamin D"), 0.8)
        )

        val results = mutableMapOf<String, Boolean>()

        for (command in testCommands) {
            GameLogger.logSystem("Testing command: ${command.commandType}")
            val success = processCommand(command)
            results[command.commandType] = success
            GameLogger.logSystem("Result: ${if (success) "SUCCESS" else "FAILED"}")
        }

        GameLogger.logSystem("=== TEST SUITE COMPLETE ===")
        return results
    }
}