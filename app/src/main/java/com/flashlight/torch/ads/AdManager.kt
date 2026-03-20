package com.flashlight.torch.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {

    private const val TAG = "AdManager"

    // ── Test IDs — replace with real ones before publishing ──
    const val INTERSTITIAL_ID = "ca-app-pub-5963680420417931/4409543201"
    const val BANNER_ID       = "ca-app-pub-5963680420417931/9083285380"

    private var interstitialAd: InterstitialAd? = null
    private var isLoading     = false
    private var lastShownTime = 0L

    // Show interstitial at most once every 3 minutes
    private const val MIN_INTERVAL_MS = 3 * 60 * 1000L

    fun preload(context: Context) {
        if (isLoading || interstitialAd != null) return
        isLoading = true
        Log.d(TAG, "Preloading interstitial...")

        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading      = false
                    Log.d(TAG, "✅ Interstitial loaded")

                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            isLoading      = false
                            preload(context)
                        }
                        override fun onAdFailedToShowFullScreenContent(e: AdError) {
                            interstitialAd = null
                            isLoading      = false
                            preload(context)
                        }
                        override fun onAdShowedFullScreenContent() {
                            lastShownTime = System.currentTimeMillis()
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading      = false
                    Log.e(TAG, "Load failed: ${error.message}")
                    // Retry after 30s
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        preload(context)
                    }, 30_000)
                }
            }
        )
    }

    fun show(activity: Activity, force: Boolean = false): Boolean {
        val ad = interstitialAd ?: run {
            preload(activity)
            return false
        }
        val elapsed = System.currentTimeMillis() - lastShownTime
        if (!force && elapsed < MIN_INTERVAL_MS) return false
        ad.show(activity)
        return true
    }

    fun isReady() = interstitialAd != null
}