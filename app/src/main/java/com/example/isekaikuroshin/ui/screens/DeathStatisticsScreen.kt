package com.example.isekaikuroshin.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.ai.GlobalAIManager
import com.example.isekaikuroshin.data.DeathRecord
import com.example.isekaikuroshin.data.PersistentGameData
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.game.GameStateManager
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.system.exitProcess

/**
 * G140: Hilt EntryPoint for GameStateManager injection
 * Composable fonksiyonlarda @Inject constructor kullanılamadığı için
 * EntryPoint ile Context üzerinden inject ediyoruz
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface GameStateManagerEntryPoint {
    fun gameStateManager(): GameStateManager
}

/**
 * Death Statistics Screen - AI-Powered Ölüm Raporu Ekranı
 *
 * Özellikler:
 * - AI-generated death report (ölüm raporu)
 * - Badge/Ünvan sistemi
 * - Achievements/Yetenekler
 * - Hikaye özeti analizi
 * - Kopyalama, ekran görüntüsü, paylaşma
 * - Background: DEATH_STATISTICS etiketli foto/video
 */
@Composable
fun DeathStatisticsScreen(
    dynamicVideoIds: List<Int> = emptyList(),
    dynamicPhotoIds: List<Int> = emptyList(),
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current

    // G140: GameStateManager injection via Hilt EntryPoint
    val gameStateManager = remember {
        val appContext = context.applicationContext as android.app.Application
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            appContext,
            GameStateManagerEntryPoint::class.java
        ).gameStateManager()
    }
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val gameData by PersistentDataManager.gameData.collectAsState()
    val deathArchive = gameData.deathArchive
    val lastDeath = deathArchive.lastOrNull()

    // AI State
    var aiReport by remember { mutableStateOf("") }
    var isAIGenerating by remember { mutableStateOf(false) }
    var badges by remember { mutableStateOf<List<String>>(emptyList()) }

    // Background medya
    val backgroundVideos = remember(dynamicVideoIds) {
        if (dynamicVideoIds.isNotEmpty()) {
            dynamicVideoIds
        } else {
            // Fallback: DEATH_STATISTICS ekranı için MediaDatabase'den video
            com.example.isekaikuroshin.engine.MediaDatabaseHelper.getMediaForScreen(
                screenType = "DEATH_TRANSITION",
                depth = "D1"
            ).filter { it.fileName.startsWith("v_") }
             .map { it.resourceId }
             .ifEmpty { listOf(R.raw.intro_animation) }
        }
    }

    val backgroundImages = remember(dynamicPhotoIds) {
        if (dynamicPhotoIds.isNotEmpty()) {
            dynamicPhotoIds
        } else {
            // Fallback: Negatif karma fotoğraflar (ölüm teması)
            com.example.isekaikuroshin.engine.MediaDatabaseHelper.getMediaForKarmaRange(
                screenType = "POSTDEATH",
                karmaRange = -1.0f..-0.3f,
                depth = "D1"
            ).filter { it.fileName.startsWith("p_") }
             .map { it.resourceId }
             .ifEmpty { emptyList() }
        }
    }

    var currentImageIndex by remember { mutableIntStateOf(0) }

    // Background slideshow - Sadece liste doluysa çalıştır
    LaunchedEffect(backgroundImages.size) {
        if (backgroundImages.isNotEmpty()) {
            while (true) {
                kotlinx.coroutines.delay(5000)
                currentImageIndex = (currentImageIndex + 1) % backgroundImages.size
            }
        }
    }

    // Background video
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
        }
    }

    DisposableEffect(Unit) {
        if (backgroundVideos.isNotEmpty()) {
            val mediaItem = MediaItem.fromUri("android.resource://${context.packageName}/${backgroundVideos.random()}")
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }

        onDispose { exoPlayer.release() }
    }

    // AI Report Generation
    LaunchedEffect(lastDeath) {
        if (lastDeath != null && aiReport.isEmpty()) {
            isAIGenerating = true

            val prompt = buildDeathReportPrompt(
                deathArchive = deathArchive,
                lastDeath = lastDeath,
                gameData = gameData
            )

            try {
                val response = withContext(Dispatchers.IO) {
                    GlobalAIManager.generateResponse(
                        userMessage = prompt
                    )
                }
                aiReport = response

                // Badge'leri hesapla
                badges = calculateBadges(deathArchive, gameData)

            } catch (e: Exception) {
                aiReport = "Ölüm raporu oluşturulamadı. Ruhun kaybolmuş..."
            } finally {
                isAIGenerating = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Background - Sadece liste doluysa göster
        if (backgroundImages.isNotEmpty()) {
            Image(
                painter = painterResource(id = backgroundImages[currentImageIndex]),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.3f
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💀 ÖLÜM RAPORU",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // G140: "Yenile" Butonu - TÜM VERİLERİ SIFIRLA + FIRSTUSER
                    IconButton(
                        onClick = {
                            scope.launch {
                                android.util.Log.d("DeathStatisticsScreen", "🔄 [G140] Yenile butonu - TÜM VERİLER sıfırlanıyor...")

                                // 1. GameState sıfırla (inventory, quests, NPCs, karma, etc.)
                                gameStateManager.resetGameState()

                                // 2. Death Archive temizle
                                PersistentDataManager.clearDeathArchive()

                                // 3. API Keys sıfırla
                                PersistentDataManager.resetAPIKeys()

                                // 4. Player data sıfırla (FIRSTUSER için)
                                PersistentDataManager.updatePlayerData { it.copy(name = "", isAlive = true) }

                                // 5. First Launch flag'ini true yap
                                val sharedPrefs = context.getSharedPreferences("IsekaiKuroshin", Context.MODE_PRIVATE)
                                sharedPrefs.edit().putBoolean("is_first_launch", true).commit()

                                android.util.Log.d("DeathStatisticsScreen", "✅ [G140] Yenile: TÜM VERİLER sıfırlandı → FIRSTUSER")
                                Toast.makeText(context, "🔄 Veriler sıfırlandı - Yeni oyun başlıyor", Toast.LENGTH_LONG).show()
                                kotlinx.coroutines.delay(1000)
                                onClose() // user_entry → FIRSTUSER
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Yenile - Yeni Oyun",
                            tint = Color.Yellow,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // G140: Close button - TÜM VERİLERİ SIFIRLA + Uygulamayı kapat
                    IconButton(
                        onClick = {
                            scope.launch {
                                android.util.Log.d("DeathStatisticsScreen", "🔄 [G140] Close butonu - TÜM VERİLER sıfırlanıyor + Uygulama kapanıyor...")

                                // 1. GameState sıfırla (inventory, quests, NPCs, karma, etc.)
                                gameStateManager.resetGameState()

                                // 2. Death Archive temizle
                                PersistentDataManager.clearDeathArchive()

                                // 3. API Keys sıfırla
                                PersistentDataManager.resetAPIKeys()

                                // 4. Player data sıfırla
                                PersistentDataManager.resetPlayerData()

                                // 5. First Launch flag'ini sıfırla (SYNC!)
                                val sharedPrefs = context.getSharedPreferences("IsekaiKuroshin", Context.MODE_PRIVATE)
                                sharedPrefs.edit().putBoolean("is_first_launch", true).commit()

                                android.util.Log.d("DeathStatisticsScreen", "✅ [G140] Close: TÜM VERİLER sıfırlandı, uygulama kapanıyor")

                                Toast.makeText(context, "❌ Veriler silindi - Uygulama kapatılıyor", Toast.LENGTH_SHORT).show()
                                kotlinx.coroutines.delay(1000)

                                // Uygulamayı kapat
                                android.os.Process.killProcess(android.os.Process.myPid())
                                exitProcess(0)
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Kapat - Sil + Çıkış",
                            tint = Color.Red,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BÜYÜK: "Sonraki Aşamaya Geç" Butonu (Sayaç korunur, veriler silinir, uygulama kapanır)
            if (deathArchive.size > 1) {
                Button(
                    onClick = {
                        android.util.Log.d("DeathStatisticsScreen", "📜 [G140] Sonraki Aşamaya Geç: Sayaç KORUNACAK, GameState + API sıfırlanacak")
                        scope.launch {
                            // G140 FIX: GameState ve API Keys de sıfırlanmalı!

                            // 1. GameState sıfırla (inventory, quests, NPCs, karma, etc.)
                            gameStateManager.resetGameState()

                            // 2. API Keys sıfırla
                            PersistentDataManager.resetAPIKeys()

                            // 3. Oyuncu adını ve verilerini sil (SAYAÇ HARİÇ!)
                            // NOT: clearDeathArchive() ÇAĞIRMA! Sayaç korunmalı!
                            PersistentDataManager.updatePlayerData {
                                it.copy(
                                    name = "",  // KRİTİK: FIRSTUSER akışı için
                                    isAlive = true
                                    // Sayaç (karmaCounter) burada SİLİNMEMELİ!
                                )
                            }

                            // 4. First Launch flag'ini true yap
                            val sharedPrefs = context.getSharedPreferences("IsekaiKuroshin", Context.MODE_PRIVATE)
                            sharedPrefs.edit().putBoolean("is_first_launch", true).commit()

                            android.util.Log.d("DeathStatisticsScreen", "✅ [G140] Sonraki Aşama: GameState + API sıfırlandı, sayaç korundu")
                            Toast.makeText(context, "Veriler silindi, sayaç korundu. Uygulama kapatılıyor...", Toast.LENGTH_LONG).show()
                            kotlinx.coroutines.delay(1500) // Toast görünsün diye kısa bekle
                            // Uygulamayı kapat (kullanıcı tekrar açınca FIRSTUSER'da başlayacak)
                            exitProcess(0)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4444).copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "📜 SON KAYITA GEÇ (${deathArchive.size}. Ölüm)",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Statistics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 İSTATİSTİKLER",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    QuickStat("💀 Toplam Ölüm", deathArchive.size.toString())
                    if (lastDeath != null) {
                        QuickStat("⚰️ Son Sebep", lastDeath.deathCause)
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val deathTime = Date(lastDeath.deathTimestamp)
                        QuickStat("⏰ Zaman", dateFormat.format(deathTime))
                    }

                    val umbrosAccepted = deathArchive.count { it.wasUmbrosOfferAccepted }
                    val umbrosRejected = deathArchive.count { !it.wasUmbrosOfferAccepted }
                    QuickStat("✅ Umbros Kabul", umbrosAccepted.toString())
                    QuickStat("❌ Umbros Red", umbrosRejected.toString())
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Badges
            if (badges.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🏆 ÜNVANLAR & BAŞARILAR",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        badges.forEach { badge ->
                            Text(
                                text = "• $badge",
                                color = Color(0xFFFFD700),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // AI Report
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📜 ÖLÜM RAPORU",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isAIGenerating) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("AI raporu oluşturuyor...", color = Color.White.copy(0.7f))
                        }
                    } else {
                        Text(
                            text = aiReport,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Close message
            Text(
                text = "Bu pencereyi kapatınca oyun sonlanacak.\nTekrar giriş yaptığınızda yeni bir başlangıç yapacaksınız.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun QuickStat(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(0.7f), fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// AI Report Prompt Builder
private fun buildDeathReportPrompt(
    deathArchive: List<DeathRecord>,
    lastDeath: DeathRecord,
    gameData: PersistentGameData
): String {
    return """
Oyuncu Adı: ${gameData.playerData.name}
Toplam Ölüm: ${deathArchive.size}
Son Ölüm Sebebi: ${lastDeath.deathCause}
Umbros Kabul: ${deathArchive.count { it.wasUmbrosOfferAccepted }}
Umbros Red: ${deathArchive.count { !it.wasUmbrosOfferAccepted }}

Lütfen bu oyuncunun ölüm yolculuğu hakkında 3-4 cümlelik derin, felsefi bir rapor yaz.
Ölüm tarzını, karakterini, kaderini analiz et. Ciddi ve mitolojik bir ton kullan.
    """.trimIndent()
}

// Badge Calculation
private fun calculateBadges(
    deathArchive: List<DeathRecord>,
    @Suppress("UNUSED_PARAMETER") gameData: PersistentGameData
): List<String> {
    val badges = mutableListOf<String>()

    val totalDeaths = deathArchive.size
    val umbrosAccepted = deathArchive.count { it.wasUmbrosOfferAccepted }
    val umbrosRejected = deathArchive.count { !it.wasUmbrosOfferAccepted }

    // Death count badges
    when {
        totalDeaths >= 50 -> badges.add("🔥 Sonsuz Döngü - 50+ Ölüm")
        totalDeaths >= 20 -> badges.add("💀 Ölüm Ustası - 20+ Ölüm")
        totalDeaths >= 10 -> badges.add("⚰️ Deneyimli Ruh - 10+ Ölüm")
        totalDeaths >= 5 -> badges.add("👻 Yolcu Ruh - 5+ Ölüm")
        totalDeaths >= 1 -> badges.add("🌱 İlk Adım - İlk Ölüm")
    }

    // Umbros badges
    if (umbrosRejected >= 5) badges.add("⛔ Umbros'un Düşmanı - 5+ Red")
    if (umbrosAccepted >= 5) badges.add("🤝 Umbros'un Dostu - 5+ Kabul")
    if (umbrosRejected == totalDeaths && totalDeaths > 0) badges.add("🛡️ Saf Ruh - Hiç Kabul Etmedi")
    if (umbrosAccepted == totalDeaths && totalDeaths > 0) badges.add("😈 Karanlığın Çocuğu - Hep Kabul Etti")

    // Special badges
    if (totalDeaths == 0) badges.add("✨ Yeni Başlangıç")

    return badges
}

// Utility Functions
private fun generateFullReport(
    deathArchive: List<DeathRecord>,
    aiReport: String,
    badges: List<String>
): String {
    val sb = StringBuilder()
    sb.appendLine("═══ ÖLÜM RAPORU ═══\n")
    sb.appendLine("📊 İSTATİSTİKLER:")
    sb.appendLine("Toplam Ölüm: ${deathArchive.size}")
    sb.appendLine("Umbros Kabul: ${deathArchive.count { it.wasUmbrosOfferAccepted }}")
    sb.appendLine("Umbros Red: ${deathArchive.count { !it.wasUmbrosOfferAccepted }}")
    sb.appendLine()

    if (badges.isNotEmpty()) {
        sb.appendLine("🏆 ÜNVANLAR:")
        badges.forEach { sb.appendLine("• $it") }
        sb.appendLine()
    }

    sb.appendLine("📜 AI RAPORU:")
    sb.appendLine(aiReport)
    sb.appendLine()
    sb.appendLine("═══════════════════")

    return sb.toString()
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Death Report", text))
}

private suspend fun takeScreenshot(context: Context, view: android.view.View) {
    withContext(Dispatchers.IO) {
        try {
            view.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(view.drawingCache)
            view.isDrawingCacheEnabled = false

            val file = File(context.cacheDir, "death_report_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Ekran görüntüsü kaydedildi: ${file.name}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun shareReport(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, "Ölüm Raporu - Isekai Kuroshin")
    }
    context.startActivity(Intent.createChooser(intent, "Raporu Paylaş"))
}

/**
 * Ekran görüntülerini temizle
 */
private fun clearScreenshotCache(context: Context) {
    try {
        val cacheDir = context.cacheDir
        var deletedCount = 0

        cacheDir.listFiles()?.filter {
            it.name.startsWith("death_report_") && it.extension == "png"
        }?.forEach {
            if (it.delete()) {
                deletedCount++
            }
        }

        android.util.Log.d("DeathStatisticsScreen", "🗑️ $deletedCount ekran görüntüsü silindi")
    } catch (e: Exception) {
        android.util.Log.e("DeathStatisticsScreen", "Ekran görüntüleri silinemedi: ${e.message}")
    }
}
