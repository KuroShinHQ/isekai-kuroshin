package com.example.isekaikuroshin.data

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LanguageManager {
    // TODO-FIX-LANG-07: Default dil İngilizce olarak değiştirildi (TR -> EN)
    private val _currentLanguage = MutableStateFlow("EN") // Default English
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val translations get() = Translations.ALL

    fun setLanguage(language: String) {
        if (translations.containsKey(language)) {
            // G144: Language change logging
            val oldLanguage = _currentLanguage.value
            if (oldLanguage != language) {
                android.util.Log.w("LANG-CHANGE", "🌐 G144: Language changing: $oldLanguage → $language")
                val stackTrace = Thread.currentThread().stackTrace.take(6).joinToString("\n  ") { it.toString() }
                android.util.Log.w("LANG-CHANGE", "📍 G144: Call stack:\n  $stackTrace")
            }

            _currentLanguage.value = language
            // Save to persistent storage
            PersistentDataManager.updateSettingsData { settings ->
                settings.copy(
                    gameSettings = settings.gameSettings.copy(language = language)
                )
            }
        }
    }

    fun getText(key: String): String {
        return translations[_currentLanguage.value]?.get(key) ?: key
    }

    /**
     * Mevcut dil kodunu Android Speech Recognition formatında döndürür (örn: "tr-TR", "en-US")
     */
    fun getCurrentLanguageCode(): String {
        return when (_currentLanguage.value) {
            "TR" -> "tr-TR"
            "EN" -> "en-US"
            "DE" -> "de-DE"
            "SV" -> "sv-SE"
            "FR" -> "fr-FR"
            "ES" -> "es-ES"
            "JA" -> "ja-JP"
            else -> "en-US" // Default
        }
    }

    fun initializeFromSettings() {
        val savedLanguage = PersistentDataManager.gameData.value.settingsData.gameSettings.language
        _currentLanguage.value = savedLanguage
    }

    // ✨ A. Dil İsimleri Göster (User-Friendly)
    fun getLanguageDisplayName(code: String): String {
        return when(code) {
            "TR" -> "Türkçe 🇹🇷"
            "EN" -> "English 🇬🇧"
            "DE" -> "Deutsch 🇩🇪"
            "SV" -> "Svenska 🇸🇪"
            "FR" -> "Français 🇫🇷"
            "ES" -> "Español 🇪🇸"
            "JA" -> "日本語 🇯🇵"
            "KO" -> "한국어 🇰🇷"
            "ZH" -> "中文 🇨🇳"
            else -> code
        }
    }

    // ✨ B. Eksik Çeviri Kontrolü (Debug Mode)
    fun checkMissingTranslations(): Map<String, List<String>> {
        val allKeys = translations["TR"]?.keys ?: emptySet()
        val missingTranslations = mutableMapOf<String, List<String>>()

        translations.forEach { (lang, map) ->
            val missing = allKeys - map.keys
            if (missing.isNotEmpty()) {
                missingTranslations[lang] = missing.toList()
            }
        }

        return missingTranslations
    }

    // ✨ C. Tüm Desteklenen Dilleri Getir
    fun getSupportedLanguages(): List<String> {
        return translations.keys.toList()
    }

    // ✨ C. Çeviri İstatistikleri
    fun getTranslationStats(): Map<String, Int> {
        return translations.mapValues { (_, map) -> map.size }
    }

    // ✨ D. Kapitalizasyon Standartlaştırma (Alt Görev 2.2)
    /**
     * Metni Title Case formatına çevirir (Her Kelimenin İlk Harfi Büyük)
     * Örnek: "strength" -> "Strength", "INTELLIGENCE" -> "Intelligence"
     */
    fun formatStatName(text: String): String {
        if (text.isBlank()) return text

        // Özel durumlar (tümü büyük kalmalı)
        val allCapsWords = setOf("HP", "MP", "EXP", "STR", "AGI", "INT", "VIT", "XP")
        if (text.uppercase() in allCapsWords) return text.uppercase()

        // Title Case: Her kelimenin ilk harfi büyük, geri kalanı küçük
        return text.lowercase()
            .split(" ", "-", "_")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    /**
     * Stat değerlerini formatlar (sayı + metin)
     * Örnek: "123 strength" -> "123 Strength"
     */
    fun formatStatDisplay(value: Any, statName: String): String {
        return "$value ${formatStatName(statName)}"
    }
}

@Composable
fun rememberLocalizedText(key: String): String {
    val language by LanguageManager.currentLanguage.collectAsState()
    return remember(language, key) {
        LanguageManager.getText(key)
    }
}