package com.example.isekaikuroshin.sound

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.isekaikuroshin.utils.GameLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SoundManager - Merkezi Ses Yönetim Sistemi
 *
 * Tüm ses operasyonlarını (müzik, ortam sesleri, video sesleri, UI efektleri)
 * yöneten, Hilt ile @Singleton olarak sağlanan, yaşam döngüsüne duyarlı ses yöneticisi.
 *
 * FB#12: Ses sistemi tamamen bozuk - tüm sorunlar bu sınıfla çözülecek
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DefaultLifecycleObserver {

    // Farklı kanallar için ayrı oynatıcılar
    private var musicPlayer: ExoPlayer? = null
    private var ambiancePlayer: ExoPlayer? = null
    private var videoPlayer: ExoPlayer? = null
    private var uiEffectsPlayer: ExoPlayer? = null

    // Global ses durumu
    private var isGlobalMuted = false
    private var musicVolume = 1.0f
    private var ambianceVolume = 0.5f
    private var videoVolume = 1.0f
    private var uiEffectsVolume = 0.7f

    // Lifecycle state
    private var isInForeground = true

    init {
        GameLogger.logSystem("🔊 SoundManager initialized")
        initializePlayers()
    }

    private fun initializePlayers() {
        try {
            musicPlayer = ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                volume = if (isGlobalMuted) 0f else musicVolume
            }

            ambiancePlayer = ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                volume = if (isGlobalMuted) 0f else ambianceVolume
            }

            uiEffectsPlayer = ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                volume = if (isGlobalMuted) 0f else uiEffectsVolume
            }

            GameLogger.logSystem("🎵 All audio players initialized successfully")
        } catch (e: Exception) {
            GameLogger.logSystem("❌ Error initializing audio players: ${e.message}")
        }
    }

    // === MUSIC MANAGEMENT ===

    fun playMusic(trackResId: Int) {
        try {
            val uri = "android.resource://${context.packageName}/$trackResId"
            val mediaItem = MediaItem.fromUri(uri)
            musicPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                if (isInForeground && !isGlobalMuted) {
                    play()
                }
            }
            GameLogger.logSystem("🎵 Playing music: $trackResId")
        } catch (e: Exception) {
            GameLogger.logSystem("❌ Error playing music: ${e.message}")
        }
    }

    fun stopMusic() {
        try {
            musicPlayer?.stop()
            GameLogger.logSystem("⏹️ Music stopped")
        } catch (e: Exception) {
            GameLogger.logSystem("❌ Error stopping music: ${e.message}")
        }
    }

    fun pauseMusic() {
        try {
            musicPlayer?.pause()
            GameLogger.logSystem("⏸️ Music paused")
        } catch (e: Exception) {
            GameLogger.logSystem("❌ Error pausing music: ${e.message}")
        }
    }

    fun resumeMusic() {
        try {
            if (isInForeground && !isGlobalMuted) {
                musicPlayer?.play()
                GameLogger.logSystem("▶️ Music resumed")
            }
        } catch (e: Exception) {
            GameLogger.logSystem("❌ Error resuming music: ${e.message}")
        }
    }

    // === AMBIANCE MANAGEMENT ===

    fun playAmbiance(trackResId: Int) {
        try {
            val uri = "android.resource://${context.packageName}/$trackResId"
            val mediaItem = MediaItem.fromUri(uri)
            ambiancePlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                if (isInForeground && !isGlobalMuted) {
                    play()
                }
            }
            GameLogger.logSystem("🌊 Playing ambiance: $trackResId")
        } catch (e: Exception) {
            GameLogger.logSystem("❌ Error playing ambiance: ${e.message}")
        }
    }

    fun stopAmbiance() {
        try {
            ambiancePlayer?.stop()
            GameLogger.logSystem("⏹️ Ambiance stopped")
        } catch (e: Exception) {
            GameLogger.logSystem("❌ Error stopping ambiance: ${e.message}")
        }
    }

    // === VIDEO SOUND MANAGEMENT ===

    /**
     * Video oynatıcının sesini SoundManager üzerinden kontrol etmek için.
     * Video oynatıcının kendisi ekranda gösterilir, ama sesi burada yönetilir.
     */
    fun setVideoPlayerVolume(player: ExoPlayer?, muted: Boolean) {
        try {
            player?.volume = if (muted || isGlobalMuted) 0f else videoVolume
            GameLogger.logSystem("🎥 Video volume set: muted=$muted")
        } catch (e: Exception) {
            GameLogger.logSystem("❌ Error setting video volume: ${e.message}")
        }
    }

    // === UI EFFECTS ===

    fun playUIEffect(effectResId: Int) {
        try {
            val uri = "android.resource://${context.packageName}/$effectResId"
            val mediaItem = MediaItem.fromUri(uri)
            uiEffectsPlayer?.apply {
                setMediaItem(mediaItem)
                prepare()
                if (!isGlobalMuted) {
                    play()
                }
            }
            GameLogger.logSystem("🔔 Playing UI effect: $effectResId")
        } catch (e: Exception) {
            GameLogger.logSystem("❌ Error playing UI effect: ${e.message}")
        }
    }

    // === GLOBAL CONTROLS ===

    fun setGlobalMute(muted: Boolean) {
        isGlobalMuted = muted

        musicPlayer?.volume = if (muted) 0f else musicVolume
        ambiancePlayer?.volume = if (muted) 0f else ambianceVolume
        uiEffectsPlayer?.volume = if (muted) 0f else uiEffectsVolume

        GameLogger.logSystem("🔇 Global mute set to: $muted")
    }

    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        if (!isGlobalMuted) {
            musicPlayer?.volume = musicVolume
        }
        GameLogger.logSystem("🔊 Music volume set to: $musicVolume")
    }

    fun setAmbianceVolume(volume: Float) {
        ambianceVolume = volume.coerceIn(0f, 1f)
        if (!isGlobalMuted) {
            ambiancePlayer?.volume = ambianceVolume
        }
        GameLogger.logSystem("🔊 Ambiance volume set to: $ambianceVolume")
    }

    fun setVideoVolume(volume: Float) {
        videoVolume = volume.coerceIn(0f, 1f)
        GameLogger.logSystem("🔊 Video volume set to: $videoVolume")
    }

    // === LIFECYCLE MANAGEMENT ===

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        isInForeground = true
        GameLogger.logSystem("▶️ SoundManager: App resumed - restarting audio")

        // Ön plana geldiğinde sesleri tekrar başlat
        if (!isGlobalMuted) {
            musicPlayer?.let { if (it.mediaItemCount > 0) it.play() }
            ambiancePlayer?.let { if (it.mediaItemCount > 0) it.play() }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        isInForeground = false
        GameLogger.logSystem("⏸️ SoundManager: App paused - stopping audio")

        // Arka plana geçtiğinde tüm sesleri durdur
        musicPlayer?.pause()
        ambiancePlayer?.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        GameLogger.logSystem("🗑️ SoundManager: Releasing all resources")
        releaseAll()
    }

    // === RESOURCE MANAGEMENT ===

    fun releaseAll() {
        try {
            musicPlayer?.release()
            ambiancePlayer?.release()
            videoPlayer?.release()
            uiEffectsPlayer?.release()

            musicPlayer = null
            ambiancePlayer = null
            videoPlayer = null
            uiEffectsPlayer = null

            GameLogger.logSystem("🧹 All audio players released")
        } catch (e: Exception) {
            GameLogger.logSystem("❌ Error releasing audio players: ${e.message}")
        }
    }

    // === UTILITY ===

    fun getGlobalMuteState(): Boolean = isGlobalMuted

    fun getMusicVolume(): Float = musicVolume

    fun getAmbianceVolume(): Float = ambianceVolume

    fun getVideoVolume(): Float = videoVolume
}

/**
 * Hilt EntryPoint for SoundManager
 * Composable fonksiyonlardan SoundManager'a erişim için kullanılır.
 */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface SoundManagerEntryPoint {
    fun soundManager(): SoundManager
}
