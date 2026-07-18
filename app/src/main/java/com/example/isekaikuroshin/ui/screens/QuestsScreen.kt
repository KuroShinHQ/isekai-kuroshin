package com.example.isekaikuroshin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.ui.components.CardData
import com.example.isekaikuroshin.ui.components.CardRarity
import com.example.isekaikuroshin.ui.components.StandardCard
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import com.example.isekaikuroshin.data.rememberLocalizedText

private val backgroundColor = Color(0xFF0B1426)
private val accentColor = Color(0xFF00BFFF)
private val textColor = Color.White

// Sample Quest Data
private val sampleQuests = listOf(
    CardData.QuestCard(
        id = "main_quest_1",
        name = "Karanlığın Uyanışı",
        description = "Gölge Lordu'nun geri dönüşünü araştır ve krallığı tehdit eden kadim kötülüğü durdur.",
        imageRes = R.drawable.ic_launcher_foreground, // Placeholder
        rarity = CardRarity.LEGENDARY,
        suggestedLevel = 50,
        rewards = listOf("1500 XP", "Efsanevi Zırh Parçası")
    ),
    CardData.QuestCard(
        id = "main_quest_2",
        name = "Kayıp Kristal",
        description = "Hayat Kristali'ni bulmak için Unutulmuş Harabeler'e git.",
        imageRes = R.drawable.ic_launcher_foreground, // Placeholder
        rarity = CardRarity.LEGENDARY,
        suggestedLevel = 45,
        rewards = listOf("1200 XP", "Kadim Tılsım")
    ),
    CardData.QuestCard(
        id = "side_quest_1",
        name = "Tüccarın Sorunu",
        description = "Tüccar Elara'nın kayıp kargosunu haydut kampından geri al.",
        imageRes = R.drawable.ic_launcher_foreground, // Placeholder
        rarity = CardRarity.RARE,
        suggestedLevel = 25,
        rewards = listOf("300 XP", "50 Altın")
    ),
    CardData.QuestCard(
        id = "side_quest_2",
        name = "Zehirli Bataklık",
        description = "Bataklıktaki nadir bulunan bir bitkiyi toplayarak simyacıya yardım et.",
        imageRes = R.drawable.ic_launcher_foreground, // Placeholder
        rarity = CardRarity.RARE,
        suggestedLevel = 30,
        rewards = listOf("450 XP", "Nadir İksir Tarifi")
    ),
    CardData.QuestCard(
        id = "completed_quest_1",
        name = "İlk Adımlar",
        description = "Eğitimi tamamla ve maceraya hazır ol.",
        imageRes = R.drawable.ic_launcher_foreground, // Placeholder
        rarity = CardRarity.COMMON,
        suggestedLevel = 1,
        rewards = listOf("100 XP", "Acemi Kılıcı")
    )
)

@Composable
fun QuestsScreen(
    viewModel: QuestsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(top = 16.dp)
    ) {
        Text(
            text = rememberLocalizedText("quests_title"),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        TabRow(
            selectedTabIndex = uiState.selectedTabIndex,
            containerColor = backgroundColor,
            contentColor = accentColor,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTabIndex]),
                    height = 3.dp,
                    color = accentColor
                )
            },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            uiState.tabs.forEachIndexed { index, titleKey ->
                Tab(
                    selected = uiState.selectedTabIndex == index,
                    onClick = { viewModel.onTabSelected(index) },
                    text = {
                        Text(
                            text = rememberLocalizedText(titleKey),
                            color = if (uiState.selectedTabIndex == index) accentColor else textColor.copy(alpha = 0.7f),
                            fontWeight = if (uiState.selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState.quests, key = { it.id }) { quest ->
                StandardCard(data = quest)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun QuestsScreenPreview() {
    IsekaiKuroshinTheme {
        // This preview now uses the ViewModel with its sample data by default
        QuestsScreen()
    }
}
