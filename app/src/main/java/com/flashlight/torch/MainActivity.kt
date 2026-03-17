package com.flashlight.torch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.ads.MobileAds
import com.flashlight.torch.ui.screens.MainScreen
import com.flashlight.torch.ui.theme.FlashlightTorchStrobeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MobileAds.initialize(this)
        setContent {
            FlashlightTorchStrobeTheme {
                MainScreen()
            }
        }
    }
}