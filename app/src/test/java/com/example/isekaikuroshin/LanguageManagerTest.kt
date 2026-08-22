package com.example.isekaikuroshin

import com.example.isekaikuroshin.data.LanguageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LanguageManager mantık birim testleri.
 *
 * NOT 1: android.util.Log no-op (gradle: unitTests.isReturnDefaultValues).
 * NOT 2: LanguageManager JVM'de TEK OBJECT — testler arası state sızıntısı
 *        olduğundan her test kendi dil durumunu KENDİ kurar (sıra-bağımsız).
 */
class LanguageManagerTest {

    private fun resetToEn() = LanguageManager.setLanguage("EN")

    @Test
    fun `setLanguage desteklenen dili atar`() {
        resetToEn()
        LanguageManager.setLanguage("TR")
        assertEquals("TR", LanguageManager.currentLanguage.value)
    }

    @Test
    fun `getText bilinen anahtar icin ceviri dondurur`() {
        resetToEn()
        val text = LanguageManager.getText("nav_settings")
        assertTrue(
            "EN ceviri donmedi: $text",
            text.isNotEmpty() && text != "nav_settings"
        )
    }

    @Test
    fun `getText bilinmeyen anahtarda key fallback`() {
        // Hata durumu: olmayan anahtar — kendisi doner (sessiz kirilma yok).
        assertEquals("yok_boyle_anahtar_xyz", LanguageManager.getText("yok_boyle_anahtar_xyz"))
    }

    @Test
    fun `setLanguage desteklenmeyen kodu yok sayar`() {
        // Hata durumu: tanimsiz dil — mevcut dil DEGISMEZ.
        resetToEn()
        LanguageManager.setLanguage("XX")
        assertEquals("EN", LanguageManager.currentLanguage.value)
    }

    @Test
    fun `dil degisimi getText ciktisini degistirir`() {
        val en = run { resetToEn(); LanguageManager.getText("nav_settings") }
        val tr = run { LanguageManager.setLanguage("TR"); LanguageManager.getText("nav_settings") }
        assertTrue("EN ve TR ayni geldi: $en", en != tr)
    }

    @Test
    fun `getCurrentLanguageCode mevcut diller icin konusma formati`() {
        // 2026-08-22: veride TR ve EN var; diger dallar olukod (bkz. TranslationsTest).
        val cases = mapOf("TR" to "tr-TR", "EN" to "en-US")
        for ((code, expected) in cases) {
            LanguageManager.setLanguage(code)
            assertEquals(expected, LanguageManager.getCurrentLanguageCode())
        }
    }

    @Test
    fun `bilinmeyen dil icin konusma kodu en-US fallback`() {
        // Sinir durumu: when'in else kolu (verideki hicbir dil buna dusmez).
        resetToEn()
        assertEquals("en-US", LanguageManager.getCurrentLanguageCode())
    }

    @Test
    fun `getSupportedLanguages gercek dil setini listeler`() {
        resetToEn()
        val supported = LanguageManager.getSupportedLanguages()
        assertEquals(setOf("TR", "EN"), supported.toSet())
    }

    @Test
    fun `checkMissingTranslations tam veride bos doner`() {
        resetToEn()
        val missing = LanguageManager.checkMissingTranslations()
        val nonEmpty = missing.filterValues { it.isNotEmpty() }
        assertTrue("Eksik ceviriler bulundu: $nonEmpty", nonEmpty.isEmpty())
    }
}
