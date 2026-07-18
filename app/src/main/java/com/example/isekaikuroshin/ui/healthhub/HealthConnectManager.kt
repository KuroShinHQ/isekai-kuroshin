package com.example.isekaikuroshin.ui.healthhub

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "HealthConnectManager"
        private const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"
    }

    private var healthConnectClient: HealthConnectClient? = null
    private var permissionsLauncher: ManagedActivityResultLauncher<Set<String>, Set<String>>? = null

    val permissions = setOf(
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class)
    )

    fun initialize() {
        try {
            if (isHealthConnectAvailable()) {
                healthConnectClient = HealthConnectClient.getOrCreate(context)
                Log.d(TAG, "HealthConnectClient başarıyla oluşturuldu")
            } else {
                Log.w(TAG, "Health Connect bu cihazda mevcut değil")
            }
        } catch (e: Exception) {
            Log.e(TAG, "HealthConnectClient oluşturulamadı", e)
        }
    }

    private fun isHealthConnectAvailable(): Boolean {
        return try {
            val packageManager = context.packageManager
            packageManager.getPackageInfo(PROVIDER_PACKAGE_NAME, PackageManager.GET_ACTIVITIES)
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Health Connect provider yüklü değil")
            false
        }
    }

    fun setPermissionsLauncher(launcher: ManagedActivityResultLauncher<Set<String>, Set<String>>) {
        this.permissionsLauncher = launcher
        Log.d(TAG, "Permissions launcher ayarlandı")
    }

    suspend fun checkPermissions(): Boolean {
        return try {
            val client = healthConnectClient ?: return false
            val grantedPermissions = client.permissionController.getGrantedPermissions()
            val hasAllPermissions = permissions.all { it in grantedPermissions }
            Log.d(TAG, "İzin kontrolü - Tüm izinler verildi: $hasAllPermissions")
            hasAllPermissions
        } catch (e: Exception) {
            Log.e(TAG, "İzin kontrolü başarısız", e)
            false
        }
    }

    fun requestPermissions() {
        val launcher = permissionsLauncher
        if (launcher != null) {
            Log.d(TAG, "Health Connect izinleri isteniyor...")
            launcher.launch(permissions)
        } else {
            Log.e(TAG, "Permissions launcher ayarlanmamış!")
        }
    }

    suspend fun writeWeight(weightInKg: Double): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val client = healthConnectClient ?: run {
                    Log.e(TAG, "HealthConnectClient başlatılmamış")
                    return@withContext false
                }

                if (!checkPermissions()) {
                    Log.w(TAG, "Kilo yazma izni verilmemiş")
                    return@withContext false
                }

                val now = Instant.now()
                val weightRecord = WeightRecord(
                    weight = Mass.kilograms(weightInKg),
                    time = now,
                    zoneOffset = ZoneOffset.systemDefault().rules.getOffset(now)
                )

                client.insertRecords(listOf(weightRecord))
                Log.d(TAG, "Kilo verisi başarıyla Health Connect'e yazıldı: ${weightInKg}kg")

                // G83 P4: EventLogger entegrasyonu - Weight logging
                com.example.isekaikuroshin.utils.EventLogger.logWeight(weightInKg)

                true
            } catch (e: Exception) {
                Log.e(TAG, "Kilo verisi yazılamadı", e)
                false
            }
        }
    }

    suspend fun readWeightHistory(days: Int = 30): List<WeightRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val client = healthConnectClient ?: run {
                    Log.e(TAG, "TODO-HUB-04: HealthConnectClient başlatılmamış")
                    return@withContext emptyList()
                }

                if (!checkPermissions()) {
                    Log.w(TAG, "TODO-HUB-04: Kilo okuma izni verilmemiş, izin isteniyor...")
                    return@withContext emptyList()
                }

                val endTime = Instant.now()
                val startTime = endTime.minusSeconds(days * 24 * 60 * 60L)

                val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                val request = ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = timeRangeFilter
                )

                val response = client.readRecords(request)
                Log.d(TAG, "TODO-HUB-04: ${response.records.size} adet kilo kaydı okundu (son $days gün)")
                response.records.sortedByDescending { it.time }
            } catch (e: Exception) {
                Log.e(TAG, "TODO-HUB-04: Kilo geçmişi okunamadı", e)
                emptyList()
            }
        }
    }

    suspend fun getLatestWeight(): Double? {
        return withContext(Dispatchers.IO) {
            try {
                val client = healthConnectClient ?: return@withContext null

                if (!checkPermissions()) {
                    Log.w(TAG, "Kilo okuma izni verilmemiş")
                    return@withContext null
                }

                // Bu fonksiyon sonra implement edilecek
                // Şimdilik sadece yazma odaklıyız
                null
            } catch (e: Exception) {
                Log.e(TAG, "Kilo verisi okunamadı", e)
                null
            }
        }
    }

    suspend fun writeExerciseSession(
        exerciseType: Int,
        startTime: Instant,
        duration: Duration
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val client = healthConnectClient ?: run {
                    Log.e(TAG, "TODO-HUB-06: HealthConnectClient başlatılmamış")
                    return@withContext false
                }

                if (!checkPermissions()) {
                    Log.w(TAG, "TODO-HUB-06: Egzersiz yazma izni verilmemiş")
                    return@withContext false
                }

                val endTime = startTime.plus(duration)
                val exerciseSessionRecord = ExerciseSessionRecord(
                    exerciseType = exerciseType,
                    title = "Egzersiz",
                    startTime = startTime,
                    startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(startTime),
                    endTime = endTime,
                    endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(endTime)
                )

                client.insertRecords(listOf(exerciseSessionRecord))
                Log.d(TAG, "TODO-HUB-06: Egzersiz verisi başarıyla Health Connect'e yazıldı: $exerciseType, ${duration.toMinutes()} dakika")

                // G83 P4: EventLogger entegrasyonu - Exercise session logging
                com.example.isekaikuroshin.utils.EventLogger.logExerciseSession(exerciseType, duration.toMinutes())

                true
            } catch (e: Exception) {
                Log.e(TAG, "TODO-HUB-06: Egzersiz verisi yazılamadı", e)
                false
            }
        }
    }

    suspend fun readExerciseHistory(days: Int = 7): List<ExerciseSessionRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val client = healthConnectClient ?: run {
                    Log.e(TAG, "TODO-HUB-07: HealthConnectClient başlatılmamış")
                    return@withContext emptyList()
                }

                if (!checkPermissions()) {
                    Log.w(TAG, "TODO-HUB-07: Egzersiz okuma izni verilmemiş, izin isteniyor...")
                    return@withContext emptyList()
                }

                val endTime = Instant.now()
                val startTime = endTime.minusSeconds(days * 24 * 60 * 60L)

                val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                val request = ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = timeRangeFilter
                )

                val response = client.readRecords(request)
                Log.d(TAG, "TODO-HUB-07: ${response.records.size} adet egzersiz kaydı okundu (son $days gün)")
                response.records.sortedByDescending { it.startTime }
            } catch (e: Exception) {
                Log.e(TAG, "TODO-HUB-07: Egzersiz geçmişi okunamadı", e)
                emptyList()
            }
        }
    }

    /**
     * TODO-HUB-09: Beslenme verisi yazmak için
     * @param mealName Yemek adı (örn: "yulaf ezmesi", "2 yumurta")
     * @param mealType Öğün türü (BREAKFAST, LUNCH, DINNER, SNACK)
     */
    suspend fun writeNutrition(mealName: String, mealType: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val client = healthConnectClient ?: run {
                    Log.e(TAG, "TODO-HUB-09: HealthConnectClient başlatılmamış")
                    return@withContext false
                }

                if (!checkPermissions()) {
                    Log.w(TAG, "TODO-HUB-09: Beslenme yazma izni verilmemiş")
                    return@withContext false
                }

                val now = Instant.now()

                // Basit bir NutritionRecord oluştur
                // Bu aşamada tam besin veritabanı entegrasyonu yapmıyoruz
                val nutritionRecord = NutritionRecord(
                    name = mealName, // Yemek adı
                    mealType = mapMealType(mealType), // Öğün türü
                    startTime = now,
                    startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(now),
                    endTime = now,
                    endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(now)
                    // Kalori, protein vb. bilgiler şimdilik boş bırakılıyor
                )

                client.insertRecords(listOf(nutritionRecord))
                Log.d(TAG, "TODO-HUB-09: Beslenme verisi başarıyla Health Connect'e yazıldı: $mealName ($mealType)")

                // G83 P4: EventLogger entegrasyonu - Nutrition logging
                com.example.isekaikuroshin.utils.EventLogger.logNutrition(mealName, mealType)

                true
            } catch (e: Exception) {
                Log.e(TAG, "TODO-HUB-09: Beslenme verisi yazılamadı", e)
                false
            }
        }
    }

    /**
     * Öğün türü string'ini NutritionRecord.MealType'a çevirir
     */
    private fun mapMealType(mealType: String): Int {
        return when (mealType.uppercase()) {
            "BREAKFAST" -> 1 // MEAL_TYPE_BREAKFAST
            "LUNCH" -> 2 // MEAL_TYPE_LUNCH
            "DINNER" -> 3 // MEAL_TYPE_DINNER
            "SNACK" -> 4 // MEAL_TYPE_SNACK
            else -> 0 // MEAL_TYPE_UNKNOWN
        }
    }

    /**
     * TODO-HUB-10: Beslenme geçmişini okumak için
     * @param startTime Başlangıç zamanı
     * @param endTime Bitiş zamanı
     * @return Belirtilen zaman aralığındaki beslenme kayıtları
     */
    suspend fun readNutritionHistory(startTime: Instant, endTime: Instant): List<NutritionRecord> {
        return withContext(Dispatchers.IO) {
            try {
                val client = healthConnectClient ?: run {
                    Log.e(TAG, "TODO-HUB-10: HealthConnectClient başlatılmamış")
                    return@withContext emptyList()
                }

                if (!checkPermissions()) {
                    Log.w(TAG, "TODO-HUB-10: Beslenme okuma izni verilmemiş, izin isteniyor...")
                    return@withContext emptyList()
                }

                val timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                val request = ReadRecordsRequest(
                    recordType = NutritionRecord::class,
                    timeRangeFilter = timeRangeFilter
                )

                val response = client.readRecords(request)
                Log.d(TAG, "TODO-HUB-10: ${response.records.size} adet beslenme kaydı okundu")
                response.records.sortedBy { it.startTime }
            } catch (e: Exception) {
                Log.e(TAG, "TODO-HUB-10: Beslenme geçmişi okunamadı", e)
                emptyList()
            }
        }
    }

    /**
     * Öğün türü ID'sini açıklayıcı string'e çevirir
     */
    fun getMealTypeName(mealType: Int): String {
        return when (mealType) {
            1 -> "Kahvaltı"
            2 -> "Öğle Yemeği"
            3 -> "Akşam Yemeği"
            4 -> "Atıştırmalık"
            else -> "Diğer"
        }
    }

    /**
     * Egzersiz tipi string'ini ExerciseSessionRecord.ExerciseType'a çevirir
     */
    fun mapExerciseType(typeString: String): Int {
        return when (typeString.uppercase()) {
            "RUNNING" -> 56 // EXERCISE_TYPE_RUNNING
            "WALKING" -> 79 // EXERCISE_TYPE_WALKING
            "CYCLING" -> 8 // EXERCISE_TYPE_BIKING
            "SWIMMING" -> 71 // EXERCISE_TYPE_SWIMMING
            "FITNESS" -> 65 // EXERCISE_TYPE_STRENGTH_TRAINING
            "YOGA" -> 86 // EXERCISE_TYPE_YOGA
            "PILATES" -> 52 // EXERCISE_TYPE_PILATES
            "FOOTBALL" -> 27 // EXERCISE_TYPE_FOOTBALL_AMERICAN
            "BASKETBALL" -> 6 // EXERCISE_TYPE_BASKETBALL
            "TENNIS" -> 73 // EXERCISE_TYPE_TENNIS
            else -> 0 // EXERCISE_TYPE_UNKNOWN
        }
    }
}