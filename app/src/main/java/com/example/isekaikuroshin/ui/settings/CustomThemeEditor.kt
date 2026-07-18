package com.example.isekaikuroshin.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.isekaikuroshin.data.*
import com.example.isekaikuroshin.data.rememberLocalizedText
import com.example.isekaikuroshin.ui.theme.DashboardColorScheme

@Composable
fun CustomThemeEditorSection(
    settings: SettingsData,
    dashboardColors: DashboardColorScheme
) {
    // Seçili custom tema numarasını belirleme
    val selectedCustomSlot = when (settings.uiSettings.theme) {
        "CUSTOM1" -> 1
        "CUSTOM2" -> 2
        "CUSTOM3" -> 3
        else -> null
    }

    // Sadece Custom tema seçiliyse göster
    if (selectedCustomSlot != null) {
        val currentTheme = when (selectedCustomSlot) {
            1 -> settings.uiSettings.customTheme1
            2 -> settings.uiSettings.customTheme2
            3 -> settings.uiSettings.customTheme3
            else -> settings.uiSettings.customTheme1
        }

        var primaryColor by remember(currentTheme.primary) { mutableStateOf(currentTheme.primary) }
        var surfaceColor by remember(currentTheme.surface) { mutableStateOf(currentTheme.surface) }
        var backgroundC by remember(currentTheme.background) { mutableStateOf(currentTheme.background) }
        var showSuccessMessage by remember { mutableStateOf(false) }

        SettingsCategory(
            title = rememberLocalizedText("theme_customizer"),
            icon = Icons.Default.ColorLens,
            dashboardColors = dashboardColors
        ) {
            // Primary Color
            ColorInputField(
                label = rememberLocalizedText("theme_primary_color"),
                colorHex = primaryColor,
                onColorChange = { primaryColor = it },
                dashboardColors = dashboardColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Surface Color
            ColorInputField(
                label = rememberLocalizedText("theme_surface_color"),
                colorHex = surfaceColor,
                onColorChange = { surfaceColor = it },
                dashboardColors = dashboardColors
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Background Color
            ColorInputField(
                label = rememberLocalizedText("theme_background_color"),
                colorHex = backgroundC,
                onColorChange = { backgroundC = it },
                dashboardColors = dashboardColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    val newTheme = CustomThemeColors(
                        primary = primaryColor,
                        surface = surfaceColor,
                        background = backgroundC
                    )

                    PersistentDataManager.updateSettingsData { settingsData ->
                        val updatedSettings = when (selectedCustomSlot) {
                            1 -> settingsData.copy(uiSettings = settingsData.uiSettings.copy(customTheme1 = newTheme))
                            2 -> settingsData.copy(uiSettings = settingsData.uiSettings.copy(customTheme2 = newTheme))
                            3 -> settingsData.copy(uiSettings = settingsData.uiSettings.copy(customTheme3 = newTheme))
                            else -> settingsData
                        }
                        updatedSettings
                    }
                    showSuccessMessage = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(rememberLocalizedText("theme_save_custom"))
            }

            // Success Message
            if (showSuccessMessage) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showSuccessMessage = false
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✓ " + rememberLocalizedText("theme_saved_success"),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorInputField(
    label: String,
    colorHex: String,
    onColorChange: (String) -> Unit,
    dashboardColors: DashboardColorScheme
) {
    var textValue by remember(colorHex) { mutableStateOf(colorHex) }
    var isValid by remember(colorHex) { mutableStateOf(isValidHexColor(colorHex)) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Color Preview
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (isValid) parseHexColor(textValue) else Color.Gray,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text Input
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue.uppercase()
                    isValid = isValidHexColor(newValue)
                    if (isValid) {
                        onColorChange(newValue.uppercase())
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(rememberLocalizedText("theme_hex_hint")) },
                isError = !isValid,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }

        if (!isValid && textValue.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rememberLocalizedText("theme_invalid_hex"),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Helper Functions
private fun isValidHexColor(hex: String): Boolean {
    return try {
        if (!hex.startsWith("#")) return false
        if (hex.length != 7) return false
        android.graphics.Color.parseColor(hex)
        true
    } catch (e: Exception) {
        false
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}
