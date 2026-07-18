package com.example.isekaikuroshin.ui.screens.training

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import com.example.isekaikuroshin.data.rememberLocalizedText
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import com.example.isekaikuroshin.ui.theme.orbitronFamily

@Composable
fun QiCultivationScreen(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    viewModel: QiCultivationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // G91: Scaffold + 9 ikonlu navigation bar eklendi (tutarlılık için)
    Scaffold(
        bottomBar = {
            if (navController != null) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = "qi_cultivation",
                    selectedItem = com.example.isekaikuroshin.ui.navigation.BottomNavItem.Camp
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LeftDantianPanel(uiState = uiState, modifier = Modifier.weight(1f))
            ChakraSystemVisualization(modifier = Modifier.weight(1.5f))
            RightRealmPanel(uiState = uiState, modifier = Modifier.weight(1f))
        }
        BottomQiActionBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onMeditateClicked = viewModel::onMeditateClicked,
            onBreakthroughClicked = viewModel::onBreakthroughClicked,
            onTechniquesClicked = viewModel::onTechniquesClicked
        )
        // Add a back button
        IconButton(
            onClick = { navController?.popBackStack() },
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Geri Dön",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(32.dp)
            )
        }
        }
    }
}

// --- Main UI Components ---

@Composable
private fun ChakraSystemVisualization(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        // Silhouette PNG removed as requested
        // Placeholder for all 7 chakras
    }
}

@Composable
private fun LeftDantianPanel(uiState: QiCultivationUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.holographicStyle()) {
        MetricText(label = "Mevcut Alan", value = uiState.currentStageName)
        Spacer(modifier = Modifier.height(16.dp))
        MetricText(label = "Qi Seviyesi", value = uiState.qiLevel.toString())
        Spacer(modifier = Modifier.height(16.dp))
        MetricText(label = "Mevcut Qi", value = "${uiState.currentQi} / ${uiState.maxQi}")
        LinearProgressIndicator(
            progress = { uiState.currentQi / uiState.maxQi.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        MetricText(label = "Saflık", value = "${(uiState.qiPurity * 100).toInt()}%")
        LinearProgressIndicator(
            progress = { uiState.qiPurity },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            color = Color.White,
            trackColor = Color.Magenta.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun RightRealmPanel(uiState: QiCultivationUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.holographicStyle()) {
        LazyColumn {
            itemsIndexed(uiState.cultivationRealms) { index, realm ->
                val isActive = index == uiState.currentRealmIndex
                RealmTimelineItem(realmInfo = realm, isActive = isActive)
                if (index < uiState.cultivationRealms.lastIndex) {
                    TimelineConnector()
                }
            }
        }
    }
}

@Composable
private fun BottomQiActionBar(
    modifier: Modifier = Modifier,
    onMeditateClicked: () -> Unit,
    onBreakthroughClicked: () -> Unit,
    onTechniquesClicked: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QiActionButton(icon = Icons.AutoMirrored.Filled.TrendingUp, text = rememberLocalizedText("breakthrough"), onClick = onBreakthroughClicked)
        QiActionButton(icon = Icons.Default.SelfImprovement, text = rememberLocalizedText("meditation"), onClick = onMeditateClicked, isPrimary = true)
        QiActionButton(icon = Icons.AutoMirrored.Filled.MenuBook, text = rememberLocalizedText("techniques"), onClick = onTechniquesClicked)
    }
}

// --- Helper and Style Components ---

@Composable
private fun QiActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    val contentColor = if (isPrimary) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f)
    val sizeModifier = if (isPrimary) Modifier.size(56.dp) else Modifier.size(40.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = sizeModifier
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text, color = contentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RealmTimelineItem(realmInfo: RealmInfo, isActive: Boolean) {
    val contentColor = when {
        isActive -> MaterialTheme.colorScheme.primary
        realmInfo.isUnlocked -> Color.White
        else -> Color.Gray.copy(alpha = 0.5f)
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Icon(
            painter = painterResource(id = R.drawable.ic_circle),
            contentDescription = "Realm point",
            tint = contentColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(realmInfo.name, color = contentColor, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
        if (!realmInfo.isUnlocked) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.Lock, contentDescription = "Kilitli", tint = contentColor, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun TimelineConnector() {
    Box(modifier = Modifier.padding(start = 5.dp).height(24.dp).width(2.dp).background(Color.Gray.copy(alpha = 0.3f)))
}

@Composable
private fun MetricText(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Modifier.holographicStyle(): Modifier = this
    .padding(16.dp)
    .fillMaxHeight(0.8f)
    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    .padding(16.dp)

// G91: 9 ikonlu navigation bar component (NavController tabanlı)
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

// G91: Preview removed - QiCultivationViewModel now requires Hilt DI