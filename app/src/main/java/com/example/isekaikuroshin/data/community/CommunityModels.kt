package com.example.isekaikuroshin.data.community

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * GÖREV O - FAZ 2: Firestore Veri Modelleri
 *
 * Bu dosya Firebase Firestore için veri modellerini içerir:
 * - User: Kullanıcı bilgileri ve bağışçı durumu
 * - Poll: Topluluk oylamaları
 * - Vote: Kullanıcı oyları
 * - PollOption: Oylama seçenekleri
 */

// ============================================
// USER MODEL
// ============================================

/**
 * Firestore'da saklanan kullanıcı verisi
 *
 * Koleksiyon: users/{userId}
 *
 * @property uid Kullanıcının benzersiz ID'si (Firebase Auth UID)
 * @property displayName Görünen ad (opsiyonel)
 * @property role Kullanıcı rolü (user, supporter, moderator)
 * @property supporterSince Destekçi olduğu tarih (null ise destekçi değil)
 * @property totalDonationAmount Toplam bağış miktarı (TL)
 * @property votingAccessGranted Oylama erişimi var mı?
 * @property createdAt Hesap oluşturulma tarihi
 * @property lastActive Son aktif olma zamanı
 */
data class FirestoreUser(
    @DocumentId
    val uid: String = "",
    val displayName: String = "Anonymous",
    val role: UserRole = UserRole.USER,
    @ServerTimestamp
    val supporterSince: Timestamp? = null,
    val totalDonationAmount: Double = 0.0,
    val votingAccessGranted: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val lastActive: Timestamp? = null
)

/**
 * Kullanıcı rolleri
 *
 * - USER: Normal kullanıcı (ücretsiz)
 * - SUPPORTER: Bağış yapmış kullanıcı (oylama erişimi var)
 * - MODERATOR: Moderatör (poll oluşturabilir)
 */
enum class UserRole {
    USER,
    SUPPORTER,
    MODERATOR
}

// ============================================
// POLL MODEL
// ============================================

/**
 * Topluluk oylama verisi
 *
 * Koleksiyon: polls/{pollId}
 *
 * @property id Poll benzersiz ID'si
 * @property titleTr Türkçe başlık
 * @property titleEn İngilizce başlık
 * @property descriptionTr Türkçe açıklama
 * @property descriptionEn İngilizce açıklama
 * @property options Oylama seçenekleri
 * @property createdBy Poll'u oluşturan moderatör UID
 * @property createdAt Oluşturulma tarihi
 * @property endsAt Bitiş tarihi
 * @property isActive Aktif mi? (false ise sonuçlar görünür ama oy verilemez)
 * @property totalVotes Toplam oy sayısı
 */
data class Poll(
    @DocumentId
    val id: String = "",
    val titleTr: String = "",
    val titleEn: String = "",
    val descriptionTr: String = "",
    val descriptionEn: String = "",
    val options: List<PollOption> = emptyList(),
    val createdBy: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val endsAt: Timestamp? = null,
    val isActive: Boolean = true,
    val totalVotes: Int = 0
) {
    /**
     * Dil bazlı başlık getir
     */
    fun getTitle(language: String): String {
        return if (language == "tr") titleTr else titleEn
    }

    /**
     * Dil bazlı açıklama getir
     */
    fun getDescription(language: String): String {
        return if (language == "tr") descriptionTr else descriptionEn
    }

    /**
     * Poll bitti mi?
     */
    fun isExpired(): Boolean {
        val now = Timestamp.now()
        return endsAt != null && endsAt < now
    }

    /**
     * Oy verilebilir mi?
     */
    fun canVote(): Boolean {
        return isActive && !isExpired()
    }
}

/**
 * Oylama seçeneği
 *
 * @property id Seçenek ID'si (0, 1, 2, ...)
 * @property textTr Türkçe metin
 * @property textEn İngilizce metin
 * @property voteCount Bu seçeneğe verilen oy sayısı
 */
data class PollOption(
    val id: String = "",
    val textTr: String = "",
    val textEn: String = "",
    val voteCount: Int = 0
) {
    /**
     * Dil bazlı metin getir
     */
    fun getText(language: String): String {
        return if (language == "tr") textTr else textEn
    }

    /**
     * Yüzdelik oran hesapla
     */
    fun getPercentage(totalVotes: Int): Float {
        if (totalVotes == 0) return 0f
        return (voteCount.toFloat() / totalVotes) * 100f
    }
}

// ============================================
// VOTE MODEL
// ============================================

/**
 * Kullanıcı oyu
 *
 * Koleksiyon: polls/{pollId}/votes/{userId}
 *
 * @property userId Oy veren kullanıcı UID
 * @property pollId Hangi poll'a oy verildi
 * @property selectedOptionId Seçilen seçenek ID'si
 * @property votedAt Oy verme zamanı
 */
data class Vote(
    @DocumentId
    val userId: String = "",
    val pollId: String = "",
    val selectedOptionId: String = "",
    @ServerTimestamp
    val votedAt: Timestamp? = null
)

// ============================================
// DONATION MODEL
// ============================================

/**
 * Bağış kaydı (opsiyonel - analytics için)
 *
 * Koleksiyon: donations/{donationId}
 *
 * @property id Bağış benzersiz ID'si
 * @property userId Bağış yapan kullanıcı UID
 * @property amount Bağış miktarı (TL)
 * @property productId Google Play product ID (donation_5tl, donation_10tl, vb.)
 * @property purchaseToken Google Play purchase token
 * @property verified Sunucu tarafında doğrulandı mı?
 * @property createdAt Bağış tarihi
 */
data class Donation(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val currency: String = "TRY",  // Para birimi (TRY, USD, EUR...)
    val productId: String = "",
    val purchaseToken: String = "",
    val verified: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
