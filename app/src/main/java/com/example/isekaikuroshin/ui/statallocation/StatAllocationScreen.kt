package com.example.isekaikuroshin.ui.statallocation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // items importu doğru
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.PlayerState // PlayerState importu eklendi/güncellendi
import com.example.isekaikuroshin.engine.StatType // Bu import doğru ve engine.StatType'ı işaret ediyor
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import com.example.isekaikuroshin.data.LanguageManager

@Composable
fun rememberLocalizedText(key: String): String {
    val currentLanguage by LanguageManager.currentLanguage.collectAsState()
    return remember(key, currentLanguage) {
        LanguageManager.getText(key)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatAllocationScreen(
    navController: NavController,
    viewModel: StatAllocationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // uiState.statPoints PlayerState'den geliyor (ViewModel aracılığıyla)
    // uiState.pendingIncreases anahtarı engine.StatType
    val availablePoints = uiState.statPoints - uiState.pendingIncreases.values.sum()

    // Translations
    val titleText = rememberLocalizedText("stat_allocation_title")
    val backText = rememberLocalizedText("back")
    val resetText = rememberLocalizedText("reset")
    val confirmText = rememberLocalizedText("confirm")
    val availablePointsText = rememberLocalizedText("available_points")

    // Debug logging
    android.util.Log.d("StatAllocationScreen", "📊 Screen recomposed:")
    android.util.Log.d("StatAllocationScreen", "   Total stat points: ${uiState.statPoints}")
    android.util.Log.d("StatAllocationScreen", "   Pending increases: ${uiState.pendingIncreases}")
    android.util.Log.d("StatAllocationScreen", "   Available points: $availablePoints")

    IsekaiKuroshinTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titleText) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = backText)
                        }
                    }
                )
            },
            bottomBar = {
                BottomAppBar(actions = {
                    Button(onClick = { viewModel.resetAllocation() }) {
                        Text(resetText)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            viewModel.confirmAllocation()
                            navController.popBackStack()
                        },
                        enabled = uiState.pendingIncreases.isNotEmpty()
                    ) {
                        Text(confirmText)
                    }
                }, modifier = Modifier.fillMaxWidth())
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    "$availablePointsText: $availablePoints",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // StatType.values() engine.StatType.values() olacak
                    // it.isAllocatable() engine.StatType üzerinde çalışacak
                    items(StatType.values().filter { it.isAllocatable() }) { stat ->
                        StatAllocationRow(
                            statType = stat, // stat burada engine.StatType
                            // uiState.playerStats artık PlayerState tipinde
                            baseValue = uiState.playerStats.getStatValue(stat), // stat engine.StatType
                            pendingIncrease = uiState.pendingIncreases[stat] ?: 0,
                            onIncrease = {
                                android.util.Log.d("StatAllocationScreen", "🔵 + button clicked for ${stat.name}")
                                viewModel.increaseStat(stat)
                            },
                            onDecrease = {
                                android.util.Log.d("StatAllocationScreen", "🔵 - button clicked for ${stat.name}")
                                viewModel.decreaseStat(stat)
                            },
                            canIncrease = availablePoints > 0
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatAllocationRow(
    statType: StatType, // engine.StatType
    baseValue: Long,    // getStatValue Long döndürecek şekilde güncellendi
    pendingIncrease: Int,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    canIncrease: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Stat name (localized)
            val statNameText = when (statType) {
                StatType.STR -> rememberLocalizedText("stat_str")
                StatType.AGI -> rememberLocalizedText("stat_agi")
                StatType.INT -> rememberLocalizedText("stat_int")
                StatType.VIT -> rememberLocalizedText("stat_vit")
                StatType.SPIRIT -> rememberLocalizedText("stat_spirit")
                StatType.LUCK -> rememberLocalizedText("stat_luck")
                else -> statType.name
            }
            Text(
                text = statNameText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Base value
            Text(
                text = "$baseValue",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )

            // Pending increase (green color for visibility)
            if (pendingIncrease > 0) {
                Text(
                    text = " +$pendingIncrease",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Control buttons
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Decrease button
                IconButton(
                    onClick = onDecrease,
                    enabled = pendingIncrease > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Azalt",
                        tint = if (pendingIncrease > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                // Pending count
                Text(
                    text = "$pendingIncrease",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Increase button
                IconButton(
                    onClick = onIncrease,
                    enabled = canIncrease
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Artır",
                        tint = if (canIncrease) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

// Helper extension to get stat value from PlayerState (DEĞİŞTİRİLDİ)
// Alıcı PlayerState olarak değiştirildi. StatType parametresi engine.StatType.
fun PlayerState.getStatValue(statType: StatType): Long {
    return when (statType) { // Karşılaştırılan statType engine.StatType
        StatType.STR -> this.strength.toLong()
        StatType.AGI -> this.agility.toLong()
        StatType.INT -> this.intelligence.toLong()
        StatType.VIT -> this.vitality.toLong()
        StatType.SPIRIT -> this.spirit.toLong() // PlayerState'de spirit Int
        StatType.LUCK -> this.luck.toLong()     // PlayerState'de luck Int
        // isAllocatable() ile filtrelendiği için diğer durumların gelmemesi beklenir,
        // ama güvenlik için bir else eklenebilir.
        else -> 0L // Varsayılan değer
    }
}

// Helper to identify which stats can be allocated
// Bu fonksiyon engine.StatType üzerinde çalışıyor (dosya importu sayesinde)
fun StatType.isAllocatable(): Boolean {
    return when (this) {
        StatType.STR, StatType.AGI, StatType.INT, StatType.VIT, StatType.SPIRIT, StatType.LUCK -> true
        else -> false
    }
}
