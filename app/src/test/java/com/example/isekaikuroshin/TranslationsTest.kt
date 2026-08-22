package com.example.isekaikuroshin

import com.example.isekaikuroshin.data.Translations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Translations veri havuzu bütünlük testleri — cihaz/emülatör GEREKTİRMEZ.
 *
 * 2026-08-22 denetim bulgusu: veri havuzunda SADECE TR ve EN var.
 * LanguageManager.getCurrentLanguageCode()'daki DE/SV/FR/ES/JA dalları ÖLÜ KOD.
 * Bu test gerçekliği sabitler; yeni dil eklenince buraya da eklenmelidir.
 */
class TranslationsTest {

    /** Veri havuzundaki gercek diller (denetim: 2026-08-22). */
    private val actualLanguages = setOf("TR", "EN")

    @Test
    fun `tum beklenen diller mevcut`() {
        assertEquals(actualLanguages, Translations.ALL.keys)
    }

    @Test
    fun `her dil ayni anahtar kumesine sahip`() {
        val trKeys = Translations.ALL.getValue("TR").keys
        val enKeys = Translations.ALL.getValue("EN").keys
        assertTrue("EN bos olamaz", enKeys.isNotEmpty())

        val missingInEn = trKeys - enKeys
        val missingInTr = enKeys - trKeys
        assertTrue(
            "TR'de olup EN'de eksik (${missingInEn.size}): ${missingInEn.take(5)}",
            missingInEn.isEmpty()
        )
        assertTrue(
            "EN'de olup TR'de eksik (${missingInTr.size}): ${missingInTr.take(5)}",
            missingInTr.isEmpty()
        )
    }

    @Test
    fun `hicbir ceviri degeri bos degil`() {
        val empties = mutableListOf<String>()
        for ((lang, map) in Translations.ALL) {
            for ((key, value) in map) {
                if (value.isBlank()) empties += "$lang/$key"
            }
        }
        assertTrue("Bos ceviriler: ${empties.take(10)}", empties.isEmpty())
    }

    @Test
    fun `anahtarlar snake_case - bilinen istisnalar haric`() {
        // 2026-08-22 denetimi: bu 27 anahtar mevcut UI cagri sitelerinde
        // ayni formatta kullaniliyor (davranis korundu). YENI karisma = kirmizi.
        val knownExceptions = setOf(
            "BLE_STATUS", "COMPASS_CALIBRATION", "CONNECTED", "CRUISE_TOGGLE",
            "DISCONNECTED", "DRONE_BATTERY", "ELRS_SIGNAL", "FLIGHT_MODE",
            "FLIP_BACKWARD", "FLIP_FORWARD", "FLIP_LEFT", "FLIP_RIGHT",
            "FLIP_SECTION", "GAUNTLET_BATTERY", "MODE_CINEMATIC", "MODE_REFLEX",
            "ORIENTATION_TOGGLE", "SAFE_ARM", "SMART_CATCH", "SMART_LANDING",
            "TELEMETRY", "THROTTLE_MANA",
            "archetype_Craftsman", "archetype_Explorer", "archetype_Mystic",
            "archetype_Unknown", "archetype_Warrior",
        )
        val badKeys = Translations.ALL.getValue("EN").keys.filter {
            !it.matches(Regex("[a-z0-9_]+")) && it !in knownExceptions
        }
        assertTrue("Kural disi anahtarlar: ${badKeys.take(10)}", badKeys.isEmpty())
        // Istisna listesi taze kalmali: listedeki her anahtar gercekten var olmali.
        val allKeys = Translations.ALL.getValue("EN").keys
        for (k in knownExceptions) {
            assertTrue("Istisna listede ama veride yok: $k", k in allKeys)
        }
    }

    @Test
    fun `ceviri hacmi makul aralikta`() {
        // Sinir durumu: her dil en az 300 anahtar icermeli (regresyon sigortasi).
        for ((lang, map) in Translations.ALL) {
            assertTrue("$lang sadece ${map.size} anahtar iceriyor", map.size >= 300)
        }
    }
}
