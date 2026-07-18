package com.example.isekaikuroshin.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme
import com.example.isekaikuroshin.workers.DocumentProcessingWorker
import kotlinx.coroutines.delay

@Composable
fun StoryAndAISettingsSection(
    settings: SettingsData,
    dashboardColors: DashboardColorScheme,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current

    // Observe processing status from ViewModel
    val processingStatus by viewModel.processingStatus.collectAsState()

    // Observe game data for usage statistics
    val gameData by PersistentDataManager.gameData.collectAsState()

    // File picker launcher
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                // Take persistent permission for long-term access
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                // Save URI to PersistentDataManager
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        storySettings = settingsData.storySettings.copy(
                            externalStorySource = uri.toString()
                        )
                    )
                }

                // Create WorkManager task
                val inputData = workDataOf("KEY_DOCUMENT_URI" to uri.toString())
                val workRequest = OneTimeWorkRequestBuilder<DocumentProcessingWorker>()
                    .setInputData(inputData)
                    .build()

                // Enqueue the worker
                WorkManager.getInstance(context).enqueue(workRequest)

                // Start monitoring the work status
                viewModel.startWorkMonitoring(workRequest.id)

            } catch (e: Exception) {
                // Handle permission or WorkManager errors
                e.printStackTrace()
            }
        }
    }
    SettingsCategory(
        title = rememberLocalizedText("story_ai_settings"),
        icon = Icons.AutoMirrored.Filled.MenuBook,
        dashboardColors = dashboardColors
    ) {
        // Hikaye Ayarları
        SwitchSetting(
            title = rememberLocalizedText("hide_json"),
            subtitle = rememberLocalizedText("hide_json_desc"),
            checked = settings.storySettings.hideJSON,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        storySettings = settingsData.storySettings.copy(
                            hideJSON = enabled
                        )
                    )
                }
            }
        )

        SwitchSetting(
            title = rememberLocalizedText("character_hints"),
            subtitle = rememberLocalizedText("character_hints_desc"),
            checked = settings.storySettings.showCharacterHints,
            onCheckedChange = { enabled ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        storySettings = settingsData.storySettings.copy(
                            showCharacterHints = enabled
                        )
                    )
                }
            }
        )

        // External Story Source - Dynamic File Picker Interface
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Dynamic status text based on processing state
            when (processingStatus.state) {
                ProcessingState.IDLE -> {
                    Text(
                        text = if (settings.storySettings.externalStorySource.isEmpty()) {
                            rememberLocalizedText("external_story_not_selected")
                        } else {
                            "${rememberLocalizedText("selected_source")} ${settings.storySettings.externalStorySource}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (settings.storySettings.externalStorySource.isEmpty()) {
                            Color.Gray
                        } else {
                            Color.White
                        },
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                ProcessingState.PROCESSING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = dashboardColors.primary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rememberLocalizedText("processing_file"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = dashboardColors.primary
                        )
                    }
                }
                ProcessingState.SUCCESS -> {
                    Text(
                        text = rememberLocalizedText("processed_chunks").replace("{count}", processingStatus.chunkCount.toString()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Green,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                ProcessingState.FAILED -> {
                    Text(
                        text = processingStatus.errorMessage.ifEmpty { rememberLocalizedText("processing_failed") },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            // Buttons Row - disabled during processing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File Selection Button
                Button(
                    onClick = {
                        documentLauncher.launch(arrayOf("text/plain", "application/pdf"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = dashboardColors.primary
                    ),
                    modifier = Modifier.weight(1f),
                    enabled = processingStatus.state != ProcessingState.PROCESSING
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Dosya Seç",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(rememberLocalizedText("select_story_file"))
                }

                // Clear Button
                IconButton(
                    onClick = {
                        PersistentDataManager.updateSettingsData { settingsData ->
                            settingsData.copy(
                                storySettings = settingsData.storySettings.copy(
                                    externalStorySource = ""
                                )
                            )
                        }
                        viewModel.resetProcessingStatus()
                    },
                    enabled = settings.storySettings.externalStorySource.isNotEmpty() &&
                             processingStatus.state != ProcessingState.PROCESSING
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Temizle",
                        tint = if (settings.storySettings.externalStorySource.isNotEmpty() &&
                                   processingStatus.state != ProcessingState.PROCESSING) {
                            Color.White
                        } else {
                            Color.Gray
                        }
                    )
                }
            }

            // Built-in Story Selection
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = rememberLocalizedText("or_select_builtin"),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Monte Cristo Button
            OutlinedButton(
                onClick = {
                    PersistentDataManager.updateSettingsData { settingsData ->
                        settingsData.copy(
                            storySettings = settingsData.storySettings.copy(
                                externalStorySource = "stories/monte_kristo_kontu_en.txt"
                            )
                        )
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = dashboardColors.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = processingStatus.state != ProcessingState.PROCESSING
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = "Built-in Story",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(rememberLocalizedText("builtin_story_monte_cristo"))
            }
        }

        // FIX #46: LEGACY "AI Provider" SECTION REMOVED
        // This section confused users - removed per user request
        // The active setting is "Text Generation Model" below
        // Data field "selectedProvider" kept in PersistentDataManager for backward compatibility

        // Kullanım İstatistikleri Bölümü
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(16.dp))

        // Kullanım İstatistikleri
        Text(
            text = rememberLocalizedText("usage_statistics"),
            style = MaterialTheme.typography.titleMedium,
            color = dashboardColors.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // İstatistik kartları
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = dashboardColors.holographicBg.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toplam Token Kullanımı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = rememberLocalizedText("total_tokens_used"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${gameData.usageStats.totalTokensUsed}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = dashboardColors.primary
                    )
                }

                // Bu Oturumdaki Token Kullanımı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = rememberLocalizedText("session_tokens_used"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${gameData.usageStats.sessionTokensUsed}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = dashboardColors.primary
                    )
                }

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                // Toplam API Çağrısı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = rememberLocalizedText("total_api_calls"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${gameData.usageStats.totalApiCalls}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = dashboardColors.primary
                    )
                }

                // Bu Oturumdaki API Çağrısı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = rememberLocalizedText("session_api_calls"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${gameData.usageStats.sessionApiCalls}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = dashboardColors.primary
                    )
                }
            }
        }

        // Tahmini Maliyet Bölümü
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = rememberLocalizedText("estimated_cost"),
            style = MaterialTheme.typography.titleMedium,
            color = dashboardColors.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Maliyet hesaplama kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = dashboardColors.holographicBg.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Milyon token başına maliyet girişi
                OutlinedTextField(
                    value = settings.apiSettings.costPerMillionTokens.toString(),
                    onValueChange = { newValue ->
                        val cost = newValue.toDoubleOrNull() ?: 0.0
                        PersistentDataManager.updateSettingsData { settingsData ->
                            settingsData.copy(
                                apiSettings = settingsData.apiSettings.copy(
                                    costPerMillionTokens = cost
                                )
                            )
                        }
                    },
                    label = { Text(rememberLocalizedText("cost_per_million_tokens")) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                // Maliyet hesaplamaları
                val estimatedTotalCost = (gameData.usageStats.totalTokensUsed / 1_000_000.0) * settings.apiSettings.costPerMillionTokens
                val estimatedSessionCost = (gameData.usageStats.sessionTokensUsed / 1_000_000.0) * settings.apiSettings.costPerMillionTokens

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = rememberLocalizedText("estimated_total_cost"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${"%.4f".format(estimatedTotalCost)} $",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Green
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = rememberLocalizedText("session_cost"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = "${"%.4f".format(estimatedSessionCost)} $",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Green
                    )
                }

                // Bilgi notu
                Text(
                    text = rememberLocalizedText("cost_disclaimer"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Token Kota Yönetimi Bölümü
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = rememberLocalizedText("token_quota_management"),
            style = MaterialTheme.typography.titleMedium,
            color = dashboardColors.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Kota yönetimi kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = dashboardColors.holographicBg.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Kota limiti giriş alanı
                OutlinedTextField(
                    value = settings.apiSettings.tokenQuotaLimit.toString(),
                    onValueChange = { newValue ->
                        val quotaLimit = newValue.toLongOrNull() ?: 0
                        PersistentDataManager.updateSettingsData { settingsData ->
                            settingsData.copy(
                                apiSettings = settingsData.apiSettings.copy(
                                    tokenQuotaLimit = quotaLimit
                                )
                            )
                        }
                    },
                    label = { Text(rememberLocalizedText("monthly_token_limit")) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                // Açıklama metni
                Text(
                    text = rememberLocalizedText("quota_warning_limit"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                // Kota uyarısı anahtarı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rememberLocalizedText("enable_quota_warning"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = settings.apiSettings.enableQuotaWarning,
                        onCheckedChange = { enabled ->
                            PersistentDataManager.updateSettingsData { settingsData ->
                                settingsData.copy(
                                    apiSettings = settingsData.apiSettings.copy(
                                        enableQuotaWarning = enabled
                                    )
                                )
                            }
                        }
                    )
                }

                // Durum bilgisi
                val currentUsage = gameData.usageStats.totalTokensUsed
                val quotaPercentage = if (settings.apiSettings.tokenQuotaLimit > 0) {
                    (currentUsage.toDouble() / settings.apiSettings.tokenQuotaLimit.toDouble() * 100).toInt()
                } else {
                    0
                }

                Text(
                    text = "${rememberLocalizedText("current_usage")} $currentUsage / ${settings.apiSettings.tokenQuotaLimit} token (%${quotaPercentage})",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (quotaPercentage >= 100) Color.Red else if (quotaPercentage >= 80) Color.Yellow else Color.Green,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Bilgi notu
                Text(
                    text = rememberLocalizedText("quota_note"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Çeviri Servisi Ayarları Bölümü (Evrensel Çeviri Katmanı - Faz 1)
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = rememberLocalizedText("translation_service_settings"),
            style = MaterialTheme.typography.titleMedium,
            color = dashboardColors.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Açıklama metni
        Text(
            text = rememberLocalizedText("translation_service_desc"),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Translation Provider seçim radio buttonları
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // ML Kit (Offline/Default)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = settings.apiSettings.translationProvider == "ML_KIT",
                    onClick = {
                        PersistentDataManager.updateSettingsData { settingsData ->
                            settingsData.copy(
                                apiSettings = settingsData.apiSettings.copy(
                                    translationProvider = "ML_KIT"
                                )
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = rememberLocalizedText("ml_kit_translation_offline"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = rememberLocalizedText("ml_kit_offline_info"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // Google Cloud Translation (Online)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = settings.apiSettings.translationProvider == "GOOGLE_CLOUD",
                    onClick = {
                        PersistentDataManager.updateSettingsData { settingsData ->
                            settingsData.copy(
                                apiSettings = settingsData.apiSettings.copy(
                                    translationProvider = "GOOGLE_CLOUD"
                                )
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = rememberLocalizedText("google_cloud_translation"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    Text(
                        text = rememberLocalizedText("google_cloud_online_info"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        // GÖREV: googleCloudTranslateApiKey DEPRECATED - geminiApiKey birincil oldu
        // Translation için de aynı Gemini key kullanılıyor
        if (settings.apiSettings.translationProvider == "GOOGLE_CLOUD") {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = dashboardColors.holographicBg.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rememberLocalizedText("unified_api_key_system"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = rememberLocalizedText("unified_api_key_description"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }

        // ============================
        // GÖREV: AI MODEL SEÇİMİ VE GÖRSEL ANALİZ
        // ============================

        Spacer(modifier = Modifier.height(32.dp))

        // Text Generation Model Section
        AIModelSelectionSection(
            settings = settings,
            dashboardColors = dashboardColors
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Image Analysis Section
        ImageAnalysisSection(
            settings = settings,
            dashboardColors = dashboardColors
        )
    }
}

// ============================
// GÖREV: AI MODEL SEÇİMİ - TEXT GENERATION
// ============================

@Composable
private fun AIModelSelectionSection(
    settings: SettingsData,
    dashboardColors: DashboardColorScheme
) {
    Column {
        // Başlık
        Text(
            text = rememberLocalizedText("text_generation_model_title"),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = rememberLocalizedText("text_generation_model_desc"),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // FIX #46: Clear info card explaining what this setting controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = dashboardColors.primary.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = dashboardColors.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = rememberLocalizedText("journal_ai_control_title"),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rememberLocalizedText("journal_ai_control_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gemma 3-bit Local Model
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    PersistentDataManager.updateSettingsData { settingsData ->
                        settingsData.copy(
                            apiSettings = settingsData.apiSettings.copy(
                                textGenerationModel = "GEMMA_LOCAL"
                            )
                        )
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = settings.apiSettings.textGenerationModel == "GEMMA_LOCAL",
                onClick = {
                    PersistentDataManager.updateSettingsData { settingsData ->
                        settingsData.copy(
                            apiSettings = settingsData.apiSettings.copy(
                                textGenerationModel = "GEMMA_LOCAL"
                            )
                        )
                    }
                },
                colors = RadioButtonDefaults.colors(
                    selectedColor = dashboardColors.primary
                )
            )

            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = rememberLocalizedText("gemma_local_model"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = rememberLocalizedText("gemma_local_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Google Gemini Cloud Model
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    PersistentDataManager.updateSettingsData { settingsData ->
                        settingsData.copy(
                            apiSettings = settingsData.apiSettings.copy(
                                textGenerationModel = "GEMINI_CLOUD"
                            )
                        )
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = settings.apiSettings.textGenerationModel == "GEMINI_CLOUD",
                onClick = {
                    PersistentDataManager.updateSettingsData { settingsData ->
                        settingsData.copy(
                            apiSettings = settingsData.apiSettings.copy(
                                textGenerationModel = "GEMINI_CLOUD"
                            )
                        )
                    }
                },
                colors = RadioButtonDefaults.colors(
                    selectedColor = dashboardColors.primary
                )
            )

            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = rememberLocalizedText("gemini_cloud_model"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = rememberLocalizedText("gemini_cloud_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Google API Key Input (sadece Cloud seçiliyse görünür)
        if (settings.apiSettings.textGenerationModel == "GEMINI_CLOUD") {
            Spacer(modifier = Modifier.height(16.dp))

            GeminiAPIKeyInput(
                settings = settings,
                dashboardColors = dashboardColors
            )
        }
    }
}

// ============================
// GÖREV: GOOGLE GEMINI API KEY INPUT
// ============================

@Composable
private fun GeminiAPIKeyInput(
    settings: SettingsData,
    dashboardColors: DashboardColorScheme
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isApiKeyVisible by rememberSaveable { mutableStateOf(false) }

    Column {
        // Başlık
        Text(
            text = rememberLocalizedText("gemini_api_key_title"),
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // API Key TextField
        OutlinedTextField(
            value = settings.apiSettings.geminiApiKey,
            onValueChange = { newKey ->
                PersistentDataManager.updateSettingsData { settingsData ->
                    settingsData.copy(
                        apiSettings = settingsData.apiSettings.copy(
                            geminiApiKey = newKey
                        )
                    )
                }
            },
            label = { Text(rememberLocalizedText("gemini_api_key_title")) },
            placeholder = { Text(rememberLocalizedText("gemini_api_key_placeholder")) },
            visualTransformation = if (isApiKeyVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                    Icon(
                        imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (isApiKeyVisible) "Hide" else "Show"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = dashboardColors.primary,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = dashboardColors.primary,
                unfocusedLabelColor = Color.Gray,
                cursorColor = dashboardColors.primary
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // FIX #46: Save API Key Button - User-requested visual confirmation
        var showSavedConfirmation by remember { mutableStateOf(false) }

        Button(
            onClick = {
                // API key is already auto-saved, just show confirmation
                showSavedConfirmation = true
                android.util.Log.d("StoryAndAISettingsSection", "✅ API Key saved confirmation shown")
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = dashboardColors.primary,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Save",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(rememberLocalizedText("save_api_key"))
        }

        // Success confirmation message
        if (showSavedConfirmation) {
            LaunchedEffect(Unit) {
                delay(3000)
                showSavedConfirmation = false
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rememberLocalizedText("api_key_saved_confirmation"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Google AI Studio Link Button
        Button(
            onClick = {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://aistudio.google.com/app/apikey")
                )
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = dashboardColors.primary.copy(alpha = 0.1f),
                contentColor = dashboardColors.primary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = "Link",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(rememberLocalizedText("google_ai_studio_link"))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // How to Get API Key - Accordion (Açılır/Kapanır)
        var howToExpanded by remember { mutableStateOf(false) }

        ExpandableSettingsCard(
            title = rememberLocalizedText("api_key_howto_title"),
            icon = Icons.Default.Help,
            expanded = howToExpanded,
            onExpandChange = { howToExpanded = it },
            dashboardColors = dashboardColors
        ) {
            // Adımlar
            listOf(
                "api_key_step1",
                "api_key_step2",
                "api_key_step3",
                "api_key_step4",
                "api_key_step5"
            ).forEach { stepKey ->
                Text(
                    text = rememberLocalizedText(stepKey),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Güvenlik Bilgisi
            Text(
                text = rememberLocalizedText("api_key_security_title"),
                style = MaterialTheme.typography.titleSmall,
                color = Color.Yellow,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            listOf(
                "api_key_security_device",
                "api_key_security_noshare",
                "api_key_security_billing"
            ).forEach { securityKey ->
                Text(
                    text = rememberLocalizedText(securityKey),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

// ============================
// GÖREV: IMAGE ANALYSIS SECTION
// ============================

@Composable
private fun ImageAnalysisSection(
    settings: SettingsData,
    dashboardColors: DashboardColorScheme
) {
    Column {
        // Başlık
        Text(
            text = rememberLocalizedText("image_analysis_title"),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = rememberLocalizedText("image_analysis_desc"),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Toggle Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rememberLocalizedText("image_analysis_enabled"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = rememberLocalizedText("image_analysis_model"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = rememberLocalizedText("image_analysis_cost"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Yellow
                )
                Text(
                    text = rememberLocalizedText("image_analysis_warning"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Switch(
                checked = settings.apiSettings.imageAnalysisEnabled,
                onCheckedChange = { enabled ->
                    PersistentDataManager.updateSettingsData { settingsData ->
                        settingsData.copy(
                            apiSettings = settingsData.apiSettings.copy(
                                imageAnalysisEnabled = enabled
                            )
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = dashboardColors.primary,
                    checkedTrackColor = dashboardColors.primary.copy(alpha = 0.5f)
                )
            )
        }

        // API Key gerekli uyarısı
        if (settings.apiSettings.geminiApiKey.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = rememberLocalizedText("image_analysis_requires_key"),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}