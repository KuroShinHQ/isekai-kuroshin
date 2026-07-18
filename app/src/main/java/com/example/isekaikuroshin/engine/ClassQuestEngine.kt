package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.PlayerState // DEĞİŞTİRİLDİ

object ClassQuestEngine {

    fun checkAndTriggerQuests(playerState: PlayerState): String? { // DEĞİŞTİRİLDİ
        val profile = playerState.playerProfile
        val archetypeScores = profile.archetypeScores
        val triggeredQuests = profile.triggeredClassQuests

        // Check each archetype to see if it has reached the threshold (100 points)
        // and hasn't already triggered a class quest
        for ((archetype, score) in archetypeScores) { // DEĞİŞTİRİLDİ (forEach -> for)
            if (score >= 100 && !triggeredQuests.contains(archetype)) {
                return archetype // Bu return artık geçerli
            }
        }

        return null
    }
}