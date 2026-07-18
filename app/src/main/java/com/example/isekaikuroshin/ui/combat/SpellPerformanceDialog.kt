package com.example.isekaikuroshin.ui.combat

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.isekaikuroshin.engine.SpellPerformanceAnalyzer
import com.example.isekaikuroshin.data.LanguageManager

/**
 * G�REV N - FAZ 5: B�y� Performans Sonu� Dialog
 *
 * Kullan1c1n1n b�y� performans1n1 g�sterir:
 * - Genel skor (y�zde ve rank)
 * - Her ad1m1n detay1
 * - Ba_ar1/Ba_ar1s1zl1k durumu
 * - Animasyonlu skor g�stergesi
 */
@Composable
fun SpellPerformanceDialog(
    result: SpellPerformanceAnalyzer.PerformanceResult,
    spellName: String,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    combatResult: com.example.isekaikuroshin.engine.SpellCombatHelper.SpellCombatResult? = null  // FAZ 6
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Ba_l1k
                Text(
                    text = "B�y� Performans1",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = spellName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Animasyonlu skor g�stergesi
                AnimatedScoreDisplay(result.overallScore)

                Spacer(modifier = Modifier.height(16.dp))

                // Ba_ar1 durumu
                SuccessIndicator(result.passingGrade)

                Spacer(modifier = Modifier.height(24.dp))

                // FAZ 6: Combat Result (Zar sonucu ve hasar)
                if (combatResult != null) {
                    CombatResultCard(combatResult)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Ad1m detaylar1
                Text(
                    text = "Ad1m Detaylar1",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(result.stepResults.values.toList()) { stepPerf ->
                        StepPerformanceCard(stepPerf)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Devam et butonu
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (result.passingGrade) {
                            Color(0xFF4CAF50)  // Ye_il
                        } else {
                            Color(0xFFFF9800)  // Turuncu
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (result.passingGrade) "Devam Et " else "Tekrar Dene",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Animasyonlu skor g�stergesi (dairesel progress + y�zde)
 */
@Composable
fun AnimatedScoreDisplay(score: Float) {
    var animatedScore by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(score) {
        animate(
            initialValue = 0f,
            targetValue = score,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animatedScore = value
        }
    }

    val percentage = SpellPerformanceAnalyzer.scoreToPercentage(animatedScore)
    val rank = SpellPerformanceAnalyzer.getRank(score)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(180.dp)
    ) {
        // Dairesel progress
        CircularProgressIndicator(
            progress = { animatedScore },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 12.dp,
            color = getScoreColor(score),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Skor metni
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$percentage%",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = getScoreColor(score)
            )
            Text(
                text = rank,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Ba_ar1/Ba_ar1s1zl1k g�stergesi
 */
@Composable
fun SuccessIndicator(passed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (passed) {
                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                } else {
                    Color(0xFFFF9800).copy(alpha = 0.1f)
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (passed) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (passed) Color(0xFF4CAF50) else Color(0xFFFF9800),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (passed) "Ba_ar1l1! B�y� kullan1ld1 " else "Yetersiz performans. Tekrar dene!",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (passed) Color(0xFF4CAF50) else Color(0xFFFF9800)
        )
    }
}

/**
 * Tek bir ad1m1n performans kart1
 */
@Composable
fun StepPerformanceCard(stepPerf: SpellPerformanceAnalyzer.StepPerformance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skor indicator (k���k daire)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(getScoreColor(stepPerf.score).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${SpellPerformanceAnalyzer.scoreToPercentage(stepPerf.score)}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = getScoreColor(stepPerf.score)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Feedback mesaj1
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stepPerf.triggerType.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = stepPerf.feedback,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * FAZ 6: Combat Result Card (Zar sonucu ve hasar)
 */
@Composable
fun CombatResultCard(combatResult: com.example.isekaikuroshin.engine.SpellCombatHelper.SpellCombatResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (combatResult.diceRoll.success) {
                Color(0xFF4CAF50).copy(alpha = 0.15f)
            } else {
                Color(0xFFFF5722).copy(alpha = 0.15f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Başlık
            Text(
                text = "⚔️ Savaş Sonucu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Zar sonucu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎲 Zar Atışı",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${combatResult.diceRoll.total} (${combatResult.advantageLevel.name})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "💥 Hasar",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${combatResult.spellPower}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (combatResult.diceRoll.success) Color(0xFF4CAF50) else Color(0xFFFF5722)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Modifier bilgisi
            Text(
                text = "Modifier: ${if (combatResult.totalModifier >= 0) "+" else ""}${combatResult.totalModifier}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sonuç mesajı
            Text(
                text = combatResult.successMessage,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (combatResult.diceRoll.success) Color(0xFF4CAF50) else Color(0xFFFF5722)
            )
        }
    }
}

/**
 * Skor rengini hesapla
 */
fun getScoreColor(score: Float): Color {
    return when {
        score >= 0.9f -> Color(0xFF4CAF50)   // Ye_il
        score >= 0.7f -> Color(0xFF8BC34A)   // A�1k ye_il
        score >= 0.5f -> Color(0xFFFF9800)   // Turuncu
        else -> Color(0xFFF44336)             // K1rm1z1
    }
}
