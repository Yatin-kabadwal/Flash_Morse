package com.flashlight.torch.ui.screens

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flashlight.torch.ads.AdManager
import com.flashlight.torch.ui.theme.*
import com.flashlight.torch.utils.BannerAd
import com.flashlight.torch.viewmodel.FlashMode
import com.flashlight.torch.viewmodel.FlashlightViewModel

@Composable
fun MainScreen(
    vm: FlashlightViewModel = viewModel()   // ← no Activity param — fixes composable error
) {
    val state   by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current          // ← get context here inside composable
    val activity = context as? Activity         // ← cast to Activity safely

    var selectedTab     by remember { mutableStateOf(0) }
    var modeChangeCount by remember { mutableStateOf(0) }

    // Live battery monitoring
    DisposableEffect(Unit) {
        vm.startBatteryMonitoring()
        onDispose { vm.stopBatteryMonitoring() }
    }

    // Show interstitial on every 3rd mode switch
    fun onModeSwitch(newTab: Int) {
        if (newTab == selectedTab) return
        selectedTab     = newTab
        modeChangeCount++
        if (modeChangeCount % 3 == 0) {
            activity?.let { AdManager.show(it) }
        }
    }

    // Show interstitial when torch turned off after 60s of use
    var torchOnTime by remember { mutableStateOf(0L) }
    LaunchedEffect(state.isOn) {
        if (state.isOn) {
            torchOnTime = System.currentTimeMillis()
        } else if (torchOnTime > 0L) {
            val usedMs = System.currentTimeMillis() - torchOnTime
            if (usedMs >= 60_000L) {
                activity?.let { AdManager.show(it) }
            }
            torchOnTime = 0L
        }
    }

    // ── Animations ──
    val infiniteAnim = rememberInfiniteTransition(label = "main")
    val glowAlpha by infiniteAnim.animateFloat(
        initialValue  = 0.35f, targetValue = 0.85f,
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
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.TouchApp, null,
                        tint     = Color.Black.copy(0.3f),
                        modifier = Modifier.size(32.dp))
                    Text("Tap to exit",
                        color    = Color.Black.copy(0.3f),
                        fontSize = 12.sp)
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
        // Ambient blobs
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
                    Text("FLASHLIGHT",
                        color         = Color.White,
                        fontSize      = 22.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 4.sp)
                    Text("Pro · Strobe · Morse · Sound",
                        color         = Color.White.copy(0.35f),
                        fontSize      = 11.sp,
                        letterSpacing = 1.sp)
                }

                // Live Battery Pill
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
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
                        Text("${state.batteryLevel}%",
                            color = when {
                                state.batteryLevel >= 50 -> GreenAcc
                                state.batteryLevel >= 20 -> YellowMain
                                else                     -> RedSOS
                            },
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── MEGA TORCH BUTTON ──
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(300.dp)
            ) {
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

                Box(
                    modifier = Modifier
                        .size(256.dp)
                        .graphicsLayer { rotationZ = if (state.isOn) rotAnim else 0f }
                        .clip(CircleShape)
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.sweepGradient(
                                    listOf(
                                        if (state.isOn) YellowMain.copy(0.9f)
                                        else Color.White.copy(0.08f),
                                        Color.Transparent,
                                        if (state.isOn) OrangeAcc.copy(0.6f)
                                        else Color.White.copy(0.05f),
                                        Color.Transparent
                                    )
                                )
                            ), CircleShape
                        )
                )

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

                Box(
                    modifier = Modifier
                        .size(176.dp)
                        .clip(CircleShape)
                        .background(
                            if (state.isOn)
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFFFFAA), YellowGlow,
                                        YellowMain, OrangeAcc, Color(0xFFAA4400)
                                    )
                                )
                            else Brush.radialGradient(
                                listOf(
                                    Color(0xFF242436),
                                    Color(0xFF15151F),
                                    Color(0xFF0A0A14)
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
                            tint     = if (state.isOn) Color(0xFF1A0E00)
                            else Color.White.copy(0.45f),
                            modifier = Modifier.size(62.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (state.isOn) "ON" else "OFF",
                            color         = if (state.isOn) Color(0xFF1A0E00)
                            else Color.White.copy(0.25f),
                            fontSize      = 12.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            letterSpacing = 6.sp
                        )
                    }
                }

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

            // Mode Pill
            AnimatedContent(
                targetState  = state.mode,
                transitionSpec = {
                    fadeIn(tween(300)) + slideInVertically { -24 } togetherWith
                            fadeOut(tween(200))
                },
                label = "modePill"
            ) { mode ->
                val (modeColor, modeLabel) = when (mode) {
                    FlashMode.ON        -> Pair(YellowMain,        "⚡  TORCH  ON")
                    FlashMode.STROBE    -> Pair(BlueAcc,           "💙  STROBE  ACTIVE")
                    FlashMode.SOS       -> Pair(RedSOS,            "🆘  SOS  ACTIVE")
                    FlashMode.MORSE     -> Pair(PurpleAcc,         "📡  MORSE  PLAYING")
                    FlashMode.SOUND     -> Pair(GreenAcc,          "🎵  SOUND  SYNC  ON")
                    FlashMode.BREATHING -> Pair(Color(0xFF00CED1), "🌊  BREATHING  ACTIVE")
                    FlashMode.OFF       -> Pair(Color.White.copy(0.25f), "TAP  TO  TURN  ON")
                }
                Surface(
                    shape  = RoundedCornerShape(20.dp),
                    color  = modeColor.copy(0.1f),
                    border = BorderStroke(1.dp, modeColor.copy(0.3f))
                ) {
                    Text(modeLabel,
                        modifier      = Modifier.padding(
                            horizontal = 20.dp, vertical = 10.dp
                        ),
                        color         = modeColor,
                        fontWeight    = FontWeight.ExtraBold,
                        fontSize      = 12.sp,
                        letterSpacing = 2.sp)
                }
            }

            Spacer(Modifier.height(28.dp))

            Text("FEATURES",
                color         = Color.White.copy(0.28f),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, bottom = 14.dp))

            // Feature Cards Row 1
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
                    onClick  = { onModeSwitch(0) }
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Filled.Warning,
                    label    = "SOS",
                    sublabel = "Emergency signal",
                    color    = RedSOS,
                    isActive = selectedTab == 1,
                    onClick  = { onModeSwitch(1) }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Feature Cards Row 2
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
                    onClick  = { onModeSwitch(2) }
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Filled.Code,
                    label    = "Morse Code",
                    sublabel = "Type & flash",
                    color    = PurpleAcc,
                    isActive = selectedTab == 3,
                    onClick  = { onModeSwitch(3) }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Feature Cards Row 3
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
                    onClick  = { onModeSwitch(4) }
                )
                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon     = Icons.Filled.Air,
                    label    = "Breathing",
                    sublabel = "Calm & focus",
                    color    = Color(0xFF00CED1),
                    isActive = selectedTab == 5,
                    onClick  = { onModeSwitch(5) }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Active Panel
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
                            text         = state.morseText,
                            isPlaying    = state.isMorsePlaying,
                            onTextChange = { vm.setMorseText(it) },
                            onPlay       = { vm.playMorse(state.morseText) },
                            onStop       = { vm.stopMorse() }
                        )
                        4 -> SoundSyncPanel(
                            isActive = state.mode == FlashMode.SOUND,
                            onStart  = { vm.startSoundReactive() },
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

// ── Feature Card ──
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
                    Brush.linearGradient(listOf(color.copy(0.25f), color.copy(0.1f)))
                else Brush.linearGradient(listOf(Color(0xFF1A1A28), Color(0xFF111120)))
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
                Text(label,
                    color      = if (isActive) Color.White else Color.White.copy(0.7f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize   = 14.sp, lineHeight = 16.sp)
                Text(sublabel,
                    color      = Color.White.copy(0.35f),
                    fontSize   = 11.sp, lineHeight = 14.sp)
            }
        }
    }
}

// ── Strobe Panel ──
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
            .background(Brush.linearGradient(listOf(Color(0xFF0A1530), Color(0xFF0D1020))))
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
                    Text("LIVE",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = BlueAcc, fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(Pair("SLOW", 0.15f), Pair("MED", 0.5f),
                Pair("FAST", 0.8f), Pair("ULTRA", 1.0f)).forEach { (label, value) ->
                val isSel = kotlin.math.abs(speed - value) < 0.2f
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) BlueAcc.copy(0.25f) else Color.White.copy(0.05f))
                        .border(1.dp, if (isSel) BlueAcc.copy(0.8f) else Color.Transparent,
                            RoundedCornerShape(12.dp))
                        .clickable { onSpeed(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label,
                        color         = if (isSel) BlueAcc else Color.White.copy(0.3f),
                        fontSize      = 9.sp,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 1.sp)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Precise", color = Color.White.copy(0.4f), fontSize = 11.sp)
                Text("${(1000 - speed * 950).toInt()}ms",
                    color = BlueAcc, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Slider(value = speed, onValueChange = onSpeed,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor         = BlueAcc,
                    activeTrackColor   = BlueAcc,
                    inactiveTrackColor = BlueAcc.copy(0.2f)))
        }
        Button(
            onClick  = if (isActive) onStop else onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isActive) RedSOS else BlueAcc)
        ) {
            Icon(if (isActive) Icons.Filled.Stop else Icons.Filled.Bolt,
                null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(if (isActive) "Stop Strobe" else "Start Strobe",
                fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

// ── SOS Panel ──
@Composable
fun SosPanel(isActive: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "sos")
    val sosAlpha by inf.animateFloat(
        initialValue  = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "sa"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1A0505), Color(0xFF100305))))
            .border(1.dp, RedSOS.copy(0.25f), RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement   = Arrangement.spacedBy(18.dp),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically) {
            Column {
                Text("SOS Signal", color = Color.White,
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("International distress morse",
                    color = Color.White.copy(0.4f), fontSize = 12.sp)
            }
            Icon(Icons.Filled.Warning, null,
                tint     = if (isActive) RedSOS.copy(sosAlpha) else RedSOS.copy(0.6f),
                modifier = Modifier.size(32.dp))
        }
        Button(
            onClick  = if (isActive) onStop else onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isActive) Color(0xFF3A0A0A) else RedSOS),
            border = if (isActive) BorderStroke(2.dp, RedSOS.copy(0.7f)) else null
        ) {
            Icon(if (isActive) Icons.Filled.Stop else Icons.Filled.Warning,
                null, modifier = Modifier.size(22.dp),
                tint = if (isActive) RedSOS else Color.White)
            Spacer(Modifier.width(10.dp))
            Text(if (isActive) "Stop SOS" else "ACTIVATE SOS",
                fontWeight    = FontWeight.ExtraBold, fontSize = 16.sp,
                color         = if (isActive) RedSOS else Color.White,
                letterSpacing = if (!isActive) 2.sp else 0.sp)
        }
    }
}

// ── Screen Light Panel ──
@Composable
fun ScreenLightPanel(onColor: (Color) -> Unit) {
    val cols = listOf(
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
            .background(Brush.linearGradient(listOf(Color(0xFF1A1500), Color(0xFF100F05))))
            .border(1.dp, YellowMain.copy(0.2f), RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Screen Flashlight", color = Color.White,
            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        cols.chunked(4).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (_, color, label) ->
                    Column(
                        modifier = Modifier.weight(1f).clickable { onColor(color) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp)).background(color)
                            .border(2.dp, Color.White.copy(0.15f), RoundedCornerShape(16.dp)))
                        Text(label, color = Color.White.copy(0.5f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ── Morse Panel ──
@Composable
fun MorsePanel(
    text: String, isPlaying: Boolean,
    onTextChange: (String) -> Unit, onPlay: () -> Unit, onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF120A1A), Color(0xFF0D0812))))
            .border(1.dp, PurpleAcc.copy(0.2f), RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Morse Code", color = Color.White,
            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        OutlinedTextField(
            value = text, onValueChange = onTextChange,
            modifier    = Modifier.fillMaxWidth(),
            placeholder = { Text("Type message...", color = Color.White.copy(0.25f)) },
            maxLines    = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = PurpleAcc,
                unfocusedBorderColor    = Color.White.copy(0.15f),
                focusedTextColor        = Color.White,
                unfocusedTextColor      = Color.White,
                cursorColor             = PurpleAcc,
                focusedContainerColor   = Color.White.copy(0.04f),
                unfocusedContainerColor = Color.White.copy(0.02f)),
            shape = RoundedCornerShape(14.dp)
        )
        if (isPlaying) {
            Button(onClick = onStop, modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedSOS)) {
                Icon(Icons.Filled.Stop, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Stop Flashing", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        } else {
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = text.isNotBlank(), shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAcc)) {
                Icon(Icons.Filled.FlashOn, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text("Flash in Morse", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

// ── Sound Sync Panel ──
@Composable
fun SoundSyncPanel(isActive: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val micPerm = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) onStart() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF051A0A), Color(0xFF030D06))))
            .border(1.dp, GreenAcc.copy(0.25f), RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Sound Sync", color = Color.White,
            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text("Flash reacts to music & sound",
            color = Color.White.copy(0.4f), fontSize = 12.sp)
        Button(
            onClick  = { if (isActive) onStop() else micPerm.launch(android.Manifest.permission.RECORD_AUDIO) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isActive) RedSOS else GreenAcc)
        ) {
            Icon(if (isActive) Icons.Filled.Stop else Icons.Filled.GraphicEq,
                null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(if (isActive) "Stop Sound Sync" else "Start Sound Sync",
                fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

// ── Breathing Panel ──
@Composable
fun BreathingPanel(
    isActive: Boolean, breathPhase: String, breathProgress: Float,
    onStart: () -> Unit, onStop: () -> Unit
) {
    val teal = Color(0xFF00CED1)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF001A1A), Color(0xFF000D10))))
            .border(1.dp, teal.copy(0.25f), RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement   = Arrangement.spacedBy(18.dp),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Text("Breathing Guide", color = Color.White,
            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text("Calm & focus with light patterns",
            color = Color.White.copy(0.4f), fontSize = 12.sp)
        Button(
            onClick  = if (isActive) onStop else onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = if (isActive) RedSOS else teal)
        ) {
            Icon(if (isActive) Icons.Filled.Stop else Icons.Filled.Air,
                null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(if (isActive) "Stop Breathing" else "Start Breathing Guide",
                fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
    }
}