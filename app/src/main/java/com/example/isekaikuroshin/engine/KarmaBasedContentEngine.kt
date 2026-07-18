package com.example.isekaikuroshin.engine

/*
 * ================================================================
 * LEGACY SYSTEM - DISABLED (KRM-SYS-22-LEGACY-REMOVAL)
 * ================================================================
 *
 * Bu dosya "Eski Kalp" (KarmaBasedContentEngine) motorunu içeriyordu.
 * Artık sadece "Akıllı Kalp" (IntelligentContentEngine) kullanılıyor.
 *
 * Devre Dışı Bırakılma Tarihi: 2025-10-18
 * Neden: Android kaynak dosya isimlendirme hatası + Eski video referansları
 *
 * Bu kod, geri dönüş için saklanmıştır. Silinmemiştir.
 * ================================================================
 */

/*
import android.content.Context
import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.data.PlayerState
import com.example.isekaikuroshin.utils.GameLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KarmaBasedContentEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun generateAndSavePlaylist(playerState: PlayerState, persistentDataManager: PersistentDataManager) {
        // Karma sistemini verbose seviyesine taşı - 3 günlük hafıza testi için gürültü azaltma
        GameLogger.logVerbose("KARMA-ENGINE", "=== KARMA-BASED VIDEO PLAYLIST GENERATION ===")
        GameLogger.logVerbose("KARMA-ENGINE", "🤖 KarmaBasedContentEngine çalıştırıldı")
        GameLogger.logVerbose("KARMA-ENGINE", "📊 Mevcut Karma Değeri: ${playerState.moralityScore}")

        val goodVideos = listOf(
            R.raw.video1p5happy,
            R.raw.video9p1happy,
            R.raw.video4m2happy
        )
        val badVideos = listOf(
            R.raw.video2m3sad,
            R.raw.video5p7sad,
            R.raw.video11p9sad,
            R.raw.video8m10angry,
            R.raw.video10m2fear
        )
        val neutralVideos = listOf(
            R.raw.video3p2neutral,
            R.raw.video6m1neutral
        )

        GameLogger.logVerbose("KARMA-ENGINE", "📹 Kullanılabilir Videolar:")
        GameLogger.logVerbose("KARMA-ENGINE", "  - İyi Videolar: ${goodVideos.size}")
        GameLogger.logVerbose("KARMA-ENGINE", "  - Kötü Videolar: ${badVideos.size}")
        GameLogger.logVerbose("KARMA-ENGINE", "  - Nötr Videolar: ${neutralVideos.size}")

        val (selectedVideos, karmaCategory) = when {
            playerState.moralityScore > 0.3f -> {
                GameLogger.logVerbose("KARMA-ENGINE", "🙂 KARMA KATEGORİSİ: İYİ (Karma > 0.3)")
                GameLogger.logVerbose("KARMA-ENGINE", "✅ İyi ve Nötr videolar seçiliyor.")
                (goodVideos + neutralVideos) to "GOOD"
            }
            playerState.moralityScore < -0.3f -> {
                GameLogger.logVerbose("KARMA-ENGINE", "😠 KARMA KATEGORİSİ: KÖTÜ (Karma < -0.3)")
                GameLogger.logVerbose("KARMA-ENGINE", "✅ Kötü ve Nötr videolar seçiliyor.")
                (badVideos + neutralVideos) to "EVIL"
            }
            else -> {
                GameLogger.logVerbose("KARMA-ENGINE", "😐 KARMA KATEGORİSİ: NÖTR (Karma: ${playerState.moralityScore})")
                GameLogger.logVerbose("KARMA-ENGINE", "✅ TÜM videolar seçiliyor (Good + Bad + Neutral).")
                (goodVideos + badVideos + neutralVideos) to "NEUTRAL"
            }
        }

        GameLogger.logVerbose("KARMA-ENGINE", "📋 Seçilen Video Sayısı: ${selectedVideos.size}")

        val shuffledPlaylist = selectedVideos.shuffled().map { resourceId ->
            "android.resource://${context.packageName}/$resourceId"
        }

        persistentDataManager.updateStoryData { storyData ->
            storyData.copy(dynamicPlaylist = shuffledPlaylist)
        }

        GameLogger.logVerbose("KARMA-ENGINE", "✅ Dinamik Oynatma Listesi Oluşturuldu ve Kaydedildi")
        GameLogger.logVerbose("KARMA-ENGINE", "📊 Playlist Özeti:")
        GameLogger.logVerbose("KARMA-ENGINE", "  - Karma Kategorisi: $karmaCategory")
        GameLogger.logVerbose("KARMA-ENGINE", "  - Toplam Video Sayısı: ${shuffledPlaylist.size}")
        GameLogger.logVerbose("KARMA-ENGINE", "  - İlk 3 Video: ${shuffledPlaylist.take(3).map { it.substringAfterLast("/") }}")
        GameLogger.logVerbose("KARMA-ENGINE", "=== KARMA VIDEO PLAYLIST GENERATION COMPLETE ===")
    }

    /**
     * Umbros Dialog öncesi geçiş videosu seçer (karma bazlı)
     *
     * KARMA SİSTEMİ:
     * - Yüksek Karma (>0.3): Kutsal/parlak Umbros videoları (DIVINE, LIGHT, PEACE)
     * - Düşük Karma (<-0.3): Karanlık/ürkütücü Umbros videoları (DARK, FEAR, CORRUPTION)
     * - Nötr Karma: Orta ton Umbros videoları (NEUTRAL, MYSTERY)
     *
     * @param karmaScore Kullanıcının karma değeri (-1.0 - 1.0)
     * @return Seçilen Umbros geçiş videosunun resource path'i
     */
    fun getUmbrosTransitionVideo(karmaScore: Float): String {
        GameLogger.logInfo("KARMA-ENGINE", "🎬 Umbros geçiş videosu seçiliyor...")
        GameLogger.logInfo("KARMA-ENGINE", "📊 Karma Skoru: $karmaScore")

        // Karma kategorisine göre video havuzları
        val divineVideos = listOf(
            // Yüksek karma - kutsal/parlak videolar (etiketli: UMBROS + DIVINE/LIGHT)
            R.raw.vid_umbros_divine_calm_d1,
            R.raw.vid_umbros_light_calm_d2
        )

        val darkVideos = listOf(
            // Düşük karma - karanlık/ürkütücü videolar (etiketli: UMBROS + DARK/FEAR)
            R.raw.vid_umbros_dark_fear_d1,
            R.raw.vid_umbros_corruption_fear_d2
        )

        val neutralVideos = listOf(
            // Nötr karma - orta ton videolar (etiketli: UMBROS + MYSTERY/NEUTRAL)
            R.raw.vid_umbros_mystery_calm_d1,
            R.raw.vid_umbros_neutral_calm_d2
        )

        val (selectedPool, karmaCategory) = when {
            karmaScore > 0.3f -> {
                GameLogger.logInfo("KARMA-ENGINE", "✨ KARMA KATEGORİSİ: DIVINE (İyi Karma)")
                divineVideos to "DIVINE"
            }
            karmaScore < -0.3f -> {
                GameLogger.logInfo("KARMA-ENGINE", "🌑 KARMA KATEGORİSİ: DARK (Kötü Karma)")
                darkVideos to "DARK"
            }
            else -> {
                GameLogger.logInfo("KARMA-ENGINE", "🔮 KARMA KATEGORİSİ: MYSTERY (Nötr Karma)")
                neutralVideos to "MYSTERY"
            }
        }

        // Havuzdan rastgele bir video seç
        val selectedVideo = selectedPool.randomOrNull() ?: run {
            GameLogger.logWarning("KARMA-ENGINE", "⚠️ Video havuzu boş! Fallback video kullanılıyor.")
            R.raw.vid_umbros_mystery_calm_d1 // Fallback video
        }

        val videoPath = "android.resource://${context.packageName}/$selectedVideo"

        GameLogger.logInfo("KARMA-ENGINE", "✅ Umbros geçiş videosu seçildi:")
        GameLogger.logInfo("KARMA-ENGINE", "  - Kategori: $karmaCategory")
        GameLogger.logInfo("KARMA-ENGINE", "  - Video: ${videoPath.substringAfterLast("/")}")

        return videoPath
    }
}
*/
