package com.flashlight.torch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flashlight.torch.ads.AdManager
import com.flashlight.torch.ui.screens.MainScreen
import com.flashlight.torch.ui.theme.FlashlightTorchStrobeTheme   // ← change this to match YOUR Theme.kt
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Init AdMob
        MobileAds.initialize(this)

        // Preload interstitial immediately
        AdManager.preload(this)

        setContent {
            // ── Use whatever theme name is in your Theme.kt ──
            // Open ui/theme/Theme.kt and copy the exact function name
            FlashlightTorchStrobeTheme {
                // Activity is accessed via LocalContext inside MainScreen
                // No need to pass it as a parameter
                MainScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!AdManager.isReady()) AdManager.preload(this)
    }
}