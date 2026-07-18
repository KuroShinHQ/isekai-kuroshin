package com.example.isekaikuroshin.data

import android.util.Log

/**
 * Developer tool for checking translation quality and consistency
 */
object TranslationChecker {

    data class TranslationIssue(
        val key: String,
        val type: IssueType,
        val trText: String?,
        val enText: String?,
        val description: String
    )

    enum class IssueType {
        MISSING_TR,           // TR'de yok
        MISSING_EN,           // EN'de yok
        CAPITALIZATION,       // Büyük/küçük harf uyuşmazlığı
        FORMAT_MISMATCH,      // Format farkı (... : ! vs)
        LENGTH_DIFFERENCE     // Çok farklı uzunluklar (opsiyonel)
    }

    /**
     * Tüm çevirileri kontrol eder ve sorunları bulur
     */
    fun checkAllTranslations(): List<TranslationIssue> {
        val issues = mutableListOf<TranslationIssue>()

        // LanguageManager'dan çeviri haritalarını al
        val trTranslations = getTranslationsForLanguage("TR")
        val enTranslations = getTranslationsForLanguage("EN")

        // Tüm anahtarları topla (TR + EN)
        val allKeys = (trTranslations.keys + enTranslations.keys).toSet()

        allKeys.forEach { key ->
            val trText = trTranslations[key]
            val enText = enTranslations[key]

            // 1. Eksik çeviri kontrolü
            when {
                trText == null -> {
                    issues.add(TranslationIssue(
                        key = key,
                        type = IssueType.MISSING_TR,
                        trText = null,
                        enText = enText,
                        description = "Turkish translation missing"
                    ))
                }
                enText == null -> {
                    issues.add(TranslationIssue(
                        key = key,
                        type = IssueType.MISSING_EN,
                        trText = trText,
                        enText = null,
                        description = "English translation missing"
                    ))
                }
                else -> {
                    // 2. Kapitalizasyon kontrolü
                    checkCapitalization(key, trText, enText)?.let { issues.add(it) }

                    // 3. Format kontrolü
                    checkFormatConsistency(key, trText, enText)?.let { issues.add(it) }
                }
            }
        }

        return issues
    }

    /**
     * Büyük/küçük harf tutarsızlıklarını kontrol eder
     */
    private fun checkCapitalization(key: String, trText: String, enText: String): TranslationIssue? {
        // TR'deki kapitalizasyon stilini belirle
        val trStyle = when {
            trText == trText.uppercase() && trText.length > 1 -> CapStyle.ALL_CAPS
            trText.firstOrNull()?.isUpperCase() == true -> CapStyle.TITLE_CASE
            else -> CapStyle.LOWERCASE
        }

        // EN'deki kapitalizasyon stilini belirle
        val enStyle = when {
            enText == enText.uppercase() && enText.length > 1 -> CapStyle.ALL_CAPS
            enText.firstOrNull()?.isUpperCase() == true -> CapStyle.TITLE_CASE
            else -> CapStyle.LOWERCASE
        }

        // Stil uyuşmazlığı varsa rapor et
        if (trStyle != enStyle && trStyle != CapStyle.LOWERCASE) {
            val suggestion = when (trStyle) {
                CapStyle.ALL_CAPS -> enText.uppercase()
                CapStyle.TITLE_CASE -> enText.replaceFirstChar { it.uppercase() }
                else -> enText
            }

            return TranslationIssue(
                key = key,
                type = IssueType.CAPITALIZATION,
                trText = trText,
                enText = enText,
                description = "TR is $trStyle but EN is $enStyle. Suggestion: \"$suggestion\""
            )
        }

        return null
    }

    /**
     * Format tutarlılığını kontrol eder (:, ..., !, ? vb)
     */
    private fun checkFormatConsistency(key: String, trText: String, enText: String): TranslationIssue? {
        val formatChars = listOf(":", "...", "!", "?", ".", ",")

        formatChars.forEach { char ->
            val trHas = trText.contains(char)
            val enHas = enText.contains(char)

            if (trHas != enHas) {
                return TranslationIssue(
                    key = key,
                    type = IssueType.FORMAT_MISMATCH,
                    trText = trText,
                    enText = enText,
                    description = "TR ${if (trHas) "has" else "doesn't have"} '$char' but EN ${if (enHas) "has" else "doesn't have"} it"
                )
            }
        }

        return null
    }

    /**
     * LanguageManager'dan belirli bir dil için çeviri haritasını alır
     * Not: LanguageManager private olduğu için reflection kullanacağız
     */
    private fun getTranslationsForLanguage(languageCode: String): Map<String, String> {
        return try {
            val translationsField = LanguageManager::class.java.getDeclaredField("translations")
            translationsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val allTranslations = translationsField.get(LanguageManager) as Map<String, Map<String, String>>
            allTranslations[languageCode] ?: emptyMap()
        } catch (e: Exception) {
            Log.e("TranslationChecker", "Failed to access translations", e)
            emptyMap()
        }
    }

    /**
     * Sorunları kategorilere ayırarak raporlar
     */
    fun generateReport(issues: List<TranslationIssue>): String {
        if (issues.isEmpty()) {
            return "✅ PERFECT! No translation issues found.\n\nAll translations are complete and consistent!"
        }

        val report = StringBuilder()
        report.appendLine("⚠️ TRANSLATION ISSUES FOUND\n")
        report.appendLine("Total Issues: ${issues.size}\n")

        // Kategorilere ayır
        val byType = issues.groupBy { it.type }

        // Eksik TR çevirileri
        byType[IssueType.MISSING_TR]?.let { missing ->
            report.appendLine("❌ Missing Turkish Translations (${missing.size}):")
            missing.take(10).forEach { issue ->
                report.appendLine("  • $issue.key")
                report.appendLine("    EN: \"${issue.enText}\"")
            }
            if (missing.size > 10) report.appendLine("  ... and ${missing.size - 10} more")
            report.appendLine()
        }

        // Eksik EN çevirileri
        byType[IssueType.MISSING_EN]?.let { missing ->
            report.appendLine("❌ Missing English Translations (${missing.size}):")
            missing.take(10).forEach { issue ->
                report.appendLine("  • ${issue.key}")
                report.appendLine("    TR: \"${issue.trText}\"")
            }
            if (missing.size > 10) report.appendLine("  ... and ${missing.size - 10} more")
            report.appendLine()
        }

        // Kapitalizasyon sorunları
        byType[IssueType.CAPITALIZATION]?.let { capIssues ->
            report.appendLine("🔤 Capitalization Issues (${capIssues.size}):")
            capIssues.take(15).forEach { issue ->
                report.appendLine("  • ${issue.key}")
                report.appendLine("    TR: \"${issue.trText}\"")
                report.appendLine("    EN: \"${issue.enText}\"")
                report.appendLine("    ${issue.description}")
            }
            if (capIssues.size > 15) report.appendLine("  ... and ${capIssues.size - 15} more")
            report.appendLine()
        }

        // Format sorunları
        byType[IssueType.FORMAT_MISMATCH]?.let { formatIssues ->
            report.appendLine("📝 Format Issues (${formatIssues.size}):")
            formatIssues.take(10).forEach { issue ->
                report.appendLine("  • ${issue.key}")
                report.appendLine("    TR: \"${issue.trText}\"")
                report.appendLine("    EN: \"${issue.enText}\"")
                report.appendLine("    ${issue.description}")
            }
            if (formatIssues.size > 10) report.appendLine("  ... and ${formatIssues.size - 10} more")
            report.appendLine()
        }

        return report.toString()
    }

    /**
     * Kapitalizasyon stilleri
     */
    private enum class CapStyle {
        ALL_CAPS,      // YETENEKLER
        TITLE_CASE,    // Skills
        LOWERCASE      // skills
    }
}
