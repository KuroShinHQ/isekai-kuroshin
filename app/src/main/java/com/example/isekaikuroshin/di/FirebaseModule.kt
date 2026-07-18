package com.example.isekaikuroshin.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * GÖREV O - FAZ 2: Firebase Dependency Injection Module
 *
 * Firebase Authentication ve Firestore instance'larını provide eder.
 *
 * ✅ GERÇEK Firebase kullanılıyor (Mock'lar kaldırıldı)
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    /**
     * Firebase Auth instance sağla
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    /**
     * Firebase Firestore instance sağla
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}
