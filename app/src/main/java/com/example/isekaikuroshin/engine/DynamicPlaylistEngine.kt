package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.R
import com.example.isekaikuroshin.data.PersistentDataManager
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicPlaylistEngine @Inject constructor() {

    fun generatePlaylist() {
        val gameData = PersistentDataManager.gameData.value
        val moralityScore = 0.0f // PersistentDataManager'da playerState yok, varsayılan değer
        val externalStory = gameData.settingsData.storySettings.externalStorySource

        // Generate video playlist
        val allVideos = getRawMediaFiles()
        val scoredVideos = allVideos.mapNotNull { media ->
            parseContentFileName(media.name)?.let {
                val karmaDistance = abs(it.karma - moralityScore.toInt())
                val storyRelevance = if (externalStory.contains(it.emotion, ignoreCase = true)) 1.0 else 0.0
                val score = karmaDistance.toDouble() - storyRelevance
                Pair(media.resId, score)
            }
        }
        val sortedVideos = scoredVideos.sortedBy { it.second }.map { "android.resource://com.example.isekaikuroshin/${it.first}" }

        // Generate photo playlist
        val allPhotos = getDrawablePhotoFiles()
        val scoredPhotos = allPhotos.mapNotNull { media ->
            parseContentFileName(media.name)?.let {
                val karmaDistance = abs(it.karma - moralityScore.toInt())
                val storyRelevance = if (externalStory.contains(it.emotion, ignoreCase = true)) 1.0 else 0.0
                val score = karmaDistance.toDouble() - storyRelevance
                Pair(media.resId, score)
            }
        }
        val sortedPhotos = scoredPhotos.sortedBy { it.second }.map { it.first }

        PersistentDataManager.updateDynamicPlaylist(sortedVideos)
        PersistentDataManager.updateDynamicPhotoPlaylist(sortedPhotos)
    }

    private fun getRawMediaFiles(): List<MediaFile> {
        return R.raw::class.java.fields.mapNotNull { field ->
            try {
                val resId = field.getInt(null)
                val name = field.name
                if (name.startsWith("video")) {
                    MediaFile(name, resId)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getDrawablePhotoFiles(): List<MediaFile> {
        return R.drawable::class.java.fields.mapNotNull { field ->
            try {
                val resId = field.getInt(null)
                val name = field.name
                if (name.startsWith("photo")) {
                    MediaFile(name, resId)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private data class MediaFile(val name: String, val resId: Int)

    private fun parseContentFileName(fileName: String): ContentMetadata? {
        // Parse filenames like "video2+7happy" or "photo1-3sad"
        val regex = """(video|photo)(\d+)([+-]\d+)(\w+)""".toRegex()
        val match = regex.find(fileName) ?: return null

        val (_, _, karmaStr, emotion) = match.destructured // Changed 'type' and 'number' to '_'
        val karma = karmaStr.toIntOrNull() ?: return null

        return ContentMetadata(karma, emotion)
    }

    private data class ContentMetadata(val karma: Int, val emotion: String)
}
