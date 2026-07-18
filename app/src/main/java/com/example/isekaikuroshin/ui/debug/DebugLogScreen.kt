package com.example.isekaikuroshin.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.isekaikuroshin.utils.GameLogger
import com.example.isekaikuroshin.utils.StoryContextManager
import kotlinx.coroutines.launch

/**
 * Debug Log Screen - Test sonuçlarını görüntülemek için
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(navController: NavController) {
    var logContent by remember { mutableStateOf("Loading logs...") }
    var savedDays by remember { mutableStateOf<List<Int>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Log dosyasını yükle
    LaunchedEffect(Unit) {
        scope.launch {
            logContent = GameLogger.readLogFile()
            savedDays = StoryContextManager.getSavedDays()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1426))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🐛 Debug Logs",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Text("Back", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        logContent = GameLogger.readLogFile()
                        savedDays = StoryContextManager.getSavedDays()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Text("🔄 Refresh", color = Color.White)
            }

            Button(
                onClick = {
                    GameLogger.clearLogs()
                    logContent = "Logs cleared."
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Text("🗑️ Clear", color = Color.White)
            }

            Button(
                onClick = {
                    scope.launch {
                        StoryContextManager.debugLogAllContexts()
                        logContent = GameLogger.readLogFile()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
            ) {
                Text("📖 Story Debug", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Saved story days info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "📚 Saved Story Days: ${savedDays.size}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (savedDays.isNotEmpty()) {
                    Text(
                        text = "Days: ${savedDays.joinToString(", ")}",
                        color = Color(0xFFCCCCCC),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Log content
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val logLines = logContent.split("\n").filter { it.isNotBlank() }

                items(logLines) { line ->
                    val color = when {
                        line.contains("[ERROR]") -> Color(0xFFFF4444)
                        line.contains("[SESSION]") -> MaterialTheme.colorScheme.primary
                        line.contains("[TIME_SYSTEM]") -> Color(0xFF32CD32)
                        line.contains("[STORY_SYSTEM]") -> Color(0xFFFFB347)
                        line.contains("[AI_SYSTEM]") -> Color(0xFF9370DB)
                        line.contains("[SAVE_SYSTEM]") -> Color(0xFF20B2AA)
                        line.contains("[DICE_SYSTEM]") -> Color(0xFFFF6347)
                        else -> Color(0xFFCCCCCC)
                    }

                    Text(
                        text = line,
                        color = color,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 12.sp
                    )
                }
            }
        }
    }
}