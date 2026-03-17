package com.flashlight.torch.ui.screens

import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flashlight.torch.ui.theme.*
import com.flashlight.torch.utils.BannerAd
import com.flashlight.torch.viewmodel.FlashMode
import com.flashlight.torch.viewmodel.FlashlightViewModel

@Composable
fun MainScreen(vm: FlashlightViewModel = viewModel()) {
    val state        by vm.state.collectAsStateWithLifecycle()
    var selectedTab  by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { vm.updateBattery() }

    // ── Animations ──
    val infiniteAnim = rememberInfiniteTransition(label = "main")
    val glowAlpha by infiniteAnim.animateFloat(
        initialValue  = 0.35f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = EaseInOut), RepeatMode.Reverse
        ), label = "glow"
    )
    val ringScale by infiniteAnim.animateFloat(
        initialValue  = 1f, targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = EaseInOut), RepeatMode.Reverse
        ), label = "ring"
    )
    val rotAnim by infiniteAnim.animateFloat(
        initialValue  = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(8000, easing = LinearEasing), RepeatMode.Restart
        ), label = "rot"
    )

    // ── SCREEN LIGHT TAKEOVER ──
    if (state.screenLightOn) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(state.screenLightColor.toInt()))
                .clickable { vm.toggleScreenLight() }
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.TouchApp, null,
                        tint     = Color.Black.copy(0.3f),
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        "Tap to exit",
                        color    = Color.Black.copy(0.3f),
                        fontSize = 11.sp
                    )
                }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF06060F))
    ) {
        // Background ambient glow blobs
        Box(
            modifier = Modifier
                .size(420.dp)
                .offset(x = (-90).dp, y = (-70).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (state.isOn) YellowMain.copy(0.1f)
                            else Color(0xFF1A0A5C).copy(0.35f),
                            Color.Transparent
                        )
                    )
                )
                .blur(70.dp)
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 70.dp, y = 70.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (state.isOn) OrangeAcc.copy(0.07f)
                            else Color(0xFF0A1A5C).copy(0.25f),
                            Color.Transparent
                        )
                    )
                )
                .blur(60.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── TOP BAR ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "FLASHLIGHT",
                        color         = Color.White,
                        fontSize      = 22.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 4.sp
                    )
                    Text(
                        "Pro · Strobe · Morse · Sound",
                        color         = Color.White.copy(0.35f),
                        fontSize      = 11.sp,
                        letterSpacing = 1.sp
                    )
                }

                // Battery pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        state.batteryLevel >= 50 -> GreenAcc.copy(0.15f)
                        state.batteryLevel >= 20 -> YellowMain.copy(0.15f)
                        else                     -> RedSOS.copy(0.15f)
                    },
                    border = BorderStroke(
                        1.dp, when {
                            state.batteryLevel >= 50 -> GreenAcc.copy(0.5f)
                            state.batteryLevel >= 20 -> YellowMain.copy(0.5f)
                            else                     -> RedSOS.copy(0.5f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = 12.dp, vertical = 7.dp
                        ),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            if (state.batteryCharging) Icons.Filled.BatteryChargingFull
                            else when {
                                state.batteryLevel >= 80 -> Icons.Filled.BatteryFull
                                state.batteryLevel >= 50 -> Icons.Filled.Battery5Bar
                                state.batteryLevel >= 20 -> Icons.Filled.Battery3Bar
                                else                     -> Icons.Filled.Battery1Bar
                            },
                            contentDescription = null,
                            tint = when {
                                state.batteryLevel >= 50 -> GreenAcc
                                state.batteryLevel >= 20 -> YellowMain
                                else                     -> RedSOS
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "${state.batteryLevel}%",
                            color = when {
                                state.batteryLevel >= 50 -> GreenAcc
                                state.batteryLevel >= 20 -> YellowMain
                                else                     -> RedSOS
                            },
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── MEGA TORCH BUTTON ──
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(300.dp)
            ) {
                // Outer glow
                if (state.isOn) {
                    Box(
                        modifier = Modifier
                            .size(295.dp)
                            .scale(ringScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        YellowMain.copy(glowAlpha * 0.3f),
                                        OrangeAcc.copy(glowAlpha * 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                            .blur(22.dp)
                    )
                }

                // Rotating sweep ring
                Box(
                    modifier = Modifier
                        .size(256.dp)
                        .graphicsLayer {
                            rotationZ = if (state.isOn) rotAnim else 0f
                        }
                        .clip(CircleShape)
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        if (state.isOn) YellowMain.copy(0.9f)
                                        else Color.White.copy(0.08f),
                                        Color.Transparent,
                                        if (state.isOn) OrangeAcc.copy(0.7f)
                                        else Color.White.copy(0.04f),
                                        Color.Transparent
                                    )
                                )
                            ),
                            CircleShape
                        )
                )

                // Inner dark ring
                Box(
                    modifier = Modifier
                        .size(226.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                if (state.isOn)
                                    listOf(Color(0xFF2A1E00), Color(0xFF110C00))
                                else listOf(Color(0xFF131320), Color(0xFF08080F))
                            )
                        )
                        .border(
                            1.5.dp,
                            if (state.isOn) YellowMain.copy(0.25f)
                            else Color.White.copy(0.07f),
                            CircleShape
                        )
                )

                // Main button
                Box(
                    modifier = Modifier
                        .size(176.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.isOn)
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFFFFAA),
                                        YellowGlow,
                                        YellowMain,
                                        OrangeAcc,
                                        Color(0xFFAA4400)
                                    )
                                )
                            else Brush.radialGradient(
                                listOf(
                                    Color(0xFF242436),
                                    Color(0xFF14141F),
                                    Color(0xFF09090F)
                                )
                            )
                        )
                        .border(
                            2.dp,
                            if (state.isOn) Color.White.copy(0.35f)
                            else Color.White.copy(0.05f),
                            CircleShape
                        )
                        .clickable { vm.toggleFlashlight() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (state.isOn) Color(0xFF1A0E00)
                            else Color.White.copy(0.45f),
                            modifier = Modifier.size(62.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (state.isOn) "ON" else "OFF",
                            color = if (state.isOn) Color(0xFF1A0E00)
                            else Color.White.copy(0.25f),
                            fontSize      = 12.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = 6.sp
                        )
                    }
                }

                // Shine dot
                if (state.isOn) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .offset(x = (-48).dp, y = (-48).dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.55f))
                            .blur(4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Mode pill
            AnimatedContent(
                targetState  = state.mode,
                transitionSpec = {
                    fadeIn(tween(300)) + slideInVertically { -24 } togetherWith
                            fadeOut(tween(200))
                },
                label = "modePill"
            ) { mode ->
                val (modeColor, modeLabel) = when (mode) {
                    FlashMode.ON        -> Pair(YellowMain,          "⚡  TORCH  ON")
                    FlashMode.STROBE    -> Pair(BlueAcc,             "💙  STROBE  ACTIVE")
                    FlashMode.SOS       -> Pair(RedSOS,              "🆘  SOS  ACTIVE")
                    FlashMode.MORSE     -> Pair(PurpleAcc,           "📡  MORSE  PLAYING")
                    FlashMode.SOUND     -> Pair(GreenAcc,            "🎵  SOUND  SYNC  ON")
                    FlashMode.BREATHING -> Pair(Color(0xFF00CED1),   "🌊  BREATHING  ACTIVE")
                    FlashMode.OFF       -> Pair(Color.White.copy(0.25f), "TAP  TO  TURN  ON")
                }
                Surface(
                    shape  = RoundedCornerShape(20.dp),
                    color  = modeColor.copy(0.1f),
                    border = BorderStroke(1.dp, modeColor.copy(0.3f))
                ) {
                    Text(
                        modeLabel,
                        modifier      = Modifier.padding(
                            horizontal = 20.dp, vertical = 10.dp
                        ),
                        color         = modeColor,
                        fontWeight    = FontWeight.ExtraBold,
                        fontSize      = 12.sp,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── SECTION LABEL ──
            Text(
                "FEATURES",
                color         = Color.White.copy(0.28f),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, bottom = 14.dp)
            )

            // ── FEATURE CARDS — Row 1 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Filled.Bolt,
                    label    = "Strobe",
                    sublabel = "Adjustable speed",
                    color    = BlueAcc,
                    isActive = selectedTab == 0,
                    onClick  = { selectedTab = 0 }
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Filled.Warning,
                    label    = "SOS",
                    sublabel = "Emergency signal",
                    color    = RedSOS,
                    isActive = selectedTab == 1,
                    onClick  = { selectedTab = 1 }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── FEATURE CARDS — Row 2 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Filled.LightMode,
                    label    = "Screen Light",
                    sublabel = "8 color modes",
                    color    = YellowMain,
                    isActive = selectedTab == 2,
                    onClick  = { selectedTab = 2 }
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Filled.Code,
                    label    = "Morse Code",
                    sublabel = "Type & flash",
                    color    = PurpleAcc,
                    isActive = selectedTab == 3,
                    onClick  = { selectedTab = 3 }
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── FEATURE CARDS — Row 3 (X-Factor) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Filled.GraphicEq,
                    label    = "Sound Sync",
                    sublabel = "Flash to music",
                    color    = GreenAcc,
                    isActive = selectedTab == 4,
                    onClick  = { selectedTab = 4 }
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Filled.Air,
                    label    = "Breathing",
                    sublabel = "Calm & focus",
                    color    = Color(0xFF00CED1),
                    isActive = selectedTab == 5,
                    onClick  = { selectedTab = 5 }
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── ACTIVE PANEL ──
            AnimatedContent(
                targetState  = selectedTab,
                transitionSpec = {
                    fadeIn(tween(280)) + slideInHorizontally { 60 } togetherWith
                            fadeOut(tween(200)) + slideOutHorizontally { -60 }
                },
                label = "panel"
            ) { tab ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    when (tab) {
                        0 -> StrobePanel(
                            speed    = state.strobeSpeed,
                            isActive = state.mode == FlashMode.STROBE,
                            onStart  = { vm.startStrobe() },
                            onStop   = { vm.turnOff() },
                            onSpeed  = { vm.setStrobeSpeed(it) }
                        )
                        1 -> SosPanel(
                            isActive = state.mode == FlashMode.SOS,
                            onStart  = { vm.startSOS() },
                            onStop   = { vm.turnOff() }
                        )
                        2 -> ScreenLightPanel { color ->
                            vm.setScreenColor(color.toArgb().toLong())
                            vm.toggleScreenLight()
                        }
                        3 -> MorsePanel(
                            text          = state.morseText,
                            isPlaying     = state.isMorsePlaying,
                            onTextChange  = { vm.setMorseText(it) },
                            onPlay        = { vm.playMorse(state.morseText) },
                            onStop        = { vm.stopMorse() }
                        )
                        4 -> SoundSyncPanel(
                            isActive = state.mode == FlashMode.SOUND,
                            onStart  = @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO) { vm.startSoundReactive() },
                            onStop   = { vm.stopSoundReactive() }
                        )
                        5 -> BreathingPanel(
                            isActive       = state.mode == FlashMode.BREATHING,
                            breathPhase    = state.breathPhase,
                            breathProgress = state.breathProgress,
                            onStart        = { vm.startBreathing() },
                            onStop         = { vm.stopBreathing() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            BannerAd()
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ── FEATURE CARD ──
@Composable
fun FeatureCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    sublabel: String,
    color: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .wrapContentHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActive)
                    Brush.linearGradient(
                        listOf(color.copy(0.25f), color.copy(0.1f))
                    )
                else Brush.linearGradient(
                    listOf(Color(0xFF1A1A28), Color(0xFF111120))
                )
            )
            .border(
                1.5.dp,
                if (isActive) color.copy(0.75f) else Color.White.copy(0.07f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 20.dp, y = (-20).dp)
                    .clip(CircleShape)
                    .background(color.copy(0.3f))
                    .blur(18.dp)
            )
        }
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(if (isActive) 0.25f else 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(
                    label,
                    color      = if (isActive) Color.White else Color.White.copy(0.7f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 14.sp,
                    lineHeight = 16.sp
                )
                Text(
                    sublabel,
                    color      = Color.White.copy(0.35f),
                    fontSize   = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

// ── STROBE PANEL ──
@Composable
fun StrobePanel(
    speed: Float,
    isActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSpeed: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0A1530), Color(0xFF0D1020))
                )
            )
            .border(1.dp, BlueAcc.copy(0.2f), RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Strobe Light", color = Color.White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("Adjustable flash speed",
                    color = Color.White.copy(0.4f), fontSize = 12.sp)
            }
            if (isActive) {
                Surface(
                    shape  = RoundedCornerShape(20.dp),
                    color  = BlueAcc.copy(0.2f),
                    border = BorderStroke(1.dp, BlueAcc.copy(0.5f))
                ) {
                    Text(
                        "LIVE",
                        modifier      = Modifier.padding(
                            horizontal = 10.dp, vertical = 5.dp
                        ),
                        color         = BlueAcc,
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Speed preset buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                Pair("SLOW",  0.15f),
                Pair("MED",   0.5f),
                Pair("FAST",  0.8f),
                Pair("ULTRA", 1.0f)
            ).forEach { (label, value) ->
                val isSelected = kotlin.math.abs(speed - value) < 0.2f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { onSpeed(value) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) BlueAcc.copy(0.25f)
                                else Color.White.copy(0.05f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) BlueAcc.copy(0.8f)
                                else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color         = if (isSelected) BlueAcc
                            else Color.White.copy(0.3f),
                            fontSize      = 9.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Precise slider
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Precise", color = Color.White.copy(0.4f), fontSize = 11.sp)
                Text(
                    "${(1000 - speed * 950).toInt()}ms",
                    color = BlueAcc, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Slider(
                value         = speed,
                onValueChange = onSpeed,
                modifier      = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor        = BlueAcc,
                    activeTrackColor  = BlueAcc,
                    inactiveTrackColor = BlueAcc.copy(0.2f)
                )
            )
        }

        Button(
            onClick  = if (isActive) onStop else onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isActive) RedSOS else BlueAcc
            )
        ) {
            Icon(
                if (isActive) Icons.Filled.Stop else Icons.Filled.Bolt,
                null, modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (isActive) "Stop Strobe" else "Start Strobe",
                fontWeight = FontWeight.ExtraBold, fontSize = 16.sp
            )
        }
    }
}

// ── SOS PANEL ──
@Composable
fun SosPanel(
    isActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "sos")
    val sosAlpha by inf.animateFloat(
        initialValue  = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "sa"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1A0505), Color(0xFF100305))
                )
            )
            .border(1.dp, RedSOS.copy(0.25f), RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement   = Arrangement.spacedBy(18.dp),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("SOS Signal", color = Color.White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("International distress morse",
                    color = Color.White.copy(0.4f), fontSize = 12.sp)
            }
            Icon(
                Icons.Filled.Warning, null,
                tint = if (isActive) RedSOS.copy(sosAlpha) else RedSOS.copy(0.6f),
                modifier = Modifier.size(32.dp)
            )
        }

        // Visual morse dots/dashes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(0.3f))
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    Pair("S", listOf(true, false, true, false, true)),
                    Pair("O", listOf(false, false, false)),
                    Pair("S", listOf(true, false, true, false, true))
                ).forEach { (letter, pattern) ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            letter, color = RedSOS,
                            fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                            modifier = Modifier.width(20.dp)
                        )
                        pattern.forEach { isDot ->
                            Box(
                                modifier = Modifier
                                    .width(if (isDot) 14.dp else 36.dp)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isActive) RedSOS.copy(sosAlpha)
                                        else RedSOS.copy(0.5f)
                                    )
                            )
                        }
                    }
                }
            }
        }

        Text(
            "Flashes · · · — — — · · · continuously\nUse in emergencies to signal for help",
            color     = Color.White.copy(0.4f),
            fontSize  = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Button(
            onClick  = if (isActive) onStop else onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isActive) Color(0xFF3A0A0A) else RedSOS
            ),
            border = if (isActive) BorderStroke(2.dp, RedSOS.copy(0.7f)) else null
        ) {
            Icon(
                if (isActive) Icons.Filled.Stop else Icons.Filled.Warning,
                null, modifier = Modifier.size(22.dp),
                tint = if (isActive) RedSOS else Color.White
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (isActive) "Stop SOS" else "ACTIVATE SOS",
                fontWeight    = FontWeight.ExtraBold,
                fontSize      = 16.sp,
                color         = if (isActive) RedSOS else Color.White,
                letterSpacing = if (!isActive) 2.sp else 0.sp
            )
        }
    }
}

// ── SCREEN LIGHT PANEL ──
@Composable
fun ScreenLightPanel(onColor: (Color) -> Unit) {
    val screenColors = listOf(
        Triple("White",  Color(0xFFFFFFFF), "Pure"),
        Triple("Warm",   Color(0xFFFFE4A0), "Warm"),
        Triple("Yellow", Color(0xFFFFFF44), "Neon"),
        Triple("Red",    Color(0xFFFF2222), "Red"),
        Triple("Green",  Color(0xFF22FF44), "Green"),
        Triple("Blue",   Color(0xFF2244FF), "Blue"),
        Triple("Purple", Color(0xFFCC44FF), "Purple"),
        Triple("Pink",   Color(0xFFFF44AA), "Pink")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1A1500), Color(0xFF100F05))
                )
            )
            .border(1.dp, YellowMain.copy(0.2f), RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Screen Flashlight", color = Color.White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("Turn screen into colored light",
                    color = Color.White.copy(0.4f), fontSize = 12.sp)
            }
            Icon(Icons.Filled.LightMode, null,
                tint = YellowMain, modifier = Modifier.size(28.dp))
        }

        Text("Select color:", color = Color.White.copy(0.4f), fontSize = 12.sp)

        // 4x2 color grid
        screenColors.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (_, color, label) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onColor(color) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(color)
                                .border(
                                    2.dp,
                                    Color.White.copy(0.15f),
                                    RoundedCornerShape(16.dp)
                                )
                        )
                        Text(
                            label,
                            color      = Color.White.copy(0.5f),
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Text(
            "Tap a color — screen fills instantly. Great as a signal or reading light.",
            color     = Color.White.copy(0.3f),
            fontSize  = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

// ── MORSE PANEL ──
@Composable
fun MorsePanel(
    text: String,
    isPlaying: Boolean,
    onTextChange: (String) -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF120A1A), Color(0xFF0D0812))
                )
            )
            .border(1.dp, PurpleAcc.copy(0.2f), RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Morse Code", color = Color.White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("Type any message — torch spells it",
                    color = Color.White.copy(0.4f), fontSize = 12.sp)
            }
            if (isPlaying) {
                Surface(
                    shape  = RoundedCornerShape(20.dp),
                    color  = PurpleAcc.copy(0.2f),
                    border = BorderStroke(1.dp, PurpleAcc.copy(0.5f))
                ) {
                    Text(
                        "FLASHING",
                        modifier      = Modifier.padding(
                            horizontal = 10.dp, vertical = 5.dp
                        ),
                        color         = PurpleAcc,
                        fontSize      = 10.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        OutlinedTextField(
            value         = text,
            onValueChange = onTextChange,
            modifier      = Modifier.fillMaxWidth(),
            placeholder   = {
                Text("Type message here...", color = Color.White.copy(0.25f))
            },
            label    = { Text("Message") },
            maxLines = 2,
            colors   = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = PurpleAcc,
                unfocusedBorderColor    = Color.White.copy(0.15f),
                focusedTextColor        = Color.White,
                unfocusedTextColor      = Color.White,
                focusedLabelColor       = PurpleAcc,
                unfocusedLabelColor     = Color.White.copy(0.3f),
                cursorColor             = PurpleAcc,
                focusedContainerColor   = Color.White.copy(0.04f),
                unfocusedContainerColor = Color.White.copy(0.02f)
            ),
            shape = RoundedCornerShape(14.dp)
        )

        // Quick phrases
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("HELP", "SOS", "HELLO", "YES", "NO", "OK", "DANGER").forEach { phrase ->
                Surface(
                    onClick = { onTextChange(phrase) },
                    shape   = RoundedCornerShape(20.dp),
                    color   = if (text == phrase) PurpleAcc.copy(0.25f)
                    else PurpleAcc.copy(0.08f),
                    border  = BorderStroke(
                        1.dp,
                        if (text == phrase) PurpleAcc.copy(0.9f)
                        else PurpleAcc.copy(0.2f)
                    )
                ) {
                    Text(
                        phrase,
                        modifier   = Modifier.padding(
                            horizontal = 14.dp, vertical = 8.dp
                        ),
                        color      = PurpleAcc,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (isPlaying) {
            Button(
                onClick  = onStop,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = RedSOS)
            ) {
                Icon(Icons.Filled.Stop, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Stop Flashing", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        } else {
            Button(
                onClick  = onPlay,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled  = text.isNotBlank(),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = PurpleAcc)
            ) {
                Icon(Icons.Filled.FlashOn, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Flash in Morse Code", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

// ── SOUND SYNC PANEL ──
@Composable
fun SoundSyncPanel(
    isActive: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val micPerm = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) onStart() }

    val inf = rememberInfiniteTransition(label = "eq")
    val barAnim by inf.animateFloat(
        initialValue  = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(300, easing = EaseInOut), RepeatMode.Reverse
        ), label = "bar"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF051A0A), Color(0xFF030D06))
                )
            )
            .border(1.dp, GreenAcc.copy(0.25f), RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement   = Arrangement.spacedBy(18.dp),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Sound Sync", color = Color.White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("Flash reacts to music & sound",
                    color = Color.White.copy(0.4f), fontSize = 12.sp)
            }
            if (isActive) {
                Surface(
                    shape  = RoundedCornerShape(20.dp),
                    color  = GreenAcc.copy(0.2f),
                    border = BorderStroke(1.dp, GreenAcc.copy(0.5f))
                ) {
                    Text(
                        "LIVE",
                        modifier      = Modifier.padding(
                            horizontal = 10.dp, vertical = 5.dp
                        ),
                        color         = GreenAcc,
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Equalizer bars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(0.3f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.Bottom
            ) {
                val heights = listOf(0.4f, 0.75f, 0.5f, 1f, 0.6f, 0.9f, 0.45f, 0.8f, 0.55f, 0.7f)
                heights.forEachIndexed { i, h ->
                    val animH = if (isActive)
                        h * barAnim * (0.5f + (i % 3) * 0.25f)
                    else h * 0.25f
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .fillMaxHeight(animH.coerceIn(0.08f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(GreenAcc, GreenAcc.copy(0.25f))
                                )
                            )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(0.04f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listOf(
                Pair("🎵", "Play music on your phone or near a speaker"),
                Pair("🎙️", "Microphone detects the beat in real-time"),
                Pair("⚡", "Flashlight fires with every beat — instant sync!")
            ).forEach { (emoji, text) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(emoji, fontSize = 18.sp)
                    Text(text, color = Color.White.copy(0.6f),
                        fontSize = 13.sp, lineHeight = 17.sp)
                }
            }
        }

        Button(
            onClick  = {
                if (isActive) onStop()
                else micPerm.launch(Manifest.permission.RECORD_AUDIO)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isActive) RedSOS else GreenAcc
            )
        ) {
            Icon(
                if (isActive) Icons.Filled.Stop else Icons.Filled.GraphicEq,
                null, modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (isActive) "Stop Sound Sync" else "Start Sound Sync",
                fontWeight = FontWeight.ExtraBold, fontSize = 16.sp
            )
        }
    }
}

// ── BREATHING PANEL ──
@Composable
fun BreathingPanel(
    isActive: Boolean,
    breathPhase: String,
    breathProgress: Float,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val tealColor = Color(0xFF00CED1)
    val inf = rememberInfiniteTransition(label = "breath")
    val circleScale by inf.animateFloat(
        initialValue  = 0.82f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            tween(4000, easing = EaseInOut), RepeatMode.Reverse
        ), label = "cs"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF001A1A), Color(0xFF000D10))
                )
            )
            .border(1.dp, tealColor.copy(0.25f), RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement   = Arrangement.spacedBy(18.dp),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Breathing Guide", color = Color.White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("Calm & focus with light patterns",
                    color = Color.White.copy(0.4f), fontSize = 12.sp)
            }
            Icon(Icons.Filled.Air, null,
                tint = tealColor, modifier = Modifier.size(28.dp))
        }

        // Breathing circle
        Box(
            modifier         = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .scale(if (isActive) circleScale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(tealColor.copy(0.15f), Color.Transparent)
                        )
                    )
                    .blur(14.dp)
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(
                        if (isActive) 0.7f + breathProgress * 0.5f else 1f
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(tealColor.copy(0.4f), tealColor.copy(0.1f))
                        )
                    )
                    .border(2.dp, tealColor.copy(0.6f), CircleShape)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (isActive && breathPhase.isNotBlank()) breathPhase else "Ready",
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 16.sp
                )
                if (isActive) {
                    Text(
                        when (breathPhase) {
                            "Inhale" -> "breathe in..."
                            "Hold"   -> "hold..."
                            "Exhale" -> "breathe out..."
                            "Rest"   -> "relax..."
                            else     -> ""
                        },
                        color    = Color.White.copy(0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Pattern presets
        Text(
            "Breathing pattern",
            color    = Color.White.copy(0.4f),
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("4-4-4", "Box\nBreathing",  {}),
                Triple("4-7-8", "Calm &\nSleep",   {}),
                Triple("5-0-5", "Simple\nRelax",   {})
            ).forEach { (label, name, _) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (!isActive) tealColor.copy(0.1f)
                            else Color.White.copy(0.04f)
                        )
                        .border(
                            1.dp,
                            if (!isActive) tealColor.copy(0.3f)
                            else Color.White.copy(0.07f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { if (!isActive) onStart() }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        label,
                        color      = tealColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 15.sp
                    )
                    Text(
                        name,
                        color     = Color.White.copy(0.5f),
                        fontSize  = 10.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 13.sp
                    )
                }
            }
        }

        Button(
            onClick  = if (isActive) onStop else onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isActive) RedSOS else tealColor
            )
        ) {
            Icon(
                if (isActive) Icons.Filled.Stop else Icons.Filled.Air,
                null, modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                if (isActive) "Stop Breathing" else "Start Breathing Guide",
                fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
            )
        }
    }
}