package com.example.isekaikuroshin.ui.spellstudio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.isekaikuroshin.data.spell.SpellRecipe
import com.example.isekaikuroshin.data.rememberLocalizedText

/**
 * Büyü Tasarım Stüdyosu - Ana Ekran
 *
 * Kullanıcının oluşturduğu büyülerin listesi ve "Yeni Büyü Yarat" butonu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpellStudioScreen(
    onNavigateBack: () -> Unit,
    viewModel: SpellStudioViewModel = hiltViewModel()
) {
    val userSpells by viewModel.userSpells.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var showNewSpellDialog by remember { mutableStateOf(false) }

    // UI state'e göre farklı ekranlar göster
    when (val state = uiState) {
        SpellStudioUiState.SpellList -> {
            // Ana liste ekranı
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(rememberLocalizedText("spell_design_studio")) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, rememberLocalizedText("back"))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { showNewSpellDialog = true },
                        icon = { Icon(Icons.Default.Add, rememberLocalizedText("new")) },
                        text = { Text(rememberLocalizedText("create_new_spell")) },
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            ) { padding ->
                SpellListContent(
                    spells = userSpells,
                    onSpellClick = { viewModel.editSpell(it) },
                    onTestClick = { viewModel.testSpell(it) },
                    onDeleteClick = { viewModel.deleteSpell(it.id) },
                    modifier = Modifier.padding(padding)
                )
            }

            // Yeni büyü dialog'u
            if (showNewSpellDialog) {
                NewSpellDialog(
                    onConfirm = { name ->
                        viewModel.createNewSpell(name)
                        showNewSpellDialog = false

                        // G83: Log spell creation
                        // TODO: Element bilgisi eklendikçe güncelle
                        com.example.isekaikuroshin.utils.EventLogger.logSpellCreation(
                            spellName = name,
                            elements = listOf("Unknown") // Placeholder - spell detayları sonra eklenecek
                        )
                    },
                    onDismiss = { showNewSpellDialog = false }
                )
            }
        }

        is SpellStudioUiState.EditingRecipe -> {
            // Büyü tarifi düzenleme ekranı
            SpellRecipeScreen(
                spell = state.spell,
                viewModel = viewModel
            )
        }

        // G88: Tetikleyici kalibrasyon ekranı (22 Ekim backup'tan geri eklendi)
        is SpellStudioUiState.CalibratingTrigger -> {
            TriggerCalibrationScreen(
                step = state.step,
                viewModel = viewModel
            )
        }

        is SpellStudioUiState.ConfiguringAction -> {
            // Eylem ayarları ekranı
            ActionConfigurationScreen(
                step = state.step,
                viewModel = viewModel
            )
        }

        is SpellStudioUiState.TestingSpell -> {
            // Büyü test ekranı
            SpellTestScreen(
                spell = state.spell,
                viewModel = viewModel
            )
        }
    }
}

/**
 * Büyü listesi içeriği
 */
@Composable
private fun SpellListContent(
    spells: List<SpellRecipe>,
    onSpellClick: (SpellRecipe) -> Unit,
    onTestClick: (SpellRecipe) -> Unit,
    onDeleteClick: (SpellRecipe) -> Unit,
    modifier: Modifier = Modifier
) {
    if (spells.isEmpty()) {
        // Boş durum
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                Text(
                    text = rememberLocalizedText("no_spells_yet"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = rememberLocalizedText("click_create_spell_hint"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(spells, key = { it.id }) { spell ->
                SpellCard(
                    spell = spell,
                    onClick = { onSpellClick(spell) },
                    onTestClick = { onTestClick(spell) },
                    onDeleteClick = { onDeleteClick(spell) }
                )
            }
        }
    }
}

/**
 * Büyü kartı
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpellCard(
    spell: SpellRecipe,
    onClick: () -> Unit,
    onTestClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Başlık ve adım sayısı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spell.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${spell.steps.size} ${rememberLocalizedText("steps")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Sil butonu
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, rememberLocalizedText("delete"), tint = MaterialTheme.colorScheme.error)
                }
            }

            // Açıklama (varsa)
            if (spell.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = spell.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test butonu
            FilledTonalButton(
                onClick = onTestClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, rememberLocalizedText("test"), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(rememberLocalizedText("test"))
            }
        }
    }

    // Silme onay dialog'u
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(rememberLocalizedText("delete_spell")) },
            text = { Text(rememberLocalizedText("delete_spell_confirm").replace("{name}", spell.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text(rememberLocalizedText("delete"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(rememberLocalizedText("cancel"))
                }
            }
        )
    }
}

/**
 * Yeni büyü dialog'u
 */
@Composable
private fun NewSpellDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoAwesome, null) },
        title = { Text(rememberLocalizedText("create_new_spell")) },
        text = {
            Column {
                Text(rememberLocalizedText("give_spell_name"))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(rememberLocalizedText("spell_name")) },
                    placeholder = { Text(rememberLocalizedText("spell_name_example")) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(rememberLocalizedText("create"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(rememberLocalizedText("cancel"))
            }
        }
    )
}
