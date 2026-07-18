package com.example.isekaikuroshin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
// NavController için Preview'da rememberNavController kullanacağız
import androidx.navigation.compose.rememberNavController
import com.example.isekaikuroshin.R

import com.example.isekaikuroshin.ui.components.OverlayData
import com.example.isekaikuroshin.ui.components.SystemOverlay
import com.example.isekaikuroshin.ui.theme.IsekaiKuroshinTheme
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.data.PlayerState
// CampViewModel import'unuzu buraya eklemeniz gerekebilir, örneğin:
// import com.example.isekaikuroshin.ui.screens.CampViewModel

@Composable
fun CampScreen(
    navController: NavController,
    viewModel: CampViewModel = hiltViewModel()
) {
    var showOverlay by remember { mutableStateOf(false) }
    var overlayData by remember { mutableStateOf<OverlayData?>(null) }
    // SORUN 4: Collapsible navigation menu state
    var isMenuExpanded by remember { mutableStateOf(false) }

    // Localized texts for onClick lambdas
    val spiritualTrainingTitle = rememberLocalizedText("camp_spiritual_training_title")
    val spiritualChoiceQuestion = rememberLocalizedText("camp_spiritual_choice_question")
    val infoText = rememberLocalizedText("info")
    val gameSavedMessage = rememberLocalizedText("game_saved_karma_message")
    val campRestMessage = rememberLocalizedText("camp_rest_message")
    val ancientTechniquesText = rememberLocalizedText("ancient_techniques")
    val manaCoreText = rememberLocalizedText("mana_core")
    val qiCultivationText = rememberLocalizedText("qi_cultivation")

    // G91: CampScreen MainScreen içinde çalıştığı için kendi bottomBar'ına ihtiyacı yok
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Image(
            painter = painterResource(id = R.drawable.camp),
            contentDescription = "Atmospheric background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // ADIM-1: StatusPanel sol üst köşeye taşındı - yarı saydam, küçültülmüş
        StatusPanel(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp)
        )

        // SORUN 4: Collapsible Navigation Menu - replaces grid buttons
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 100.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Expandable menu items
                AnimatedVisibility(
                    visible = isMenuExpanded,
                    enter = expandVertically(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(300))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // G29-FIX: Training Physical REMOVED - Ancient Techniques moved to Training Spiritual
                        CompactMenuItem(
                            icon = Icons.Default.Favorite,
                            label = rememberLocalizedText("training_spiritual"),
                            onClick = {
                                overlayData = OverlayData.Choice(
                                    title = spiritualTrainingTitle,
                                    message = spiritualChoiceQuestion,
                                    options = listOf(
                                        manaCoreText to { navController.navigate("mana_cultivation") },
                                        qiCultivationText to { navController.navigate("qi_cultivation") },
                                        ancientTechniquesText to { navController.navigate("seal_practice") }
                                    )
                                )
                                showOverlay = true
                                isMenuExpanded = false
                            }
                        )
                        CompactMenuItem(
                            icon = Icons.Default.Settings,
                            label = rememberLocalizedText("save_game"),
                            onClick = {
                                viewModel.saveGame()
                                overlayData = OverlayData.Alert(infoText, gameSavedMessage)
                                showOverlay = true
                                isMenuExpanded = false
                            }
                        )
                        CompactMenuItem(
                            icon = Icons.Default.Place,
                            label = rememberLocalizedText("rest"),
                            onClick = {
                                viewModel.restAtCamp()
                                overlayData = OverlayData.Alert(infoText, campRestMessage)
                                showOverlay = true
                                isMenuExpanded = false
                            }
                        )
                        CompactMenuItem(
                            icon = Icons.Default.Handyman,
                            label = rememberLocalizedText("crafting"),
                            onClick = {
                                navController.navigate("crafting")
                                isMenuExpanded = false
                            }
                        )
                        CompactMenuItem(
                            icon = Icons.Default.AccountTree,
                            label = rememberLocalizedText("skill_tree"),
                            onClick = {
                                navController.navigate("skill_tree")
                                isMenuExpanded = false
                            }
                        )
                        CompactMenuItem(
                            icon = Icons.Default.AutoAwesome,
                            label = rememberLocalizedText("spell_studio_title"),
                            onClick = {
                                navController.navigate("spell_studio")
                                isMenuExpanded = false
                            }
                        )
                    }
                }

                // Main FAB toggle button
                FloatingActionButton(
                    onClick = { isMenuExpanded = !isMenuExpanded },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = if (isMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.Menu,
                        contentDescription = "Menu"
                    )
                }
            }
        }

        SystemOverlay(
            overlayData = overlayData,
            isVisible = showOverlay,
            onDismiss = { showOverlay = false }
        )
    }
}

@Composable
fun HUDActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glowColor = Color(0xFF00D1FF)
    val shape = RoundedCornerShape(8.dp)

    Button(
        onClick = onClick,
        modifier = modifier
            .size(150.dp, 120.dp)
            .border(
                width = 1.dp,
                brush = Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.6f), glowColor.copy(alpha = 0.2f), Color.Transparent),
                    radius = 120f
                ),
                shape = shape
            )
            .clip(shape),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0x900A1A2F),
            contentColor = Color.White
        ),
        shape = shape
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = glowColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun StatusPanel(
    modifier: Modifier = Modifier,
    viewModel: CampViewModel = hiltViewModel()
) {
    val gameState = viewModel.gameStateManager.gameState.collectAsState()
    val playerState = gameState.value.playerState

    // ADIM-1: Küçültülmüş, yarı saydam HP/Mana/Seviye göstergesi
    Card(
        modifier = modifier.width(160.dp).wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = Color(0xAA0A1A2F)), // Daha saydam
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Level
            Text(
                text = "Lv ${playerState.level}",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            // HP Bar
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "HP: ${playerState.currentHealth}/${playerState.maxHealth}",
                    color = Color(0xFFFF6B6B),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((playerState.currentHealth.toFloat() / playerState.maxHealth.toFloat()).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color(0xFFFF6B6B), RoundedCornerShape(3.dp))
                    )
                }
            }
            // Mana Bar
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "MP: ${playerState.currentMana}/${playerState.maxMana}",
                    color = Color(0xFF63B3ED),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((playerState.currentMana.toFloat() / playerState.maxMana.toFloat()).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(Color(0xFF63B3ED), RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

// SORUN 4: Compact menu item for collapsible navigation
@Composable
private fun CompactMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        modifier = Modifier.width(200.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CampScreenPreview() {
    IsekaiKuroshinTheme {
        @Suppress("UNUSED_VARIABLE")
        val navController = rememberNavController() // Preview için NavController

        // ÖNEMLİ: Bu önizlemenin çalışması için sahte (fake) bir CampViewModel sağlamanız gerekir,
        // çünkü hiltViewModel() önizlemelerde kullanılamaz.
        // Aşağıda sahte bir ViewModel oluşturma örneği bulunmaktadır.
        // Bunu kendi CampViewModel sınıfınıza/arayüzünüze göre uyarlamanız gerekecektir.
        //
        // class FakeCampViewModel : CampViewModel(/* gerekli constructor argümanları */) {
        //     override fun saveGame() {
        //         println("Preview: saveGame çağrıldı")
        //     }
        //     override fun restAtCamp() {
        //         println("Preview: restAtCamp çağrıldı")
        //     }
        //     // CampViewModel soyut bir sınıfsa veya CampScreen başka public üyelerini kullanıyorsa
        //     // diğer üyeleri de override etmeniz gerekebilir.
        //     // Eğer CampViewModel constructor'ında Hilt ile inject edilen bağımlılıklar varsa,
        //     // bunları sahte ViewModel için manuel olarak sağlamanız veya daha basit bir constructor kullanmanız gerekir.
        // }
        // val fakeViewModel = remember { FakeCampViewModel() } // Kendi FakeCampViewModel'inizi oluşturduktan sonra kullanın

        // Sahte ViewModel'inizi oluşturduktan sonra aşağıdaki CampScreen çağrısının yorumunu kaldırabilirsiniz:
        /*
        CampScreen(
            navController = navController,
            viewModel = fakeViewModel // Buraya kendi oluşturduğunuz sahte ViewModel örneğini ekleyin
        )
        */

        // Şimdilik bir yer tutucu metin gösteriliyor:
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Text("CampScreen Önizlemesi", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = rememberLocalizedText("preview_instruction") +
                       "1. CampViewModel için sahte (fake) bir sınıf oluşturun.\n" +
                       "2. Bu sahte sınıfı örnekleyin (örn: val fakeViewModel = FakeCampViewModel()).\n" +
                       "3. Yukarıdaki yorum satırını kaldırıp CampScreen(navController, fakeViewModel) çağrısını aktif edin.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}