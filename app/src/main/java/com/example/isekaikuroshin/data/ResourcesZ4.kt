package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

@Serializable
data class ResourcesZ4(
    val dal: Int = 0,    // EKLENDİ
    val tas: Int = 0,    // DEĞİŞTİRİLDİ (stone -> tas)
    val odun: Int = 0,   // DEĞİŞTİRİLDİ (wood -> odun)
    val deri: Int = 0,   // EKLENDİ
    val demir: Int = 0   // EKLENDİ
)