package com.example.isekaikuroshin.engine

import android.content.Context
import android.media.MediaPlayer
import com.example.isekaikuroshin.R

/**
 * Weather Sound Manager
 *
 * Manages ambient weather sounds that loop continuously when a specific weather condition is active.
 * Sounds are short clips that repeat seamlessly to create an immersive atmosphere.
 */
class WeatherSoundManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentWeather: Weather? = null
    private var isEnabled: Boolean = true
    private var currentVolume: Float = 1.0f

    /**
     * Plays the appropriate weather sound based on the current weather condition.
     * The sound will loop continuously until stopped or changed.
     *
     * @param weather The current weather condition
     * @param volume Volume level (0.0 to 1.0)
     */
    fun playWeatherSound(weather: Weather, volume: Float = 1.0f) {
        if (!isEnabled) return

        // Don't restart if same weather is already playing
        if (currentWeather == weather && mediaPlayer?.isPlaying == true) {
            // Just update volume if changed
            if (currentVolume != volume) {
                mediaPlayer?.setVolume(volume, volume)
                currentVolume = volume
            }
            return
        }

        val soundResId = when (weather) {
            Weather.SUNNY -> R.raw.weather_sunny
            Weather.RAINY -> R.raw.weather_rainy
            Weather.STORMY -> R.raw.weather_stormy
            Weather.FOGGY -> R.raw.weather_foggy
            Weather.SNOWY -> R.raw.weather_snowy
            Weather.WINDY -> R.raw.weather_windy
            Weather.CLOUDY -> R.raw.weather_cloudy
            Weather.HOT -> R.raw.weather_hot
            Weather.COLD -> R.raw.weather_cold
            Weather.BLIZZARD -> R.raw.weather_blizzard
        }

        // Release previous player
        stopSound()

        // Create and start new player with looping
        try {
            mediaPlayer = MediaPlayer.create(context, soundResId)?.apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
            currentWeather = weather
            currentVolume = volume
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Stops the currently playing weather sound.
     */
    fun stopSound() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        currentWeather = null
    }

    /**
     * Pauses the weather sound without releasing resources.
     */
    fun pauseSound() {
        mediaPlayer?.pause()
    }

    /**
     * Resumes the weather sound if it was paused.
     */
    fun resumeSound() {
        if (isEnabled) {
            mediaPlayer?.start()
        }
    }

    /**
     * Sets the volume of the currently playing sound.
     *
     * @param volume Volume level (0.0 to 1.0)
     */
    fun setVolume(volume: Float) {
        currentVolume = volume
        mediaPlayer?.setVolume(volume, volume)
    }

    /**
     * Enables or disables weather sounds.
     * When disabled, any playing sound is stopped.
     *
     * @param enabled True to enable, false to disable
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            stopSound()
        } else if (currentWeather != null) {
            // Restart sound if we have a weather condition
            currentWeather?.let { playWeatherSound(it, currentVolume) }
        }
    }

    /**
     * Checks if weather sounds are currently enabled.
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Releases all resources. Should be called when the manager is no longer needed.
     */
    fun release() {
        stopSound()
    }
}
