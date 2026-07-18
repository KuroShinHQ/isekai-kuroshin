package com.example.isekaikuroshin.engine

import javax.inject.Inject
import javax.inject.Singleton

data class StoryAnalysisResult(
    val themes: List<String>,
    val atmosphere: String
)

@Singleton
class StoryAnalysisEngine @Inject constructor() {

    fun analyzeTextChunk(): StoryAnalysisResult { // text parameter removed
        return StoryAnalysisResult(
            themes = listOf("kayıp elf şehri", "kadim bir kılıç"),
            atmosphere = "gizemli ve tekinsiz"
        )
    }
}