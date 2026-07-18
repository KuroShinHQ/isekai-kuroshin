package com.example.isekaikuroshin.data

import kotlinx.serialization.Serializable

@Serializable
data class PlayerState(
    // --- Temel Kimlik ve İlerleme ---
    val playerName: String = "Kuroshin",
    val playerClass: String = "Maceracı",
    val level: Int = 1,
    val experience: Int = 0,
    val experienceToNextLevel: Int = 100,
    val gold: Int = 0,
    val statPoints: Int = 0, 

    // --- Temel Nitelikler (Attributes) ---
    val strength: Float = 10f,
    val vitality: Float = 10f,
    val agility: Float = 10f,
    val intelligence: Float = 10f,

    // --- Savaş İstatistikleri (Combat Stats) ---
    val currentHealth: Int = 100,
    val maxHealth: Int = 100,
    val currentMana: Int = 50,
    val maxMana: Int = 50,
    val physicalAttack: Int = 10,
    val magicPower: Int = 5,
    val defense: Int = 5,
    val critChance: Float = 0.05f,
    val critDamage: Float = 1.5f,

    // --- İkincil İstatistikler (Oyuncu Durumu) ---
    val stamina: Int = 100,      // Fiziksel enerji (Training, Combat)
    val maxStamina: Int = 100,   // Maksimum stamina
    val hunger: Int = 100,       // Açlık barı (0 = aç, 100 = tok)
    val maxHunger: Int = 100,    // Maksimum açlık
    val fatigue: Int = 0,        // Yorgunluk (0 = dinç, 100 = bitkin)
    val spirit: Int = 10,        // Ruhsal güç
    val luck: Int = 10,          // Şans

    // --- Oyuncu Profili ve Arketipler ---
    val playerProfile: PlayerProfile = PlayerProfile(),

    // --- Element Affinities (Element Yatkınlığı) ---
    val elementAffinities: List<ElementAffinity> = emptyList(), // Tüm elementlere olan yatkınlıklar

    // --- Ahlak ve Pasif Gelişim Sistemleri ---
    val moralityScore: Float = 0.0f,
    val sinPoints: Int = 0,
    val passiveBonuses: Map<String, Float> = emptyMap(),

    // --- Paladin'in İkilemi: Kutsallık/Kötülük Sistemi ---
    val holinessPoints: Int = 0,        // Mevcut kutsallık puanları
    val maxHolinessPoints: Int = 100,   // Maksimum kutsallık puanları
    val unholyPoints: Int = 0,          // Mevcut kötülük puanları
    val maxUnholyPoints: Int = 100,     // Maksimum kötülük puanları

    // --- Hibrit Rozet/Unvan Sistemi ---
    val equippedTitleId: String? = null, // Aktif unvan ID'si

    // --- G82: Status Effect System (Buff/Debuff) ---
    val activeStatusEffects: List<ActiveStatusEffect> = emptyList(), // Aktif buff/debuff'lar
    val currentTurn: Int = 0 // Mevcut tur sayısı (status effect süreleri için)
)
