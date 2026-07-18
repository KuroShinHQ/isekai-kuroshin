package com.example.isekaikuroshin.engine

data class ContentData(val karma: Int, val emotion: String)

fun parseContentFileName(fileName: String): ContentData? {
    val regex = "[a-zA-Z]+(\\d+)([pm]\\d+)([a-zA-Z]+)\\..*".toRegex()
    val matchResult = regex.find(fileName)

    return matchResult?.let {
        val karmaString = it.groupValues[2].replace('p', '+').replace('m', '-')
        val karma = karmaString.toIntOrNull() ?: 0
        val emotion = it.groupValues[3]
        ContentData(karma, emotion)
    }
}