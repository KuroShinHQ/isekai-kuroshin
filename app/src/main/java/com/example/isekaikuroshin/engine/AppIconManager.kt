package com.example.isekaikuroshin.engine

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * AppIconManager - Dinamik Launcher Icon Yönetimi
 * ================================================
 *
 * GÖREV: GM 3 günlük özet sonrası karma değerine göre uygulama ikonunu değiştir
 *
 * KARMA SİSTEMİ:
 * - Karma > 0.5  → DIVINE icon (ic_launcher_divine.png)
 * - Karma < -0.5 → DARK icon (ic_launcher_dark.png)
 * - -0.5 ≤ Karma ≤ 0.5 → MYSTERY icon (ic_launcher_mystery.png)
 *
 * TEKNİK DETAYLAR:
 * - Activity-Alias sistemi kullanılıyor (AndroidManifest.xml)
 * - PackageManager.setComponentEnabledSetting() ile değiştirme
 * - DONT_KILL_APP flag ile uygulamayı kapatmadan değiştirme
 *
 * ÖRNEK KULLANIM:
 * ```kotlin
 * // GameMasterEngine.kt - generate3DaySummary() içinde
 * val newKarma = calculateKarmaScore(recentActions)
 * AppIconManager.updateIconBasedOnKarma(context, newKarma)
 * ```
 *
 * NOT:
 * - Launcher'ın güncellenmesi 2-5 saniye sürebilir
 * - İlk değişimde uygulama kapanabilir (Android sınırlaması)
 * - Bazı launcher'lar (Xiaomi MIUI gibi) desteklemeyebilir
 *
 * Geliştirici: Claude Code (Anthropic)
 * Görev ID: KRM-SYS-DYNAMIC-ICON
 * Tarih: 2025-10-21
 */
object AppIconManager {

    private const val TAG = "AppIconManager"

    /**
     * Uygulama icon türleri (Activity-Alias isimleriyle eşleşmeli)
     */
    enum class IconType {
        DIVINE,  // Yüksek pozitif karma (> 0.5)
        DARK,    // Yüksek negatif karma (< -0.5)
        MYSTERY  // Nötr karma (-0.5 ~ 0.5)
    }

    /**
     * Karma attribute'una göre icon türünü belirler
     *
     * NOT: Karma sistemi IntelligentContentEngine'de çalışıyor!
     * ATTRIBUTES config'den: DIVINE (#12), DARK (#13), MYSTERY (#14)
     *
     * Bu fonksiyon GM 3 günlük özet sonrası dominant attribute'u alır:
     * - DIVINE attribute → DIVINE icon
     * - DARK attribute → DARK icon
     * - MYSTERY attribute → MYSTERY icon
     * - Diğer attribute'lar → MYSTERY (varsayılan)
     *
     * @param dominantAttribute GM Engine'den gelen dominant karma attribute (örn: "DIVINE", "DARK", "MYSTERY")
     * @return IconType
     */
    fun getIconTypeForAttribute(dominantAttribute: String): IconType {
        return when (dominantAttribute.uppercase()) {
            "DIVINE" -> {
                Log.d(TAG, "🌟 Dominant attribute: DIVINE → DIVINE icon")
                IconType.DIVINE
            }
            "DARK" -> {
                Log.d(TAG, "😈 Dominant attribute: DARK → DARK icon")
                IconType.DARK
            }
            "MYSTERY" -> {
                Log.d(TAG, "🔮 Dominant attribute: MYSTERY → MYSTERY icon")
                IconType.MYSTERY
            }
            else -> {
                Log.d(TAG, "❓ Unknown attribute: $dominantAttribute → MYSTERY icon (fallback)")
                IconType.MYSTERY
            }
        }
    }

    /**
     * @Deprecated Eski karma skoru bazlı hesaplama (artık kullanılmıyor)
     * Yeni sistem: GM Engine'den dominant attribute alınıyor
     */
    @Deprecated("Use getIconTypeForAttribute instead", ReplaceWith("getIconTypeForAttribute(dominantAttribute)"))
    fun getIconTypeForKarma(karma: Float): IconType {
        return when {
            karma > 0.5f -> IconType.DIVINE
            karma < -0.5f -> IconType.DARK
            else -> IconType.MYSTERY
        }
    }

    /**
     * Dominant karma attribute'una göre uygulama ikonunu günceller
     *
     * KULLANIM:
     * ```kotlin
     * // GM Engine'den dominant attribute al
     * val dominantAttribute = intelligentContentEngine.getDominantKarmaAttribute()
     * AppIconManager.updateIconBasedOnAttribute(context, dominantAttribute)
     * ```
     *
     * @param context Application context
     * @param dominantAttribute Dominant karma attribute ("DIVINE", "DARK", "MYSTERY")
     */
    fun updateIconBasedOnAttribute(context: Context, dominantAttribute: String) {
        val targetIcon = getIconTypeForAttribute(dominantAttribute)
        switchIcon(context, targetIcon)
    }

    /**
     * @Deprecated Eski karma skoru bazlı güncelleme (artık kullanılmıyor)
     */
    @Deprecated("Use updateIconBasedOnAttribute instead", ReplaceWith("updateIconBasedOnAttribute(context, dominantAttribute)"))
    fun updateIconBasedOnKarma(context: Context, karma: Float) {
        val targetIcon = getIconTypeForKarma(karma)
        switchIcon(context, targetIcon)
    }

    /**
     * Uygulama ikonunu belirtilen türe değiştirir
     *
     * @param context Application context
     * @param targetIcon Hedef icon türü
     */
    fun switchIcon(context: Context, targetIcon: IconType) {
        Log.d(TAG, "🔄 Icon değişimi başlatılıyor: $targetIcon")

        val packageName = context.packageName

        // Tüm icon türlerini kontrol et
        for (iconType in IconType.values()) {
            val componentName = ComponentName(
                packageName,
                "$packageName.${iconType.name}"
            )

            // Hedef icon aktif edilir, diğerleri devre dışı bırakılır
            val newState = if (iconType == targetIcon) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            try {
                context.packageManager.setComponentEnabledSetting(
                    componentName,
                    newState,
                    PackageManager.DONT_KILL_APP  // Uygulamayı kapatma
                )

                val stateText = if (iconType == targetIcon) "✅ AKTIF" else "❌ DEVRE DIŞI"
                Log.d(TAG, "   $iconType → $stateText")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Icon değişimi hatası ($iconType): ${e.message}", e)
            }
        }

        Log.d(TAG, "✅ Icon değişimi tamamlandı: $targetIcon")
        Log.d(TAG, "⏳ Launcher güncellenmesi 2-5 saniye sürebilir...")
    }

    /**
     * Mevcut aktif icon türünü döndürür
     *
     * @param context Application context
     * @return Aktif IconType veya null (tespit edilemezse)
     */
    fun getCurrentIcon(context: Context): IconType? {
        val packageName = context.packageName

        for (iconType in IconType.values()) {
            val componentName = ComponentName(
                packageName,
                "$packageName.${iconType.name}"
            )

            val state = context.packageManager.getComponentEnabledSetting(componentName)

            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                Log.d(TAG, "📱 Mevcut icon: $iconType")
                return iconType
            }
        }

        Log.w(TAG, "⚠️ Aktif icon tespit edilemedi (varsayılan kullanılıyor)")
        return null
    }

    /**
     * Debug: Tüm icon durumlarını logla
     */
    fun debugLogIconStates(context: Context) {
        Log.d(TAG, "=== ICON DURUMLARI ===")
        val packageName = context.packageName

        for (iconType in IconType.values()) {
            val componentName = ComponentName(
                packageName,
                "$packageName.${iconType.name}"
            )

            val state = context.packageManager.getComponentEnabledSetting(componentName)
            val stateText = when (state) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> "✅ AKTIF"
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> "❌ DEVRE DIŞI"
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> "🔧 VARSAYILAN"
                else -> "❓ BİLİNMEYEN ($state)"
            }

            Log.d(TAG, "$iconType: $stateText")
        }
        Log.d(TAG, "====================")
    }
}
