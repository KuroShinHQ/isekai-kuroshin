package com.example.isekaikuroshin.ui.archive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.isekaikuroshin.data.DeathRecord
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.ui.theme.getDashboardColors
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeathArchiveDetailScreen(
    navController: NavController,
    deathId: String
) {
    val dashboardColors = getDashboardColors()
    val context = LocalContext.current

    // Death record'u bul
    val gameData by PersistentDataManager.gameData.collectAsState()
    val deathRecord = gameData.deathArchive.find { it.id == deathId }

    if (deathRecord == null) {
        // Death record bulunamadı
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dashboardColors.backgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "💀 Ölüm kaydı bulunamadı",
                    color = Color.White,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Geri Dön")
                }
            }
        }
        return
    }

    // Umbros teklifi kabul edilmişse gösterme
    if (deathRecord.wasUmbrosOfferAccepted) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dashboardColors.backgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⚫ Bu ölüm gerçek değil",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 18.sp
                )
                Text(
                    text = "(Umbros teklifi kabul edildi)",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Geri Dön")
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💀 Ölüm Detayları") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Copy butonu
                    IconButton(
                        onClick = {
                            copyDeathRecordToClipboard(context, deathRecord)
                            Toast.makeText(context, "📋 Ölüm kaydı kopyalandı!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Kopyala",
                            tint = dashboardColors.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = dashboardColors.backgroundDark,
                    titleContentColor = dashboardColors.primary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(dashboardColors.backgroundDark)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screenshot (eğer varsa)
            if (!deathRecord.screenshotPath.isNullOrEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = dashboardColors.holographicBg
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(deathRecord.screenshotPath),
                            contentDescription = "Ölüm Anı",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // GM Özeti (eğer varsa)
            if (!deathRecord.gmDeathSummary.isNullOrEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF3D1F1F) // Koyu kırmızı ton
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📜 Ölüm Hikayesi",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B6B)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = deathRecord.gmDeathSummary,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Karakter Bilgileri
            item {
                DeathInfoCard(
                    title = "⚰️ Karakter Bilgileri",
                    dashboardColors = dashboardColors
                ) {
                    InfoRow("İsim", deathRecord.characterName)
                    InfoRow("Gün", deathRecord.deathDay.toString())
                    InfoRow(
                        "Tarih",
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            .format(Date(deathRecord.deathTimestamp))
                    )
                    InfoRow("Toplam Oyun Süresi", formatPlayTime(deathRecord.totalPlayTime))
                }
            }

            // Ölüm Detayları
            item {
                DeathInfoCard(
                    title = "💀 Ölüm Detayları",
                    dashboardColors = dashboardColors
                ) {
                    InfoRow("Sebep", deathRecord.deathCause)
                    InfoRow("Lokasyon", deathRecord.deathLocation)
                    if (deathRecord.xpGainedFromDeath > 0) {
                        InfoRow("Kazanılan XP", "+${deathRecord.xpGainedFromDeath}")
                    }
                }
            }

            // Aktif Ünvanlar
            if (deathRecord.activeTitles.isNotEmpty()) {
                item {
                    DeathInfoCard(
                        title = "🏆 Aktif Ünvanlar",
                        dashboardColors = dashboardColors
                    ) {
                        deathRecord.activeTitles.forEach { title ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "▪",
                                    color = Color(0xFFFFD700),
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = title,
                                    color = Color(0xFFFFD700),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Başarımlar
            if (deathRecord.achievements.isNotEmpty()) {
                item {
                    DeathInfoCard(
                        title = "⭐ Kazanılan Başarımlar",
                        dashboardColors = dashboardColors
                    ) {
                        deathRecord.achievements.forEach { achievement ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✓",
                                    color = dashboardColors.primary,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = achievement,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // Final Stats
            item {
                DeathInfoCard(
                    title = "📊 Final İstatistikler",
                    dashboardColors = dashboardColors
                ) {
                    val stats = deathRecord.finalStats
                    InfoRow("HP", "${stats.hp} / ${stats.maxHp}")
                    InfoRow("MP", "${stats.mp} / ${stats.maxMp}")
                    InfoRow("Stamina", "${stats.stamina} / ${stats.maxStamina}")
                    InfoRow("Gün", deathRecord.deathDay.toString())
                }
            }

            // Spacer at bottom
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DeathInfoCard(
    title: String,
    dashboardColors: com.example.isekaikuroshin.ui.theme.DashboardColorScheme,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.holographicBg
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = dashboardColors.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatPlayTime(millis: Long): String {
    val hours = millis / (1000 * 60 * 60)
    val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
    return if (hours > 0) {
        "${hours}s ${minutes}dk"
    } else {
        "${minutes}dk"
    }
}

private fun copyDeathRecordToClipboard(context: Context, deathRecord: DeathRecord) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val text = buildString {
        appendLine("💀 ÖLÜM KAYDI 💀")
        appendLine("=" . repeat(40))
        appendLine()
        appendLine("👤 Karakter: ${deathRecord.characterName}")
        appendLine("📅 Gün: ${deathRecord.deathDay}")
        appendLine("📍 Lokasyon: ${deathRecord.deathLocation}")
        appendLine("⚔️ Ölüm Sebebi: ${deathRecord.deathCause}")
        appendLine()

        if (!deathRecord.gmDeathSummary.isNullOrEmpty()) {
            appendLine("📜 Hikaye:")
            appendLine(deathRecord.gmDeathSummary)
            appendLine()
        }

        if (deathRecord.activeTitles.isNotEmpty()) {
            appendLine("🏆 Ünvanlar:")
            deathRecord.activeTitles.forEach { appendLine("  • $it") }
            appendLine()
        }

        if (deathRecord.achievements.isNotEmpty()) {
            appendLine("⭐ Başarımlar:")
            deathRecord.achievements.forEach { appendLine("  • $it") }
            appendLine()
        }

        appendLine("📊 Final Stats:")
        appendLine("  HP: ${deathRecord.finalStats.hp}/${deathRecord.finalStats.maxHp}")
        appendLine("  MP: ${deathRecord.finalStats.mp}/${deathRecord.finalStats.maxMp}")
        appendLine("  Stamina: ${deathRecord.finalStats.stamina}/${deathRecord.finalStats.maxStamina}")
        appendLine("  STR: ${deathRecord.finalStats.strength}, AGI: ${deathRecord.finalStats.agility}, INT: ${deathRecord.finalStats.intelligence}")
        appendLine()
        appendLine("⏱️ Toplam Süre: ${formatPlayTime(deathRecord.totalPlayTime)}")
        appendLine("=========================================")
    }

    val clip = ClipData.newPlainText("Death Record", text)
    clipboard.setPrimaryClip(clip)
}
