package com.example.isekaikuroshin.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.example.isekaikuroshin.data.community.FirebaseRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GÖREV O - FAZ 3: Google Play Billing Manager
 *
 * Google Play In-App Purchases entegrasyonu
 * - Bağış paketlerini yönetir (10 TL, 25 TL, 50 TL)
 * - Satın alma akışını handle eder
 * - Firebase ile webhook entegrasyonu sağlar
 *
 * @see https://developer.android.com/google/play/billing
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseRepository: FirebaseRepository
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Billing Client
    private lateinit var billingClient: BillingClient

    // Bağış Paketleri (Product IDs)
    companion object {
        const val DONATION_SMALL = "donation_10tl"   // 10 TL
        const val DONATION_MEDIUM = "donation_25tl"  // 25 TL
        const val DONATION_LARGE = "donation_50tl"   // 50 TL

        private const val TAG = "BillingManager"
    }

    // UI State
    private val _billingState = MutableStateFlow<BillingState>(BillingState.Idle)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val availableProducts: StateFlow<List<ProductDetails>> = _availableProducts.asStateFlow()

    /**
     * Initialize Billing Client
     */
    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        connectToBillingService()
    }

    /**
     * Connect to Google Play Billing Service
     */
    private fun connectToBillingService() {
        Log.d(TAG, "Connecting to Google Play Billing...")
        _billingState.value = BillingState.Connecting

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ Billing connected successfully")
                    _billingState.value = BillingState.Connected
                    queryProducts()
                } else {
                    Log.e(TAG, "❌ Billing connection failed: ${billingResult.debugMessage}")
                    _billingState.value = BillingState.Error(billingResult.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "⚠️ Billing service disconnected")
                _billingState.value = BillingState.Disconnected
                // Retry connection
                connectToBillingService()
            }
        })
    }

    /**
     * Query available products (donation packages)
     */
    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(DONATION_SMALL)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(DONATION_MEDIUM)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(DONATION_LARGE)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "✅ Found ${productDetailsList.size} products")
                _availableProducts.value = productDetailsList
            } else {
                Log.e(TAG, "❌ Product query failed: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Launch purchase flow for a donation package
     *
     * @param activity Current activity (required for Google Play UI)
     * @param productDetails Product to purchase
     */
    fun purchaseDonation(activity: Activity, productDetails: ProductDetails) {
        if (!billingClient.isReady) {
            Log.e(TAG, "❌ Billing client not ready")
            _billingState.value = BillingState.Error("Billing service not ready")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        Log.d(TAG, "🛒 Launching purchase flow for: ${productDetails.productId}")
        _billingState.value = BillingState.PurchasingInProgress

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "❌ Purchase flow failed: ${billingResult.debugMessage}")
            _billingState.value = BillingState.Error(billingResult.debugMessage)
        }
    }

    /**
     * Called when purchases are updated (PurchasesUpdatedListener)
     */
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.w(TAG, "⚠️ User canceled purchase")
            _billingState.value = BillingState.PurchaseCanceled
        } else {
            Log.e(TAG, "❌ Purchase failed: ${billingResult.debugMessage}")
            _billingState.value = BillingState.Error(billingResult.debugMessage)
        }
    }

    /**
     * Handle a purchase (verify and acknowledge)
     */
    private fun handlePurchase(purchase: Purchase) {
        Log.d(TAG, "🎁 Processing purchase: ${purchase.products.joinToString()}")

        // 1. Verify purchase state
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // 2. Acknowledge purchase (required for consumables)
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }

            // 3. Grant user entitlement (upgrade to supporter + record donation)
            grantDonationEntitlement(purchase)
        }
    }

    /**
     * Acknowledge purchase with Google Play
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "✅ Purchase acknowledged")
            } else {
                Log.e(TAG, "❌ Acknowledge failed: ${billingResult.debugMessage}")
            }
        }
    }

    /**
     * Grant donation entitlement to user
     * - Save donation to Firestore
     * - Upgrade user to SUPPORTER role
     */
    private fun grantDonationEntitlement(purchase: Purchase) {
        scope.launch {
            try {
                val productId = purchase.products.first()
                val amount = when (productId) {
                    DONATION_SMALL -> 10.0
                    DONATION_MEDIUM -> 25.0
                    DONATION_LARGE -> 50.0
                    else -> 0.0
                }

                // Get current user ID (Firebase Auth)
                val userId = firebaseRepository.getCurrentUserId()
                if (userId == null) {
                    Log.e(TAG, "❌ User not authenticated")
                    _billingState.value = BillingState.Error("User not authenticated")
                    return@launch
                }

                // Save donation to Firestore
                val donationResult = firebaseRepository.saveDonation(
                    userId = userId,
                    amount = amount,
                    productId = productId,
                    purchaseToken = purchase.purchaseToken
                )

                if (donationResult.isSuccess) {
                    Log.d(TAG, "✅ Donation saved to Firestore")

                    // Upgrade to supporter (if amount >= 10 TL)
                    if (amount >= 10.0) {
                        val upgradeResult = firebaseRepository.upgradeToSupporter(userId, amount)
                        if (upgradeResult.isSuccess) {
                            Log.d(TAG, "🎉 User upgraded to SUPPORTER!")
                            _billingState.value = BillingState.PurchaseSuccess(amount)
                        } else {
                            Log.e(TAG, "❌ Supporter upgrade failed: ${upgradeResult.exceptionOrNull()}")
                            _billingState.value = BillingState.Error("Failed to upgrade user")
                        }
                    }
                } else {
                    Log.e(TAG, "❌ Donation save failed: ${donationResult.exceptionOrNull()}")
                    _billingState.value = BillingState.Error("Failed to record donation")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Donation entitlement error: ${e.message}")
                _billingState.value = BillingState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Clean up resources
     */
    fun destroy() {
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
            Log.d(TAG, "Billing client disconnected")
        }
    }
}

/**
 * Billing State for UI
 */
sealed class BillingState {
    data object Idle : BillingState()
    data object Connecting : BillingState()
    data object Connected : BillingState()
    data object Disconnected : BillingState()
    data object PurchasingInProgress : BillingState()
    data object PurchaseCanceled : BillingState()
    data class PurchaseSuccess(val amount: Double) : BillingState()
    data class Error(val message: String) : BillingState()
}
