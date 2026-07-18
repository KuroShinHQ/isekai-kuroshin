package com.example.isekaikuroshin.data.community

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GÖREV O - FAZ 2: Firebase Repository
 *
 * Firebase Firestore ile iletişim kuran repository.
 * Tüm CRUD işlemleri burada yapılır.
 *
 * Dependency Injection: Hilt @Singleton
 */
@Singleton
class FirebaseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    // ============================================
    // USER OPERATIONS
    // ============================================

    /**
     * Mevcut kullanıcının ID'sini getir
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Kullanıcı kaydı oluştur veya güncelle
     */
    suspend fun createOrUpdateUser(user: FirestoreUser): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(user.uid)
                .set(user)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kullanıcı bilgilerini getir
     */
    suspend fun getUser(uid: String): Result<FirestoreUser?> {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            val user = snapshot.toObject(FirestoreUser::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mevcut kullanıcının bilgilerini getir
     */
    suspend fun getCurrentUser(): Result<FirestoreUser?> {
        val currentUserId = auth.currentUser?.uid
            ?: return Result.failure(Exception("User not authenticated"))

        return getUser(currentUserId)
    }

    /**
     * Kullanıcıyı supporter yap
     */
    suspend fun upgradeToSupporter(
        uid: String,
        donationAmount: Double
    ): Result<Unit> {
        return try {
            val userRef = firestore.collection("users").document(uid)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentTotal = snapshot.getDouble("totalDonationAmount") ?: 0.0

                transaction.update(userRef, mapOf(
                    "role" to UserRole.SUPPORTER.name,
                    "supporterSince" to Timestamp.now(),
                    "totalDonationAmount" to (currentTotal + donationAmount),
                    "votingAccessGranted" to true
                ))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Son aktif zamanını güncelle
     */
    suspend fun updateLastActive(uid: String): Result<Unit> {
        return try {
            firestore.collection("users")
                .document(uid)
                .update("lastActive", Timestamp.now())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // POLL OPERATIONS
    // ============================================

    /**
     * Tüm aktif poll'ları getir
     */
    suspend fun getActivePolls(): Result<List<Poll>> {
        return try {
            val snapshot = firestore.collection("polls")
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val polls = snapshot.toObjects(Poll::class.java)
            Result.success(polls)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Belirli bir poll'u getir
     */
    suspend fun getPoll(pollId: String): Result<Poll?> {
        return try {
            val snapshot = firestore.collection("polls")
                .document(pollId)
                .get()
                .await()

            val poll = snapshot.toObject(Poll::class.java)
            Result.success(poll)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Poll oluştur (sadece moderatörler)
     */
    suspend fun createPoll(poll: Poll): Result<String> {
        return try {
            val docRef = firestore.collection("polls")
                .add(poll)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Poll'u kapat (bitir)
     */
    suspend fun closePoll(pollId: String): Result<Unit> {
        return try {
            firestore.collection("polls")
                .document(pollId)
                .update("isActive", false)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // VOTE OPERATIONS
    // ============================================

    /**
     * Oy ver
     *
     * Transaction kullanarak:
     * 1. Kullanıcının daha önce oy verip vermediğini kontrol et
     * 2. Vote kaydı oluştur
     * 3. Poll'un totalVotes ve option voteCount'unu artır
     */
    suspend fun castVote(
        pollId: String,
        optionId: String,
        userId: String
    ): Result<Unit> {
        return try {
            val pollRef = firestore.collection("polls").document(pollId)
            val voteRef = firestore.collection("polls")
                .document(pollId)
                .collection("votes")
                .document(userId)

            firestore.runTransaction { transaction ->
                // 1. Daha önce oy verilmiş mi kontrol et
                val voteSnapshot = transaction.get(voteRef)
                if (voteSnapshot.exists()) {
                    throw Exception("User already voted")
                }

                // 2. Vote kaydı oluştur
                val vote = Vote(
                    userId = userId,
                    pollId = pollId,
                    selectedOptionId = optionId,
                    votedAt = Timestamp.now()
                )
                transaction.set(voteRef, vote)

                // 3. Poll'u güncelle
                val pollSnapshot = transaction.get(pollRef)
                val poll = pollSnapshot.toObject(Poll::class.java)
                    ?: throw Exception("Poll not found")

                // Option vote count'u artır
                val updatedOptions = poll.options.map { option ->
                    if (option.id == optionId) {
                        option.copy(voteCount = option.voteCount + 1)
                    } else {
                        option
                    }
                }

                transaction.update(pollRef, mapOf(
                    "options" to updatedOptions,
                    "totalVotes" to (poll.totalVotes + 1)
                ))
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kullanıcının verdiği oyu getir
     */
    suspend fun getUserVote(pollId: String, userId: String): Result<Vote?> {
        return try {
            val snapshot = firestore.collection("polls")
                .document(pollId)
                .collection("votes")
                .document(userId)
                .get()
                .await()

            val vote = snapshot.toObject(Vote::class.java)
            Result.success(vote)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kullanıcı bu poll'a oy vermiş mi?
     */
    suspend fun hasUserVoted(pollId: String, userId: String): Result<Boolean> {
        return try {
            val snapshot = firestore.collection("polls")
                .document(pollId)
                .collection("votes")
                .document(userId)
                .get()
                .await()

            Result.success(snapshot.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============================================
    // DONATION OPERATIONS
    // ============================================

    /**
     * Bağış kaydet (Google Play purchase token ile)
     *
     * @param userId Kullanıcı ID
     * @param amount Bağış miktarı (TL)
     * @param productId Google Play product ID (donation_10tl, donation_25tl, donation_50tl)
     * @param purchaseToken Google Play purchase token
     */
    suspend fun saveDonation(
        userId: String,
        amount: Double,
        productId: String,
        purchaseToken: String
    ): Result<String> {
        val donation = Donation(
            userId = userId,
            amount = amount,
            currency = "TRY",
            productId = productId,
            purchaseToken = purchaseToken,
            verified = false, // Cloud Function ile doğrulanacak
            createdAt = Timestamp.now()
        )
        return recordDonation(donation)
    }

    /**
     * Bağış kaydı oluştur
     */
    suspend fun recordDonation(donation: Donation): Result<String> {
        return try {
            val docRef = firestore.collection("donations")
                .add(donation)
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bağışı doğrulanmış olarak işaretle
     */
    suspend fun verifyDonation(donationId: String): Result<Unit> {
        return try {
            firestore.collection("donations")
                .document(donationId)
                .update("verified", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Kullanıcının toplam bağışını getir
     */
    suspend fun getUserDonations(userId: String): Result<List<Donation>> {
        return try {
            val snapshot = firestore.collection("donations")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val donations = snapshot.toObjects(Donation::class.java)
            Result.success(donations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
