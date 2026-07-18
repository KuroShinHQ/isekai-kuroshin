package com.example.isekaikuroshin.engine

import com.example.isekaikuroshin.data.GameStateZ7
import com.example.isekaikuroshin.data.PlayerState
import com.example.isekaikuroshin.data.GameStateManager
import com.example.isekaikuroshin.utils.GameLogger
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UmbrosEngine - Gelişmiş Umbros Paktı ve AI güdümlü müzakere motoru
 *
 * Bu motor:
 * 1. GameState'i analiz eder (Gözlemci)
 * 2. Dinamik monologlar üretir (AI destekli)
 * 3. Pazarlık mantığını yönetir
 */
@Singleton
class UmbrosEngine @Inject constructor() {

    /**
     * FAZ 3.1: Gözlemci Fonksiyonu
     * Oyuncunun GameState'ini analiz ederek Umbros için bir özet çıkarır
     */
    fun analyzePlayerState(gameState: GameStateZ7): PlayerAnalysis {
        val playerState = gameState.playerState

        // Ahlaki durum analizi
        val moralAssessment = analyzeMorality(playerState)

        // Ölüm nedeni analizi
        val deathCause = analyzeDeathCause(playerState)

        // Rozet ve başarım analizi
        val achievementSummary = analyzeAchievements(gameState)

        // Oyun ilerlemesi analizi
        val progressAnalysis = analyzeProgress(gameState)

        // Umbros paktı uygunluk analizi
        val pactEligibility = analyzePactEligibility(gameState)

        return PlayerAnalysis(
            level = playerState.level,
            moralBalance = moralAssessment,
            holinessPoints = playerState.holinessPoints,
            unholyPoints = playerState.unholyPoints,
            deathCause = deathCause,
            achievements = achievementSummary,
            questProgress = progressAnalysis.questsCompleted,
            threatLevel = gameState.threatCounter,
            daysAlive = gameState.currentDay,
            pactEligible = pactEligibility.eligible,
            pactReason = pactEligibility.reason,
            overallAssessment = generateOverallAssessment(playerState, gameState)
        )
    }

    /**
     * Ahlaki durumu analiz eder
     */
    private fun analyzeMorality(playerState: PlayerState): String {
        val holinessRatio = if (playerState.maxHolinessPoints > 0)
            playerState.holinessPoints.toFloat() / playerState.maxHolinessPoints else 0f
        val unholyRatio = if (playerState.maxUnholyPoints > 0)
            playerState.unholyPoints.toFloat() / playerState.maxUnholyPoints else 0f

        val moralityScore = playerState.moralityScore

        return when {
            holinessRatio > 0.8f -> "Saf Aziz - Kutsallık gücüyle dolu"
            holinessRatio > 0.6f -> "Kutsal Savaşçı - İyiliğin savunucusu"
            unholyRatio > 0.8f -> "Karanlık Ruh - Kötülükle yozlaşmış"
            unholyRatio > 0.6f -> "Gölge Yürüyen - Karanlığa meyilli"
            moralityScore > 0.5f -> "İyi Kalp - Genel olarak iyilik yanlısı"
            moralityScore < -0.5f -> "Kötü Niyet - Genelde kötülük eğilimli"
            else -> "Dengeli Ruh - İyi ve kötü arasında"
        }
    }

    /**
     * Ölüm nedenini tahmin eder
     */
    private fun analyzeDeathCause(playerState: PlayerState): String {
        return when {
            playerState.currentHealth <= 0 && playerState.currentMana <= 0 -> "Savaşta tükenerek öldü"
            playerState.currentHealth <= 0 -> "Fiziksel yaralanmalardan öldü"
            playerState.hunger < 20 -> "Açlıktan öldü"
            playerState.fatigue > 80 -> "Aşırı yorgunluktan öldü"
            else -> "Bilinmeyen nedenlerle öldü"
        }
    }

    /**
     * Başarımları ve rozetleri analiz eder
     */
    private fun analyzeAchievements(gameState: GameStateZ7): String {
        val badgeCount = gameState.activeBadges.size
        val completedQuests = gameState.completedQuests.size

        return when {
            badgeCount > 10 -> "Çok sayıda rozet ve başarım sahibi"
            badgeCount > 5 -> "Birkaç önemli başarım elde etmiş"
            completedQuests > 20 -> "Deneyimli görev tamamlayıcısı"
            completedQuests > 10 -> "Orta düzey görev deneyimi"
            else -> "Henüz deneyimsiz bir maceraperest"
        }
    }

    /**
     * Oyuncu ilerlemesini analiz eder
     */
    private fun analyzeProgress(gameState: GameStateZ7): ProgressAnalysis {
        val questsCompleted = gameState.completedQuests.size
        val daysAlive = gameState.currentDay
        val locationsDiscovered = gameState.knownLocations.size

        return ProgressAnalysis(
            questsCompleted = questsCompleted,
            daysAlive = daysAlive,
            locationsDiscovered = locationsDiscovered,
            progressRating = when {
                questsCompleted > 15 && daysAlive > 30 -> "Excellent"
                questsCompleted > 10 && daysAlive > 20 -> "Good"
                questsCompleted > 5 && daysAlive > 10 -> "Average"
                else -> "Beginner"
            }
        )
    }

    /**
     * Umbros paktı uygunluğunu analiz eder
     */
    private fun analyzePactEligibility(gameState: GameStateZ7): PactEligibility {
        val playerState = gameState.playerState
        val hasEnoughUnholy = playerState.unholyPoints >= 50
        val hasNoActivePact = gameState.umbrosContract == null
        val isDead = playerState.currentHealth <= 0
        val hasInterestingProfile = gameState.activeBadges.isNotEmpty() || gameState.completedQuests.size > 5

        return when {
            !isDead -> PactEligibility(false, "Henüz ölmemiş - pakt için çok erken")
            !hasEnoughUnholy -> PactEligibility(false, "Yeterli kötülük puanı yok - en az 50 gerekli")
            !hasNoActivePact -> PactEligibility(false, "Zaten aktif bir Umbros paktı var")
            !hasInterestingProfile -> PactEligibility(false, "Ruh yeterince ilginç değil - daha fazla deneyim gerekli")
            else -> PactEligibility(true, "Pakt için tüm koşullar sağlanmış")
        }
    }

    /**
     * Genel değerlendirme oluşturur
     */
    private fun generateOverallAssessment(playerState: PlayerState, gameState: GameStateZ7): String {
        val level = playerState.level
        val questCount = gameState.completedQuests.size
        val days = gameState.currentDay
        val morality = playerState.moralityScore

        return when {
            level > 20 && questCount > 15 -> "Deneyimli ve güçlü bir ruh"
            level > 10 && days > 20 -> "Orta düzey deneyimli bir maceracı"
            morality > 0.5f -> "İyi kalbi olan umut verici bir ruh"
            morality < -0.5f -> "Karanlık eğilimleri olan tehlikeli bir ruh"
            else -> "Potansiyeli olan genç bir ruh"
        }
    }

    /**
     * FAZ 3.2: Dinamik Monolog Üretici (AI Destekli)
     * Gözlemci'den gelen veriyi kullanarak 3 aşamalı monolog üretir
     */
    fun generateDynamicMonologue(analysis: PlayerAnalysis, phase: MonologuePhase): String {
        return when (phase) {
            MonologuePhase.PAST -> generatePastReflection(analysis)
            MonologuePhase.STATS -> generateStatAnalysis(analysis)
            MonologuePhase.OFFER -> generateOffer(analysis)
        }
    }

    /**
     * Geçmiş yansıması monologu
     */
    private fun generatePastReflection(analysis: PlayerAnalysis): String {
        val baseGreeting = "Merhabalar, Kuroshin... ${analysis.daysAlive} gün süren yolculuğun sona erdi."

        val reflection = when {
            analysis.questProgress > 15 ->
                "Birçok görevi tamamlamış, deneyimli bir maceracı olduğunu görmüş bulunuyorum. ${analysis.deathCause}, ne yazık ki..."
            analysis.questProgress > 5 ->
                "Orta seviyede deneyim kazanmışsın. ${analysis.deathCause}, ama bu sadece başlangıç..."
            else ->
                "Henüz yolculuğunun başındaydın. ${analysis.deathCause}, ancak bu sona değil, yeni bir başlangıça işaret..."
        }

        val moralComment = when {
            analysis.moralBalance.contains("Saf Aziz") ->
                "Ruhunda taşıdığın kutsallık beni... ilgilendiriyor."
            analysis.moralBalance.contains("Karanlık") ->
                "Ruhundaki karanlık bana hiç de yabancı gelmiyor..."
            else ->
                "Ruhundaki denge ilginç. İyi ve kötü arasında gidip geliyorsun..."
        }

        return "$baseGreeting\n\n$reflection\n\n$moralComment"
    }

    /**
     * Stat analizi monologu
     */
    private fun generateStatAnalysis(analysis: PlayerAnalysis): String {
        val levelComment = when {
            analysis.level > 20 -> "Seviye ${analysis.level}... İlginç. Güçlü bir ruha sahipsin."
            analysis.level > 10 -> "Seviye ${analysis.level}. Orta düzey güç, fakat potansiyel mevcut."
            else -> "Seviye ${analysis.level}. Henüz genç bir ruh, ama bu bazen avantajdır..."
        }

        val moralityAnalysis = when {
            analysis.holinessPoints > 80 ->
                "Kutsallık puanların yüksek (${analysis.holinessPoints}). Bu... ironik. Karanlık güçlerden yardım isteyen bir aziz..."
            analysis.unholyPoints > 80 ->
                "Kötülük puanların yüksek (${analysis.unholyPoints}). Görüyorum ki karanlığa aşina bir ruhsun. Bu bizim işimizi kolaylaştırır."
            else ->
                "Ahlaki dengen ilginç. Kutsallık: ${analysis.holinessPoints}, Kötülük: ${analysis.unholyPoints}. Çelişkili bir ruh..."
        }

        val threatAssessment = when {
            analysis.threatLevel > 150 -> "Tehdit seviyenin yüksek (${analysis.threatLevel}). Tehlikeli bir yaşam sürmüşsün."
            analysis.threatLevel > 50 -> "Orta düzey risk almışsın (${analysis.threatLevel}). Cesur ama temkinli."
            else -> "Güvenli bir oyun oynamışsın (${analysis.threatLevel}). Belki de çok güvenli..."
        }

        return "$levelComment\n\n$moralityAnalysis\n\n$threatAssessment\n\n${analysis.achievements}"
    }

    /**
     * Teklif monologu
     */
    private fun generateOffer(analysis: PlayerAnalysis): String {
        val baseOffer = "İşte sana teklifim, Kuroshin..."

        val offerDetails = if (analysis.pactEligible) {
            when {
                analysis.unholyPoints > 80 ->
                    "Karanlık ruhun beni etkiledi. Sana sadece ikinci bir şans değil, güçlendirilmiş yetenekler de sunabilirim. Karşılığında... ruhunun büyük bir kısmını istiyorum."
                analysis.holinessPoints > 80 ->
                    "Kutsallığına rağmen burada olman ironik. Belki de o kutsallığın seni koruyamadı? Ben seni koruyabilirim... tabii ki bir bedeli var."
                else ->
                    "Standart bir pakt öneriyorum: Hayata geri dön, ama ruhundan bir parça benimle kalacak. Zamanla bu parça büyüyecek..."
            }
        } else {
            "Ne yazık ki şu anda pakt için uygun değilsin. ${analysis.pactReason} Ama merak etme, bu sadece şimdilik..."
        }

        val finalWarning = when {
            analysis.moralBalance.contains("Saf") ->
                "Dikkatli ol, aziz. Bu pakt seni sonsuza dek değiştirecek. Kutsallığın... bulanabilir."
            analysis.moralBalance.contains("Karanlık") ->
                "Zaten karanlık bir ruhsun. Bu pakt sadece... doğanı güçlendirecek."
            else ->
                "Bu karar hayatının geri kalanını şekillendirecek. Seç bakalım: ölüm mü, yoksa benimle bir ömür boyu bağ mı?"
        }

        return "$baseOffer\n\n$offerDetails\n\n$finalWarning"
    }

    /**
     * FAZ 3.3: Pazarlık Fonksiyonu
     * Oyuncunun input'una göre nihai teklifi oluşturur
     */
    fun negotiatePact(
        playerInput: String,
        analysis: PlayerAnalysis,
        currentPhase: MonologuePhase
    ): NegotiationResult {

        // Oyuncu input'unu analiz et
        val inputAnalysis = analyzePlayerInput(playerInput)

        // Mevcut duruma göre Umbros'un tepkisini belirle
        val umbrosResponse = generateUmbrosResponse(inputAnalysis, analysis, currentPhase)

        // Oyun etkilerini hesapla
        val gameActions = calculateGameActions(inputAnalysis, analysis)

        return NegotiationResult(
            umbrosResponse = umbrosResponse,
            gameActions = gameActions,
            nextPhase = getNextPhase(currentPhase), // inputAnalysis argument removed
            pactAccepted = inputAnalysis.indicatesAcceptance && analysis.pactEligible,
            negotiationComplete = inputAnalysis.indicatesAcceptance || inputAnalysis.indicatesRejection
        )
    }

    /**
     * Oyuncu input'unu analiz eder
     */
    private fun analyzePlayerInput(input: String): InputAnalysis {
        val lowerInput = input.lowercase()

        val indicatesAcceptance = lowerInput.contains("kabul") ||
                                 lowerInput.contains("evet") ||
                                 lowerInput.contains("tamam") ||
                                 lowerInput.contains("anlaştık")

        val indicatesRejection = lowerInput.contains("hayır") ||
                                lowerInput.contains("reddet") ||
                                lowerInput.contains("istemiyorum") ||
                                lowerInput.contains("olmaz")

        val isQuestionAboutPact = lowerInput.contains("pakt") ||
                                 lowerInput.contains("sözleşme") ||
                                 lowerInput.contains("bedel") ||
                                 lowerInput.contains("şartlar")

        val showsInterest = lowerInput.contains("ilgileniyorum") ||
                           lowerInput.contains("dinliyorum") ||
                           lowerInput.contains("anlat")

        val showsFear = lowerInput.contains("korkuyorum") ||
                       lowerInput.contains("endişeliyim") ||
                       lowerInput.contains("tehlikeli")

        return InputAnalysis(
            indicatesAcceptance = indicatesAcceptance,
            indicatesRejection = indicatesRejection,
            isQuestionAboutPact = isQuestionAboutPact,
            showsInterest = showsInterest,
            showsFear = showsFear,
            sentiment = when {
                indicatesAcceptance -> "POSITIVE"
                indicatesRejection -> "NEGATIVE"
                showsInterest -> "CURIOUS"
                showsFear -> "FEARFUL"
                else -> "NEUTRAL"
            }
        )
    }

    /**
     * Input analizine göre Umbros tepkisi üretir
     */
    private fun generateUmbrosResponse(
        inputAnalysis: InputAnalysis,
        playerAnalysis: PlayerAnalysis,
        phase: MonologuePhase
    ): String {
        return when (inputAnalysis.sentiment) {
            "POSITIVE" -> when (phase) {
                MonologuePhase.PAST -> "İyi, iyi... Geçmişini kabul ettiğini görmek güzel. Şimdi mevcut durumuna bakalım..."
                MonologuePhase.STATS -> "Statlarını beğendiğimi söylemek isterim. Şimdi asıl teklifime geliyorum..."
                MonologuePhase.OFFER -> if (playerAnalysis.pactEligible)
                    "Mükemmel! Pakt kabul edildi. Ruhun artık benimle bağlı... Hayata dön, Kuroshin!"
                else
                    "Kabul etmek istediğini anlıyorum ama maalesef şartları sağlamıyorsun. Daha fazla kötülük eylemi gerektiriyor..."
            }
            "NEGATIVE" -> when (phase) {
                MonologuePhase.PAST -> "Ret mi? İlginç... Çoğu insan geçmişiyle yüzleşmek istemez. Ama ben sabırlıyım..."
                MonologuePhase.STATS -> "Statlarım hoşuna gitmedi mi? Peki, teklifimi duyduğunda fikrini değiştirebilirsin..."
                MonologuePhase.OFFER -> "Reddettin... İlginç seçim. Ölümü seçtğin için seni suçlayamam. Ama unutma, ben hep buradayım..."
            }
            "CURIOUS" -> when (phase) {
                MonologuePhase.PAST -> "Meraklısın, bu hoşuma gidiyor. Merak öğrenmenin başlangıcıdır..."
                MonologuePhase.STATS -> "Sorular soruyorsun, bu akıllıca. Bilgi güçtür, özellikle bu tür... anlaşmalarda..."
                MonologuePhase.OFFER -> "Dikkatli olmak istiyorsun, anlıyorum. Ama zamanımız sınırlı, Kuroshin..."
            }
            "FEARFUL" -> when (phase) {
                MonologuePhase.PAST -> "Korku... Doğal bir tepki. Ama korkunun seni paraliz etmesine izin verme..."
                MonologuePhase.STATS -> "Korkuyor musun? İyi... Korku seni dikkatli yapar. Bu iyi bir şey."
                MonologuePhase.OFFER -> "Korkun haklı, bu büyük bir karar. Ama alternatif... ebedi ölüm."
            }
            else -> when (phase) {
                MonologuePhase.PAST -> "Sessizlik... Düşünüyorsun. Bu da iyi."
                MonologuePhase.STATS -> "Analiz ediyorsun, beğendim. Devam edelim..."
                MonologuePhase.OFFER -> "Karar zamanı geldi, Kuroshin. Ne diyorsun?"
            }
        }
    }

    /**
     * Pazarlık sonucuna göre oyun eylemlerini hesaplar
     */
    private fun calculateGameActions(inputAnalysis: InputAnalysis, analysis: PlayerAnalysis): List<GameAction> {
        val actions = mutableListOf<GameAction>()

        if (inputAnalysis.indicatesAcceptance && analysis.pactEligible) {
            // Pakt kabul edildi
            actions.add(GameAction.REVIVE_PLAYER)
            actions.add(GameAction.CREATE_UMBROS_CONTRACT)
            actions.add(GameAction.ADD_UNHOLY_POINTS(20))
            actions.add(GameAction.MODIFY_STATS_BASED_ON_PACT)
        } else if (inputAnalysis.indicatesRejection) {
            // Pakt reddedildi
            actions.add(GameAction.REMAIN_DEAD)
            actions.add(GameAction.ADD_STORY_PAGE("Umbros'u reddettin ve ölümü seçtin..."))
        }

        return actions
    }

    /**
     * Sonraki aşamayı belirler
     */
    private fun getNextPhase(currentPhase: MonologuePhase): MonologuePhase { // inputAnalysis parameter removed
        return when (currentPhase) {
            MonologuePhase.PAST -> MonologuePhase.STATS
            MonologuePhase.STATS -> MonologuePhase.OFFER
            MonologuePhase.OFFER -> MonologuePhase.OFFER // Son aşamada kalır
        }
    }
}

// Data sınıfları
@Serializable
data class PlayerAnalysis(
    val level: Int,
    val moralBalance: String,
    val holinessPoints: Int,
    val unholyPoints: Int,
    val deathCause: String,
    val achievements: String,
    val questProgress: Int,
    val threatLevel: Int,
    val daysAlive: Int,
    val pactEligible: Boolean,
    val pactReason: String,
    val overallAssessment: String
)

@Serializable
data class ProgressAnalysis(
    val questsCompleted: Int,
    val daysAlive: Int,
    val locationsDiscovered: Int,
    val progressRating: String
)

@Serializable
data class PactEligibility(
    val eligible: Boolean,
    val reason: String
)

@Serializable
data class InputAnalysis(
    val indicatesAcceptance: Boolean,
    val indicatesRejection: Boolean,
    val isQuestionAboutPact: Boolean,
    val showsInterest: Boolean,
    val showsFear: Boolean,
    val sentiment: String
)

@Serializable
data class NegotiationResult(
    val umbrosResponse: String,
    val gameActions: List<GameAction>,
    val nextPhase: MonologuePhase,
    val pactAccepted: Boolean,
    val negotiationComplete: Boolean
)

enum class MonologuePhase {
    PAST,    // Geçmiş yansıması
    STATS,   // Stat analizi
    OFFER    // Teklif
}

@Serializable
sealed class GameAction {
    @Serializable
    object REVIVE_PLAYER : GameAction()
    @Serializable
    object CREATE_UMBROS_CONTRACT : GameAction()
    @Serializable
    object REMAIN_DEAD : GameAction()
    @Serializable
    object MODIFY_STATS_BASED_ON_PACT : GameAction()
    @Serializable
    data class ADD_UNHOLY_POINTS(val amount: Int) : GameAction()
    @Serializable
    data class ADD_HOLINESS_POINTS(val amount: Int) : GameAction()
    @Serializable
    data class ADD_STORY_PAGE(val content: String) : GameAction()
    @Serializable
    data class MODIFY_DICE_ROLLS(val modifier: Int) : GameAction()
}