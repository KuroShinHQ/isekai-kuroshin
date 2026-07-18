package com.example.isekaikuroshin.ui.combat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.isekaikuroshin.combat.CombatAction
import com.example.isekaikuroshin.combat.CombatRewards
import com.example.isekaikuroshin.combat.CombatState
import com.example.isekaikuroshin.data.Enemy
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.data.PlayerStats
import com.example.isekaikuroshin.data.rememberLocalizedText // G144

/**
 * G93: Basic Combat Dialog
 *
 * KURAL 9: rememberLocalizedText() for all text
 * KURAL 10: MaterialTheme.colorScheme for colors
 *
 * Phase 1 (G93): Simple dialog with 3 buttons
 * Phase 2 (G95): Full screen with animations, HP bars, etc.
 */

/**
 * Combat Dialog - Shown during combat
 */
@Composable
fun CombatDialog(
    combatState: CombatState,
    playerStats: PlayerStats,
    onUseSpell: () -> Unit,
    onFreestyleAction: () -> Unit,
    onRun: () -> Unit,
    onCombatEnd: () -> Unit,
    modifier: Modifier = Modifier,
    showFreestyleInput: Boolean = false, // G95: Show freestyle input field
    freestyleInputText: String = "", // G95: Freestyle input text
    onFreestyleInputChange: (String) -> Unit = {}, // G95: Text change callback
    onFreestyleSubmit: () -> Unit = {} // G95: Submit callback
) {
    // KURAL 9: Localized text
    val screenTitle = rememberLocalizedText("combat_screen_title")
    val yourTurn = rememberLocalizedText("combat_your_turn")
    val enemyTurn = rememberLocalizedText("combat_enemy_turn")
    val btnUseSpell = rememberLocalizedText("combat_btn_use_spell")
    val btnFreestyle = rememberLocalizedText("combat_btn_freestyle")
    val btnRun = rememberLocalizedText("combat_btn_run")
    val btnContinue = rememberLocalizedText("combat_btn_continue")
    val hpLabel = rememberLocalizedText("combat_hp")
    val mpLabel = rememberLocalizedText("combat_mp")

    when (combatState) {
        is CombatState.Idle -> {
            // No dialog
        }
        is CombatState.CombatStart -> {
            CombatActionDialog(
                title = screenTitle,
                subtitle = "⚔️ Combat Started!",
                enemies = combatState.enemies,
                playerStats = playerStats,
                showActions = false, // Wait for player turn
                hpLabel = hpLabel,
                mpLabel = mpLabel,
                onUseSpell = {},
                onFreestyle = {},
                onRun = {},
                onDismiss = {}
            )
        }
        is CombatState.PlayerTurnSelect -> {
            CombatActionDialog(
                title = screenTitle,
                subtitle = yourTurn,
                enemies = combatState.enemies,
                playerStats = playerStats,
                showActions = true, // Player can act
                hpLabel = hpLabel,
                mpLabel = mpLabel,
                onUseSpell = onUseSpell,
                onFreestyle = onFreestyleAction,
                onRun = onRun,
                onDismiss = {},
                showFreestyleInput = showFreestyleInput, // G95
                freestyleInputText = freestyleInputText, // G95
                onFreestyleInputChange = onFreestyleInputChange, // G95
                onFreestyleSubmit = onFreestyleSubmit // G95
            )
        }
        is CombatState.PlayerTurnExecute, is CombatState.EnemyTurnExecute -> {
            CombatActionDialog(
                title = screenTitle,
                subtitle = if (combatState is CombatState.EnemyTurnExecute) enemyTurn else yourTurn,
                enemies = (combatState as? CombatState.EnemyTurnExecute)?.enemies ?: emptyList(),
                playerStats = playerStats,
                showActions = false, // Wait for action to complete
                hpLabel = hpLabel,
                mpLabel = mpLabel,
                onUseSpell = {},
                onFreestyle = {},
                onRun = {},
                onDismiss = {}
            )
        }
        is CombatState.Victory -> {
            VictoryDialog(
                rewards = combatState.rewards,
                onContinue = onCombatEnd
            )
        }
        is CombatState.Defeat -> {
            DefeatDialog(
                message = combatState.finalMessage,
                onRetry = onCombatEnd
            )
        }
        is CombatState.RunSuccess -> {
            RunSuccessDialog(
                message = combatState.escapeMessage,
                onContinue = onCombatEnd
            )
        }
    }
}

/**
 * Combat Action Dialog - Main combat UI
 */
@Composable
private fun CombatActionDialog(
    title: String,
    subtitle: String,
    enemies: List<Enemy>,
    playerStats: PlayerStats,
    showActions: Boolean,
    hpLabel: String,
    mpLabel: String,
    onUseSpell: () -> Unit,
    onFreestyle: () -> Unit,
    onRun: () -> Unit,
    onDismiss: () -> Unit,
    showFreestyleInput: Boolean = false, // G95
    freestyleInputText: String = "", // G95
    onFreestyleInputChange: (String) -> Unit = {}, // G95
    onFreestyleSubmit: () -> Unit = {} // G95
) {
    // G144: Localized text for enemy section
    val enemiesLabel = rememberLocalizedText("combat_enemies")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface, // KURAL 10
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error, // KURAL 10
                    textAlign = TextAlign.Center
                )

                // Subtitle
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // KURAL 10
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant // KURAL 10
                )

                // Enemy Section
                if (enemies.isNotEmpty()) {
                    Text(
                        text = "👹 $enemiesLabel", // G144: KURAL 9 - Localized
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error // KURAL 10
                    )

                    enemies.forEach { enemy ->
                        EnemyCard(
                            enemy = enemy,
                            hpLabel = hpLabel
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Player Stats
                PlayerStatsCard(
                    playerStats = playerStats,
                    hpLabel = hpLabel,
                    mpLabel = mpLabel
                )

                // Action Buttons (only if showActions = true)
                if (showActions) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant // KURAL 10
                    )

                    // Button 1: Use Spell
                    Button(
                        onClick = onUseSpell,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary // KURAL 10
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rememberLocalizedText("combat_btn_use_spell"), // KURAL 9
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Button 2: Freestyle Action
                    OutlinedButton(
                        onClick = onFreestyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rememberLocalizedText("combat_btn_freestyle"), // KURAL 9
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Button 3: Run Away
                    TextButton(
                        onClick = onRun,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = rememberLocalizedText("combat_btn_run"), // KURAL 9
                            color = MaterialTheme.colorScheme.onSurfaceVariant // KURAL 10
                        )
                    }

                    // G95: Freestyle Input Field (shown when Freestyle Action clicked)
                    if (showFreestyleInput) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant // KURAL 10
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = rememberLocalizedText("combat_freestyle_input_title"), // KURAL 9
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // KURAL 10
                                )

                                OutlinedTextField(
                                    value = freestyleInputText,
                                    onValueChange = onFreestyleInputChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(rememberLocalizedText("combat_freestyle_input_placeholder")) // KURAL 9
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary, // KURAL 10
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Submit button
                                    Button(
                                        onClick = onFreestyleSubmit,
                                        enabled = freestyleInputText.isNotBlank(),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary // KURAL 10
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(rememberLocalizedText("combat_btn_submit")) // KURAL 9
                                    }

                                    // Cancel button
                                    OutlinedButton(
                                        onClick = onFreestyle, // Toggle to hide
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(rememberLocalizedText("combat_btn_cancel")) // KURAL 9
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * G144: Format enemy name if AI didn't provide displayName
 * Fallback: "enemy_goblin_1" -> "Goblin"
 */
private fun formatEnemyNameFallback(rawName: String): String {
    // Remove "enemy_" prefix
    val withoutPrefix = rawName.removePrefix("enemy_")
    // Remove trailing numbers
    val withoutNumber = withoutPrefix.replace(Regex("_\\d+$"), "")
    // Capitalize words
    return withoutNumber.split("_")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

/**
 * Enemy Card - Shows enemy HP with emoji
 * G144: Uses enemy.displayName (localized by AI based on current language)
 */
@Composable
private fun EnemyCard(
    enemy: Enemy,
    hpLabel: String
) {
    // G144: Use AI-provided displayName, fallback to formatted name
    val displayName = if (enemy.displayName.isNotEmpty()) {
        enemy.displayName
    } else {
        formatEnemyNameFallback(enemy.name)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer // KURAL 10
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "👹 $displayName", // G144: Formatted name
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer // KURAL 10
            )
            Text(
                text = "$hpLabel: ${enemy.hp}/${enemy.maxHp}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer // KURAL 10
            )
        }
    }
}

/**
 * Player Stats Card
 */
@Composable
private fun PlayerStatsCard(
    playerStats: PlayerStats,
    hpLabel: String,
    mpLabel: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer // KURAL 10
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "🧙 ${rememberLocalizedText("combat_you")}", // KURAL 9
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer // KURAL 10
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "❤️ $hpLabel: ${playerStats.hp}/${playerStats.maxHp}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer // KURAL 10
                )
                Text(
                    text = "💙 $mpLabel: ${playerStats.mp}/${playerStats.maxMp}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer // KURAL 10
                )
            }
        }
    }
}

/**
 * Victory Dialog
 */
@Composable
private fun VictoryDialog(
    rewards: CombatRewards,
    onContinue: () -> Unit
) {
    val title = rememberLocalizedText("combat_victory_title") // KURAL 9
    val message = rememberLocalizedText("combat_victory_message")
    val rewardsLabel = rememberLocalizedText("combat_rewards")
    val btnContinue = rememberLocalizedText("combat_btn_continue")

    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface, // KURAL 10
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, // KURAL 10
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // KURAL 10
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) // KURAL 10

                Text(
                    text = rewardsLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface // KURAL 10
                )

                Text(
                    text = "⭐ +${rewards.xp} XP",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary // KURAL 10
                )

                Text(
                    text = "💰 +${rewards.gold} Gold",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary // KURAL 10
                )

                if (rewards.items.isNotEmpty()) {
                    rewards.items.forEach { item ->
                        Text(
                            text = "🗡️ $item",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // KURAL 10
                        )
                    }
                }

                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary // KURAL 10
                    )
                ) {
                    Text(text = btnContinue) // KURAL 9
                }
            }
        }
    }
}

/**
 * Defeat Dialog
 */
@Composable
private fun DefeatDialog(
    message: String,
    onRetry: () -> Unit
) {
    val title = rememberLocalizedText("combat_defeat_title") // KURAL 9
    val btnRetry = rememberLocalizedText("combat_btn_retry")

    Dialog(
        onDismissRequest = onRetry,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface, // KURAL 10
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error, // KURAL 10
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // KURAL 10
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error // KURAL 10
                    )
                ) {
                    Text(text = btnRetry) // KURAL 9
                }
            }
        }
    }
}

/**
 * Run Success Dialog
 */
@Composable
private fun RunSuccessDialog(
    message: String,
    onContinue: () -> Unit
) {
    val title = rememberLocalizedText("combat_run_success") // KURAL 9
    val btnContinue = rememberLocalizedText("combat_btn_continue")

    Dialog(
        onDismissRequest = onContinue,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface, // KURAL 10
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, // KURAL 10
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // KURAL 10
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary // KURAL 10
                    )
                ) {
                    Text(text = btnContinue) // KURAL 9
                }
            }
        }
    }
}

/**
 * Helper function for localized text
 */
@Composable
private fun rememberLocalizedText(key: String): String {
    val language by LanguageManager.currentLanguage.collectAsState()
    return LanguageManager.getText(key)
}
