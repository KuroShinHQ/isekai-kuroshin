package com.example.isekaikuroshin.ui.combat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.isekaikuroshin.combat.CombatAction
import com.example.isekaikuroshin.combat.CombatState
import com.example.isekaikuroshin.data.Enemy
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.data.PlayerStats
import kotlinx.coroutines.launch

/**
 * G95: Full Combat Screen - Phase 2A
 *
 * KURAL 9: rememberLocalizedText() for all text
 * KURAL 10: MaterialTheme.colorScheme for colors
 * KURAL 14: Performance optimizations (remember{}, derivedStateOf{})
 *
 * Features:
 * - Animated HP bars
 * - Enemy section with status effects
 * - Player section with stats
 * - Combat log (scrollable)
 * - Action buttons (Attack, Spell, Item, Run)
 * - Turn indicator
 * - Victory/Defeat overlays
 */

@Composable
fun CombatScreen(
    combatState: CombatState,
    playerStats: PlayerStats,
    onUseSpell: (String) -> Unit, // targetId
    onFreestyleAction: (String) -> Unit, // user input
    onRun: () -> Unit,
    onCombatEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    // KURAL 9: Localized text
    val screenTitle = rememberLocalizedText("combat_screen_title")
    val yourTurn = rememberLocalizedText("combat_your_turn")
    val enemyTurn = rememberLocalizedText("combat_enemy_turn")
    val waiting = rememberLocalizedText("combat_waiting")

    // Combat log state
    val combatLog = remember { mutableStateListOf<String>() }
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Track combat state changes for log updates
    LaunchedEffect(combatState) {
        when (combatState) {
            is CombatState.CombatStart -> {
                combatLog.clear()
                combatLog.add("⚔️ Combat started!")
            }
            is CombatState.PlayerTurnSelect -> {
                combatLog.add("🧙 Your turn!")
            }
            is CombatState.EnemyTurnExecute -> {
                combatLog.add("👹 Enemy's turn...")
            }
            else -> {}
        }
        // Auto-scroll to bottom
        if (combatLog.isNotEmpty()) {
            scope.launch {
                scrollState.animateScrollToItem(combatLog.size - 1)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background, // KURAL 10
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title Bar
            CombatTitleBar(
                title = screenTitle,
                subtitle = when (combatState) {
                    is CombatState.PlayerTurnSelect -> yourTurn
                    is CombatState.EnemyTurnExecute -> enemyTurn
                    else -> waiting
                }
            )

            // Enemy Section (Top 40%)
            EnemySection(
                enemies = when (combatState) {
                    is CombatState.CombatStart -> combatState.enemies
                    is CombatState.PlayerTurnSelect -> combatState.enemies
                    is CombatState.PlayerTurnExecute -> combatState.enemies
                    is CombatState.EnemyTurnExecute -> combatState.enemies
                    else -> emptyList()
                },
                currentTurnEntityId = when (combatState) {
                    is CombatState.EnemyTurnExecute -> combatState.enemyId
                    else -> null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
            )

            // Combat Log (Middle 20%)
            CombatLogSection(
                logs = combatLog,
                scrollState = scrollState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.2f)
            )

            // Player Section (Bottom 40%)
            PlayerSection(
                playerStats = playerStats,
                isPlayerTurn = combatState is CombatState.PlayerTurnSelect,
                onAttack = {
                    // TODO-G95: Implement attack action
                },
                onUseSpell = {
                    // TODO-G95: Open spell selection
                },
                onUseItem = {
                    // TODO-G95: Open item selection
                },
                onRun = onRun,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
            )
        }

        // Victory/Defeat Overlays
        when (combatState) {
            is CombatState.Victory -> {
                VictoryOverlay(
                    rewards = combatState.rewards,
                    onContinue = onCombatEnd
                )
            }
            is CombatState.Defeat -> {
                DefeatOverlay(
                    message = combatState.finalMessage,
                    onRetry = onCombatEnd
                )
            }
            is CombatState.RunSuccess -> {
                RunSuccessOverlay(
                    message = combatState.escapeMessage,
                    onContinue = onCombatEnd
                )
            }
            else -> {}
        }
    }
}

/**
 * Combat Title Bar - Shows combat status
 */
@Composable
private fun CombatTitleBar(
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer // KURAL 10
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer // KURAL 10
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) // KURAL 10
            )
        }
    }
}

/**
 * Enemy Section - Shows all enemies with HP bars
 */
@Composable
private fun EnemySection(
    enemies: List<Enemy>,
    currentTurnEntityId: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) // KURAL 10
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "👹 ${rememberLocalizedText("combat_enemies")}", // KURAL 9
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error // KURAL 10
            )

            if (enemies.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rememberLocalizedText("combat_no_enemies"), // KURAL 9
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // KURAL 10
                    )
                }
            } else {
                enemies.forEach { enemy ->
                    EnemyCard(
                        enemy = enemy,
                        isCurrentTurn = enemy.id == currentTurnEntityId,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Enemy Card - Individual enemy with HP bar
 */
@Composable
private fun EnemyCard(
    enemy: Enemy,
    isCurrentTurn: Boolean,
    modifier: Modifier = Modifier
) {
    // KURAL 14: Performance - remember calculations
    val hpPercentage = remember(enemy.hp, enemy.maxHp) {
        (enemy.hp.toFloat() / enemy.maxHp.toFloat()).coerceIn(0f, 1f)
    }

    // Pulsing animation for current turn
    val scale by animateFloatAsState(
        targetValue = if (isCurrentTurn) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "enemy_turn_pulse"
    )

    val hpLabel = rememberLocalizedText("combat_hp") // KURAL 9

    Card(
        modifier = modifier.scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTurn)
                MaterialTheme.colorScheme.error.copy(alpha = 0.2f) // KURAL 10
            else
                MaterialTheme.colorScheme.surface // KURAL 10
        ),
        border = if (isCurrentTurn) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.errorContainer
                    )
                )
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Enemy name + HP text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👹 ${enemy.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface // KURAL 10
                )
                Text(
                    text = "$hpLabel: ${enemy.hp}/${enemy.maxHp}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // KURAL 10
                )
            }

            // Animated HP Bar
            AnimatedHPBar(
                currentHp = enemy.hp,
                maxHp = enemy.maxHp,
                hpPercentage = hpPercentage,
                color = MaterialTheme.colorScheme.error // KURAL 10
            )
        }
    }
}

/**
 * Animated HP Bar - Smooth HP decrease animation
 */
@Composable
private fun AnimatedHPBar(
    currentHp: Int,
    maxHp: Int,
    hpPercentage: Float,
    color: androidx.compose.ui.graphics.Color
) {
    // KURAL 14: Performance - animateFloatAsState for smooth animation
    val animatedPercentage by animateFloatAsState(
        targetValue = hpPercentage,
        animationSpec = tween(durationMillis = 500),
        label = "hp_bar_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant) // KURAL 10
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedPercentage)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(color, color.copy(alpha = 0.7f))
                    )
                )
        )
    }
}

/**
 * Combat Log Section - Scrollable list of recent actions
 */
@Composable
private fun CombatLogSection(
    logs: List<String>,
    scrollState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // KURAL 10
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = "📜 ${rememberLocalizedText("combat_log")}", // KURAL 9
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface // KURAL 10
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // KURAL 10
                    )
                }
            }
        }
    }
}

/**
 * Player Section - Player stats + action buttons
 */
@Composable
private fun PlayerSection(
    playerStats: PlayerStats,
    isPlayerTurn: Boolean,
    onAttack: () -> Unit,
    onUseSpell: () -> Unit,
    onUseItem: () -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) // KURAL 10
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Player stats
            PlayerStatsDisplay(playerStats = playerStats)

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant // KURAL 10
            )

            // Action buttons
            ActionButtonsGrid(
                isEnabled = isPlayerTurn,
                onAttack = onAttack,
                onUseSpell = onUseSpell,
                onUseItem = onUseItem,
                onRun = onRun
            )
        }
    }
}

/**
 * Player Stats Display
 */
@Composable
private fun PlayerStatsDisplay(playerStats: PlayerStats) {
    val hpLabel = rememberLocalizedText("combat_hp") // KURAL 9
    val mpLabel = rememberLocalizedText("combat_mp")

    // KURAL 14: Performance - remember calculations
    val hpPercentage = remember(playerStats.hp, playerStats.maxHp) {
        (playerStats.hp.toFloat() / playerStats.maxHp.toFloat()).coerceIn(0f, 1f)
    }
    val mpPercentage = remember(playerStats.mp, playerStats.maxMp) {
        (playerStats.mp.toFloat() / playerStats.maxMp.toFloat()).coerceIn(0f, 1f)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🧙 ${rememberLocalizedText("combat_you")}", // KURAL 9
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary // KURAL 10
        )

        // HP Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "❤️ $hpLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface // KURAL 10
            )
            Text(
                text = "${playerStats.hp}/${playerStats.maxHp}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error // KURAL 10
            )
        }
        AnimatedHPBar(
            currentHp = playerStats.hp,
            maxHp = playerStats.maxHp,
            hpPercentage = hpPercentage,
            color = MaterialTheme.colorScheme.error // KURAL 10
        )

        // MP Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "💙 $mpLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface // KURAL 10
            )
            Text(
                text = "${playerStats.mp}/${playerStats.maxMp}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary // KURAL 10
            )
        }
        AnimatedHPBar(
            currentHp = playerStats.mp,
            maxHp = playerStats.maxMp,
            hpPercentage = mpPercentage,
            color = MaterialTheme.colorScheme.primary // KURAL 10
        )
    }
}

/**
 * Action Buttons Grid - 2x2 grid of combat actions
 */
@Composable
private fun ActionButtonsGrid(
    isEnabled: Boolean,
    onAttack: () -> Unit,
    onUseSpell: () -> Unit,
    onUseItem: () -> Unit,
    onRun: () -> Unit
) {
    // KURAL 9: Localized text
    val btnAttack = rememberLocalizedText("combat_btn_attack")
    val btnSpell = rememberLocalizedText("combat_btn_use_spell")
    val btnItem = rememberLocalizedText("combat_btn_use_item")
    val btnRun = rememberLocalizedText("combat_btn_run")

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Attack + Spell
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CombatActionButton(
                text = btnAttack,
                icon = Icons.Default.Gavel,
                enabled = isEnabled,
                onClick = onAttack,
                modifier = Modifier.weight(1f)
            )
            CombatActionButton(
                text = btnSpell,
                icon = Icons.Default.AutoAwesome,
                enabled = isEnabled,
                onClick = onUseSpell,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Item + Run
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CombatActionButton(
                text = btnItem,
                icon = Icons.Default.Inventory,
                enabled = isEnabled,
                onClick = onUseItem,
                modifier = Modifier.weight(1f)
            )
            CombatActionButton(
                text = btnRun,
                icon = Icons.Default.DirectionsRun,
                enabled = isEnabled,
                onClick = onRun,
                modifier = Modifier.weight(1f),
                isSecondary = true
            )
        }
    }
}

/**
 * Combat Action Button - Individual action button
 */
@Composable
private fun CombatActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSecondary)
                MaterialTheme.colorScheme.secondaryContainer // KURAL 10
            else
                MaterialTheme.colorScheme.primary,
            contentColor = if (isSecondary)
                MaterialTheme.colorScheme.onSecondaryContainer
            else
                MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Victory Overlay - Shown after winning combat
 */
@Composable
private fun VictoryOverlay(
    rewards: com.example.isekaikuroshin.combat.CombatRewards,
    onContinue: () -> Unit
) {
    val title = rememberLocalizedText("combat_victory_title") // KURAL 9
    val rewardsLabel = rememberLocalizedText("combat_rewards")
    val btnContinue = rememberLocalizedText("combat_btn_continue")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.8f)), // KURAL 10
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface // KURAL 10
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⭐ $title ⭐",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, // KURAL 10
                    textAlign = TextAlign.Center
                )

                Text(
                    text = rewardsLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface // KURAL 10
                )

                // XP Reward
                RewardItem(
                    icon = "⭐",
                    text = "+${rewards.xp} XP"
                )

                // Gold Reward
                RewardItem(
                    icon = "💰",
                    text = "+${rewards.gold} Gold"
                )

                // Item Rewards
                if (rewards.items.isNotEmpty()) {
                    rewards.items.forEach { item ->
                        RewardItem(
                            icon = "🗡️",
                            text = item
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
 * Reward Item - Individual reward display
 */
@Composable
private fun RewardItem(icon: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$icon $text",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface // KURAL 10
        )
    }
}

/**
 * Defeat Overlay - Shown after losing combat
 */
@Composable
private fun DefeatOverlay(
    message: String,
    onRetry: () -> Unit
) {
    val title = rememberLocalizedText("combat_defeat_title") // KURAL 9
    val btnRetry = rememberLocalizedText("combat_btn_retry")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.9f)), // KURAL 10
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface // KURAL 10
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "💀 $title 💀",
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
 * Run Success Overlay - Shown after successful escape
 */
@Composable
private fun RunSuccessOverlay(
    message: String,
    onContinue: () -> Unit
) {
    val title = rememberLocalizedText("combat_run_success") // KURAL 9
    val btnContinue = rememberLocalizedText("combat_btn_continue")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)), // KURAL 10
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface // KURAL 10
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🏃 $title",
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
 * KURAL 9: Always use LanguageManager for text
 */
@Composable
private fun rememberLocalizedText(key: String): String {
    val language by LanguageManager.currentLanguage.collectAsState()
    return LanguageManager.getText(key)
}
