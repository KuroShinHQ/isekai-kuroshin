package com.example.isekaikuroshin.ui.intro

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.isekaikuroshin.data.rememberLocalizedText

/**
 * GÖREV I.2: Yasal Uyarı ve Gizlilik Ekranı
 * KVKK/GDPR uyumluluğu için kullanıcıdan açık rıza alır
 */
@Composable
fun LegalConsentScreen(
    onConsentAccepted: () -> Unit,
    onConsentRejected: () -> Unit
) {
    var scrolledToBottom by remember { mutableStateOf(false) }
    var checkboxState by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val activity = LocalContext.current as? Activity

    // Scroll sonu kontrolü
    LaunchedEffect(scrollState.value) {
        if (scrollState.value >= scrollState.maxValue - 50) {
            scrolledToBottom = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Başlık
            Text(
                text = rememberLocalizedText("legal_consent_title"),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Kaydırılabilir metin kutusu
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    // 18+ Uyarısı
                    SectionHeader(rememberLocalizedText("legal_age_warning"))
                    LegalText(rememberLocalizedText("legal_age_warning_text"))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Veri Toplama
                    SectionHeader(rememberLocalizedText("legal_data_collection"))
                    LegalText(rememberLocalizedText("legal_data_collection_text"))
                    BulletPoint(rememberLocalizedText("legal_data_exercise"))
                    BulletPoint(rememberLocalizedText("legal_data_journal"))
                    BulletPoint(rememberLocalizedText("legal_data_gameplay"))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Google API Kullanımı
                    SectionHeader(rememberLocalizedText("legal_google_api"))
                    LegalText(rememberLocalizedText("legal_google_api_text"))
                    BulletPoint(rememberLocalizedText("legal_google_gemini"))
                    BulletPoint(rememberLocalizedText("legal_google_translation"))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Üçüncü Taraf Paylaşımı
                    SectionHeader(rememberLocalizedText("legal_third_party"))
                    LegalText(rememberLocalizedText("legal_third_party_text"))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Kullanıcı Hakları (KVKK)
                    SectionHeader(rememberLocalizedText("legal_user_rights"))
                    LegalText(rememberLocalizedText("legal_user_rights_text"))
                    BulletPoint(rememberLocalizedText("legal_right_access"))
                    BulletPoint(rememberLocalizedText("legal_right_delete"))
                    BulletPoint(rememberLocalizedText("legal_right_export"))

                    Spacer(modifier = Modifier.height(16.dp))

                    // İletişim
                    SectionHeader(rememberLocalizedText("legal_contact"))
                    LegalText(rememberLocalizedText("legal_contact_text"))

                    // Scroll sonunu belirtmek için boşluk
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Checkbox ve Onay Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checkboxState,
                    onCheckedChange = { checkboxState = it },
                    enabled = scrolledToBottom
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = rememberLocalizedText("legal_checkbox_text"),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Butonlar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Uygulamayı kapat
                        activity?.finish()
                        onConsentRejected()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(rememberLocalizedText("legal_reject"))
                }

                Button(
                    onClick = { onConsentAccepted() },
                    enabled = checkboxState && scrolledToBottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(rememberLocalizedText("legal_accept"))
                }
            }
        }
    }
}

/**
 * Bölüm başlığı komponenti
 */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

/**
 * Normal yasal metin komponenti
 */
@Composable
private fun LegalText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Madde işaretli liste elemanı
 */
@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
