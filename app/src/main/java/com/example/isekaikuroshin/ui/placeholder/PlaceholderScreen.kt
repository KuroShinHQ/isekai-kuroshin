package com.example.isekaikuroshin.ui.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.ui.theme.getDashboardColors

@Composable
fun PlaceholderScreen(
    title: String,
    description: String,
    navController: NavController
) {
    val dashboardColors = getDashboardColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dashboardColors.backgroundDark)
    ) {
        // Header without back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = dashboardColors.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .padding(bottom = 80.dp), // Space for bottom button
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🚧",
                fontSize = 80.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = dashboardColors.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = description,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = dashboardColors.holographicBg
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = rememberLocalizedText("feature_planned"),
                    fontSize = 14.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp),
                    lineHeight = 20.sp
                )
            }
        }

        // Back button at bottom-left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = dashboardColors.primary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = rememberLocalizedText("back"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FriendCard(character: com.example.isekaikuroshin.data.GameCharacter) {
    val dashboardColors = getDashboardColors()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = dashboardColors.holographicBg
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Green),
                contentAlignment = Alignment.Center
            ) {
                // G86: Use nameKey with LanguageManager
                val name = com.example.isekaikuroshin.data.LanguageManager.getText(character.nameKey)
                Text(
                    text = name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // G86: Use nameKey with LanguageManager
                val name = com.example.isekaikuroshin.data.LanguageManager.getText(character.nameKey)
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // G86: Use locationKey with LanguageManager
                if (character.locationKey.isNotEmpty()) {
                    val location = com.example.isekaikuroshin.data.LanguageManager.getText(character.locationKey)
                    Text(
                        text = "📍 $location",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Relationship
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        character.relationship >= 80 -> "❤️"
                        character.relationship >= 60 -> "😊"
                        character.relationship >= 40 -> "🙂"
                        else -> "😐"
                    },
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+${character.relationship}",
                    fontSize = 12.sp,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Specific placeholder screens
@Composable
fun QuestsScreen(navController: NavController) {
    PlaceholderScreen(
        title = rememberLocalizedText("quests"),
        description = "Günlük görevler, ana hikaye görevleri ve yan görevler bu ekranda yer alacak.",
        navController = navController
    )
}

@Composable
fun GuildScreen(navController: NavController) {
    PlaceholderScreen(
        title = rememberLocalizedText("guild"),
        description = "Lonca sistemi, takım aktiviteleri ve grup maceraları bu ekranda yer alacak.",
        navController = navController
    )
}

@Composable
fun FriendsScreen(navController: NavController) {
    val friendlyCharacters = remember {
        com.example.isekaikuroshin.utils.CharacterManager.getAllCharacters()
            .filter { it.relationship >= 10 && it.firstMetDay > 0 }
    }
    val dashboardColors = getDashboardColors()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dashboardColors.backgroundDark)
    ) {
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(bottom = 80.dp) // Space for bottom button
        ) {
            // Header (without back button)
            Text(
                text = rememberLocalizedText("friends"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = dashboardColors.primary,
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (friendlyCharacters.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = dashboardColors.holographicBg
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🤝",
                        fontSize = 48.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = rememberLocalizedText("no_friends_yet"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = rememberLocalizedText("build_relationships"),
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            } else {
                LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                    items(friendlyCharacters.sortedByDescending { it.relationship }) { character ->
                        FriendCard(character = character)
                    }
                }
            }
        }

        // Back button at bottom-left
        Button(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(
                containerColor = dashboardColors.primary
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = rememberLocalizedText("back"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun LeaderboardScreen(navController: NavController) {
    PlaceholderScreen(
        title = rememberLocalizedText("leaderboard"),
        description = "Sıralama tabloları, başarım karşılaştırmaları ve rekabet sistemi bu ekranda yer alacak.",
        navController = navController
    )
}