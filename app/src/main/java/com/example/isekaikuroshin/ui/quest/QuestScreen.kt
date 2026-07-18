package com.example.isekaikuroshin.ui.quest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.isekaikuroshin.data.LanguageManager
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.ui.theme.getDashboardColors
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestScreen(
    navController: NavController,
    viewModel: QuestViewModel = hiltViewModel()
) {
    val dashboardColors = getDashboardColors()
    val uiState by viewModel.uiState.collectAsState()

    // Tab state: 0 = Ana Görevler, 1 = Yan Görevler
    var selectedTab by remember { mutableStateOf(0) }

    // Quest'leri ana/yan olarak ayır
    val mainQuests = uiState.activeQuests.filter { it.isDeveloperQuest }
    val sideQuests = uiState.activeQuests.filter { !it.isDeveloperQuest }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dashboardColors.backgroundDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }

                Text(
                    text = LanguageManager.getText("quests"),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = dashboardColors.primary
                )

                Spacer(modifier = Modifier.width(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = dashboardColors.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = dashboardColors.primary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "⭐ ${rememberLocalizedText("main_quests")} (${mainQuests.size})",
                            color = if (selectedTab == 0) dashboardColors.primary else Color.White.copy(alpha = 0.6f)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "📋 ${rememberLocalizedText("side_quests")} (${sideQuests.size})",
                            color = if (selectedTab == 1) dashboardColors.primary else Color.White.copy(alpha = 0.6f)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quest List
            val questsToShow = if (selectedTab == 0) mainQuests else sideQuests

            if (questsToShow.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTab == 0)
                            rememberLocalizedText("no_main_quest_yet")
                        else
                            rememberLocalizedText("no_active_quests"),
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(questsToShow) { quest ->
                        QuestCard(
                            quest = quest,
                            dashboardColors = dashboardColors,
                            isMainQuest = quest.isDeveloperQuest
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestCard(
    quest: com.example.isekaikuroshin.data.QuestEntry,
    dashboardColors: DashboardColorScheme,
    isMainQuest: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isMainQuest)
                dashboardColors.primary.copy(alpha = 0.15f) // Ana görev vurgusu
            else
                dashboardColors.holographicBg
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Quest icon
            Icon(
                imageVector = if (isMainQuest) Icons.Default.Check else Icons.AutoMirrored.Filled.Assignment,
                contentDescription = null,
                tint = if (isMainQuest) Color(0xFFFFD700) else dashboardColors.primary, // Altın rengi ana görev için
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Ana görev badge
                if (isMainQuest) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "⭐ ${rememberLocalizedText("main_quest_badge")}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            modifier = Modifier
                                .background(
                                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = quest.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMainQuest) Color(0xFFFFD700) else Color.White
                )

                Text(
                    text = quest.description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                if (quest.progress > 0) {
                    Column {
                        LinearProgressIndicator(
                            progress = { quest.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = if (isMainQuest) Color(0xFFFFD700) else dashboardColors.primary,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(quest.progress * 100).toInt()}${rememberLocalizedText("quest_progress_completed")}",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Giver ve due date bilgisi
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${rememberLocalizedText("quest_giver")} ${quest.giver}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                if (quest.dueDay > 0) {
                    Text(
                        text = "${rememberLocalizedText("quest_due_day")} ${quest.dueDay}",
                        fontSize = 12.sp,
                        color = if (quest.dueDay <= quest.startDay + 2)
                            Color(0xFFFF5722) // Kırmızı (acil)
                        else
                            Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}