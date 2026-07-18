package com.example.isekaikuroshin.di

import android.content.Context
import com.example.isekaikuroshin.billing.BillingManager
import com.example.isekaikuroshin.data.community.FirebaseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * GÖREV O - FAZ 3: Billing Dependency Injection Module
 *
 * BillingManager instance'ını provide eder.
 */
@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    @Provides
    @Singleton
    fun provideBillingManager(
        @ApplicationContext context: Context,
        firebaseRepository: FirebaseRepository
    ): BillingManager {
        val billingManager = BillingManager(context, firebaseRepository)
        billingManager.initialize()
        return billingManager
    }
}
