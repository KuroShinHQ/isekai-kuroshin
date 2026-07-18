package com.example.isekaikuroshin.ui.screens.training

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.isekaikuroshin.data.rememberLocalizedText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
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
fun ManaCoreScreen(
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    viewModel: ManaCoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // G91: Scaffold + 9 ikonlu navigation bar eklendi (tutarlılık için)
    Scaffold(
        bottomBar = {
            if (navController != null) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = "mana_cultivation",
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
            LeftInfoPanel(uiState = uiState, modifier = Modifier.weight(1f))
            CentralVisualization(modifier = Modifier.weight(1.5f))
            RightInfoPanel(uiState = uiState, modifier = Modifier.weight(1f))
        }
        
        BottomActionBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            onMeditateClicked = viewModel::onMeditateClicked,
            onCondenseClicked = viewModel::onCondenseClicked,
            onExpandClicked = viewModel::onExpandClicked,
            onPurifyClicked = viewModel::onPurifyClicked
        )

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

// --- UI Components ---

@Composable
private fun CentralVisualization(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        PulsingParticles()
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Body Silhouette",
            modifier = Modifier.size(350.dp),
            tint = Color.White.copy(alpha = 0.15f)
        )
        // Replaced static PNG with a procedural, animated core
        AnimatedManaCore(modifier = Modifier.size(120.dp))
    }
}

@Composable
private fun AnimatedManaCore(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "core_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val center = this.center
        val radius = (size.minDimension / 2) * pulse
        
        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Rotating inner shapes
        rotate(rotation, center) {
            // Main crystal shape
            drawPath(createCrystalPath(center, radius * 0.8f), color = Color.White, style = Stroke(width = 3f))
            // Inner lines
            drawPath(createCrystalPath(center, radius * 0.5f), color = Color.Cyan.copy(alpha = 0.8f), style = Stroke(width = 1.5f))
        }

        // Center core
        drawCircle(
            color = Color.White,
            radius = radius * 0.1f,
            center = center
        )
    }
}

private fun createCrystalPath(center: Offset, radius: Float): androidx.compose.ui.graphics.Path {
    return androidx.compose.ui.graphics.Path().apply {
        val points = 6
        for (i in 0..points) {
            val angle = i * (360f / points) + 90f
            val x = center.x + radius * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
            val y = center.y + radius * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

@Composable
private fun LeftInfoPanel(uiState: ManaCoreUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.holographicStyle()) {
        MetricText(label = "Mevcut Mana", value = "${uiState.currentMana} / ${uiState.maxMana} MP")
        LinearProgressIndicator(
            progress = { uiState.currentMana / uiState.maxMana.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.1f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        MetricText(label = "Mana Rejenerasyonu", value = "+${uiState.manaRegenRate} MP/s")
        Spacer(modifier = Modifier.height(16.dp))
        MetricText(label = "Çekirdek Saflığı", value = "${(uiState.corePurity * 100).toInt()}%")
        Spacer(modifier = Modifier.height(16.dp))
        MetricText(label = "Çekirdek Aşaması", value = uiState.coreStageName)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Element Uyumu", color = Color.White, fontWeight = FontWeight.Bold)
        uiState.elementalAffinities.forEach {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(imageVector = it.icon, contentDescription = it.name, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(it.name, color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun RightInfoPanel(uiState: ManaCoreUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.holographicStyle(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = uiState.timelineState.currentStage,
            fontFamily = orbitronFamily,
            fontSize = 20.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            uiState.timelineState.subStages.forEachIndexed { index, stage ->
                val isActive = index == uiState.timelineState.currentSubStageIndex
                TimelineItem(stageName = stage, isActive = isActive)
                if (index < uiState.timelineState.subStages.lastIndex) {
                    TimelineConnector()
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = "Kilitli", tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(uiState.timelineState.nextMajorStage, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
private fun BottomActionBar(
    modifier: Modifier = Modifier,
    onMeditateClicked: () -> Unit,
    onCondenseClicked: () -> Unit,
    onExpandClicked: () -> Unit,
    onPurifyClicked: () -> Unit
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
        ActionButton(icon = Icons.Default.Compress, text = rememberLocalizedText("condense"), onClick = onCondenseClicked)
        ActionButton(icon = Icons.Default.SelfImprovement, text = rememberLocalizedText("meditation"), onClick = onMeditateClicked, isPrimary = true)
        ActionButton(icon = Icons.Default.FilterVintage, text = rememberLocalizedText("purify"), onClick = onPurifyClicked)
        ActionButton(icon = Icons.Default.AspectRatio, text = rememberLocalizedText("expand"), onClick = onExpandClicked)
    }
}

// --- Helper and Style Components ---

@Composable
private fun ActionButton(
    icon: ImageVector, 
    text: String, 
    onClick: () -> Unit, 
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    val contentColor = if (isPrimary) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f)
    val sizeModifier = if (isPrimary) Modifier.size(64.dp) else Modifier.size(48.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.clickable(onClick = onClick).padding(4.dp)
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
private fun MetricText(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TimelineItem(stageName: String, isActive: Boolean) {
    val color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = R.drawable.ic_circle),
            contentDescription = "Stage point",
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(stageName, color = color, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun TimelineConnector() {
    Box(modifier = Modifier.padding(start = 5.dp).height(24.dp).width(2.dp).background(Color.Gray.copy(alpha = 0.3f)))
}

@Composable
private fun Modifier.holographicStyle(): Modifier = this
    .padding(16.dp)
    .fillMaxHeight(0.8f)
    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    .padding(16.dp)

@Composable
private fun PulsingParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val animatedRadius by infiniteTransition.animateFloat(
        initialValue = 50f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius_animation"
    )
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha_animation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) { 
        drawCircle(
            color = Color(0xFF8A2BE2).copy(alpha = animatedAlpha), // BlueViolet with animated alpha
            radius = animatedRadius,
            center = Offset(this.size.width / 2, this.size.height / 2)
        )
    }
}

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

// G90: Preview removed - ManaCoreViewModel now requires GameStateManager injection
// Preview fonksiyonu Hilt dependency injection olmadan çalışmaz