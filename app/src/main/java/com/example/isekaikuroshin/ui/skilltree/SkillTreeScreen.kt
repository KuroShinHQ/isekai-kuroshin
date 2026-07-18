package com.example.isekaikuroshin.ui.skilltree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.SkillNode
import com.example.isekaikuroshin.data.rememberLocalizedText
import kotlin.math.max
import kotlin.math.min
import android.util.Log
import com.example.isekaikuroshin.ui.components.ElementAffinityCard
import com.example.isekaikuroshin.data.PersistentDataManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillTreeScreen(
    navController: NavController, // TODO-FIX-CAMP-06: NavController eklendi
    viewModel: SkillTreeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    var selectedSkill by remember { mutableStateOf<SkillNode?>(null) }
    var zoomLevel by remember { mutableStateOf(1.0f) }

    // SORUN 3: Element Affinity moved from CharacterStatus to SkillTree
    val gameData by PersistentDataManager.gameData.collectAsState()

    // G31: Element Affinity collapsible state
    var isAffinityExpanded by remember { mutableStateOf(false) }

    // Zoom constraints
    val minZoom = 0.5f
    val maxZoom = 2.5f

    // TODO-FIX-CAMP-06: Scaffold + 9 ikonlu navigation bar eklendi
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                currentRoute = "skill_tree",
                selectedItem = com.example.isekaikuroshin.ui.navigation.BottomNavItem.Camp
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A1A))
                .padding(paddingValues)
        ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with skill points
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A2332)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = rememberLocalizedText("skill_tree_title"),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = rememberLocalizedText("develop_your_skills"),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        // G64: Display learned skills count
                        val learnedSkillsCount = gameData.playerData.skills.size
                        if (learnedSkillsCount > 0) {
                            Text(
                                text = "🎓 Öğrenilen Yetenekler: $learnedSkillsCount",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF00BFFF),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF00BFFF).copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "Puan: ${uiState.skillPoints}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00BFFF),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // G31: Collapsible Element Affinity - User-friendly toggle
            Column {
                // Header - Always visible
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A2332)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rememberLocalizedText("elementAffinity"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { isAffinityExpanded = !isAffinityExpanded }) {
                            Icon(
                                imageVector = if (isAffinityExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isAffinityExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Expandable content
                AnimatedVisibility(
                    visible = isAffinityExpanded,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    ElementAffinityCard(
                        affinities = gameData.playerData.elementAffinities.affinities
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skill tree canvas
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A2332)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .graphicsLayer(
                                scaleX = zoomLevel,
                                scaleY = zoomLevel
                            )
                            .pointerInput(uiState.skillNodes, zoomLevel) {
                                detectTapGestures { offset ->
                                    val canvasWidth = size.width.toFloat()
                                    val canvasHeight = size.height.toFloat()
                                    val adjustedClickRadius = with(density) { (35.dp * zoomLevel).toPx() }

                                    // Adjust offset for zoom
                                    val adjustedOffset = Offset(
                                        offset.x / zoomLevel,
                                        offset.y / zoomLevel
                                    )

                                    val clickedNode = uiState.skillNodes.find { node ->
                                        val nodeCenter = Offset(
                                            node.x * canvasWidth,
                                            node.y * canvasHeight
                                        )
                                        (adjustedOffset - nodeCenter).getDistance() <= adjustedClickRadius / zoomLevel
                                    }

                                    clickedNode?.let { skill ->
                                        val isUnlocked = uiState.unlockedSkills.contains(skill.id)
                                        val isAvailable = !isUnlocked &&
                                            (skill.parentId == null ||
                                             uiState.unlockedSkills.contains(skill.parentId))

                                        if (isAvailable && uiState.skillPoints > 0) {
                                            viewModel.unlockSkill(skill.id)
                                        } else {
                                            selectedSkill = skill
                                        }
                                    }
                                }
                            }
                    ) {
                        val baseNodeRadius = 35f
                        val nodeRadius = baseNodeRadius
                        val nodePositions = uiState.skillNodes.associate {
                            it.id to Offset(it.x * size.width, it.y * size.height)
                        }

                        // Draw connections first
                        uiState.skillNodes.forEach { node ->
                            node.parentId?.let { parentId ->
                                val parentPos = nodePositions[parentId]
                                val nodePos = nodePositions[node.id]
                                if (parentPos != null && nodePos != null) {
                                    val isParentUnlocked = uiState.unlockedSkills.contains(parentId)
                                    val isNodeUnlocked = uiState.unlockedSkills.contains(node.id)

                                    val lineColor = when {
                                        isParentUnlocked && isNodeUnlocked -> Color(0xFF00BFFF)
                                        isParentUnlocked -> Color(0xFF555555)
                                        else -> Color(0xFF202020)
                                    }

                                    drawLine(
                                        color = lineColor,
                                        start = parentPos,
                                        end = nodePos,
                                        strokeWidth = 6f
                                    )
                                }
                            }
                        }

                        // Draw nodes
                        uiState.skillNodes.forEach { node ->
                            val isUnlocked = uiState.unlockedSkills.contains(node.id)
                            val isAvailable = !isUnlocked &&
                                (node.parentId == null ||
                                 uiState.unlockedSkills.contains(node.parentId))
                            val center = nodePositions[node.id] ?: return@forEach

                            // Node styling based on state
                            val (nodeColor, borderColor, innerColor) = when {
                                isUnlocked -> Triple(
                                    Color(0xFFFFD700),
                                    Color(0xFFFF8C00),
                                    Color(0xFFFFA500)
                                )
                                isAvailable && uiState.skillPoints > 0 -> Triple(
                                    Color(0xFF00BFFF),
                                    Color(0xFF0080FF),
                                    Color(0xFF4A90E2)
                                )
                                isAvailable -> Triple(
                                    Color(0xFF666666),
                                    Color(0xFF888888),
                                    Color(0xFF555555)
                                )
                                else -> Triple(
                                    Color(0xFF333333),
                                    Color(0xFF444444),
                                    Color(0xFF222222)
                                )
                            }

                            // Draw node with glow effect for available skills
                            if (isAvailable && uiState.skillPoints > 0) {
                                drawCircle(
                                    color = nodeColor.copy(alpha = 0.3f),
                                    radius = nodeRadius + 8f,
                                    center = center
                                )
                            }

                            drawCircle(
                                color = borderColor,
                                radius = nodeRadius + 3f,
                                center = center
                            )
                            drawCircle(
                                color = nodeColor,
                                radius = nodeRadius,
                                center = center
                            )
                            drawCircle(
                                color = innerColor,
                                radius = nodeRadius * 0.7f,
                                center = center
                            )
                        }
                    }

                    // Top right controls
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // TODO-FIX-CAMP-06: Back button kaldırıldı - navigation bar ile değiştirildi
                        // Zoom controls
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {

                            // Zoom in button
                            FloatingActionButton(
                                onClick = {
                                    zoomLevel = min(maxZoom, zoomLevel + 0.3f)
                                },
                                modifier = Modifier.size(40.dp),
                                containerColor = Color(0xFF00BFFF),
                                contentColor = Color.White
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Yakınlaştır",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Zoom out button
                            FloatingActionButton(
                                onClick = {
                                    zoomLevel = max(minZoom, zoomLevel - 0.3f)
                                },
                                modifier = Modifier.size(40.dp),
                                containerColor = Color(0xFF00BFFF),
                                contentColor = Color.White
                            ) {
                                Icon(
                                    Icons.Filled.Remove,
                                    contentDescription = "Uzaklaştır",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Zoom level indicator
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF00BFFF).copy(alpha = 0.2f)
                                ),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "${(zoomLevel * 100).toInt()}%",
                                        fontSize = 8.sp,
                                        color = Color(0xFF00BFFF),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Info button
                        FloatingActionButton(
                            onClick = {
                                selectedSkill = uiState.skillNodes.firstOrNull()
                            },
                            modifier = Modifier.size(40.dp),
                            containerColor = Color(0xFF00BFFF).copy(alpha = 0.8f),
                            contentColor = Color.White
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = "Bilgi",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Skill details dialog
        selectedSkill?.let { skill ->
            SkillDetailsDialog(
                skill = skill,
                isUnlocked = uiState.unlockedSkills.contains(skill.id),
                isAvailable = !uiState.unlockedSkills.contains(skill.id) &&
                    (skill.parentId == null || uiState.unlockedSkills.contains(skill.parentId)),
                canAfford = uiState.skillPoints > 0,
                onDismiss = { selectedSkill = null },
                onUnlock = {
                    viewModel.unlockSkill(skill.id)
                    selectedSkill = null
                }
            )
        }
    }
    }
}

@Composable
private fun SkillDetailsDialog(
    skill: SkillNode,
    isUnlocked: Boolean,
    isAvailable: Boolean,
    canAfford: Boolean,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A2332)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = skill.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = skill.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Status indicator
                val statusText = when {
                    isUnlocked -> "✅ Öğrenildi"
                    isAvailable && canAfford -> "🔓 Öğrenilebilir"
                    isAvailable -> "❌ Yetenek puanı yetersiz"
                    else -> "🔒 Önce bağımlılık tamamlanmalı"
                }

                val statusColor = when {
                    isUnlocked -> Color(0xFF4CAF50)
                    isAvailable && canAfford -> Color(0xFF00BFFF)
                    else -> Color(0xFFFF5722)
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = statusColor.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kapat")
                    }

                    if (isAvailable && canAfford && !isUnlocked) {
                        Button(
                            onClick = onUnlock,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00BFFF)
                            )
                        ) {
                            Text(rememberLocalizedText("learn"), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// TODO-FIX-CAMP-06: 9 ikonlu navigation bar component (NavController tabanlı)
@Composable
private fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String,
    selectedItem: com.example.isekaikuroshin.ui.navigation.BottomNavItem
) {
    val items = listOf(
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Status,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Inventory,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Quests,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Catalog,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Camp,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Map,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Journal,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.HealthHub,
        com.example.isekaikuroshin.ui.navigation.BottomNavItem.Settings
    )

    NavigationBar {
        items.forEach { item ->
            val localizedTitle = com.example.isekaikuroshin.data.rememberLocalizedText(item.titleKey)
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = localizedTitle) },
                label = { Text(text = localizedTitle) },
                selected = item == selectedItem,
                onClick = {
                    // G91 FIX: Sub-screen'den MainScreen sayfalarına dönmek için popBackStack kullan
                    if (item != selectedItem) {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}