package com.example.isekaikuroshin.ui.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.data.PersistentDataManager
import com.example.isekaikuroshin.engine.IntelligentContentEngine
import com.example.isekaikuroshin.utils.GameLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UserFlowState {
    LOADING,
    YENI_KULLANICI,
    GERI_DONEN_KULLANICI,
    OLUM_SONRASI
}

data class UserEntryUiState(
    val flowState: UserFlowState = UserFlowState.LOADING,
    val playerName: String = "",
    val moralityScore: Float = 0.0f,
    val deathCount: Int = 0,                           // DEATH-LOOP: Toplam ölüm sayısı
    val lastDeathCause: String = "",                   // DEATH-LOOP: Son ölüm sebebi
    val dynamicPlaylist: List<String> = emptyList(),
    val dynamicVideoIds: List<Int> = emptyList(),      // Yeni: Kişiselleştirilmiş video resource ID'leri
    val dynamicPhotoIds: List<Int> = emptyList(),      // Yeni: Kişiselleştirilmiş fotoğraf resource ID'leri
    val isLoading: Boolean = false // Varsayılan olarak false
)

@HiltViewModel
class UserEntryViewModel @Inject constructor(
    private val gameStateManager: GameStateManager,
    private val intelligentContentEngine: IntelligentContentEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserEntryUiState())
    val uiState: StateFlow<UserEntryUiState> = _uiState.asStateFlow()

    init {
        determineUserFlow()
    }

    private fun determineUserFlow() {
        // HİÇBİR BEKLEME YAPMA! Doğrudan anlık veriyi oku.
        val isFirst = PersistentDataManager.isFirstLaunch()
        val playerData = PersistentDataManager.gameData.value.playerData
        val storyData = PersistentDataManager.gameData.value.storyData
        val deathArchive = PersistentDataManager.gameData.value.deathArchive // DEATH-LOOP

        // KRİTİK FİX: playerData.name boşsa YENI_KULLANICI olmalı (data reset sonrası)
        val flowState = when {
            isFirst -> UserFlowState.YENI_KULLANICI
            playerData.name.isBlank() -> {
                GameLogger.logSystem("⚠️ playerName BOŞ! YENI_KULLANICI akışına yönlendiriliyor (data reset?)")
                UserFlowState.YENI_KULLANICI // Data integrity check
            }
            !playerData.isAlive -> UserFlowState.OLUM_SONRASI
            else -> UserFlowState.GERI_DONEN_KULLANICI
        }

        // State'i güncelle - İlk olarak temel bilgilerle (loading state)
        _uiState.value = UserEntryUiState(
            isLoading = true, // KRİTİK FİX: Playlist hazırlanana kadar loading göster
            flowState = flowState,
            playerName = playerData.name,
            moralityScore = playerData.moralityScore,
            deathCount = deathArchive.size,
            lastDeathCause = deathArchive.lastOrNull()?.deathCause ?: "",
            dynamicPlaylist = storyData.dynamicPlaylist
        )
        GameLogger.logSystem("Akış durumu belirlendi: ${flowState.name}")

        // KRİTİK FİX: Playlist oluşturma asenkron - BİTTİKTEN SONRA State güncellenecek
        viewModelScope.launch {
            // DEATH-LOOP: Ölüm sonrası için "Ölüm Yankısı" motorunu çalıştır
            if (flowState == UserFlowState.OLUM_SONRASI) {
                generateDeathEchoContent(deathArchive)
            }
            // Geri dönen kullanıcı için "Akıllı Kalp" motorunu çalıştır
            else if (flowState == UserFlowState.GERI_DONEN_KULLANICI) {
                generatePersonalizedContent()
            }
            // KRM-SYS-22: Yeni kullanıcılar için de "Akıllı Kalp" (varsayılan EXPLORER arketipi ile)
            else if (flowState == UserFlowState.YENI_KULLANICI) {
                generatePersonalizedContent()
            }
            // Playlist hazır, loading'i kapat
            _uiState.value = _uiState.value.copy(isLoading = false)
            GameLogger.logSystem("✅ Playlist hazır, UI gösteriliyor")
        }
    }

    /**
     * "Akıllı Kalp" Motoru - Kişiselleştirilmiş içerik üretir
     */
    private fun generatePersonalizedContent() {
        try {
            GameLogger.logSystem("🧠 Akıllı Kalp motoru başlatılıyor...")

            // Oyuncunun güncel PlayerState'ini al
            val playerState = gameStateManager.gameState.value.playerState

            // IntelligentContentEngine ile kişiselleştirilmiş playlist oluştur
            val playlist = intelligentContentEngine.generatePersonalizedPlaylist(
                playerState = playerState,
                maxVideos = 10,
                maxPhotos = 10
            )

            // UI State'i güncelle
            _uiState.value = _uiState.value.copy(
                dynamicVideoIds = playlist.videos,
                dynamicPhotoIds = playlist.photos
            )

            GameLogger.logSystem("✨ Kişiselleştirilmiş içerik hazır: ${playlist.videos.size} video, ${playlist.photos.size} fotoğraf")
            GameLogger.logSystem("👤 Karakter Profili: ${playlist.characterProfile.mainArchetype} / ${playlist.characterProfile.dominantEmotion}")

        } catch (e: Exception) {
            GameLogger.logError("IntelligentContentEngine", "Akıllı Kalp motoru hatası", e)
        }
    }

    /**
     * DEATH-LOOP: "Ölüm Yankısı" Motoru - Ölüm sonrası kişiselleştirilmiş içerik üretir
     */
    private fun generateDeathEchoContent(deathArchive: List<com.example.isekaikuroshin.data.DeathRecord>) {
        try {
            GameLogger.logSystem("💀 Ölüm Yankısı motoru başlatılıyor...")

            val deathCount = deathArchive.size
            val lastDeathRecord = deathArchive.lastOrNull()

            GameLogger.logSystem("💀 Ölüm sayısı: $deathCount")
            GameLogger.logSystem("💀 Son ölüm sebebi: ${lastDeathRecord?.deathCause ?: "BILINMIYOR"}")

            // IntelligentContentEngine ile ölüm temalı playlist oluştur
            val playlist = intelligentContentEngine.generateDeathEchoPlaylist(
                deathCount = deathCount,
                lastDeathRecord = lastDeathRecord,
                maxVideos = 8,
                maxPhotos = 8
            )

            // UI State'i güncelle
            _uiState.value = _uiState.value.copy(
                dynamicVideoIds = playlist.videos,
                dynamicPhotoIds = playlist.photos
            )

            GameLogger.logSystem("💀 Ölüm Yankısı tamamlandı: ${playlist.videos.size} video, ${playlist.photos.size} fotoğraf")
            GameLogger.logSystem("💀 Karakter Profili: ${playlist.characterProfile.mainArchetype} / ${playlist.characterProfile.dominantEmotion} / ${playlist.characterProfile.emotionalDepth}")

        } catch (e: Exception) {
            GameLogger.logError("DeathEchoEngine", "Ölüm Yankısı motoru hatası", e)
        }
    }


    fun onNewUserAccept(codename: String, age: Int, gender: String) {
        viewModelScope.launch {
            GameLogger.logSystem("🎯 Yeni kullanıcı 'KABUL ET' tıkladı - Codename: '$codename', Age: $age, Gender: '$gender'")

            if (codename.trim().isNotEmpty()) {
                // FIX-NEW-USER-RESET: Önce tüm oyun verisini sıfırla
                GameLogger.logSystem("🔄 Yeni kullanıcı için GameState sıfırlanıyor...")
                gameStateManager.resetGame()

                // 1. PersistentDataManager'a kaydet
                PersistentDataManager.updatePlayerData {
                    it.copy(
                        name = codename.trim(),
                        age = age,
                        gender = gender
                    )
                }
                PersistentDataManager.setFirstLaunchCompleted()

                // 2. GameStateManager'daki PlayerState'i güncelle
                val currentState = gameStateManager.gameState.value
                val updatedPlayerState = currentState.playerState.copy(
                    playerName = codename.trim(),
                    level = age // Seviye = Yaş
                )
                gameStateManager.updatePlayerState(updatedPlayerState)

                GameLogger.logSystem("💾 Yeni kullanıcı verileri kaydedildi (PersistentDataManager + GameStateManager) - Transition'a yönlendiriliyor")
            }
        }
    }

    fun onReturningUserContinue() {
        GameLogger.logSystem("👁️ Geri dönen kullanıcı 'GÖZ' tıkladı - Dashboard'a direkt geçiliyor")
    }
}
