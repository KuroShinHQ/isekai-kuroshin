package com.example.isekaikuroshin.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme
import java.text.NumberFormat
import java.util.Locale

/**
 * GÖREV F: Token Sayacı UI
 *
 * AI API kullanım istatistiklerini gösterir:
 * - Toplam token kullanımı
 * - Bu oturum token kullanımı
 * - Toplam API çağrısı
 * - Bu oturum API çağrısı
 */
@Composable
fun UsageStatsSection(
    dashboardColors: DashboardColorScheme
) {
    val gameData by PersistentDataManager.gameData.collectAsState()
    val usageStats = gameData.usageStats

    SettingsCategory(
        title = rememberLocalizedText("usage_stats"),
        icon = Icons.Default.Analytics,
        dashboardColors = dashboardColors
    ) {
        // Token Usage Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Başlık
                Text(
                    text = rememberLocalizedText("ai_api_usage"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                // Token istatistikleri
                UsageStatRow(
                    label = rememberLocalizedText("total_tokens"),
                    value = formatNumber(usageStats.totalTokensUsed),
                    color = MaterialTheme.colorScheme.primary
                )

                UsageStatRow(
                    label = rememberLocalizedText("session_tokens"),
                    value = formatNumber(usageStats.sessionTokensUsed),
                    color = MaterialTheme.colorScheme.secondary
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // API çağrısı istatistikleri
                UsageStatRow(
                    label = rememberLocalizedText("total_api_calls"),
                    value = formatNumber(usageStats.totalApiCalls),
                    color = MaterialTheme.colorScheme.tertiary
                )

                UsageStatRow(
                    label = rememberLocalizedText("session_api_calls"),
                    value = formatNumber(usageStats.sessionApiCalls),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                // Bilgilendirme notu
                Text(
                    text = rememberLocalizedText("usage_stats_note"),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun UsageStatRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )

        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Sayıları formatlar (binlik ayırıcı ile)
 */
private fun formatNumber(number: Long): String {
    return NumberFormat.getNumberInstance(Locale.getDefault()).format(number)
}
