package com.example.isekaikuroshin.auth

import com.example.isekaikuroshin.utils.GameLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthManager - Gelecekteki Google Auth entegrasyonu için hazırlık katmanı
 *
 * Bu sınıf şu anda sahte (stub) implementasyon olarak çalışır.
 * Gerçek Google Auth entegrasyonu yapıldığında bu sınıf güncellenerek
 * gerçek authentication işlemleri gerçekleştirilecektir.
 */
@Singleton
class AuthManager @Inject constructor() {

    companion object {
        private const val TAG = "AuthManager"
    }

    /**
     * Kullanıcının oturum açıp açmadığını kontrol eder
     * Şu anda her zaman false döner (sahte implementasyon)
     */
    fun isUserLoggedIn(): Boolean {
        GameLogger.logSystem("$TAG: Kullanıcı oturum durumu kontrol ediliyor (sahte - false)")
        return false
    }

    /**
     * Google ile oturum açma işlemini başlatır
     * Şu anda sadece log kayıt eder (sahte implementasyon)
     */
    suspend fun signInWithGoogle(): AuthResult {
        GameLogger.logSystem("$TAG: Google ile oturum açma başlatıldı (sahte implementasyon)")
        // Gerçek Google Auth burada implement edilecek
        return AuthResult.NotImplemented
    }

    /**
     * Oturum kapatma işlemini gerçekleştirir
     * Şu anda sadece log kayıt eder (sahte implementasyon)
     */
    suspend fun signOut(): AuthResult {
        GameLogger.logSystem("$TAG: Oturum kapatma işlemi başlatıldı (sahte implementasyon)")
        // Gerçek oturum kapatma işlemi burada implement edilecek
        return AuthResult.NotImplemented
    }

    /**
     * Mevcut kullanıcının bilgilerini getirir
     * Şu anda null döner (sahte implementasyon)
     */
    fun getCurrentUser(): AuthUser? {
        GameLogger.logSystem("$TAG: Mevcut kullanıcı bilgileri istendi (sahte - null)")
        return null
    }
}

/**
 * Authentication işlemlerinin sonucunu temsil eden sealed class
 */
sealed class AuthResult {
    object Success : AuthResult()
    object Failed : AuthResult()
    object Cancelled : AuthResult()
    object NotImplemented : AuthResult() // Sahte implementasyon için
}

/**
 * Authentication edilmiş kullanıcının bilgilerini temsil eden data class
 */
data class AuthUser(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?
)