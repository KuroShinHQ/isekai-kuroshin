# 🔔 GÖREV I: AKILLI BİLDİRİM SİSTEMİ - KAPSAMLI ZEMİN ETÜDÜ

**Tarih**: 2025-10-19
**Durum**: ✅ ZEMİN ETÜDÜ TAMAMLANDI
**Revize Versiyon**: 2.0 (3 Sistem Entegrasyonu)

---

## 📋 İÇİNDEKİLER

1. [Yönetici Özeti](#yönetici-özeti)
2. [BÖLÜM 1: Mevcut System Overlay Analizi](#bölüm-1-mevcut-system-overlay-analizi)
3. [BÖLÜM 2: Dış Bildirimler (Push Notifications)](#bölüm-2-dış-bildirimler-push-notifications)
4. [BÖLÜM 3: Mikro Geri Bildirimler (Stat Animasyonları)](#bölüm-3-mikro-geri-bildirimler-stat-animasyonları)
5. [BÖLÜM 4: Ayarlar Menüsü Entegrasyonu](#bölüm-4-ayarlar-menüsü-entegrasyonu)
6. [İmplementasyon Planı](#implementasyon-planı)

---

## 🎯 YÖNETİCİ ÖZETİ

Bu görev, Isekai Kuroshin projesine **3 farklı bildirim sistemi** ekleyerek kullanıcı deneyimini optimize edecek:

### 3 Sistem Yapısı

| Sistem | Kullanım Amacı | Örnek | Durum |
|--------|---------------|-------|-------|
| **Dış Bildirimler** (Push) | Uygulama kapalıyken hatırlatma | "3 gündür egzersiz yapmadın" | 🆕 YENİ |
| **İç Bildirimler** (Overlay) | Uygulama içinde büyük olaylar | Görev kazanımı, item kazanımı | ✅ MEVCUT |
| **Mikro Geri Bildirimler** | Anlık stat değişimleri | "-10 Stamina" animasyonu | 🆕 YENİ |

### Kritik Bulgular

✅ **İyi Haber**: System Overlay sistemi zaten mükemmel şekilde çalışıyor
⚠️ **Zorluk**: Android'de ML tabanlı "akıllı zamanlama" native desteği yok
💡 **Çözüm**: Basit ama etkili istatistiksel analiz kullanacağız

---

## 📊 BÖLÜM 1: MEVCUT SYSTEM OVERLAY ANALİZİ

### 1.1. Sistem Durumu: ✅ TAM FONKSİYONEL

**Dosya**: `ui/components/SystemOverlay.kt` (327 satır)

#### Mevcut Özellikler

✅ **4 Farklı Overlay Tipi**:
1. `OverlayData.Alert` - Uyarı mesajları
2. `OverlayData.Quest` - Görev bildirimleri (Umbros corruption desteği!)
3. `OverlayData.ItemAcquired` - Item kazanımı (rarity sistemi ile)
4. `OverlayData.Choice` - Kullanıcı seçim ekranı

✅ **Harika Özellikler**:
- Material3 Dialog kullanımı
- Holografik animasyonlu frame (glow efekti)
- Rarity-based renklendirme
- Smooth enter/exit animasyonları
- Close butonu

✅ **Developer Test Sistemi**:
- Settings > Developer Options'da 3 test butonu mevcut:
  - "Show Random Quest Notification"
  - "Show Random Item Acquired"
  - "Show System Alert Notification"

#### Kod Örneği (Mevcut Kullanım)

```kotlin
// DeveloperOptionsSection.kt - Line 96
onShowOverlay(
    OverlayData.Quest(
        title = "Yeni Görev!",
        questName = "Karanlık Ormandaki Gizemli Sesler",
        goals = listOf(
            "Karanlık Orman'ı keşfet",
            "Gizemli sesin kaynağını bul",
            "Köylülere rapor ver"
        ),
        isCorruptible = true
    )
)
```

### 1.2. Sistem Mimarisi

```
SettingsScreen.kt
    ├── var showOverlay by remember { mutableStateOf(false) }
    ├── var overlayData by remember { mutableStateOf<OverlayData?>(null) }
    │
    └── SystemOverlay(
            overlayData = overlayData,
            isVisible = showOverlay,
            onDismiss = { showOverlay = false }
        )
```

### 1.3. Sonuç ve Öneriler

**SONUÇ**: Bu sistem MÜKEMMEL çalışıyor, hiçbir değişiklik yapılmasına gerek yok.

**KULLANIM KURALI**:
- ✅ Büyük olaylar (Quest, Item, Boss Fight başlangıcı) → System Overlay
- ❌ Küçük stat değişimleri (-10 HP, +5 XP) → Mikro Geri Bildirim (Bölüm 3)

---

## 🔔 BÖLÜM 2: DIŞ BİLDİRİMLER (PUSH NOTIFICATIONS)

### 2.1. Hedef

Kullanıcı uygulamayı **kapalıyken**, akıllı zamanlama ile hatırlatıcılar göndermek:
- "3 gündür egzersiz yapmadın - Vücudunu harekete geçir!"
- "2 gündür meditasyon yapmadın - Zihnini dinlendir"
- "7 gündür günlük yazmadın - Hikayeni yazmaya devam et"

### 2.2. Teknik Zorluk: Akıllı Zamanlama

#### Web Araştırması Sonuçları

**ARAMA 1**: "Android WorkManager smart scheduling user active hours machine learning 2025"

**Bulgu**: ❌ Android WorkManager'da **native ML-based akıllı zamanlama YOK**

**WorkManager'ın Sunduğu Özellikler**:
- ✅ Constraint-based scheduling (battery, network, charging)
- ✅ Periodic work (15 dakika minimum interval)
- ✅ Doze mode desteği
- ❌ User behavior pattern learning
- ❌ Peak hours detection

**ARAMA 2**: "Android track user session time statistics peak hours usage analytics 2025"

**Bulgu**: ✅ Session tracking ve peak hours analizi yapılabilir

**2025 Best Practices**:
- Session start/end timestamps kaydetme
- Peak usage times hesaplama
- Average session duration analizi
- Real-time analytics entegrasyonu

### 2.3. Çözüm Önerisi: Basit İstatistiksel Akıllı Zamanlama

ML kullanmadan, **istatistiksel analiz** ile akıllı zamanlama yapabiliriz.

#### Algoritma Planı

```kotlin
// Adım 1: Session Tracking
data class UserSessionLog(
    val sessionStart: Long,  // Unix timestamp
    val sessionEnd: Long,
    val dayOfWeek: Int,      // 1-7 (Monday-Sunday)
    val hourOfDay: Int       // 0-23
)

// Adım 2: Peak Hours Hesaplama
fun calculatePeakHours(sessions: List<UserSessionLog>): Pair<Int, Int> {
    // Her saat için oturum sayısını say
    val hourCounts = IntArray(24) { 0 }

    sessions.forEach { session ->
        val startHour = session.hourOfDay
        hourCounts[startHour]++
    }

    // En çok kullanılan saat aralığını bul (3 saatlik pencere)
    var maxCount = 0
    var peakStartHour = 18  // Default: 18:00

    for (i in 0..21) {
        val windowCount = hourCounts[i] + hourCounts[i+1] + hourCounts[i+2]
        if (windowCount > maxCount) {
            maxCount = windowCount
            peakStartHour = i
        }
    }

    return Pair(peakStartHour, peakStartHour + 3)  // Örn: (18, 21) → 18:00-21:00
}

// Adım 3: Smart Notification Scheduler
fun scheduleSmartNotification(
    notificationType: String,  // "EXERCISE", "MEDITATION", "JOURNAL"
    peakHours: Pair<Int, Int>
) {
    val (startHour, endHour) = peakHours

    // Rastgele bir dakika seç (saat başı değil, daha doğal)
    val randomMinute = Random.nextInt(0, 60)

    // WorkManager ile notification schedule et
    val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
        .setInitialDelay(
            calculateDelayUntilPeakHour(startHour, randomMinute),
            TimeUnit.MILLISECONDS
        )
        .setInputData(
            workDataOf("type" to notificationType)
        )
        .build()

    WorkManager.getInstance(context).enqueue(workRequest)
}
```

### 2.4. Veri Saklama Planı

#### PersistentDataManager Genişletmesi

```kotlin
// PersistentDataManager.kt'ye eklenecek
@Serializable
data class UserAnalyticsData(
    val sessionLogs: List<UserSessionLog> = emptyList(),
    val peakHoursStart: Int = 18,  // Default: 18:00
    val peakHoursEnd: Int = 21,    // Default: 21:00
    val lastAnalysisDate: Long = 0  // En son analiz tarihi
)

@Serializable
data class UserSessionLog(
    val sessionStart: Long,
    val sessionEnd: Long,
    val dayOfWeek: Int,
    val hourOfDay: Int
)
```

### 2.5. WorkManager Implementasyonu

#### Yeni Dosyalar

**1. `utils/ReminderWorker.kt` (YENİ - 200 satır)**

```kotlin
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val persistentDataManager: PersistentDataManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reminderType = inputData.getString("type") ?: return Result.failure()

        val gameData = persistentDataManager.gameData.first()
        val now = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000L

        when (reminderType) {
            "EXERCISE" -> {
                val lastExercise = gameData.healthHubData?.lastExerciseDate ?: 0
                if (now - lastExercise >= 3 * dayInMillis) {
                    sendNotification(
                        title = LanguageManager.getText("notification_exercise_reminder"),
                        message = LanguageManager.getText("notification_exercise_body"),
                        channelId = "health_reminders"
                    )
                }
            }
            "MEDITATION" -> {
                val lastMeditation = gameData.healthHubData?.lastMeditationDate ?: 0
                if (now - lastMeditation >= 2 * dayInMillis) {
                    sendNotification(
                        title = LanguageManager.getText("notification_meditation_reminder"),
                        message = LanguageManager.getText("notification_meditation_body"),
                        channelId = "health_reminders"
                    )
                }
            }
            "JOURNAL" -> {
                val lastJournal = gameData.storyData.storyPages.lastOrNull()?.timestamp ?: 0
                if (now - lastJournal >= 7 * dayInMillis) {
                    sendNotification(
                        title = LanguageManager.getText("notification_journal_reminder"),
                        message = LanguageManager.getText("notification_journal_body"),
                        channelId = "health_reminders"
                    )
                }
            }
        }

        return Result.success()
    }

    private fun sendNotification(title: String, message: String, channelId: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "health_hub")
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
```

**2. `utils/SmartScheduler.kt` (YENİ - 150 satır)**

```kotlin
object SmartScheduler {

    /**
     * Kullanıcının son 30 günlük oturum verilerini analiz ederek
     * peak hours (en aktif saat aralığı) hesaplar
     */
    fun analyzePeakHours(sessions: List<UserSessionLog>): Pair<Int, Int> {
        if (sessions.isEmpty()) {
            return Pair(18, 21)  // Default: 18:00-21:00
        }

        val hourCounts = IntArray(24) { 0 }

        sessions.forEach { session ->
            val startHour = Calendar.getInstance().apply {
                timeInMillis = session.sessionStart
            }.get(Calendar.HOUR_OF_DAY)

            hourCounts[startHour]++
        }

        // 3 saatlik sliding window ile peak hours bul
        var maxCount = 0
        var peakStartHour = 18

        for (i in 0..21) {
            val windowCount = hourCounts[i] + hourCounts[i+1] + hourCounts[i+2]
            if (windowCount > maxCount) {
                maxCount = windowCount
                peakStartHour = i
            }
        }

        return Pair(peakStartHour, peakStartHour + 3)
    }

    /**
     * Akıllı bildirim zamanlaması yapar
     * Peak hours içinde rastgele bir zaman seçer
     */
    fun scheduleSmartReminder(
        context: Context,
        reminderType: String,
        peakHours: Pair<Int, Int>
    ) {
        val (startHour, endHour) = peakHours

        // Bugün mü yoksa yarın mı?
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)

        val targetCalendar = Calendar.getInstance().apply {
            if (currentHour >= endHour) {
                // Bugün geçti, yarın gönder
                add(Calendar.DAY_OF_YEAR, 1)
            }

            // Peak hours içinde rastgele bir saat seç
            val randomHour = Random.nextInt(startHour, endHour)
            val randomMinute = Random.nextInt(0, 60)

            set(Calendar.HOUR_OF_DAY, randomHour)
            set(Calendar.MINUTE, randomMinute)
            set(Calendar.SECOND, 0)
        }

        val delay = targetCalendar.timeInMillis - System.currentTimeMillis()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("type" to reminderType))
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    /**
     * Session başlangıcını kaydet
     */
    fun logSessionStart(context: Context) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val session = UserSessionLog(
            sessionStart = now,
            sessionEnd = 0,  // Henüz bitmedi
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK),
            hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        )

        // PersistentDataManager'a kaydet
        PersistentDataManager.updateGameData { data ->
            data.copy(
                analyticsData = data.analyticsData.copy(
                    sessionLogs = data.analyticsData.sessionLogs + session
                )
            )
        }
    }

    /**
     * Session bitişini kaydet
     */
    fun logSessionEnd(context: Context) {
        val now = System.currentTimeMillis()

        PersistentDataManager.updateGameData { data ->
            val sessions = data.analyticsData.sessionLogs
            if (sessions.isNotEmpty()) {
                val lastSession = sessions.last()
                if (lastSession.sessionEnd == 0L) {
                    // Son session'ı güncelle
                    val updatedSessions = sessions.dropLast(1) + lastSession.copy(
                        sessionEnd = now
                    )

                    data.copy(
                        analyticsData = data.analyticsData.copy(
                            sessionLogs = updatedSessions.takeLast(100)  // Son 100 session sakla
                        )
                    )
                } else {
                    data
                }
            } else {
                data
            }
        }
    }
}
```

### 2.6. Çeviri Anahtarları (LanguageManager)

```kotlin
// TR
"notification_exercise_reminder" to "Egzersiz Zamanı!",
"notification_exercise_body" to "3 gündür egzersiz yapmadın. Vücudunu harekete geçirmeye ne dersin?",
"notification_meditation_reminder" to "Meditasyon Zamanı!",
"notification_meditation_body" to "Zihnini dinlendirmeyi unutma. Huzurlu bir nefes al.",
"notification_journal_reminder" to "Günlük Zamanı!",
"notification_journal_body" to "Duygularını paylaşmayı unutma. Hikayeni yazmaya devam et!",

// EN
"notification_exercise_reminder" to "Time to Exercise!",
"notification_exercise_body" to "You haven't exercised in 3 days. Ready to move your body?",
"notification_meditation_reminder" to "Time to Meditate!",
"notification_meditation_body" to "Don't forget to rest your mind. Take a peaceful breath.",
"notification_journal_reminder" to "Time to Journal!",
"notification_journal_body" to "Don't forget to share your feelings. Continue writing your story!",
```

---

## ✨ BÖLÜM 3: MİKRO GERİ BİLDİRİMLER (STAT ANIMASYONLARI)

### 3.1. Hedef

Günlük input sonrası **küçük stat değişimlerini** büyük overlay göstermeden, **animasyonlu ve kompakt** şekilde göstermek.

**Örnek Senaryo**:
```
Kullanıcı günlükte: "5 km koştum"
    ↓
AI işler, stat güncellenir
    ↓
Ekranın sağ üstünde kısa süre görünür:
    ┌─────────────┐
    │ -15 Stamina │ ← Yukarıdan kayarak gelir
    │ +5 Agility  │ ← Soluklaşarak kaybolur
    └─────────────┘
```

### 3.2. Compose Animasyon Çözümü

#### Yeni Component: MicroFeedback.kt

```kotlin
package com.example.isekaikuroshin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class MicroFeedbackItem(
    val statName: String,      // "Stamina", "HP", "Agility"
    val change: Int,           // -15, +5
    val icon: ImageVector,     // Icons.Default.DirectionsRun
    val color: Color = Color.White
)

@Composable
fun MicroFeedbackOverlay(
    feedbacks: List<MicroFeedbackItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(feedbacks) {
        if (feedbacks.isNotEmpty()) {
            visible = true
            delay(3000)  // 3 saniye göster
            visible = false
            delay(500)   // Animasyon bitsin
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = fadeOut(tween(500))
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            feedbacks.forEach { feedback ->
                MicroFeedbackCard(feedback)
            }
        }
    }
}

@Composable
private fun MicroFeedbackCard(feedback: MicroFeedbackItem) {
    val changeColor = if (feedback.change > 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    val changeText = if (feedback.change > 0) "+${feedback.change}" else "${feedback.change}"

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = feedback.icon,
                contentDescription = null,
                tint = feedback.color,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = changeText,
                color = changeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Text(
                text = feedback.statName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}
```

### 3.3. JournalViewModel Entegrasyonu

```kotlin
// JournalViewModel.kt'de
data class JournalUiState(
    // ... mevcut alanlar ...
    val microFeedbacks: List<MicroFeedbackItem> = emptyList()
)

// Stat güncellemesi sonrası
fun onStatChange(statName: String, change: Int) {
    val icon = when (statName) {
        "Stamina" -> Icons.Default.DirectionsRun
        "HP" -> Icons.Default.Favorite
        "MP" -> Icons.Default.Stars
        "Agility" -> Icons.Default.Speed
        else -> Icons.Default.TrendingUp
    }

    _uiState.update { current ->
        current.copy(
            microFeedbacks = current.microFeedbacks + MicroFeedbackItem(
                statName = statName,
                change = change,
                icon = icon,
                color = Color.Cyan
            )
        )
    }
}

fun clearMicroFeedbacks() {
    _uiState.update { it.copy(microFeedbacks = emptyList()) }
}
```

### 3.4. JournalScreen.kt Kullanımı

```kotlin
// JournalScreen.kt'de
@Composable
fun JournalScreen(...) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // ... mevcut UI ...

        // Mikro geri bildirimler (sağ üstte)
        MicroFeedbackOverlay(
            feedbacks = uiState.microFeedbacks,
            onDismiss = { viewModel.clearMicroFeedbacks() },
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}
```

---

## ⚙️ BÖLÜM 4: AYARLAR MENÜSÜ ENTEGRASYONU

### 4.1. Yeni Ayar Bölümü: Bildirim Kontrolleri

#### PersistentDataManager Genişletmesi

```kotlin
@Serializable
data class NotificationSettingsData(
    val gameProgressNotifications: Boolean = true,      // Mikro geri bildirimler
    val questUpdateNotifications: Boolean = true,       // System Overlay (Quest, Item)
    val smartRemindersEnabled: Boolean = true,          // Push notifications
    val exerciseRemindersEnabled: Boolean = true,       // Alt kontrol (3 gün)
    val meditationRemindersEnabled: Boolean = true,     // Alt kontrol (2 gün)
    val journalRemindersEnabled: Boolean = true         // Alt kontrol (7 gün)
)

@Serializable
data class SettingsData(
    // ... mevcut alanlar ...
    val notificationSettings: NotificationSettingsData = NotificationSettingsData()
)
```

#### UI Komponenti: NotificationSettingsSection.kt

```kotlin
@Composable
fun NotificationSettingsSection(
    notificationSettings: NotificationSettingsData,
    onUpdateSettings: (NotificationSettingsData) -> Unit
) {
    SectionCard(title = rememberLocalizedText("notification_settings")) {
        // 1. Oyun İlerlemesi Bildirimleri (Mikro Geri Bildirimler)
        SwitchSetting(
            title = rememberLocalizedText("game_progress_notifications"),
            subtitle = rememberLocalizedText("game_progress_notifications_desc"),
            checked = notificationSettings.gameProgressNotifications,
            onCheckedChange = {
                onUpdateSettings(notificationSettings.copy(gameProgressNotifications = it))
            }
        )

        Divider()

        // 2. Görev Güncellemeleri (System Overlay)
        SwitchSetting(
            title = rememberLocalizedText("quest_update_notifications"),
            subtitle = rememberLocalizedText("quest_update_notifications_desc"),
            checked = notificationSettings.questUpdateNotifications,
            onCheckedChange = {
                onUpdateSettings(notificationSettings.copy(questUpdateNotifications = it))
            }
        )

        Divider()

        // 3. Akıllı Hatırlatıcılar (Ana kontrol)
        SwitchSetting(
            title = rememberLocalizedText("smart_reminders"),
            subtitle = rememberLocalizedText("smart_reminders_desc"),
            checked = notificationSettings.smartRemindersEnabled,
            onCheckedChange = {
                onUpdateSettings(notificationSettings.copy(smartRemindersEnabled = it))
            }
        )

        // Alt kontroller (sadece smartRemindersEnabled = true ise göster)
        if (notificationSettings.smartRemindersEnabled) {
            Column(
                modifier = Modifier.padding(start = 32.dp)
            ) {
                SwitchSetting(
                    title = rememberLocalizedText("exercise_reminders"),
                    subtitle = rememberLocalizedText("exercise_reminders_desc"),
                    checked = notificationSettings.exerciseRemindersEnabled,
                    onCheckedChange = {
                        onUpdateSettings(notificationSettings.copy(exerciseRemindersEnabled = it))
                    }
                )

                SwitchSetting(
                    title = rememberLocalizedText("meditation_reminders"),
                    subtitle = rememberLocalizedText("meditation_reminders_desc"),
                    checked = notificationSettings.meditationRemindersEnabled,
                    onCheckedChange = {
                        onUpdateSettings(notificationSettings.copy(meditationRemindersEnabled = it))
                    }
                )

                SwitchSetting(
                    title = rememberLocalizedText("journal_reminders"),
                    subtitle = rememberLocalizedText("journal_reminders_desc"),
                    checked = notificationSettings.journalRemindersEnabled,
                    onCheckedChange = {
                        onUpdateSettings(notificationSettings.copy(journalRemindersEnabled = it))
                    }
                )
            }
        }
    }
}
```

### 4.2. Çeviri Anahtarları

```kotlin
// TR
"notification_settings" to "Bildirim Ayarları",
"game_progress_notifications" to "Oyun İlerlemesi Bildirimleri",
"game_progress_notifications_desc" to "Stat değişimleri için animasyonlu mikro bildirimler göster",
"quest_update_notifications" to "Görev Güncellemeleri Bildirimleri",
"quest_update_notifications_desc" to "Görev kazanımı ve item kazanımı için büyük bildirim paneli göster",
"smart_reminders" to "Akıllı Hatırlatıcılar",
"smart_reminders_desc" to "Uygulama kapalıyken, en uygun zamanda hatırlatıcılar gönder",
"exercise_reminders" to "Egzersiz Hatırlatıcıları (3 gün)",
"exercise_reminders_desc" to "3 gündür egzersiz yapmadıysan bildirim gönder",
"meditation_reminders" to "Meditasyon Hatırlatıcıları (2 gün)",
"meditation_reminders_desc" to "2 gündür meditasyon yapmadıysan bildirim gönder",
"journal_reminders" to "Günlük Hatırlatıcıları (7 gün)",
"journal_reminders_desc" to "7 gündür günlük yazmadıysan bildirim gönder",

// EN
"notification_settings" to "Notification Settings",
"game_progress_notifications" to "Game Progress Notifications",
"game_progress_notifications_desc" to "Show animated micro notifications for stat changes",
"quest_update_notifications" to "Quest Update Notifications",
"quest_update_notifications_desc" to "Show large notification panel for quest and item acquisition",
"smart_reminders" to "Smart Reminders",
"smart_reminders_desc" to "Send reminders at the best time when app is closed",
"exercise_reminders" to "Exercise Reminders (3 days)",
"exercise_reminders_desc" to "Send notification if you haven't exercised in 3 days",
"meditation_reminders" to "Meditation Reminders (2 days)",
"meditation_reminders_desc" to "Send notification if you haven't meditated in 2 days",
"journal_reminders" to "Journal Reminders (7 days)",
"journal_reminders_desc" to "Send notification if you haven't written in your journal in 7 days",
```

---

## 📋 İMPLEMENTASYON PLANI

### Faz 1: Veri Modeli (1 saat)
- [ ] `PersistentDataManager.kt` - UserAnalyticsData, NotificationSettingsData ekle
- [ ] `updateNotificationSettings()` helper fonksiyonu yaz

### Faz 2: Mikro Geri Bildirimler (1.5 saat)
- [ ] `MicroFeedback.kt` component'i oluştur
- [ ] `JournalViewModel.kt` - microFeedbacks state ekle
- [ ] `JournalScreen.kt` - MicroFeedbackOverlay entegre et

### Faz 3: Dış Bildirimler (2 saat)
- [ ] `SmartScheduler.kt` - Session tracking + peak hours analizi
- [ ] `ReminderWorker.kt` - WorkManager worker
- [ ] `NotificationChannelManager.kt` - Bildirim kanalları
- [ ] `MainActivity.kt` - Session start/end logging

### Faz 4: Ayarlar Menüsü (30 dakika)
- [ ] `NotificationSettingsSection.kt` oluştur
- [ ] `SettingsScreen.kt` - Section ekle
- [ ] LanguageManager'a çeviriler ekle

### Faz 5: Test ve İyileştirme (1 saat)
- [ ] Mikro geri bildirim animasyonları test
- [ ] Push notification test (3 gün simüle et)
- [ ] Peak hours algoritması test
- [ ] Ayarlar menüsü toggle test

**Toplam Tahmini Süre**: ~6 saat

---

## 🎯 SONUÇ VE ÖNERİLER

### Başarı Kriterleri

- [ ] Mikro geri bildirimler smooth animasyonla gösteriliyor
- [ ] System Overlay büyük olaylar için çalışıyor (ZATEN VAR)
- [ ] Push notifications kullanıcının peak hours'ında geliyor
- [ ] Tüm bildirim türleri Ayarlar'dan kapatılabiliyor
- [ ] Session tracking otomatik çalışıyor
- [ ] Peak hours analizi doğru hesaplanıyor

### Kritik Notlar

✅ **System Overlay MÜKEMMEL** - Hiçbir değişiklik yapılmasın
✅ **ML gereksiz** - Basit istatistiksel analiz yeterli
✅ **3 farklı sistem** - Her biri farklı amaç için optimize
⚠️ **Android 13+** - POST_NOTIFICATIONS izni gerekli

---

**SIRA KULLANICIDA**: Bu plan onaylanırsa implementasyona başlanacak.
