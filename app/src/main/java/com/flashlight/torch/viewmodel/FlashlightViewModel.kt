package com.flashlight.torch.viewmodel

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.BatteryManager
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class FlashMode {
    OFF, ON, STROBE, SOS, MORSE, SOUND, BREATHING
}

data class FlashlightState(
    val isOn: Boolean = false,
    val mode: FlashMode = FlashMode.OFF,
    val strobeSpeed: Float = 0.5f,
    val batteryLevel: Int = 0,
    val batteryCharging: Boolean = false,
    val hasFlash: Boolean = false,
    val screenLightOn: Boolean = false,
    val screenLightColor: Long = 0xFFFFFFFF,
    val morseText: String = "",
    val isMorsePlaying: Boolean = false,
    val breathPhase: String = "",
    val breathProgress: Float = 0f,
    val errorMessage: String? = null
)

class FlashlightViewModel(application: Application) : AndroidViewModel(application) {

    private val context       = application.applicationContext
    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null

    private val _state = MutableStateFlow(FlashlightState())
    val state: StateFlow<FlashlightState> = _state

    private var strobeJob: Job? = null
    private var morseJob:  Job? = null
    private var soundJob:  Job? = null
    private var audioRecord: AudioRecord? = null

    init {
        val hasFlash = context.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        cameraId = try {
            cameraManager.cameraIdList.firstOrNull()
        } catch (e: Exception) { null }
        updateBattery()
        _state.value = _state.value.copy(hasFlash = hasFlash)
    }

    // ────────────────────────────────────────────
    // MAIN TOGGLE
    // ────────────────────────────────────────────
    fun toggleFlashlight() {
        if (_state.value.isOn) turnOff() else turnOn()
    }

    fun turnOn() {
        stopAllJobs()
        setFlash(true)
        _state.value = _state.value.copy(isOn = true, mode = FlashMode.ON)
    }

    fun turnOff() {
        stopAllJobs()
        setFlash(false)
        _state.value = _state.value.copy(
            isOn          = false,
            mode          = FlashMode.OFF,
            isMorsePlaying = false,
            breathPhase   = "",
            breathProgress = 0f
        )
    }

    // ────────────────────────────────────────────
    // STROBE MODE
    // ────────────────────────────────────────────
    fun startStrobe() {
        stopAllJobs()
        _state.value = _state.value.copy(isOn = true, mode = FlashMode.STROBE)
        strobeJob = viewModelScope.launch {
            while (true) {
                val speed   = _state.value.strobeSpeed
                val delayMs = (1000f - (speed * 950f)).toLong().coerceAtLeast(50L)
                setFlash(true);  delay(delayMs / 2)
                setFlash(false); delay(delayMs / 2)
            }
        }
    }

    fun setStrobeSpeed(speed: Float) {
        _state.value = _state.value.copy(strobeSpeed = speed)
        if (_state.value.mode == FlashMode.STROBE) startStrobe()
    }

    // ────────────────────────────────────────────
    // SOS MODE
    // ────────────────────────────────────────────
    fun startSOS() {
        stopAllJobs()
        _state.value = _state.value.copy(isOn = true, mode = FlashMode.SOS)
        strobeJob = viewModelScope.launch {
            val dot       = 150L
            val dash      = 450L
            val gap       = 150L
            val letterGap = 400L
            val wordGap   = 800L
            while (true) {
                repeat(3) { flash(dot,  gap) }; delay(letterGap)
                repeat(3) { flash(dash, gap) }; delay(letterGap)
                repeat(3) { flash(dot,  gap) }; delay(wordGap)
            }
        }
    }

    // ────────────────────────────────────────────
    // MORSE CODE
    // ────────────────────────────────────────────
    fun playMorse(text: String) {
        if (text.isBlank()) return
        stopMorse()
        _state.value = _state.value.copy(isMorsePlaying = true, mode = FlashMode.MORSE)
        morseJob = viewModelScope.launch {
            val morse   = textToMorse(text.uppercase())
            val dot     = 120L
            val dash    = 360L
            val gap     = 120L
            val letterGap = 360L
            val wordGap   = 840L

            for (char in morse) {
                when (char) {
                    '.' -> flash(dot,  gap)
                    '-' -> flash(dash, gap)
                    ' ' -> delay(letterGap)
                    '/' -> delay(wordGap)
                }
            }
            setFlash(false)
            _state.value = _state.value.copy(
                isMorsePlaying = false,
                mode = FlashMode.OFF,
                isOn = false
            )
        }
    }

    fun stopMorse() {
        morseJob?.cancel(); morseJob = null
        setFlash(false)
        _state.value = _state.value.copy(isMorsePlaying = false)
    }

    fun setMorseText(text: String) {
        _state.value = _state.value.copy(morseText = text)
    }

    // ────────────────────────────────────────────
    // SOUND REACTIVE MODE
    // Flash reacts to music / sound via mic
    // ────────────────────────────────────────────
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startSoundReactive() {
        stopAllJobs()
        _state.value = _state.value.copy(isOn = true, mode = FlashMode.SOUND)

        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 4

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            audioRecord?.startRecording()
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isOn = false, mode = FlashMode.OFF,
                errorMessage = "Microphone permission needed"
            )
            return
        }

        soundJob = viewModelScope.launch(Dispatchers.IO) {
            val buffer    = ShortArray(bufferSize / 2)
            var lastFlash = false
            val threshold = 600

            while (true) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) sum += buffer[i] * buffer[i]
                    val rms = Math.sqrt(sum / read).toInt()

                    val shouldFlash = rms > threshold
                    if (shouldFlash != lastFlash) {
                        setFlash(shouldFlash)
                        lastFlash = shouldFlash
                    }
                }
                delay(8)
            }
        }
    }

    fun stopSoundReactive() {
        soundJob?.cancel(); soundJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        setFlash(false)
        _state.value = _state.value.copy(isOn = false, mode = FlashMode.OFF)
    }

    // ────────────────────────────────────────────
    // BREATHING GUIDE MODE
    // Flash guides breathing rhythm
    // ────────────────────────────────────────────
    fun startBreathing(
        inhaleMs: Long = 4000L,
        holdMs:   Long = 1500L,
        exhaleMs: Long = 4000L
    ) {
        stopAllJobs()
        _state.value = _state.value.copy(
            isOn          = true,
            mode          = FlashMode.BREATHING,
            breathPhase   = "Get Ready...",
            breathProgress = 0f
        )
        strobeJob = viewModelScope.launch {
            delay(1500)
            while (true) {
                // ── Inhale ──
                _state.value = _state.value.copy(
                    breathPhase = "Inhale", breathProgress = 0f
                )
                val inhaleSteps = 12
                repeat(inhaleSteps) { i ->
                    _state.value = _state.value.copy(
                        breathProgress = i.toFloat() / inhaleSteps
                    )
                    setFlash(true);  delay(40)
                    setFlash(false); delay((inhaleMs / inhaleSteps) - 40)
                }
                setFlash(true)

                // ── Hold ──
                if (holdMs > 0) {
                    _state.value = _state.value.copy(
                        breathPhase = "Hold", breathProgress = 1f
                    )
                    delay(holdMs)
                }

                // ── Exhale ──
                _state.value = _state.value.copy(
                    breathPhase = "Exhale", breathProgress = 1f
                )
                val exhaleSteps = 12
                repeat(exhaleSteps) { i ->
                    _state.value = _state.value.copy(
                        breathProgress = 1f - (i.toFloat() / exhaleSteps)
                    )
                    val onTime = ((exhaleSteps - i) * 30).toLong().coerceAtLeast(20L)
                    setFlash(true);  delay(onTime)
                    setFlash(false); delay((exhaleMs / exhaleSteps) - onTime)
                }
                setFlash(false)

                // ── Rest ──
                _state.value = _state.value.copy(
                    breathPhase = "Rest", breathProgress = 0f
                )
                delay(1000)
            }
        }
    }

    fun stopBreathing() {
        strobeJob?.cancel(); strobeJob = null
        setFlash(false)
        _state.value = _state.value.copy(
            isOn          = false,
            mode          = FlashMode.OFF,
            breathPhase   = "",
            breathProgress = 0f
        )
    }

    // ────────────────────────────────────────────
    // SCREEN LIGHT
    // ────────────────────────────────────────────
    fun toggleScreenLight() {
        _state.value = _state.value.copy(
            screenLightOn = !_state.value.screenLightOn
        )
    }

    fun setScreenColor(color: Long) {
        _state.value = _state.value.copy(screenLightColor = color)
    }

    // ────────────────────────────────────────────
    // BATTERY
    // ────────────────────────────────────────────
    fun updateBattery() {
        try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            _state.value = _state.value.copy(
                batteryLevel    = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
                batteryCharging = bm.isCharging
            )
        } catch (e: Exception) { }
    }

    // ────────────────────────────────────────────
    // HELPERS
    // ────────────────────────────────────────────
    private suspend fun flash(onMs: Long, offMs: Long) {
        setFlash(true);  delay(onMs)
        setFlash(false); delay(offMs)
    }

    private fun setFlash(on: Boolean) {
        try {
            val id = cameraId ?: return
            cameraManager.setTorchMode(id, on)
        } catch (e: Exception) { }
    }

    private fun stopAllJobs() {
        strobeJob?.cancel(); strobeJob = null
        morseJob?.cancel();  morseJob  = null
        soundJob?.cancel();  soundJob  = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        setFlash(false)
    }

    private fun textToMorse(text: String): String {
        val map = mapOf(
            'A' to ".-",   'B' to "-...", 'C' to "-.-.", 'D' to "-..",
            'E' to ".",    'F' to "..-.", 'G' to "--.",  'H' to "....",
            'I' to "..",   'J' to ".---", 'K' to "-.-",  'L' to ".-..",
            'M' to "--",   'N' to "-.",   'O' to "---",  'P' to ".--.",
            'Q' to "--.-", 'R' to ".-.",  'S' to "...",  'T' to "-",
            'U' to "..-",  'V' to "...-", 'W' to ".--",  'X' to "-..-",
            'Y' to "-.--", 'Z' to "--..",
            '0' to "-----", '1' to ".----", '2' to "..---",
            '3' to "...--", '4' to "....-", '5' to ".....",
            '6' to "-....", '7' to "--...", '8' to "---..",
            '9' to "----.", ' ' to "/"
        )
        return text.mapNotNull { map[it] }
            .joinToString(" ")
    }

    override fun onCleared() {
        super.onCleared()
        stopAllJobs()
        setFlash(false)
    }
}