package com.example.isekaikuroshin.ui.exercise

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.rememberLocalizedText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseScreenTest(
    navController: NavController,
    viewModel: ExerciseViewModel = hiltViewModel()
) {
    Log.d("ExerciseScreenTest", "🎬 ExerciseScreenTest BAŞLATILDI!")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Exercise Screen") }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("✅ Exercise Screen Test")
                Text("Eğer bunu görüyorsan, navigation çalışıyor")
                Button(onClick = { navController.navigateUp() }) {
                    Text(rememberLocalizedText("back"))
                }
            }
        }
    }
}
