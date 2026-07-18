package com.example.isekaikuroshin.ui.spellstudio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.data.spell.*

/**
 * Büyü Tarifi Düzenleme Ekranı
 *
 * Kullanıcının adım adım büyüyü tasarladığı ekran
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellRecipeScreen(
    spell: SpellRecipe,
    viewModel: SpellStudioViewModel
) {
    var showAddStepDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(spell.name) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.cancelEditing() }) {
                        Icon(Icons.Default.Close, rememberLocalizedText("cancel_button"))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveSpell() }) {
                        Icon(Icons.Default.Check, rememberLocalizedText("save_button"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddStepDialog = true },
                icon = { Icon(Icons.Default.Add, rememberLocalizedText("add")) },
                text = { Text(rememberLocalizedText("add_step")) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (spell.steps.isEmpty()) {
                // Boş durum
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Text(
                            text = rememberLocalizedText("no_steps_added"),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = rememberLocalizedText("click_add_step_to_create"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                // Adım listesi
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(spell.steps, key = { _, step -> step.id }) { index, step ->
                        SpellStepCard(
                            step = step,
                            stepNumber = index + 1,
                            onEditTrigger = { /* Calibration kaldırıldı */ },
                            onEditAction = { viewModel.configureAction(step.id) },
                            onDelete = { viewModel.removeStep(step.id) }
                        )
                    }
                }
            }
        }
    }

    // Adım ekle dialog'u
    if (showAddStepDialog) {
        AddStepDialog(
            viewModel = viewModel, // G88: ViewModel geçildi
            onConfirm = { trigger, action ->
                viewModel.addStep(trigger, action)
                showAddStepDialog = false
            },
            onDismiss = { showAddStepDialog = false }
        )
    }
}

/**
 * Büyü adımı kartı
 */
@Composable
private fun SpellStepCard(
    step: SpellStep,
    stepNumber: Int,
    onEditTrigger: () -> Unit,
    onEditAction: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Başlık: Adım numarası ve sil butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Adım numarası
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = rememberLocalizedText("step_number_label").replace("{number}", stepNumber.toString()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, "Sil", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tetikleyici
            TriggerSection(
                trigger = step.trigger,
                onEdit = onEditTrigger
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Eylem
            ActionSection(
                action = step.action,
                onEdit = onEditAction
            )
        }
    }

    // Silme onay dialog'u
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(rememberLocalizedText("delete_step")) },
            text = { Text(rememberLocalizedText("confirm_delete_step")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text(rememberLocalizedText("delete_button"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(rememberLocalizedText("cancel_button"))
                }
            }
        )
    }
}

/**
 * Tetikleyici bölümü
 */
@Composable
private fun TriggerSection(
    trigger: SpellTrigger,
    onEdit: () -> Unit
) {
    OutlinedCard(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rememberLocalizedText("trigger_label"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getTriggerDescription(trigger),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(Icons.Default.Edit, "Düzenle", tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

/**
 * Eylem bölümü
 */
@Composable
private fun ActionSection(
    action: SpellAction,
    onEdit: () -> Unit
) {
    OutlinedCard(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EYLEM",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getActionDescription(action),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(Icons.Default.Edit, "Düzenle", tint = MaterialTheme.colorScheme.tertiary)
        }
    }
}

/**
 * Tetikleyici açıklaması
 */
private fun getTriggerDescription(trigger: SpellTrigger): String {
    return when (trigger) {
        is SpellTrigger.SingleHandGesture -> {
            // gestureData'dan açı sayısını çıkar (gelecekte daha iyi bir yöntem kullanılabilir)
            val gestureInfo = if (trigger.gestureData.isNotEmpty() && trigger.gestureData != "{}") {
                " (Kalibre edildi)"
            } else {
                " (Kalibrasyon gerekli)"
            }
            "Tek El: ${trigger.gestureName}$gestureInfo"
        }
        is SpellTrigger.DoubleHandGesture -> "Çift El: ${trigger.leftGestureName} + ${trigger.rightGestureName}"
        is SpellTrigger.VoiceCommand -> {
            // ⚡⚡⚡ GÖREV E: Hassasiyet göster
            val sensitivityPercent = (trigger.sensitivity * 100).toInt()
            "🎤 Sesli Komut: \"${trigger.command}\" (Hassasiyet: %$sensitivityPercent)"
        }
        is SpellTrigger.Timer -> "Zamanlayıcı: ${trigger.delayMs}ms"
    }
}

/**
 * Eylem açıklaması
 */
private fun getActionDescription(action: SpellAction): String {
    return when (action) {
        is SpellAction.EmitParticles -> "Parçacık Püskürt (${action.particleCount} adet)"
        is SpellAction.AttractParticles -> "Parçacıkları Birleştir"
        is SpellAction.RepelParticles -> "Parçacıkları Dağıt"
        is SpellAction.DirectParticles -> "Parçacıkları Yönlendir"
        is SpellAction.VortexParticles -> "Parçacıkları Döndür"
        is SpellAction.ClearAllParticles -> "Tüm Parçacıkları Temizle"
        is SpellAction.ChangeParticleColor -> "Parçacık Rengini Değiştir"
        // 🌟 Gelişmiş Efekt Şablonları
        is SpellAction.MeteorRain -> "Meteor Yağmuru (${action.meteorCount.displayName})"
        is SpellAction.Snowstorm -> "Kar Fırtınası (${action.snowDensity.displayName})"
        is SpellAction.Explosion -> "Patlama Efekti (${action.explosionPower.displayName})"
        is SpellAction.Firework -> "Havai Fişek (${action.burstParticleCount.displayName})"
        is SpellAction.FadingAura -> "Solmayan Aura (${action.auraSize.displayName})"
    }
}

/**
 * Adım ekle dialog'u (basit versiyon - gelecekte genişletilebilir)
 */
@Composable
private fun AddStepDialog(
    viewModel: SpellStudioViewModel, // G88: ViewModel eklendi (calibration navigation için)
    onConfirm: (SpellTrigger, SpellAction) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTriggerType by remember { mutableStateOf<TriggerType?>(null) }
    var showVoiceCommandDialog by remember { mutableStateOf(false) }

    when {
        // ⚡⚡⚡ GÖREV E: Sesli Komut Dialog'u
        showVoiceCommandDialog -> {
            VoiceCommandDialog(
                onConfirm = { command, sensitivity ->
                    val voiceTrigger = SpellTrigger.VoiceCommand(
                        command = command,
                        sensitivity = sensitivity
                    )
                    val defaultAction = SpellAction.EmitParticles()
                    onConfirm(voiceTrigger, defaultAction)
                    onDismiss()
                },
                onDismiss = {
                    showVoiceCommandDialog = false
                }
            )
        }

        // Tetikleyici seçim dialogu
        selectedTriggerType == null -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.Add, null) },
                title = { Text(rememberLocalizedText("add_step")) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(rememberLocalizedText("select_trigger_type"))

                        // ⚡⚡⚡ GÖREV E: Sesli Komut seçeneği
                        OutlinedButton(
                            onClick = {
                                showVoiceCommandDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(rememberLocalizedText("voice_command_trigger"))
                        }

                        // El Hareketi seçeneği
                        OutlinedButton(
                            onClick = {
                                selectedTriggerType = TriggerType.HAND_GESTURE
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PanTool,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(rememberLocalizedText("hand_gesture_trigger"))
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(rememberLocalizedText("cancel_button"))
                    }
                }
            )
        }

        // G88: El hareketi seçildi → Tek el / Çift el seçimi
        selectedTriggerType == TriggerType.HAND_GESTURE -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = { Icon(Icons.Default.PanTool, null) },
                title = { Text(rememberLocalizedText("select_hand_mode")) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(rememberLocalizedText("select_single_or_double_hand"))

                        // G88: Tek el seçeneği → Calibration ekranına git
                        OutlinedButton(
                            onClick = {
                                viewModel.startTriggerCalibration("SINGLE_HAND")
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(rememberLocalizedText("single_hand_gesture"))
                        }

                        // G88: Çift el seçeneği → Calibration ekranına git
                        OutlinedButton(
                            onClick = {
                                viewModel.startTriggerCalibration("DOUBLE_HAND")
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(rememberLocalizedText("double_hand_gesture"))
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(rememberLocalizedText("cancel_button"))
                    }
                }
            )
        }
    }
}

private enum class TriggerType {
    VOICE_COMMAND,
    HAND_GESTURE
}
